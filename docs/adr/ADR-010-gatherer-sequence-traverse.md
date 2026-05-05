---
number: 10
title: "Gatherer for sequence/traverse with true short-circuit"
status: Accepted
date: 2026-05-05
---

## Context

`sequence` and `traverse` must implement fail-fast semantics: upon encountering the first `Err`/`Failure` they must stop processing elements. The `Stream` API does not allow short-circuit with a `Collector`.

## Decision

Implement `sequence`/`traverse` using **`Stream.gather(Gatherer.ofSequential(...))`** (Java 22+) instead of `reduce`, `forEach`, or `Collector`.

## Consequences

**Positive:**
- True short-circuit: the stream is closed upon encountering the first error without consuming the rest.
- The code is declarative and composable within the stream pipeline.
- `Gatherer` is Java's official abstraction for stateful transformations in streams.

**Negative / tradeoffs:**
- Requires Java 22+ (preview feature in 22, finalized in 24).
- Collector-based operations (`toList()`, `partitioningBy()`) continue consuming the entire stream — different behavior explicitly documented.

## Alternatives considered

- **Manual `reduce`:** does not allow short-circuit in sequential streams without using exceptions as control flow.
- **`forEach` with mutable state:** works but is not idiomatic with streams and breaks composability.
- **Custom `Collector`:** cannot short-circuit by design of the `Collector` API.
