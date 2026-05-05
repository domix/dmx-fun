---
number: 6
title: "Validated (error accumulation) vs Result (fail-fast)"
status: Accepted
date: 2026-05-05
---

## Context

The library includes two types for modelling operations that can fail: `Result<V, E>` and `Validated<E, A>`. A new user may be confused about when to use each one.

## Decision

- **`Validated<E, A>`**: applicative-style error accumulation — all errors are collected before returning.
- **`Result<V, E>`**: fail-fast — the first error stops the chain.

## Consequences

**Positive:**
- Each type has clear semantics and does not try to do both things.
- `Validated` is ideal for form/DTO validation where the user needs to see all errors at once.
- `Result` is ideal for business pipelines where an intermediate error makes continuing pointless.

**Negative / tradeoffs:**
- Two types that "look" similar can confuse users coming from other libraries (e.g., Vavr only has `Validation`).
- The user must learn when to use each one.

## Alternatives considered

- **A single type with a configurable mode:** increases API complexity.
- **Only `Result`:** does not model accumulative validation well without losing the first error.
