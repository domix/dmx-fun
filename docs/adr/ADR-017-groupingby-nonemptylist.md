---
number: 17
title: "Result.groupingBy returns Map<K, NonEmptyList<V>> instead of Map<K, List<V>>"
status: Accepted
date: 2026-05-09
---

## Context

`Result.groupingBy(classifier)` is a `Collector<V, ?, Map<K, NonEmptyList<V>>>` that groups
any stream of values by a key derived from each element. The standard library equivalent —
`Collectors.groupingBy` — returns `Map<K, List<V>>`. The value type for each group must be
chosen between `List<V>` (matching the JDK convention) and a type that encodes the invariant
that every group is non-empty.

## Decision

Each group in the returned map is typed as `NonEmptyList<V>` instead of `List<V>`.
A group produced by `groupingBy` always contains at least one element by construction:
if a key appears in the map, at least one stream element was classified under it.
`NonEmptyList<V>` makes this invariant explicit and enforced at the type level.

The downstream variant — `groupingBy(classifier, downstream)` — accepts a
`Function<NonEmptyList<V>, R>` that transforms each group after grouping.
The returned maps are **insertion-order** (backed by `LinkedHashMap`) and **unmodifiable**
(wrapped with `Collections.unmodifiableMap`).

## Consequences

**Positive:**

- The type system enforces a real invariant: callers never need to guard against empty groups.
  Every `NonEmptyList<V>` in the map is guaranteed to have at least one element.
- Consistent with the library's philosophy of making illegal states unrepresentable.
- Callers can call `NonEmptyList.head()`, `NonEmptyList.tail()`, and other non-empty-aware
  operations without defensive checks.

**Negative / tradeoffs:**

- Users who need a plain `List<V>` must call `nonEmptyList.toList()`.
- `NonEmptyList<V>` is a library type; code passing the map to JDK APIs expecting `List<V>`
  must convert, though `NonEmptyList` implements `SequencedCollection<T>`.

## Alternatives considered

- **`List<V>`:** matches `Collectors.groupingBy` signature but hides the non-empty invariant;
  callers must defensively check `isEmpty()` even though it can never be true after grouping.
- **`Collection<V>`:** even less precise; discards ordering guarantees provided by the
  encounter-order-preserving `LinkedHashMap` accumulator.
