---
title: "dmx-fun 0.1.0 — Production Ready"
description: "0.1.0 is the first stable, production-ready release of dmx-fun: a battle-tested ecosystem of functional programming modules for Java backends, with first-class Spring Boot and Quarkus support."
pubDate: 2026-05-10
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "0.1.0", "Stable", "Spring Boot", "Quarkus", "Functional Programming", "Either", "Option", "Result", "Try"]
image: "https://images.pexels.com/photos/12966794/pexels-photo-12966794.jpeg"
imageCredit:
    author: "Marek Piwnicki"
    authorUrl: "https://www.pexels.com/@marek-piwnicki-3907296/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/es-es/foto/hombre-silueta-cascada-sin-camisa-12966794/"
---

`dmx-fun 0.1.0` is out — and it is the release we have been building toward since day one.

This is not a version bump. **0.1.0 is the first stable, production-ready release of dmx-fun.**
The API is stable. The ecosystem is complete. The contracts have been audited. If you have been
waiting for the right moment to adopt dmx-fun in a production backend, this is it.

---

## What does "production ready" mean here?

When we say production ready, we mean it precisely:

- **Stable API** — no breaking changes to core types or module contracts without a major version bump.
- **Verified interoperability** — every core type has been audited for composition correctness across the entire library and with the Java standard library.
- **Battle-tested** — the modules have been exercised through compatibility matrices, real database integration tests, and end-to-end reference applications.
- **Complete Javadoc** — no warnings, no gaps.
- **Transaction guarantees** — `fun-spring` and `fun-quarkus` behave exactly as Spring Boot and Quarkus developers expect from `@Transactional`, but with functional return types.

---

## A rich ecosystem, not just a library

`dmx-fun` is no longer a single artifact. It is a coordinated ecosystem of modules designed to
work together — and to integrate cleanly with the frameworks and infrastructure you already use.

![dmx-fun module ecosystem](/dmx-fun/dmx-fun-modules.png)

The modules are organized into logical groups:

| Group | Modules |
|---|---|
| **Core** | `fun-lib` · `fun-assertj` |
| **Serialization** | `fun-jackson` · `fun-jakarta-jaxb` · `fun-jakarta-validation` |
| **Observability** | `fun-micrometer` · `fun-tracing` · `fun-observation` |
| **Frameworks** | `fun-spring` · `fun-spring-boot` · `fun-quarkus` |
| **Protocols** | `fun-http` |
| **Resilience** | `fun-resilience4j` |
| **Samples** | `samples` · `spring-boot-sample` |

Every module is independently versioned and published to Maven Central. Use the BOM to keep
them all aligned without managing individual versions:

```kotlin
// Gradle (Kotlin DSL)
implementation(platform("codes.domix:fun-bom:0.1.0"))
implementation("codes.domix:fun-lib")
implementation("codes.domix:fun-spring-boot")
implementation("codes.domix:fun-resilience4j")
```

```xml
<!-- Maven -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>codes.domix</groupId>
      <artifactId>fun-bom</artifactId>
      <version>0.1.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## Spring Boot integration

`fun-spring-boot` provides zero-configuration autoconfiguration for all dmx-fun types inside
a Spring Boot application.

Drop in the dependency and you get:

- **`TxResult` / `TxTry` / `TxValidated`** registered as beans — execute functional suppliers
  inside managed transactions that roll back on failure, not just on thrown exceptions.
- **`@TransactionalResult` / `@TransactionalTry` / `@TransactionalValidated`** — annotation-driven
  equivalents backed by AOP, comparable to `@Transactional` but aware of functional failure states.
- **Jackson autoconfiguration** — `DmxFunModule` is applied automatically to the `ObjectMapper`,
  so `Option`, `Result`, `Try`, `Either`, and `Validated` serialize and deserialize out of the box.
- **Spring MVC return value handlers** — controllers can return `Option<T>`, `Result<V, E>`,
  `Try<V>`, and `Validated<E, A>` directly. `Option.some(v)` becomes HTTP 200; `Option.none()`
  becomes HTTP 404. `Result.ok(v)` becomes HTTP 200; `Result.err(e)` becomes HTTP 500.
- **`DmxTracing`** — auto-registered when Micrometer Tracing is on the classpath. Zero configuration.

```java
@RestController
class OrderController {

    private final OrderService orders;

    @GetMapping("/orders/{id}")
    Option<Order> get(@PathVariable String id) {
        return orders.findById(id);   // 200 with body, or 404 — no if/else needed
    }

    @PostMapping("/orders")
    Result<Order, OrderError> create(@RequestBody OrderRequest req) {
        return orders.create(req);    // 200 with order, or 500 with typed error
    }
}
```

Spring Boot 3.3.x through 4.0.x are all tested in CI on every pull request.

---

## Quarkus integration

`fun-quarkus` brings the same transactional guarantees to Quarkus applications.

The core problem it solves is the **silent commit problem**: Quarkus's `@Transactional` commits
even when a method returns a `Result.err(...)` or `Try.failure(...)`, because those are not
exceptions. `fun-quarkus` fixes this:

```java
@ApplicationScoped
class PaymentService {

