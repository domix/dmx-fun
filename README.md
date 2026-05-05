# dmx-fun

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/cab675c2-c8ca-4017-903f-6790309750e8" />

A Java library of functional types that make failures, absence, and validation explicit in the type system — without ceremony.

`Option<T>`, `Result<V, E>`, `Try<V>`, `Validated<E, A>`, `Either<L, R>`, `Lazy<T>`, `Tuple2/3/4`, `NonEmptyList<T>`, `NonEmptyMap<K, V>`, `NonEmptySet<T>`, `Guard<T>`, `Resource<T>`, and `Accumulator<E, A>` — each designed to compose cleanly with the others and with the Java standard library.

---

## Installation

All modules are published to Maven Central. Add only what you need.

| Module             | Latest version                                                                                                                                              | Description                                                                                   |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `fun-bom`          | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-bom)](https://central.sonatype.com/artifact/codes.domix/fun-bom)                   | Bill of Materials — import once to manage all dmx-fun versions in one place.                  |
| `fun`              | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun)](https://central.sonatype.com/artifact/codes.domix/fun)                           | Core library. `Option`, `Result`, `Try`, `Validated`, `Either`, `Lazy`, `Tuple`, `NonEmptyList/Map/Set`, `Guard`, `Resource`, `Accumulator`. |
| `fun-jackson`      | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-jackson)](https://central.sonatype.com/artifact/codes.domix/fun-jackson)           | Jackson serializers and deserializers for all dmx-fun types.                                  |
| `fun-assertj`      | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-assertj)](https://central.sonatype.com/artifact/codes.domix/fun-assertj)           | Fluent AssertJ assertions for all dmx-fun types. Use in `test` scope.                         |
| `fun-spring`       | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-spring)](https://central.sonatype.com/artifact/codes.domix/fun-spring)             | `@Transactional` support for `Result`, `Try`, `Option`, and `Validated`.                      |
| `fun-spring-boot`  | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-spring-boot)](https://central.sonatype.com/artifact/codes.domix/fun-spring-boot)   | Spring Boot auto-configuration for dmx-fun. Registers `TxResult`/`TxTry`/`TxValidated`, `DmxFunModule` (Jackson), `DmxTracing`, `DmxObservation`, and MVC handlers automatically when the corresponding dependencies are on the classpath. |
| `fun-quarkus`      | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-quarkus)](https://central.sonatype.com/artifact/codes.domix/fun-quarkus)           | Quarkus CDI transaction support for `Result` and `Try` — programmatic (`TxResult`, `TxTry`) and declarative (`@TransactionalResult`, `@TransactionalTry`). Rolls back on error/failure without relying on exceptions. |
| `fun-resilience4j` | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-resilience4j)](https://central.sonatype.com/artifact/codes.domix/fun-resilience4j) | Resilience4J adapters (Retry, CircuitBreaker, RateLimiter, Bulkhead) that return `Try`/`Result` instead of throwing. |
| `fun-micrometer`   | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-micrometer)](https://central.sonatype.com/artifact/codes.domix/fun-micrometer)     | Counters, timers, and failure metrics for `Try` and `Result` via Micrometer.                  |
| `fun-tracing`      | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-tracing)](https://central.sonatype.com/artifact/codes.domix/fun-tracing)           | Distributed tracing spans for `Try` and `Result` via Micrometer Tracing. Auto-configured by `fun-spring-boot`. |
| `fun-observation`  | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-observation)](https://central.sonatype.com/artifact/codes.domix/fun-observation)   | Metrics **and** distributed tracing spans for `Try` and `Result` via the Micrometer Observation API. Auto-configured by `fun-spring-boot`. |
| `fun-jakarta-validation` | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-jakarta-validation)](https://central.sonatype.com/artifact/codes.domix/fun-jakarta-validation) | Jakarta Validation adapter — returns `Validated<NonEmptyList<E>, A>` instead of throwing `ConstraintViolationException`. |
| `fun-jakarta-jaxb` | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-jakarta-jaxb)](https://central.sonatype.com/artifact/codes.domix/fun-jakarta-jaxb) | Jakarta JSON-B (`JsonbAdapter`) and JAXB (`XmlAdapter`) adapters for all dmx-fun types. |

Replace `LATEST_VERSION` with the version shown in the badge above.

### Using the BOM (recommended when using multiple modules)

Import `fun-bom` once and omit the version from every individual module declaration.

**Maven**
```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>codes.domix</groupId>
      <artifactId>fun-bom</artifactId>
      <version>LATEST_VERSION</version>
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
      <artifactId>fun-jackson</artifactId>
  </dependency>
</dependencies>
```

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    implementation(platform("codes.domix:fun-bom:LATEST_VERSION"))
    implementation("codes.domix:fun")
    implementation("codes.domix:fun-jackson")  // no version needed
}
```

---

### Core library

[![Coverage Status](https://coveralls.io/repos/github/domix/dmx-fun/badge.svg?branch=main)](https://coveralls.io/github/domix/dmx-fun?branch=main)

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun:LATEST_VERSION")
```

