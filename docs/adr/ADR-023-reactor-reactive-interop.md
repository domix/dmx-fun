---
number: 23
title: "Project Reactor as the reactive interop target"
status: Accepted
date: 2026-06-29
---

## Context

dmx-fun needs a reactive interop module so teams can carry `Option`, `Result`, and
`Try` across reactive boundaries without re-implementing conversions in every
service. Following [ADR-022](./ADR-022-integration-modules.md), the interop lives in
an optional module with the reactive library declared `compileOnly` — but we must
still choose which reactive library that first module targets.

## Decision

The first reactive interop module is **`fun-reactor`**, built on **Project Reactor**.
`reactor-core` is declared `compileOnly`; users bring their own version. The module
provides explicit conversions between `Mono` and dmx-fun's `Option`, `Result`, and
`Try`, with documented empty/error semantics.

Reactor is chosen because:

- **It is the reactive runtime of the JVM backend stack.** Spring WebFlux — the most
  common reactive backend on the JVM, and the subject of the planned
  `fun-spring-webflux` module — is Reactor underneath, as are R2DBC and much of the
  surrounding ecosystem. Targeting Reactor puts the interop directly on the grain of
  the frameworks users are most likely to pair it with, so `fun-reactor` composes
  with them with no impedance.
- **Its context propagation fits dmx-fun's observability story.** Reactor's
  `ContextView` carries cross-cutting metadata (tracing IDs, security, tenancy)
  through a pipeline without `ThreadLocal`s, surviving thread hops and operator
  boundaries. This is exactly the mechanism `fun-tracing` and `fun-observation` need
  to remain correct in reactive code, and it works through our conversions unchanged.
- **Its cardinality lives in the type.** `Mono` (0–1) versus `Flux` (0–N) mirrors
  dmx-fun's own preference for modeling shape in the type — the same instinct behind
  `Option` versus `List`. A `Mono<Result<V, E>>` reads as "at most one outcome,"
  which is precisely the contract our conversions express.
- **It is a single, actively maintained, standardized target.** Concentrating on one
  reactive runtime keeps the module's test, compatibility-matrix, and documentation
  surface small while covering the large majority of JVM reactive backends.

## Consequences

**Positive:**
- `fun-reactor` composes directly with WebFlux and R2DBC — the frameworks users are
  most likely to reach for — so the interop is useful out of the box.
- Context propagation and `Mono`/`Flux` cardinality come for free, reinforcing
  dmx-fun's explicit-types philosophy at the reactive boundary.
- One reactive runtime to test and document keeps the module surface small.

**Negative / tradeoffs:**
- The reactive story is tied to Reactor; its relevance tracks Reactor's continued
  ecosystem dominance.
- Teams whose stack is built on a different reactive runtime get no first-class
  adapter and must bridge through `Publisher` or hand-written conversions.

## Alternatives considered

- **RxJava:** the other mainstream JVM reactive library, strongest on Android. Not
  chosen as the first target because it sits off the grain of the JVM backend stack
  this library serves (Spring/WebFlux/R2DBC), it has no first-class context-propagation
  mechanism comparable to Reactor's `ContextView`, and it spreads cardinality across
  `Single`/`Maybe`/`Observable`/`Flowable` rather than the `Mono`/`Flux` split that
  matches dmx-fun's modeling. A `fun-rxjava` module could be added later under the same
  peer-dependency strategy if demand appears; it is not built speculatively (YAGNI).
- **Support multiple reactive runtimes from the start:** multiplies the
  test/compatibility/documentation surface for demand that has not materialized.
  Deferred — the peer-dependency pattern makes adding another runtime later cheap.
- **No reactive interop module:** leaves every team re-implementing `Mono`↔`Result`
  conversions with inconsistent empty/error semantics — the exact duplication the
  integration-module strategy exists to remove.
