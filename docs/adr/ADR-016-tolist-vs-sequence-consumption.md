---
number: 16
title: "Try.toList() collector consumes the entire stream while sequence is fail-fast"
status: Accepted
date: 2026-05-09
---

## Context

`Try` provides two ways to aggregate a stream of `Try<V>` into a single `Try<List<V>>`:
`Try.toList()` (a `Collector`) and `Try.sequence(Stream)` (using a `Gatherer`, see ADR-010).
Both return the first `Failure` encountered, but their stream-consumption semantics differ.

## Decision

`Try.toList()` (and `Try.partitioningBy()`) always consume every element of the stream.
They cannot short-circuit because the `Collector` contract — `Collector.of(supplier, accumulator, combiner, finisher)` — calls the accumulator for every element; there is no mechanism to signal early termination.

`Try.sequence` uses a `Gatherer` (finalized in Java 24, JEP 485) and stops processing at
the first `Failure` by returning `false` from the integrator. Both behaviors are intentional
and explicitly documented.

## Consequences

**Positive:**

- `toList()` and `partitioningBy()` support parallel streams: their combiner merges partial
  results from multiple threads, which `Gatherer.ofSequential` cannot do.
- `sequence` gives true fail-fast behavior with minimal resource usage when streams are large
  or when individual `Try` computations are expensive.
- The behavioral difference is explicit in the API rather than hidden in an implementation
  detail: `toList()` Javadoc states "this Collector is *not* fail-fast".

**Negative / tradeoffs:**

- Two APIs with the same apparent purpose but different consumption semantics can surprise
  callers who assume all "first-failure" APIs short-circuit.
- Callers must consciously choose between them based on whether early termination matters.

## Alternatives considered

- **Make `toList()` fail-fast:** not possible within the `Collector` contract; the accumulator
  is called for every element regardless. Switching to a `Gatherer` would lose parallel-stream
  compatibility and `Stream.collect` integration.
- **Remove `toList()` and provide only `sequence`:** loses parallel-stream support and the
  standard `Stream.collect(...)` call site that many Java developers prefer.