### Jackson integration (optional)

Serializers and deserializers for all dmx-fun types.

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-jackson</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-jackson:LATEST_VERSION")
```

### Spring integration (optional)

Transaction support for `Result`, `Try`, `Option`, and `Validated` — use dmx-fun idiomatically without giving up `@Transactional`. Spring Framework is declared as `compileOnly`; bring your own Spring dependency.

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-spring</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-spring:LATEST_VERSION")
```

### Quarkus integration (optional)

CDI-based transaction support for `Result` and `Try` — programmatic (`TxResult`, `TxTry`) and declarative (`@TransactionalResult`, `@TransactionalTry`). Quarkus is declared as `compileOnly`; bring your own version (tested: 3.11.x – 3.35.x). The runtime JAR ships `META-INF/quarkus-extension.properties` so Quarkus build tools add the deployment artifact automatically.

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-quarkus</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-quarkus:LATEST_VERSION")
```

### AssertJ integration (optional, test scope)

Fluent custom assertions for all dmx-fun types.

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-assertj</artifactId>
    <version>LATEST_VERSION</version>
    <scope>test</scope>
</dependency>
```

**Gradle**
```groovy
testImplementation("codes.domix:fun-assertj:LATEST_VERSION")
```

### Micrometer integration (optional)

Automatic counters, timers, and failure metrics for `Try` and `Result` operations. Micrometer is declared as `compileOnly`; bring your own version (1.5.x – 1.16.x) and backend.

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-micrometer</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.16.5</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-micrometer:LATEST_VERSION")
implementation("io.micrometer:micrometer-core:1.16.5")
```

### Micrometer Tracing integration (optional)

Instruments `Try` and `Result` executions with distributed tracing spans automatically.
Micrometer Tracing is declared as `compileOnly`; bring your own version (1.2.x – 1.6.x) and bridge.
When `fun-spring-boot` is also on the classpath, a `DmxTracing` bean is registered automatically
whenever a `Tracer` bean is present (Spring Boot provides one when a bridge is on the classpath).

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-tracing</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
    <version>1.6.5</version>
</dependency>
<!-- A Micrometer Tracing bridge, e.g. OpenTelemetry: -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
    <version>1.6.5</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-tracing:LATEST_VERSION")
implementation("io.micrometer:micrometer-tracing:1.6.5")
// A Micrometer Tracing bridge, e.g. OpenTelemetry:
implementation("io.micrometer:micrometer-tracing-bridge-otel:1.6.5")
```

### Micrometer Observation integration (optional)

Instruments `Try` and `Result` executions using the Micrometer Observation API — a single call
produces **both** metrics and distributed tracing spans through whatever `ObservationHandler`s are
registered. `micrometer-core` is declared as `compileOnly`; bring your own version (1.10.x – 1.16.x).
When `fun-spring-boot` is also on the classpath, a `DmxObservation` bean is registered automatically
whenever an `ObservationRegistry` bean is present (Spring Boot provides one via `ObservationAutoConfiguration`).

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-observation</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-core</artifactId>
    <version>1.16.5</version>
</dependency>
<!-- Optional: add a tracing bridge to also get distributed tracing spans -->
<!--
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
    <version>1.6.5</version>
</dependency>
-->
```

**Gradle**
```groovy
implementation("codes.domix:fun-observation:LATEST_VERSION")
implementation("io.micrometer:micrometer-core:1.16.5")
// Optional: add a tracing bridge to also get distributed tracing spans
// implementation("io.micrometer:micrometer-tracing-bridge-otel:1.6.5")
```

### Jakarta Validation integration (optional)

Validates objects and returns `Validated<NonEmptyList<E>, A>` instead of throwing `ConstraintViolationException` — all constraint violations accumulate in a single return value. Jakarta Validation is declared as `compileOnly`; bring your own version (tested: 3.0.x – 3.1.x) and provider.

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-jakarta-validation</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<dependency>
    <groupId>jakarta.validation</groupId>
    <artifactId>jakarta.validation-api</artifactId>
    <version>3.1.1</version>
</dependency>
<!-- A Jakarta Validation provider, e.g. Hibernate Validator: -->
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>9.1.0.Final</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-jakarta-validation:LATEST_VERSION")
implementation("jakarta.validation:jakarta.validation-api:3.1.1")
implementation("org.hibernate.validator:hibernate-validator:9.1.0.Final")
```

### Jakarta JSON-B + JAXB integration (optional)

