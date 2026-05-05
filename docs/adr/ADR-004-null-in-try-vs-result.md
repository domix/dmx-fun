---
number: 4
title: "Try<V> allows Success(null); Result.Ok rejects null"
status: Accepted
date: 2026-05-05
---

## Context

`Try.run()` executes a void `CheckedRunnable`. In Java, `Void` has no instances, so the successful return value is necessarily `null`. `Result.Ok`, on the other hand, models an explicit domain value that must always be meaningful.

## Decision

- **`Try.Success`** accepts `null` as a valid value (produced by `Try.run()`).
- **`Result.Ok`** rejects `null` — its constructor throws `NullPointerException`.

## Consequences

**Positive:**
- `Try.run()` can represent void side-effects without requiring a dedicated `Unit` type.
- `Result` guarantees that a successful value is always meaningful.

**Negative / tradeoffs:**
- Deliberate asymmetry between `Try` and `Result` that must be documented.
- `Try.Success(null).toOption()` always returns `None` — surprising behavior if the reason is not known.
- `Try.Success(null).toEither()` throws `NullPointerException` — `Either` does not allow null on either track.

## Alternatives considered

- **Reject null in both:** would require a `Unit` or sentinel `Void` type, adding complexity.
- **Allow null in both:** removes the non-null guarantee from `Result`, which is part of its safety contract.
