---
number: 8
title: "jspecify (@NullMarked, @Nullable) for null safety"
status: Accepted
date: 2026-05-05
---

## Context

The library needs a null-safety annotation system compatible with static analysis tools (IntelliJ, Error Prone, NullAway) and aligned with the JDK roadmap.

## Decision

Adopt **jspecify** (`org.jspecify:jspecify`) with `@NullMarked` at the module level and `@Nullable` in the few places where null is valid.

## Consequences

**Positive:**
- `@NullMarked` at the module level establishes that everything is non-null by default — minimal annotation noise.
- jspecify is the emerging standard backed by the OpenJDK team and adopted by Guava, Error Prone, and others.
- Lightweight compile/runtime dependency (`jspecify` is an `api` dependency so that users see the annotations).

**Negative / tradeoffs:**
- jspecify 1.0 is relatively recent; tooling support is still maturing.
- Not all IDEs process `@NullMarked` the same way as JSR-305.

## Alternatives considered

- **JSR-305 (Findbugs/SpotBugs `@NonNull`):** widely supported but no longer actively maintained.
- **Checker Framework:** more powerful but heavier and with a steeper learning curve.
- **JetBrains annotations:** excellent IntelliJ support but vendor-coupled.
