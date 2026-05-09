---
number: 15
title: "Option<T> as a custom type instead of java.util.Optional"
status: Accepted
date: 2026-05-09
---

## Context

Java provides `java.util.Optional<T>` as a standard absent-value container. dmx-fun must decide whether to use it directly or provide a custom `Option<T>` type.

## Decision

Provide a custom `Option<T>` sealed interface with `Some<T>` and `None<T>` record variants, rather than wrapping or extending `java.util.Optional`.

## Consequences

**Positive:**

- **Exhaustive pattern matching.** `Optional` is a `final` class; `Option` is a `sealed interface`. The compiler enforces that both `Some` and `None` branches are handled in a switch expression — no wildcard arm needed, unlike `Optional.isPresent()` checks.
- **Type-graph integration.** `Option<T>` participates in the library's type graph with first-class conversion methods: `toResult()`, `toTry()`, `toEither()`. The reverse conversions are symmetric: `Option.fromOptional()`, `Option.fromResult()`, `Option.fromTry()`.
- **Consistent pipeline API.** `map`, `flatMap`, `filter`, `zip`, `fold`, and `sequence` all return `Option<T>`, keeping pipelines within the library's type system.
- **Singleton absence value.** `Option.none()` returns a pre-allocated, cast-safe singleton (`None.INSTANCE`), avoiding repeated allocations of the absent case.

**Negative / tradeoffs:**

- Users must convert at JDK API boundaries: `Option.fromOptional(opt)` / `option.toOptional()`.
- Two absent-value types in the same codebase can be confusing without clear conventions; the rule is: use `Option<T>` within library and domain code, convert to `Optional` only at JDK or framework API boundaries.

## Alternatives considered

- **Use `java.util.Optional` directly:** no custom type needed, but `Optional` is a `final` class — not a sealed interface — so exhaustive compile-time matching is impossible. It also has no integration with `Result`, `Try`, or `Either`, and its `get()` method throws `NoSuchElementException` without statically enforcing a prior `isPresent()` check.
- **Wrap `Optional` in `Option<T>`:** adds indirection without benefit; the inner `Optional` is redundant once the sealed interface is in place.
