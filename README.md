# dmx-fun

<img width="1536" height="1024" alt="image" src="https://github.com/user-attachments/assets/cab675c2-c8ca-4017-903f-6790309750e8" />

A Java library of functional types that make failures, absence, and validation explicit in the type system — without ceremony.

`Option<T>`, `Result<V, E>`, `Try<V>`, `Validated<E, A>`, `Either<L, R>`, `Lazy<T>`, `Tuple2/3/4`, `NonEmptyList<T>`, `NonEmptyMap<K, V>`, `NonEmptySet<T>`, `Guard<T>`, `Resource<T>`, and `Accumulator<E, A>` — each designed to compose cleanly with the others and with the Java standard library.

---

## Installation

All modules are published to Maven Central. Add only what you need.

| Module        | Latest version                                                                                                                                    |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| `fun`         | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun)](https://central.sonatype.com/artifact/codes.domix/fun)                 |
| `fun-jackson` | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-jackson)](https://central.sonatype.com/artifact/codes.domix/fun-jackson) |
| `fun-assertj` | [![Maven Central](https://img.shields.io/maven-central/v/codes.domix/fun-assertj)](https://central.sonatype.com/artifact/codes.domix/fun-assertj) |

Replace `LATEST_VERSION` with the version shown in the badge above.

### Core library

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

---

## Types

| Type               | Tag                   | When to use                                                                                              |
|--------------------|-----------------------|----------------------------------------------------------------------------------------------------------|
| `Option<T>`        | Nullability           | A value that may or may not be present. The null-safe alternative to `@Nullable`.                        |
| `Result<V, E>`     | Error handling        | An operation that can succeed or fail with a typed error.                                                |
| `Try<V>`           | Exception handling    | Wraps a computation that may throw. Turns exceptions into values.                                        |
| `Validated<E, A>`  | Validation            | Like `Result` but accumulates all errors instead of failing on the first.                                |
| `Either<L, R>`     | Disjoint union        | A value that is one of two types with no success/failure semantics.                                      |
| `Lazy<T>`          | Deferred computation  | A value computed at most once, on first access. Thread-safe memoization.                                 |
| `Tuple2/3/4`       | Product types         | Typed heterogeneous tuples without a dedicated class.                                                    |
| `NonEmptyList<T>`  | Collections           | A list guaranteed to have at least one element at compile time.                                          |
| `NonEmptyMap<K,V>` | Collections           | A map guaranteed to have at least one entry at compile time. Insertion order preserved.                  |
| `NonEmptySet<T>`   | Collections           | A set guaranteed to have at least one element at compile time. No duplicates, insertion order preserved. |
| `Guard<T>`         | Validation            | A composable, named predicate that produces a `Validated` result — the reusable building block for validation pipelines. |
| `Resource<T>`      | Resource management   | A composable managed resource: acquire, use, and release with a guaranteed cleanup guarantee.            |
| `Accumulator<E,A>` | Tracing               | A value paired with a side-channel accumulation (log, metrics, audit trail). Threads cross-cutting concerns through pure computation chains without shared mutable state. |

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
