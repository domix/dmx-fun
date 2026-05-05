---
number: 2
title: "JPMS from day one"
status: Accepted
date: 2026-05-05
---

## Context

The library could be published as a plain jar (classpath) or adopt the Java Platform Module System from the first release.

## Decision

Adopt **JPMS** from the start with a `module-info.java` that declares explicit exports.

## Consequences

**Positive:**
- The public API is deliberate: only `dmx.fun` is exported.
- Strong encapsulation of internals without relying on naming conventions.
- Compatibility with environments that require modules (`jlink`, `jpackage`).

**Negative / tradeoffs:**
- Additional build complexity (classpath vs module-path in tests, `--add-opens`, `jpmsTest` configuration).
- Some testing and reflection frameworks require extra configuration.

## Alternatives considered

- **Plain jar:** simpler, wider immediate compatibility, but no strong encapsulation.
- **Automatic module:** module name inferred from the jar, no control over exports — discarded as a transitional approach.
