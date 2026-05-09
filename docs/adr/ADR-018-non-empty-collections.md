---
number: 18
title: "NonEmptyList<T>, NonEmptySet<T>, NonEmptyMap<K,V> as structural guarantee types"
status: Accepted
date: 2026-05-09
---

## Context

Many domain operations require collections with at least one element: validation
errors, grouped results, non-empty inputs to functions. The standard JDK types
`List<T>`, `Set<T>`, and `Map<K,V>` allow empty instances and carry no non-emptiness
invariant in the type system, so the check is scattered across every caller.

## Decision

Provide `NonEmptyList<T>`, `NonEmptySet<T>`, and `NonEmptyMap<K,V>` as dedicated
types that encode the non-emptiness constraint in the static type. Each type
stores a guaranteed `head` element plus an optionally-empty `tail`
(an unmodifiable sub-collection), making it structurally impossible to construct an
instance without at least one element. There is no emptiness check at a single
entry point; the constraint is a consequence of the representation.

Smart constructors that accept potentially empty sources (`fromList`, `fromSet`,
`fromMap`) return `Option<NonEmptyX>` rather than throwing, shifting the emptiness
handling to the type system — callers must handle the `None` case explicitly.

The three types choose their JDK interface based on what can be honoured without
violating the invariant or the interface contract:

- **`NonEmptyList<T>`** implements `java.util.SequencedCollection<T>` (Java 21+),
  not `List<T>`. This provides `getFirst()`, `getLast()`, and `reversed()` as total
  functions (they never throw `NoSuchElementException`), while all mutating methods
  throw `UnsupportedOperationException`. Implementing `List<T>` would expose
  `remove(int)` and `clear()`, which cannot be implemented without breaking the
  non-emptiness invariant.
- **`NonEmptySet<T>`** implements `Iterable<T>` only. Backed by `LinkedHashSet`,
  insertion order is preserved. `toSet()` provides an unmodifiable `java.util.Set`
  for standard API interop. Implementing `Set<T>` would expose `remove` and `clear`.
- **`NonEmptyMap<K,V>`** is a standalone `final` class. Backed by `LinkedHashMap`,
  insertion order is preserved. `toMap()` provides an unmodifiable `java.util.Map`.
  Implementing `Map<K,V>` would expose `put`, `remove`, and `clear`.

`Validated` and `Guard` use `NonEmptyList<String>` for error accumulation:
a `Validated.Invalid` always carries at least one error, and `Guard` always returns
at least one message when a check fails. This eliminates defensive `isEmpty()` checks
in callers.

`Result.groupingBy` returns `Map<K, NonEmptyList<V>>` rather than
`Map<K, List<V>>` — every group produced by grouping always contains at least one
element by construction. This decision is documented in
[ADR-017](https://domix.github.io/dmx-fun/adr/adr-017-groupingby-nonemptylist/).

## Consequences

**Positive:**

- The invariant is enforced once at construction, not scattered across callers.
- Method signatures that require non-empty collections express it in the type
  (`NonEmptyList<E>` vs `List<E>`); callers never need to guard against empty
  instances.
- `NonEmptyList.head()`, `getFirst()`, and `getLast()` are total functions — they
  never throw.
- Validation pipelines using `Validated<NonEmptyList<E>, A>` are guaranteed to carry
  at least one error in the `Invalid` case, removing the need for defensive
  `isEmpty()` checks on error lists.
- Consistent with the library's philosophy of making illegal states
  unrepresentable.

**Negative / tradeoffs:**

- Three additional public types; callers must convert to JDK collections at API
  boundaries via `toList()`, `toSet()`, or `toMap()`.
- `NonEmptyList<T>` does not implement `List<T>`, so it cannot be passed to APIs
  that expect `List<T>` without conversion. It does implement `SequencedCollection<T>`
  (and transitively `Collection<T>` and `Iterable<T>`), which satisfies many use sites.
- `fromList` / `fromSet` / `fromMap` return `Option<NonEmptyX>`, requiring callers to
  handle the `None` case when bridging from standard JDK types.

## Alternatives considered

- **Runtime assertion in callers:** enforces the invariant only where remembered;
  fails late and is not visible at API boundaries.
- **Implementing `List<T>` / `Set<T>` / `Map<K,V>` directly:** would expose mutating
  methods (`add`, `remove`, `clear`) that can only respond with
  `UnsupportedOperationException`, violating the interface contract and giving callers
  a false sense of compatibility.
- **Vavr's `NonEmptyList`:** external dependency; heavier than needed for this
  focused use case.
- **Guava `ImmutableList` with a size check:** doesn't encode non-emptiness in the
  type; still requires a runtime check at every call site.
