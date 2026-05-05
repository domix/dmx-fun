---
title: "dmx-fun 0.0.15 Released"
description: "Version 0.0.15 brings first-class Quarkus transaction support with full JTA TxType propagation, five new integration modules (fun-http, fun-jakarta-jaxb, fun-jakarta-validation, fun-tracing, fun-observation), a Bill of Materials, CycloneDX SBOMs, and critical correctness fixes across fun-quarkus, fun-jackson, and fun-assertj."
pubDate: 2026-05-04
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "Quarkus", "JTA", "HTTP", "JAXB", "Validation", "Tracing", "Observation", "BOM", "SBOM"]
image: "https://images.pexels.com/photos/2313669/pexels-photo-2313669.jpeg"
imageCredit:
    author: "Nelson Ribeiro"
    authorUrl: "https://www.pexels.com/@nelson-ribeiro-973316/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/es-es/foto/silhoette-de-persona-liberando-linterna-japonesa-de-papel-rojo-2313669/"
---

Version **0.0.15** is the Quarkus integration release — and a lot more. It ships
first-class Quarkus transaction support with full JTA propagation, five new
integration modules that extend `dmx-fun` to HTTP, serialization, and observability,
a published Bill of Materials, CycloneDX SBOMs for every artifact, and a set of
correctness fixes that close subtle edge cases found through expanded test coverage.

Here is everything that changed.

---

## fun-quarkus — first-class Quarkus transaction support

Quarkus's `@Transactional` (backed by Narayana JTA) rolls back only when an unchecked
exception escapes the annotated method. `Result` and `Try` capture failure as return
values — so no exception escapes, and the transaction commits even when the operation
failed, silently persisting partial writes.

`fun-quarkus` fixes this with two complementary styles.

