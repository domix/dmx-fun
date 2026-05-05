---
number: 11
title: "Guard<T> as a @FunctionalInterface with default methods"
status: Accepted
date: 2026-05-05
---

## Context

`Guard<T>` needs to be composable (`and`, `or`, `negate`) while also allowing concise definition via lambdas or method references.

## Decision

`Guard<T>` is a **`@FunctionalInterface`** whose single abstract method is `check(T value)`. All composition logic is implemented as **`default methods`** on the interface.

## Consequences

**Positive:**
- The user can define a guard with a lambda: `Guard<String> notBlank = s -> Validated.valid(s)`.
- Composition (`and`, `or`, `negate`, `contramap`) is available without inheritance.
- No state: each guard is a pure function.

**Negative / tradeoffs:**
- `default methods` cannot be `final` — any implementor could override the composition logic (though that would be a misuse of the API).
- The interface grows in surface area as more composition operators are added.

## Alternatives considered

- **Abstract class `AbstractGuard<T>`:** allows `final` on composition methods but prevents lambda usage and multiple inheritance.
- **Concrete class `Guard<T>` with a `Function` field:** more rigid, not extensible as an interface.
