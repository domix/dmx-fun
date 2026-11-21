---
title: "dmx-fun 0.3.0 — Two Jackson Lines, One JSON"
description: "0.3.0 ships fun-jackson3 for Spring Boot 4 next to fun-jackson2 (renamed from fun-jackson), JSON for NonEmptySet and NonEmptyMap, type ids for every dmx-fun value, new Guard combinators, a strict fatal border for Try, and a ten-chapter tutorial."
pubDate: 2026-11-21
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "0.3.0", "Jackson", "Jackson 3", "Spring Boot 4", "Guard", "Try", "Tutorial", "Functional Programming"]
---

`dmx-fun 0.3.0` is out.

Spring Boot 4 and Spring Framework 7 default to **Jackson 3**, which lives under a new package,
`tools.jackson`. Jackson 2 is not going anywhere: Boot 3 applications are everywhere, and both
lines are maintained upstream in parallel. So dmx-fun now does the same: **one module per Jackson
major, both maintained, both producing the same JSON.**

---

## ⚠ One breaking change: `fun-jackson` is now `fun-jackson2`

Every Jackson module now carries its major version in the name, so no unsuffixed artifact points
at the older line. If you use `fun-jackson` today, the migration is three find-and-replace edits:

| | 0.2.x | 0.3.0 |
|---|---|---|
| Artifact | `codes.domix:fun-jackson` | `codes.domix:fun-jackson2` |
| JPMS module / package | `dmx.fun.jackson` | `dmx.fun.jackson2` |
| Spring Boot opt-out property | `dmx.fun.jackson.enabled` | `dmx.fun.jackson2.enabled` |

The JSON shapes do not change. `fun-jackson:0.2.0` stays on Maven Central. The reasoning behind
the split is recorded in [ADR-024](/dmx-fun/adr/adr-024-parallel-peer-major-lines/), accepted with
this release.

---

## `fun-jackson3` — zero configuration on Spring Boot 4

With `fun-spring-boot` on the classpath, a Boot 4 application needs nothing else: the new
`DmxFunJackson3AutoConfiguration` registers the module, and controllers can return `Result`,
`Option` and `NonEmptyList` directly.

```kotlin
implementation(platform("codes.domix:fun-bom:0.3.0"))
implementation("codes.domix:fun-spring-boot")
implementation("codes.domix:fun-jackson3")
```

Outside Spring, register it like any Jackson 3 module:

```java
ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
```

Both modules can be on the same classpath, which is exactly what you want during a Boot 3 to
Boot 4 migration: a service on each side writes and reads the same documents. CI tests
`fun-jackson2` against Jackson 2.14 to 2.23 and `fun-jackson3` against 3.0 to 3.3 on every pull
request.

The `spring-boot-sample` shows the happy path: no Jackson exclusions, no hand-written
`ObjectMapper`, no deprecated converter.

---

## Every core type in JSON, including under polymorphic typing

- **`NonEmptySet` and `NonEmptyMap`** now serialize in both lines: a set is a JSON array and a map a
  JSON object, head first. An empty array or object, or a `null` element, key or value, fails with
  a clear databind exception instead of an empty "non-empty" collection.
- **Type ids** work for every dmx-fun value. Under `activateDefaultTyping` or `@JsonTypeInfo`, the
  id names the dmx-fun type (`dmx.fun.Result`, never `Result$Ok`), and payloads are written through
  the serializer of their declared type, the way Jackson treats `List`, `Set` and `Map`. A
  `Result<Object, E>` holding a record round-trips; a `Result<Item, E>` stays plain.

The [Jackson guide](/dmx-fun/guide/jackson) has the full table of shapes and type-id behaviour.

---

## `Guard` — rules as values, composed

`Guard` gains the combinators that make a set of validation rules read like a specification:

```java
static final Guard<String> NOT_BLANK = Guard.of(s -> !s.isBlank(), "must not be blank");
static final Guard<String> AGE_AT_LEAST_16 =
    Guard.ofCatching(s -> Integer.parseInt(s) >= 16, "must be a number, 16 or more");

static final Guard<EnrollmentForm> FORM = Guard.allOf(
    NOT_BLANK.contramap(EnrollmentForm::studentId, "studentId"),
    AGE_AT_LEAST_16.contramap(EnrollmentForm::age, "age")
);

Validated<NonEmptyList<String>, EnrollmentForm> result = FORM.check(form);
```

`allOf` and `anyOf` check in one pass, `ofCatching` turns a throwing predicate into a failed rule,
`contramap(mapper, field)` points a rule at a field and prefixes its messages, and `mapMessages` and
`narrow` cover the rest.

---

## `Try` — a strict border for fatal errors

`Try.of` captures every `Throwable`, including the ones you should never handle. `rethrowFatal()`
makes a border strict:

```java
Try.of(() -> repository.fetch(id))
   .rethrowFatal()
   .toResult(StoreUnavailable::new);
```

Fatal failures (`OutOfMemoryError`, `InterruptedException` with the interrupt flag restored, and
friends) propagate; domain failures stay on the track. `NonFatal` holds the one definition of
"fatal" that the rest of the library uses.

---

## A tutorial that rewrites one method

The new [tutorial](/dmx-fun/guide/tutorial) takes one `enroll` method from the Java most of us
write, four failure modes and none of them in the signature, to a signature you can trust, in ten
short chapters. Every snippet comes from `samples/enrollment-tutorial`, a module that compiles and
runs its tests on every pull request. It is also the backbone of the talk
[From exceptions to signatures you can trust](/dmx-fun/talks/).

---

## Also in 0.3.0

- **`fun-assertj`** — `NonEmptyListAssert`, `NonEmptySetAssert` and `NonEmptyMapAssert`.
- **Dependency refresh** — Spring Framework 7.0.10, Spring Boot 4.1.2, Quarkus 3.40.1,
  Jackson 2.23.0 and Jackson 3.3.0.
- **Security** — every open Dependabot alert resolved across the build, plugin and test classpaths.

See the [full changelog](https://github.com/domix/dmx-fun/blob/main/CHANGELOG.md) for the complete
list.

---

## Getting started

```kotlin
// Gradle (Kotlin DSL), Spring Boot 4
implementation(platform("codes.domix:fun-bom:0.3.0"))
implementation("codes.domix:fun")
implementation("codes.domix:fun-spring-boot")
implementation("codes.domix:fun-jackson3")   // or fun-jackson2 on Spring Boot 3
```

```xml
<!-- Maven -->
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-jackson3</artifactId>
    <version>0.3.0</version>
</dependency>
```

Full Javadoc is at [/dmx-fun/javadoc/](/dmx-fun/javadoc/index.html).

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
