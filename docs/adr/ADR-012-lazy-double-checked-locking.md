---
number: 12
title: "Lazy<T> with volatile Try<T> and double-checked locking"
status: Accepted
date: 2026-05-05
---

## Context

`Lazy<T>` must guarantee that the supplier is executed exactly once even under concurrent access, without blocking reads after the first evaluation.

## Decision

`Lazy<T>` uses a **`volatile @Nullable Try<T> state`** field (null = not yet evaluated) with **double-checked locking**: the first read is lock-free; if null, a `synchronized` block is entered and the value is checked again before evaluating.

## Consequences

**Positive:**
- Concurrent reads after the first evaluation are lock-free (only a single volatile read).
- `volatile` guarantees write visibility without needing `synchronized` on the fast path.
- The state is modelled as `Try<T>` to capture supplier exceptions on the first evaluation.

**Negative / tradeoffs:**
- Double-checked locking with `volatile` is correct in Java 5+, but the pattern can be confusing for maintainers unfamiliar with it.
- If the supplier throws, the exception is stored and rethrown on every subsequent call — behaviour that must be documented.

## Alternatives considered

- **`AtomicReference<Try<T>>`:** correct and more explicit, but `compareAndSet` can cause redundant evaluations under contention.
- **`synchronized` on `get()`:** correct but blocks every read, even after the initial evaluation.
- **`Supplier` with a holder class:** the initialization-on-demand holder pattern does not capture supplier exceptions.
