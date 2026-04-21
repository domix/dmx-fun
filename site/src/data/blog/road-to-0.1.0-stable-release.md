---
title: "Road to 0.1.0: Production-Ready dmx-fun Is Near"
description: "0.1.0 will be our first stable production-ready release. Here is the current status of 0.0.14, the concrete roadmap for 0.0.15 based on active GitHub issues, and what comes next."
pubDate: 2026-04-20
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "Roadmap", "0.0.14", "0.0.15", "0.1.0", "Spring", "Micrometer", "Resilience4j"]
image: "https://images.unsplash.com/photo-1545830571-53a9a0967c88?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Emile Guillemot"
    authorUrl: "https://unsplash.com/@emilegt"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/via-de-tren-vacia-durante-el-dia-a333ixMpr68"
---

`0.1.0` is not a distant goal. It is the next logical step — and we are almost there.

`dmx-fun` started as a focused library of functional types for Java: `Option`, `Result`, `Try`, and friends. Over the last several months the library has grown into a full ecosystem: Spring integration, Spring Boot autoconfiguration, Micrometer metrics, Resilience4J adapters, AssertJ assertions, Jackson serializers. The core type system has deepened significantly with `Guard`, `Accumulator`, `Resource`, `NonEmptyMap`, `NonEmptySet`, `Lazy`, and `Tuple` variants.

`0.1.0` will be the release where all of that lands in a stable, production-ready form with explicit API contracts, verified interoperability, and documentation that matches what is actually shipped.

This post shares:

1. The current state of `0.0.14` — the release in final stretch right now
2. What `0.0.15` will deliver — the hardening milestone before the stable release
3. A brief look at the `0.1.0` vision and what it means concretely

---

## Current status: `0.0.14` — almost done

At the time of writing (April 20, 2026), milestone `0.0.14` is essentially complete.

**44 issues closed. 1 issue open. Due April 22, 2026.**

The scale of `0.0.14` is worth pausing on. In a single milestone, the following landed:

| Area | What shipped |
|---|---|
| **New modules** | `fun-spring` · `fun-spring-boot` · `fun-micrometer` · `fun-resilience4j` |
| **Core types** | `Guard<T>` · `Accumulator<E,A>` · `Resource<T>` · `NonEmptyMap<K,V>` · `NonEmptySet<T>` |
| **Core enhancements** | `Try.timeout(Duration)` · `Validated.combine3/combine4` · `Option.zipWith/flatZip` · `NonEmptyList.first()/last()` via `SequencedCollection` |
| **Collectors** | `Results`, `Options`, `Tries` façades · `Result.groupingBy` · `Option.toOptional` · `NonEmptyList.collector` |
| **AssertJ** | Fluent assertions for `Resource`, `Guard`, and `Accumulator` |
| **Build** | JPMS test-patching extracted into the shared `dmx-fun.java-module` convention plugin |
| **Samples** | `spring-boot-sample` — end-to-end Spring Boot demo module |

The one remaining open item is:

