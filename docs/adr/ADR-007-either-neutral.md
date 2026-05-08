---
number: 7
title: "Either as a neutral type with no directional bias"
status: Accepted
date: 2026-05-05
---

## Context

In Haskell and Scala, `Either` is right-biased: `map`/`flatMap` operate on the right side. dmx-fun includes `Either<L, R>` but must decide whether to adopt that bias and what semantic contract to assign to each side.

## Decision

`Either<L, R>` in dmx-fun is **semantically neutral**: neither side carries error or success connotation — both `Left` and `Right` are equally legitimate outcomes. Operations (`map`, `mapLeft`, `flatMap`) follow the right-bias convention from Haskell/Scala for familiarity, but `Either` is not intended as a primary error-handling monad.

When one side represents an error, use `Result<V, E>` instead — it is semantically opinionated and offers richer recovery operations (`recover`, `recoverWith`, `flatMapError`).

## Consequences

**Positive:**
- Avoids semantic ambiguity: if `L` represents an error, use `Result`; if `R` is success, use `Result` or `Try`.
- Right-biased operations (`map`, `flatMap`) are familiar to FP practitioners without implying error semantics.
- `Either` remains useful as a return type in APIs that need two symmetric cases with no error/success connotation.

**Negative / tradeoffs:**
- Users coming from Scala/Haskell may be surprised that `Either` is not the primary monad for error handling.
- Some conversions require an extra step (`toResult()`, `toTry()`).

## Alternatives considered

- **Right-biased with full error-handling semantics:** introduces redundancy with `Result` and adds confusion about when to use each type.
- **Fully neutral (no `map`/`flatMap`):** would prevent integration into functional pipelines — discarded because it makes `Either` too limited as a transport type.
