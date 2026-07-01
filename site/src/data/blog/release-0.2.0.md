---
title: "dmx-fun 0.2.0 — Reactive, and One Year Old"
description: "0.2.0 is dmx-fun's first-anniversary release: two new reactive modules — fun-reactor (Project Reactor interop) and fun-spring-webflux (Spring WebFlux adapters) — plus a year of 17 releases building a functional programming ecosystem for Java backends."
pubDate: 2026-07-01
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "0.2.0", "Anniversary", "Reactive", "Project Reactor", "Spring WebFlux", "Functional Programming", "Result", "Option", "Try", "Validated"]
image: "https://images.pexels.com/photos/12966794/pexels-photo-12966794.jpeg"
imageCredit:
    author: "Marek Piwnicki"
    authorUrl: "https://www.pexels.com/@marek-piwnicki-3907296/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/es-es/foto/hombre-silueta-cascada-sin-camisa-12966794/"
---

`dmx-fun 0.2.0` is out — and this one is special.

**It is our first-anniversary release.** The first commit landed on June 30th, 2025. One year
later, dmx-fun has grown from a handful of core types into a coordinated ecosystem of functional
programming modules for Java backends — shipped across **17 releases** in that first year, from
`0.0.1` all the way to the one you are reading about now.

Thank you to everyone who filed an issue, opened a pull request, or put dmx-fun into a real
backend. This release is a celebration of that year — and a big step forward.

---

## The headline: dmx-fun goes reactive

Until now, dmx-fun's railway-oriented style lived in synchronous code. `0.2.0` extends it to the
reactive world with **two new modules**:

- **`fun-reactor`** — Project Reactor interop: convert `Option`, `Result`, and `Try` to and from
  `Mono` and `Flux`, and keep composing on the Result/Try track inside reactive pipelines.
- **`fun-spring-webflux`** — Spring WebFlux adapters: map your domain outcomes straight to HTTP
  responses, for both functional endpoints and annotation controllers.

Same idea you already know — failures, absence, and validation stay explicit in the type system —
now flowing through `Mono` and `Flux`.

---

## `fun-reactor` — interop and railway operators

`fun-reactor` bridges dmx-fun and Project Reactor in both directions, and adds railway operators
so a `Mono<Result<V, E>>` stays on the happy path without unwrapping:

```java
Mono<Result<User, ApiError>> user =
    ReactorResult.fromMono(userRepository.findById(id), ApiError::fromThrowable)
        .flatMapOk(u -> ReactorResult.fromMono(enrich(u), ApiError::fromThrowable))
        .mapOk(User::redactSecrets);
```

For collections, `sequence` aggregates a `Flux<Result<V, E>>` fail-fast, while `collectValidated`
accumulates **every** error instead of stopping at the first — the reactive counterpart of
`Validated`'s error accumulation.

---

## `fun-spring-webflux` — outcomes to HTTP

`fun-spring-webflux` maps `Option`, `Result`, `Try`, and `Validated` to WebFlux responses with
documented, overridable HTTP conventions. Use `WebfluxFun` for **functional endpoints**:

```java
RouterFunction<ServerResponse> routes() {
    return RouterFunctions.route()
        .GET("/users/{id}", request ->
            WebfluxFun.fromResult(
                userService.findById(request.pathVariable("id")),   // Mono<Result<User, ApiError>>
                error -> ServerResponse.status(error.status()).bodyValue(error.detail())))
        .build();
}
```

…or `WebfluxEntity` for **annotation controllers** (`@RestController`), returning a
`Mono<ResponseEntity<…>>`:

```java
@GetMapping("/users/{id}")
Mono<ResponseEntity<User>> user(@PathVariable String id) {
    return WebfluxEntity.fromOption(userService.find(id));   // Some -> 200, None/empty -> 404
}
```

The module goes well beyond a single mapping:

- **Streaming and aggregation** — collect a `Flux<Result>` fail-fast, accumulate every error, or
  stream a `Flux<V>` element-by-element as NDJSON/SSE.
- **Customizable success responses** — a `SuccessHttpMapper` lets you return `201 Created` with a
  `Location` header, `202 Accepted`, caching headers, and so on.
- **RFC 7807 problem responses** — `WebfluxProblem` renders failures as standard
  `application/problem+json` `ProblemDetail` documents.
- **Spring Boot auto-configuration** — with `fun-spring-boot` on the classpath, a ready-made
  `ProblemDetail` mapper bean is auto-configured, configurable and conditional.

Both `fun-reactor` and `fun-spring-webflux` declare Reactor and Spring as `compileOnly` — you
bring your own versions. `spring-webflux` is tested in CI against Spring Framework 6.0.x through
7.0.x on every pull request.

---

## Also in 0.2.0

- **`Result.mapCatching(CheckedFunction)`** — a `map` variant that captures thrown checked
  exceptions as an `Err`, so throwing mappers no longer force you out of the Result track.
- **`NonEmptyList.reduce(...)` and `joinToString(...)`** — ergonomic folds over a guaranteed
  non-empty list.
- **Dependency refresh** — Spring Boot 4.1.0, Jackson 2.22.0, JUnit 6.1.1, Micrometer 1.17.0,
  Micrometer Tracing 1.7.0, Quarkus 3.37.0, Hibernate Validator 9.1.1.Final, and more.

See the [full changelog](https://github.com/domix/dmx-fun/blob/main/CHANGELOG.md) for the complete list.

---

## A year in numbers

- **17 releases** — `0.0.1` → `0.2.0`
- **1 stable milestone** — `0.1.0`, our first production-ready release
- **13 modules** — core types plus serialization, observability, resilience, HTTP, framework, and
  now reactive integrations
- **1 interoperability audit** across every core type

From four types in a single artifact to a reactive-ready ecosystem — in twelve months.

---

## Getting started

```kotlin
// Gradle (Kotlin DSL) — reactive stack
implementation(platform("codes.domix:fun-bom:0.2.0"))
implementation("codes.domix:fun")
implementation("codes.domix:fun-reactor")
implementation("codes.domix:fun-spring-webflux")
```

```xml
<!-- Maven -->
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-spring-webflux</artifactId>
    <version>0.2.0</version>
</dependency>
```

The [Reactor guide](/dmx-fun/guide/reactor) and the
[Spring WebFlux guide](/dmx-fun/guide/spring-webflux) walk through both new modules with
real-world examples. Full Javadoc is at [/dmx-fun/javadoc/](/dmx-fun/javadoc/index.html).

Here's to the next year.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
