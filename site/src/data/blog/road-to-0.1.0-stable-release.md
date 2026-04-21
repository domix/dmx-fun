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

`0.1.0` is our target for the first **stable, production-ready** release of `dmx-fun`.
We are close, and the path is clear.

This post shares:

1. The current status of `0.0.14`
2. What is planned for `0.0.15` (based on active GitHub issues)
3. A brief look at what comes right after, on the way to `0.1.0`

---

## Current status: `0.0.14`

At the time of writing (April 20, 2026), milestone `0.0.14` is almost complete:

- **44 issues closed**
- **1 issue open**
- milestone due date: **April 22, 2026**

The remaining open item is:

- [#268](https://github.com/domix/dmx-fun/issues/268) — Spring Boot MVC return value handler for `Result`, `Validated`, and `Try`

In practice, this means `0.0.14` is in final stretch, and we are already focusing execution energy on `0.0.15`.

---

## Roadmap for `0.0.15`

Milestone `0.0.15` (due **May 2, 2026**) is where we are hardening the codebase and tightening contracts before `0.1.0`.

### 1) Codebase quality cleanup by module

A full cleanup wave is already planned:

- [#277](https://github.com/domix/dmx-fun/issues/277) `lib` cleanup
- [#278](https://github.com/domix/dmx-fun/issues/278) `assertj` cleanup
- [#279](https://github.com/domix/dmx-fun/issues/279) `jackson` cleanup
- [#280](https://github.com/domix/dmx-fun/issues/280) `spring` cleanup
- [#281](https://github.com/domix/dmx-fun/issues/281) `spring-boot` cleanup
- [#282](https://github.com/domix/dmx-fun/issues/282) `resilience4j` cleanup
- [#283](https://github.com/domix/dmx-fun/issues/283) `micrometer` cleanup

### 2) Transaction correctness and contract alignment

Before claiming production-readiness, transaction behavior must be explicit and tested:

- [#284](https://github.com/domix/dmx-fun/issues/284) verify `spring` transactional behavior vs Spring `@Transactional` contract
- [#285](https://github.com/domix/dmx-fun/issues/285) strengthen declarative transaction coverage in `spring-boot`

### 3) Documentation quality baseline

- [#276](https://github.com/domix/dmx-fun/issues/276) repository-wide Javadoc cleanup and warning elimination

### 4) Feature track that may land in or after `0.0.15`

These issues are active and influence the near-term roadmap:

- [#262](https://github.com/domix/dmx-fun/issues/262) `fun-jakarta-validation`
- [#253](https://github.com/domix/dmx-fun/issues/253) `fun-http`
- [#233](https://github.com/domix/dmx-fun/issues/233) `fun-jakarta`
- [#127](https://github.com/domix/dmx-fun/issues/127), [#128](https://github.com/domix/dmx-fun/issues/128), [#129](https://github.com/domix/dmx-fun/issues/129) Quarkus integration track

---

## Very brief look ahead: `0.1.0`

`0.1.0` is planned as the **stability release**:

- stable API expectations
- stronger interoperability guarantees across core types
- stronger docs, examples, and production-oriented guidance

You can already see this direction in the `0.1.0` milestone and active planning issues.

---

## Closing

We are not “thinking about” a production-ready release anymore; we are actively executing toward it.

`0.0.14` is nearly done.
`0.0.15` is focused on quality, contracts, and reliability.
And that puts us on a direct path to `0.1.0` very soon.

We are almost there.
