---
number: 5
title: "Guard<T> accumulates errors as a fixed NonEmptyList<String>"
status: Accepted
date: 2026-05-05
---

## Context

`Guard<T>` needs an error type to represent validation failures. The natural alternative would be to make it generic (`Guard<T, E>`), but that drastically complicates composition.

## Decision

The error type of `Guard<T>` is always **`NonEmptyList<String>`** — human-readable error messages as strings.

## Consequences

**Positive:**
- Composition (`and`, `or`, `negate`) does not require an external `Monoid<E>` — list concatenation is the only merge operator needed.
- Validation lambdas are simple: `Guard.of(predicate, "error message")`.
- Direct interop with `Validated<NonEmptyList<String>, T>`.

**Negative / tradeoffs:**
- Not suitable when the error must be a typed domain object (e.g., a `ValidationError` enum).
- In that case the user must work directly with `Validated<E, A>`.

## Alternatives considered

- **Generic `Guard<T, E>`:** requires the user to provide a `BinaryOperator<E>` for merging in `and`/`or` — more complex API.
- **`Guard<T>` with `List<String>`:** allows an empty list, which is semantically invalid for an error.
