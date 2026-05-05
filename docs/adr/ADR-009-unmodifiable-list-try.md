---
number: 9
title: "unmodifiableList instead of List.copyOf in Try"
status: Accepted
date: 2026-05-05
---

## Context

`Try.Partition` and the `Try` collectors return immutable lists of successful values. `Try.run()` produces `Success(null)`, and those nulls can appear in the result lists of `sequence`/`partitioningBy`.

## Decision

`Try` uses **`Collections.unmodifiableList(new ArrayList<>(...))`** instead of **`List.copyOf`** for lists of successful values.

## Consequences

**Positive:**
- Preserves `null` elements in the list (produced by `Try.run()`).
- `sequence(List.of(Try.run(() -> {})))` returns `Success([null])` without throwing NPE.

**Negative / tradeoffs:**
- `unmodifiableList` allows `null` internally; `List.copyOf` is stricter and clearer about nullability.
- Behavioural difference with `Result`, which does use `List.copyOf` (because `Ok` guarantees non-null).

## Alternatives considered

- **`List.copyOf` in both:** breaks compatibility with `Try.run()` and `Try<Void>`.
- **Prohibit null in `Success`:** removes the ability to represent void side-effects without a `Unit` type.
