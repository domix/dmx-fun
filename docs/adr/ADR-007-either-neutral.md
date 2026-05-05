---
number: 7
title: "Either as a neutral type with no directional bias"
status: Accepted
date: 2026-05-05
---

## Context

In Haskell and Scala, `Either` is right-biased: `map`/`flatMap` operate on the right side. dmx-fun includes `Either<L, R>` but must decide whether to adopt that bias or remain neutral.

## Decision

`Either<L, R>` in dmx-fun is **neutral** — it does not expose `map`/`flatMap`. It serves as an interoperability and transport type, not as a functional monad.

## Consequences

**Positive:**
- Avoids semantic ambiguity: if `L` represents an error, use `Result`; if `R` is success, use `Result` or `Try`.
- Forces the user to convert to a type with explicit semantics (`toResult()`, `toTry()`) in order to operate functionally.
- `Either` remains useful as a return type in APIs that need two symmetric cases with no error/success connotation.

**Negative / tradeoffs:**
- Users coming from Scala/Haskell expect `map`/`flatMap` on `Either`.
- Some conversions require an extra step.

## Alternatives considered

- **Right-biased with `map`/`flatMap`:** introduces redundancy with `Result` and adds confusion about when to use each type.