`JsonbAdapter` and `XmlAdapter` implementations for all dmx-fun types. Both APIs are declared as `compileOnly`; bring your own Jakarta EE or MicroProfile runtime.

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-jakarta-jaxb</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<!-- JSON-B RI (tests / standalone): -->
<dependency>
    <groupId>jakarta.json.bind</groupId>
    <artifactId>jakarta.json.bind-api</artifactId>
    <version>3.0.1</version>
</dependency>
<dependency>
    <groupId>org.eclipse</groupId>
    <artifactId>yasson</artifactId>
    <version>3.0.4</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-jakarta-jaxb:LATEST_VERSION")
// JSON-B RI (tests / standalone):
implementation("jakarta.json.bind:jakarta.json.bind-api:3.0.1")
implementation("org.eclipse:yasson:3.0.4")
runtimeOnly("org.eclipse.parsson:parsson:1.1.7")
```

### Resilience4J integration (optional)

Adapters for `Retry`, `CircuitBreaker`, `RateLimiter`, and `Bulkhead` that return `Try` or `Result` instead of throwing exceptions. Resilience4J is declared as `compileOnly`; bring your own version (tested: 2.0.2–2.4.0).

**Maven**
```xml
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-resilience4j</artifactId>
    <version>LATEST_VERSION</version>
</dependency>
<!-- Add the Resilience4J artifacts you use, e.g. (replace RESILIENCE4J_VERSION with the actual version): -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-retry</artifactId>
    <version>RESILIENCE4J_VERSION</version>
</dependency>
```

**Gradle**
```groovy
implementation("codes.domix:fun-resilience4j:LATEST_VERSION")
// Add the Resilience4J artifacts you use, e.g. (replace RESILIENCE4J_VERSION with the actual version):
implementation("io.github.resilience4j:resilience4j-retry:RESILIENCE4J_VERSION")
```

---

## Types

| Type               | Tag                   | When to use                                                                                                                                                               |
|--------------------|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Option<T>`        | Nullability           | A value that may or may not be present. The null-safe alternative to `@Nullable`.                                                                                         |
| `Result<V, E>`     | Error handling        | An operation that can succeed or fail with a typed error.                                                                                                                 |
| `Try<V>`           | Exception handling    | Wraps a computation that may throw. Turns exceptions into values.                                                                                                         |
| `Validated<E, A>`  | Validation            | Like `Result` but accumulates all errors instead of failing on the first.                                                                                                 |
| `Either<L, R>`     | Disjoint union        | A value that is one of two types with no success/failure semantics.                                                                                                       |
| `Lazy<T>`          | Deferred computation  | A value computed at most once, on first access. Thread-safe memoization.                                                                                                  |
| `Tuple2/3/4`       | Product types         | Typed heterogeneous tuples without a dedicated class.                                                                                                                     |
| `NonEmptyList<T>`  | Collections           | A list guaranteed to have at least one element at compile time.                                                                                                           |
| `NonEmptyMap<K,V>` | Collections           | A map guaranteed to have at least one entry at compile time. Insertion order preserved.                                                                                   |
| `NonEmptySet<T>`   | Collections           | A set guaranteed to have at least one element at compile time. No duplicates, insertion order preserved.                                                                  |
| `Guard<T>`         | Validation            | A composable, named predicate that produces a `Validated` result — the reusable building block for validation pipelines.                                                  |
| `Resource<T>`      | Resource management   | A composable managed resource: acquire, use, and release with a guaranteed cleanup.                                                                                       |
| `Accumulator<E, A>` | Tracing              | A value paired with a side-channel accumulation (log, metrics, audit trail). Threads cross-cutting concerns through pure computation chains without shared mutable state. |

---

## Quick example

```java
import dmx.fun.Result;
import dmx.fun.Try;
import dmx.fun.Validated;
import dmx.fun.NonEmptyList;

// Wrap a legacy API that throws
Try<RawUser> raw = Try.of(() -> externalService.fetchUser(id));

// Convert to Result for typed error handling
Result<User, RegistrationError> user = raw
    .toResult(e -> RegistrationError.fetchFailed(id, e))
    .flatMap(this::parseUser)
    .flatMap(this::enrichWithDefaults);

// Validate all fields at once — accumulate every error, not just the first
Validated<NonEmptyList<String>, ValidUser> validated = user
    .map(this::validate)
    .getOrElse(Validated.invalidNel("user could not be loaded"));

validated
    .onValid(userRepository::save)
    .onInvalid(errors -> log.warn("Registration rejected: {}", errors));
```

---

## Documentation

Full developer guide, API reference, and composition patterns:
**https://domix.github.io/dmx-fun/**

---

## Requirements

- Java 25 or later

---

## Contributing

Bug reports, feature requests, and pull requests are welcome. Please open an issue before submitting a large change.

---

## License

Apache License 2.0 — see [LICENSE](./LICENSE) for details.