    @Inject TxResult txResult;

    Result<Payment, PaymentError> charge(ChargeRequest req) {
        return txResult.execute(() -> {
            // if this returns Err, the transaction rolls back
            return paymentRepository.persist(req);
        });
    }
}
```

Or use the annotation-driven style:

```java
@ApplicationScoped
class PaymentService {

    @TransactionalResult
    Result<Payment, PaymentError> charge(ChargeRequest req) {
        return paymentRepository.persist(req); // Err → rollback
    }
}
```

All six JTA propagation types are supported: `REQUIRED`, `REQUIRES_NEW`, `MANDATORY`,
`SUPPORTS`, `NOT_SUPPORTED`, and `NEVER`. The module ships as a proper Quarkus extension
with `META-INF/quarkus-extension.properties` — no explicit deployment dependency needed.

Tested against Quarkus 3.11.3, 3.21.4, 3.31.4, and 3.35.1 via CI compatibility matrix,
with integration tests running against a real PostgreSQL instance via Quarkus Dev Services.

---

## Interoperability audit: the work behind the stability

The defining work of 0.1.0 was a systematic interoperability audit across all twelve core
types. Every type was verified — and expanded where needed — to compose correctly with the
rest of the library and with the Java standard library.

Here is a sample of what the audit delivered:

**`Either`** gained `toOptional()`, `stream()`, `toTry(leftMapper)`, `match(onLeft, onRight)`,
`flatMapLeft(Function)`, symmetric fallback extractors (`getRightOrElse`, `getLeftOrElse`, etc.),
and `streamLeft()`.

**`Option`** gained `fromEither(Either<L,R>)` and `toFuture()` — `Some(v)` becomes a
completed future; `None` becomes a failed future with `NoSuchElementException`.

**`Result`** and **`Try`** each gained `fromEither` static factories and `toOptional()` conversions.

**`Lazy`** gained `toOptional()`, `toEither()`, and `toEither(errorMapper)` — safe conversions
that force evaluation and capture exceptions as `Left`.

**`Resource`** gained `useAsEither(body, onError)` — the `Either`-integrated counterpart of
`useAsResult`, eliminating awkward `Try<Either<E, R>>` nesting.

**`NonEmptyList`**, **`NonEmptyMap`**, and **`NonEmptySet`** all gained `fromOptional` factories,
stream-based collectors, and `sequence*` methods for fail-fast sequencing over elements.

---

## Resilience, observability, and HTTP

Production backends need more than types. They need circuit breakers, retries, metrics, tracing,
and HTTP clients that do not leak exceptions.

`fun-resilience4j` wraps Retry, CircuitBreaker, RateLimiter, and Bulkhead — each exposing
`executeTry(supplier)`, `executeResult(supplier)`, and `executeResultTyped(supplier)` so
every policy outcome becomes a typed value rather than a thrown exception.

`fun-micrometer` wraps a `MeterRegistry` and records counters, timers, and failure tags
automatically around `Try`- and `Result`-returning suppliers.

`fun-tracing` and `fun-observation` wrap Micrometer Tracing and the Observation API respectively,
tagging failure outcomes on the active span or observation.

`fun-http` wraps the JDK `HttpClient` and returns `Result<T, HttpError>` — network failures,
non-2xx responses, and deserialization errors are all first-class typed values.

---

## What is in the box

A complete summary of the core types available in `fun-lib`:

| Type | Purpose |
|---|---|
| `Option<T>` | Presence or absence — an explicit alternative to `null` |
| `Result<V, E>` | Success or typed error — fail-fast, non-exception-based |
| `Try<V>` | Success or `Throwable` — bridges exception-based APIs |
| `Either<L, R>` | Disjoint union — two typed branches, no preferred side |
| `Validated<E, A>` | Accumulating validation — collects all errors, not just the first |
| `Lazy<T>` | Memoized deferred evaluation |
| `Guard<T>` | Composable null-safe predicate blocks |
| `Accumulator<E, A>` | Value with an accumulated side-channel |
| `Resource<T>` | Functional managed resource with guaranteed release |
| `NonEmptyList<T>` | Compile-time non-empty list |
| `NonEmptyMap<K, V>` | Compile-time non-empty map |
| `NonEmptySet<T>` | Compile-time non-empty set |
| `Tuple2/3/4` | Immutable typed product types |

---

## Getting started

```kotlin
// Gradle (Kotlin DSL) — core library only
implementation("codes.domix:fun-lib:0.1.0")

// Spring Boot project — use the BOM
implementation(platform("codes.domix:fun-bom:0.1.0"))
implementation("codes.domix:fun-lib")
implementation("codes.domix:fun-spring-boot")

// Quarkus project
implementation(platform("codes.domix:fun-bom:0.1.0"))
implementation("codes.domix:fun-lib")
implementation("codes.domix:fun-quarkus")
```

Full Javadoc is available at [/dmx-fun/javadoc/](/dmx-fun/javadoc/index.html).

The `spring-boot-sample` module in the repository is a production-like reference application
with Flyway, PostgreSQL, JPA, Micrometer metrics, and Resilience4J patterns all working together —
the best place to see the full ecosystem in action.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
