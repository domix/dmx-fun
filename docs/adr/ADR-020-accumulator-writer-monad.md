---
number: 20
title: "Accumulator<E, A> (Writer monad) — rationale for inclusion"
status: Accepted
date: 2026-05-09
---

## Context

Some computations need to produce a value alongside a side-channel accumulation
(audit log, metrics events, warning messages) without resorting to mutable state
or global logging. The standard Java approach uses mutable lists or thread-local
logging, both of which are invisible at API boundaries and lost on virtual threads
or async boundaries.

## Decision

Include `Accumulator<E, A>` as an immutable record pairing a computed value `A`
with a side-channel accumulation `E`. This is the Writer monad pattern from
functional programming.

The type parameter order is `<E, A>` where `E` is the accumulation type and `A`
is the value type. The record components are `(@Nullable A value, E accumulated)`.

**Accumulation is always explicit and composable through the API surface:**

- `Accumulator.of(value, accumulated)` — create with a value and an initial entry.
- `Accumulator.pure(value, empty)` — create with a value and an identity
  accumulation. The identity must be supplied by the caller because Java does not
  have type classes; common choices are `List.of()`, `0`, or `""`.
- `Accumulator.tell(accumulated)` — record a side-channel entry without producing
  a meaningful value; the `value()` component is `null` (`Void`).
- `flatMap(f, merge)` — chain a next step: applies `f` to the current value to
  get the next `Accumulator`, then merges both accumulations with `merge`
  (`BinaryOperator<E>`). This is the primary composition combinator.
- `combine(other, merge, f)` — combine two independently computed accumulators:
  merges their accumulations and applies `f` to both values.
- `sequence(list, merge, empty)` — fold a `List<Accumulator<E, A>>` into a single
  `Accumulator<E, List<A>>` by merging all accumulations left-to-right.
- `map(f)` — transform the value without touching the accumulation.
- `mapAccumulated(f)` — transform the accumulation without touching the value.

The key invariant: **accumulation always continues**. Unlike `Result` or `Try`,
there is no failure path — every step contributes to both the value and the
accumulation. This makes `Accumulator` the natural choice for tracing what
happened, not just whether it succeeded.

`Accumulator` integrates with `NonEmptyList<E>` via `NonEmptyList::concat` as the
`merge` function, guaranteeing that at least one log entry is always present.
For `Option`, `Try`, `Result`, and `Either`, the static `liftOption`,
`liftTry`, `liftResult`, and `liftEither` helpers record a log entry regardless
of which branch was taken while preserving the wrapped value as the accumulator's
value.

## Consequences

**Positive:**

- Side-channel data (logs, warnings, events) remains a pure value alongside the
  result — no global mutable state, no thread-local context, and no loss on
  virtual-thread or async boundaries.
- Composable: `flatMap` chains steps and merges accumulations; `combine` merges
  parallel computations; both are explicit and testable.
- The side-channel is visible in the function return type — callers can see that
  a function produces log entries without reading its implementation.
- Interoperates with the rest of the library: `toOption()`, `toResult()`,
  `toEither()`, and `toTuple2()` bridge to other types at pipeline boundaries.

**Negative / tradeoffs:**

- Less familiar than traditional logging — teams must adopt the pattern
  deliberately, and the merge function must be threaded through every `flatMap`
  call.
- Does not handle failures: a step that can fail should return
  `Accumulator<E, Result<A, Err>>` (or use `liftResult`) rather than trying to
  express both concerns inside `Accumulator` alone.
- `tell()` produces a `null` value, which requires callers to use `hasValue()`
  or `flatMap` rather than `map` — a sharp edge when the origin of an accumulator
  is not statically known.

## Alternatives considered

- **Mutable list passed as parameter:** works but breaks referential transparency
  and complicates testing and concurrent use.
- **MDC / thread-local logging:** invisible in the type system; lost on virtual
  threads or async boundaries.
- **`Result<A, List<Warning>>`:** conflates non-fatal warnings with fatal errors;
  `Accumulator` separates the two tracks — the value is always present, and
  warnings are always accumulated, regardless of success or failure.
- **Returning a custom pair type per use case:** each domain would need its own
  ad-hoc wrapper; `Accumulator<E, A>` is the general solution that works for any
  `E` and any merge strategy.