- [#268](https://github.com/domix/dmx-fun/issues/268) — `feat(spring-boot)`: Spring MVC return value handler for `Result`, `Validated`, and `Try`

This handler will allow controllers to return dmx-fun types directly, with Spring MVC resolving them transparently. Once that lands, `0.0.14` closes.

---

## Roadmap for `0.0.15` — the hardening milestone

Milestone `0.0.15` is due **May 2, 2026**. The theme is simple: earn the right to call `0.1.0` production-ready.

This is not a feature race. It is a quality wave — cleanup, contract verification, documentation, and transaction correctness — across every module that shipped in `0.0.14`.

### Codebase cleanup, module by module

Every module gets a dedicated cleanup pass. No loose ends before the stable release.

- [#277](https://github.com/domix/dmx-fun/issues/277) `lib` cleanup
- [#278](https://github.com/domix/dmx-fun/issues/278) `assertj` cleanup
- [#279](https://github.com/domix/dmx-fun/issues/279) `jackson` cleanup
- [#280](https://github.com/domix/dmx-fun/issues/280) `spring` cleanup
- [#281](https://github.com/domix/dmx-fun/issues/281) `spring-boot` cleanup
- [#282](https://github.com/domix/dmx-fun/issues/282) `resilience4j` cleanup
- [#283](https://github.com/domix/dmx-fun/issues/283) `micrometer` cleanup

### Transaction correctness

Production code uses transactions. We need to be certain that `fun-spring` and `fun-spring-boot` behave correctly — not just that they compile.

- [#284](https://github.com/domix/dmx-fun/issues/284) — verify `fun-spring` transactional behavior matches the Spring `@Transactional` contract: commit on success, rollback on failure, correct rollback rule precedence
- [#285](https://github.com/domix/dmx-fun/issues/285) — strengthen declarative transaction coverage in `fun-spring-boot`: proxy boundaries, auto-configuration behavior, nested transactional boundaries

### Documentation baseline

- [#276](https://github.com/domix/dmx-fun/issues/276) — repository-wide Javadoc cleanup and warning elimination across all modules

### Near-term feature track

These issues are active and will influence the `0.0.15`–`0.1.0` window:

- [#262](https://github.com/domix/dmx-fun/issues/262) — `fun-jakarta-validation`: bridge between Jakarta Bean Validation and `Validated`
- [#253](https://github.com/domix/dmx-fun/issues/253) — `fun-http`: functional HTTP client integration
- [#233](https://github.com/domix/dmx-fun/issues/233) — `fun-jakarta`: broader Jakarta EE integration
- [#127](https://github.com/domix/dmx-fun/issues/127), [#128](https://github.com/domix/dmx-fun/issues/128), [#129](https://github.com/domix/dmx-fun/issues/129) — Quarkus integration track

---

## The `0.1.0` vision — what stable actually means

`0.1.0` is already taking shape on GitHub. The milestone holds two major workstreams.

### Interoperability audit across all core types

This is the most systematic work planned for `0.1.0`. Every core type will go through a structured audit to verify — and expand if needed — its interoperability with the rest of the library and with the Java standard library.

- [#287](https://github.com/domix/dmx-fun/issues/287) `Accumulator` interop audit
- [#288](https://github.com/domix/dmx-fun/issues/288) `Either` interop audit
- [#289](https://github.com/domix/dmx-fun/issues/289) `Guard` interop audit
- [#290](https://github.com/domix/dmx-fun/issues/290) `Lazy` interop audit
- [#291](https://github.com/domix/dmx-fun/issues/291) `NonEmptyList` interop audit
- [#292](https://github.com/domix/dmx-fun/issues/292) `NonEmptyMap` interop audit
- [#293](https://github.com/domix/dmx-fun/issues/293) `NonEmptySet` interop audit
- [#294](https://github.com/domix/dmx-fun/issues/294) `Option` interop audit
- [#295](https://github.com/domix/dmx-fun/issues/295) `Resource` interop audit
- [#296](https://github.com/domix/dmx-fun/issues/296) `Result` interop audit
- [#297](https://github.com/domix/dmx-fun/issues/297) `Try` interop audit
- [#298](https://github.com/domix/dmx-fun/issues/298) `Validated` interop audit

Each audit covers: composition with other dmx-fun types, integration with `Optional`, `Stream`, collections, `CompletableFuture`, and Java functional interfaces. Round-trip behavior is validated and intentional asymmetries are documented. This is what makes API contracts trustworthy.

### Production-like reference application

- [#286](https://github.com/domix/dmx-fun/issues/286) — `spring-boot-sample`: a production-like reference app built with Flyway, Postgres, JPA, `Result`/`Try`, Micrometer metrics, and Resilience4J patterns all working together

This reference app will serve as the definitive proof that the entire ecosystem composes correctly in a real-world stack — not just in isolation.

### What `0.1.0` delivers

When `0.1.0` ships, the commitment is:

- **Stable API** — no breaking changes to core types or module contracts without a major version bump
- **Verified interoperability** — every type's composition surface has been audited and documented
- **Proven in a production-like stack** — the reference app demonstrates the full integration end to end
- **Complete Javadoc** — no warnings, no missing documentation
- **Transaction guarantees** — `fun-spring` and `fun-spring-boot` match what Spring developers expect

---

## Where we are, in plain terms

`0.0.14` took the library from a solid core to a full ecosystem. That work is landing now.

`0.0.15` will clean and harden every piece of it.

And then `0.1.0` is the release where we stop saying "this is pre-release" and start saying "this is ready."

We are not guessing about the path. The issues are open, the milestones are dated, and the work is in progress.

`0.1.0` is close.
