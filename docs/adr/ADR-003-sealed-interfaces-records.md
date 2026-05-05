---
number: 3
title: "Sealed interfaces + records as ADT representation"
status: Accepted
date: 2026-05-05
---

## Context

Algebraic types (`Result`, `Try`, `Option`, `Either`, `Validated`) need to represent exactly two variants without allowing external extension.

## Decision

Implement each type as a **`sealed interface`** with **`record`** variants (`Ok`/`Err`, `Success`/`Failure`, `Some`/`None`, etc.).

## Consequences

**Positive:**
- The compiler verifies exhaustiveness in `switch` — no risk of missing a variant.
- Records eliminate boilerplate (constructor, `equals`, `hashCode`, `toString`).
- Pattern matching (`case Ok<V, E>(V v) ->`) extracts values in a single expression.
- No third variant can be created externally — the contract is unbreakable.

**Negative / tradeoffs:**
- Requires Java 17+ for sealed, Java 21+ for full pattern matching in switch.
- Records do not support state inheritance — all logic must live on the interface as `default` methods.

## Alternatives considered

- **Abstract class + subclasses:** no exhaustiveness guarantee, more boilerplate.
- **Enum with generic payload:** impossible with generic types in Java.