> **Early-stage support:** The `fun-quarkus` module is in its early stages. The API is
> functional and tested, but you may encounter rough edges in real-world usage. Feedback
> and bug reports are very welcome on [GitHub](https://github.com/domix/dmx-fun/issues).

### Programmatic: TxResult and TxTry

```java
@ApplicationScoped
public class OrderService {
    @Inject TxResult tx;
    @Inject OrderRepository repo;

    public Result<Order, String> place(PlaceOrderCommand cmd) {
        return tx.execute(() -> {
            if (cmd.quantity() <= 0) return Result.err("quantity must be positive");
            Order order = repo.save(new Order(cmd));
            return Result.ok(order);
        });
    }
}
```

`TxResult.execute` commits when the result is `Ok` and rolls back when it is `Err`.
`TxTry` follows the same contract for `Try<V>`.

Both also provide `executeNew()` for **REQUIRES\_NEW** semantics — the active
transaction is suspended, an independent one starts, and the original resumes after
the action finishes regardless of the outcome:

```java
// Audit log must commit even if the outer business transaction rolls back
Result<AuditEntry, String> audit = auditTx.executeNew(() ->
    auditRepo.record(event)
);
```

### Declarative: @TransactionalResult and @TransactionalTry

```java
@ApplicationScoped
public class InventoryService {

    @TransactionalResult
    public Result<Item, String> reserve(long itemId, int qty) {
        return repo.findById(itemId)
            .map(item -> item.reserve(qty))
            .toResult(() -> "item not found");
        // Err return → rollback; unchecked exception → rollback + rethrow
    }
}
```

Both annotations accept a `Transactional.TxType` attribute (default `REQUIRED`),
covering all six JTA propagation types:

| `TxType` | Existing transaction present | No active transaction |
|---|---|---|
| `REQUIRED` *(default)* | Joins; marks rollback-only on error | Begins a new transaction |
| `REQUIRES_NEW` | Suspends outer, starts fresh, resumes outer after | Starts a fresh transaction |
| `MANDATORY` | Joins; marks rollback-only on error | Throws `TransactionalException` |
| `SUPPORTS` | Joins; marks rollback-only on error | Runs without a transaction |
| `NOT_SUPPORTED` | Suspends outer, runs without one, resumes outer after | Runs without a transaction |
| `NEVER` | Throws `TransactionalException` | Runs without a transaction |

```java
@ApplicationScoped
public class AuditService {

    @TransactionalResult(REQUIRES_NEW)
    public Result<AuditEntry, String> record(AuditEvent event) { ... }

    @TransactionalResult(NOT_SUPPORTED)
    public Result<Report, String> generateReport(ReportRequest req) { ... }

    @TransactionalResult(MANDATORY)
    public Result<Void, String> postProcess(Order order) { ... }
}
```

Both annotation types route to a single interceptor class (`TransactionalDmxInterceptor`)
via the shared `@DmxTransactionalBinding` meta-annotation (CDI §2.7.1.1), so there is
no runtime overhead of maintaining two separate interceptor registrations.

`quarkus-arc` and `quarkus-narayana-jta` are declared `compileOnly`; your own Quarkus
version is not forced. The module ships `META-INF/quarkus-extension.properties` so
Quarkus build tools automatically add `fun-quarkus-deployment` to the augmentation
classpath — no explicit deployment dependency needed in your project.

Tested against Quarkus **3.11.3, 3.21.4, 3.31.4, and 3.35.1** in CI via a
compatibility matrix. Integration tests run against a real PostgreSQL instance through
Quarkus Dev Services (Testcontainers).

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun-quarkus:0.0.15")
```

---

## fun-http — HttpClient wrapper returning Result

`fun-http` wraps the JDK `java.net.http.HttpClient` and returns `Result<T, HttpError>`
instead of throwing. All HTTP and network failures become typed `Err` values with no
unchecked exception leaking into your application code.

`HttpError` is a sealed type with variants for network failures, non-2xx responses, and
deserialization errors, making exhaustive handling straightforward with `switch` or
`fold`.

```kotlin
implementation("codes.domix:fun-http:0.0.15")
```

---

## fun-jakarta-jaxb — Jakarta JSON-B adapters

`fun-jakarta-jaxb` provides Jakarta JSON-B adapters for `Option`, `Result`, `Try`,
`Either`, and `Validated`. Register them once with `JsonbConfig` for full round-trip
serialization support across all dmx-fun types.

Jakarta JSON-B 3.x is declared `compileOnly`.

```kotlin
implementation("codes.domix:fun-jakarta-jaxb:0.0.15")
```

---

## fun-jakarta-validation — Jakarta Validation integration

`fun-jakarta-validation` integrates Jakarta Validation 3.x / 4.x constraint violations
with `Validated<E, A>` for accumulating validation errors. Constraint violations are
lifted into the `Validated` error channel instead of being collected through
`ConstraintViolationException`.

Jakarta Validation 3.x is declared `compileOnly`.

```kotlin
implementation("codes.domix:fun-jakarta-validation:0.0.15")
```

---

## fun-tracing — Micrometer Tracing integration

`fun-tracing` wraps a Micrometer `Tracer` and creates spans around `Try`- and
`Result`-returning suppliers. Failure outcomes are automatically tagged on the active
span without any manual instrumentation.

When `fun-spring-boot` is on the classpath and Micrometer Tracing is present, a
`DmxTracing` bean is auto-registered — zero configuration required.

Micrometer Tracing is declared `compileOnly`.

```kotlin
implementation("codes.domix:fun-tracing:0.0.15")
```

---

## fun-observation — Micrometer Observation API integration

`fun-observation` wraps a Micrometer `ObservationRegistry` and records observations
around `Try`- and `Result`-returning suppliers. Failure outcomes are tagged on the
active observation.

Micrometer Observation is declared `compileOnly`.

```kotlin
implementation("codes.domix:fun-observation:0.0.15")
```

---

## fun-bom — Bill of Materials

`fun-bom` is now published to Maven Central. Import it in `dependencyManagement` to
align all `dmx.fun` module versions without specifying each individually:

```kotlin
// Gradle (Kotlin DSL)
implementation(platform("codes.domix:fun-bom:0.0.15"))
implementation("codes.domix:fun")          // no version needed
implementation("codes.domix:fun-quarkus")  // no version needed
testImplementation("codes.domix:fun-assertj")
```

```xml
<!-- Maven -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>codes.domix</groupId>
      <artifactId>fun-bom</artifactId>
      <version>0.0.15</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

---

## CycloneDX SBOMs

Every published module now ships a `*-cyclonedx.json` Software Bill of Materials
alongside the JAR and sources artifact on Maven Central. SBOMs are available for all
modules starting with this release.

---

## Bug fixes

### fun-quarkus — STATUS_MARKED_ROLLBACK treated as active transaction

`hasActiveTransaction()` previously only matched `STATUS_ACTIVE`. After
`setRollbackOnly()` is called, the JTA status transitions to `STATUS_MARKED_ROLLBACK`
— a state where the transaction is still active and joinable but has been marked for
rollback. The old check caused `REQUIRED` to start a new transaction instead of
joining, `MANDATORY` to throw, and `NEVER` to silently succeed when the enclosing
transaction had already been marked rollback-only. Both statuses are now treated as an
active, joinable transaction.

### fun-quarkus — Error and infrastructure failures in executeJoined

`executeJoined` previously only caught `RuntimeException`. JVM errors such as
`AssertionError` or `OutOfMemoryError` would escape without marking the joined
transaction rollback-only. The handler now catches `RuntimeException | Error`.

Additionally, `setRollbackOnly()` infrastructure failures are no longer silently
swallowed. When a `setRollbackOnly()` failure coincides with an application exception,
the infrastructure exception is attached as a suppressed exception so neither failure
is lost.

### fun-micrometer / fun-tracing / fun-observation — high-cardinality exception tag eliminated

The default `exception` tag used `getClass().getSimpleName()`, producing an unbounded
number of tag values when arbitrary third-party exceptions appear at runtime — a
violation of Micrometer's low-cardinality contract. A configurable `exceptionClassifier`
function now maps each exception to a bounded label; the built-in default maps all
`Throwable` subclasses to `"exception"` unless overridden.

### fun-jackson — null exception message and Tuple2 null elements

`Failure` serialization now handles a `null` exception message without throwing NPE.
`Tuple2` deserializer guards against null `first` and `second` elements.

### fun-assertj — angle brackets preserved in error messages

Generic type names such as `Result<V, E>` were stripped of angle brackets in assertion
failure messages. The formatter now escapes them correctly.

---

## Other changes

- **`fun-spring`** — `TxResult`, `TxTry`, and `TxValidated` accept
  `PROPAGATION_REQUIRES_NEW` and `readOnly` flags; declarative annotations honor
  Spring's `@Transactional` propagation contract in all edge cases.
- **`fun-resilience4j`** — per-artifact version strings replaced with a
  `platform("io.github.resilience4j:resilience4j-bom")` dependency for consistent
  version management.
- **Repository structure** reorganized into logical groups: `core/`, `serialization/`,
  `observability/`, `frameworks/`, `protocols/`, `samples/`.
- **Spring Boot** upgraded to **4.0.6**.
- **Gradle wrapper** upgraded to **9.5.0**.

---

## Getting the release

```kotlin
// Gradle (Kotlin DSL) — use the BOM to manage all versions
implementation(platform("codes.domix:fun-bom:0.0.15"))

implementation("codes.domix:fun")
implementation("codes.domix:fun-quarkus")      // Quarkus transaction support
implementation("codes.domix:fun-http")         // HttpClient wrapper
implementation("codes.domix:fun-jakarta-jaxb") // Jakarta JSON-B adapters
implementation("codes.domix:fun-tracing")      // Micrometer Tracing
implementation("codes.domix:fun-observation")  // Micrometer Observation
testImplementation("codes.domix:fun-assertj")
```

```xml
<!-- Maven — import the BOM first -->
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>codes.domix</groupId>
      <artifactId>fun-bom</artifactId>
      <version>0.0.15</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun</artifactId>
  </dependency>
  <dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-quarkus</artifactId>
  </dependency>
</dependencies>
```

See the [full changelog](https://github.com/domix/dmx-fun/blob/main/CHANGELOG.md) for
the complete list of changes, fixes, and build improvements in this release.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun/issues).*
