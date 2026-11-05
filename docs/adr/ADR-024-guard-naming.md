---
number: 24
title: "Guard naming: name()/named() with decorator-preserving propagation"
status: Accepted
date: 2026-07-26
---

## Context

When a guard fails, its accumulated error messages say *which rule* was violated, but nothing
identifies *which guard* was being applied. Logs and metrics need that identity once guards
are composed, passed around, or registered in validation pipelines ([issue #507](https://github.com/domix/dmx-fun/issues/507)).

`Guard<T>` is a `@FunctionalInterface` ([ADR-011](https://domix.github.io/dmx-fun/adr/adr-011-guard-functional-interface/)), so it
cannot carry a field; any identity mechanism must fit through default methods with `check` as
the single abstract method.

## Decision

- **`default String name()`** returns the guard's identity, defaulting to the public constant
  **`Guard.ANONYMOUS`** (`"anonymous"`); **`default boolean isNamed()`** distinguishes named
  guards without comparing against the literal.
- **`default Guard<T> named(String)`** returns a wrapper that delegates `check` verbatim and
  overrides `name()`. It rejects `null`, blank names, and the `ANONYMOUS` sentinel itself — a
  nameless name would defeat the feature, and a guard literally named `"anonymous"` would be
  indistinguishable from an unnamed one for `isNamed()` and every propagation decision.
- **Name propagation follows the shape of the operation:**
  - *Unary decorators preserve the name* — `withMessage`, `mapMessages`, `negate`,
    `contramap`, and `Guard.narrow` decorate the same logical guard, so a named field guard
    keeps its identity through a `contramap` lift.
  - *Composition operators produce anonymous guards* — `and`, `or`, `andThen`, `allOf`,
    `anyOf` combine several guards into a new one; the composite is named explicitly, after
    composing. No automatic name-combining (`"a+b"`) is attempted.
- The name is identity metadata only: it never alters `check` results or error messages, and
  stays orthogonal to `withMessage` and to `contramap`'s `fieldName` prefix (which rewrite
  messages; `name()` never does). The one observability surface where the name does reach a
  message is `checkToTry(value)`'s default `IllegalArgumentException`, prefixed with
  `guard 'name': ` for named guards — a `Try.failure` often propagates far from the call site
  that knew which guard ran.

## Consequences

**Positive:**
- Failures become traceable: `log.warn("guard '{}' failed", guard.name())`, and
  `fun-assertj`'s `GuardAssert` renders `Expected Guard 'email' to accept ...`.
- Observability integrations can tag metrics by guard name and use `isNamed()` to skip or
  bucket unnamed guards without hardcoding the sentinel.
- Lambdas keep working unchanged; `check` remains the single abstract method.

**Negative / tradeoffs:**
- This refines ADR-011's "no state" consequence: it now reads as **no state affecting
  `check`** — a named guard carries identity metadata. Overriding the `name()` default via
  `named(...)` is the sanctioned pattern; overriding composition logic remains a misuse.
- `name()` is a common accessor name: an external class implementing `Guard` that already
  declares `String name()` with different semantics silently overrides the default and leaks
  that value into logs/metrics (documented on `name()`).
- Each preserved-name decoration adds one thin delegation wrapper.

## Alternatives considered

- **`Optional<String> name()`:** more honest about absence, but every logging call site pays
  an unwrap, and the sentinel plus `isNamed()` covers the same need with plainer call sites.
- **Automatic name combination in composition (`"notBlank+minLength3"`):** complexity nobody
  asked for; composite names are better chosen by the author.
- **Propagating names through composition from the left operand:** makes `a.and(b)` report
  `a`'s name for a guard that is no longer `a` — misleading in exactly the logs the feature
  exists for.
- **A `Guard.of(predicate, message, name)` overload:** redundant; `.named()` already covers
  creation-time naming.

## Related

- Amends [ADR-011 — Guard&lt;T&gt; as a @FunctionalInterface with default methods](https://domix.github.io/dmx-fun/adr/adr-011-guard-functional-interface/).
