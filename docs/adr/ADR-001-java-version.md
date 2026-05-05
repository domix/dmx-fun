---
number: 1
title: "Java 25 as the minimum required version"
status: Accepted
date: 2026-05-05
---

## Context

At project inception, a minimum Java version was chosen. This decision affects which language and JDK features can be used, and which audience can adopt the library.

## Decision

Require **Java 25** as the minimum version.

## Consequences

**Positive:**
- Access to `Gatherer` (Java 22+) to implement `sequence`/`traverse` with true short-circuit.
- Stable virtual threads (Java 21+) used in `Try.withTimeout`.
- Exhaustive pattern matching in `switch` over sealed interfaces.
- Record patterns, unnamed variables (`_`), and other post-17 features.

**Negative / tradeoffs:**
- Limits the audience to projects already migrated to Java 21+.
- Enterprise projects with slow upgrade cycles cannot adopt the library immediately.

## Alternatives considered

- **Java 21 (LTS):** would have covered most features but excludes `Gatherer`.
- **Java 17 (LTS):** wider adoption, but lacks full sealed interfaces and advanced pattern matching.
