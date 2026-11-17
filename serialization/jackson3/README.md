# fun-jackson3

Jackson **3.x** (`tools.jackson.*`) line. For Jackson 2 (`com.fasterxml.jackson.*`) see `fun-jackson2`.

Jackson serializers and deserializers for all [dmx-fun](https://github.com/domix/dmx-fun) types.

## Dependency

```xml
<!-- Maven -->
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-jackson3</artifactId>
    <version>${dmx-fun.version}</version>
</dependency>
```

```groovy
// Gradle
implementation("codes.domix:fun-jackson3:${dmxFunVersion}")
```

Jackson itself is declared `compileOnly` in this module — you must provide
`tools.jackson.core:jackson-databind` on your own classpath.
Compatible versions: **3.0.x – 3.3.x**.

## Registration

Jackson 3 mappers are immutable; modules are added through the builder.

### Manual

```java
ObjectMapper mapper = JsonMapper.builder()
    .addModule(new DmxFunModule())
    .build();
```

### Auto-discovery (recommended)

`DmxFunModule` is registered as a `tools.jackson.databind.JacksonModule`
service provider. `findAndAddModules()` picks it up automatically:

```java
ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
```

### Spring Boot 4

Automatic registration through `fun-spring-boot` (a `JacksonModule` bean picked up by Spring
Boot's `JacksonAutoConfiguration`) is tracked in [#554](https://github.com/domix/dmx-fun/issues/554).
Until it lands, declare the bean yourself:

```java
@Bean
JacksonModule dmxFunModule() {
    return new DmxFunModule();
}
```

## JSON shapes

| Type                 | Java value                 | JSON                   |
|----------------------|----------------------------|------------------------|
| `Option<T>`          | `Option.some(v)`           | `v` (unwrapped)        |
| `Option<T>`          | `Option.none()`            | `null`                 |
| `Result<V, E>`       | `Result.ok(v)`             | `{"ok": v}`            |
| `Result<V, E>`       | `Result.err(e)`            | `{"err": e}`           |
| `Try<V>`             | `Try.success(v)`           | `v` (unwrapped)        |
| `Try<V>`             | `Try.failure(ex)`          | `{"error": "message"}` |
| `Validated<E, A>`    | `Validated.valid(a)`       | `{"valid": a}`         |
| `Validated<E, A>`    | `Validated.invalid(e)`     | `{"invalid": e}`       |
| `Either<L, R>`       | `Either.right(v)`          | `{"right": v}`         |
| `Either<L, R>`       | `Either.left(v)`           | `{"left": v}`          |
| `Tuple2<A, B>`       | `new Tuple2<>(a, b)`       | `[a, b]`               |
| `Tuple3<A, B, C>`    | `Tuple3.of(a, b, c)`       | `[a, b, c]`            |
| `Tuple4<A, B, C, D>` | `Tuple4.of(a, b, c, d)`    | `[a, b, c, d]`         |
| `NonEmptyList<T>`    | `NonEmptyList.of(h, tail)` | `[h, ...tail]`         |

The JSON shapes are identical to `fun-jackson2`, so documents written by one line are
readable by the other.

## Usage examples

```java
ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

// Option
String json = mapper.writeValueAsString(Option.some("hello")); // "hello"
String none = mapper.writeValueAsString(Option.none());        // null

// Result
String ok  = mapper.writeValueAsString(Result.ok(42));         // {"ok":42}
String err = mapper.writeValueAsString(Result.err("oops"));    // {"err":"oops"}

// Tuple2
String t2 = mapper.writeValueAsString(new Tuple2<>("Alice", 30)); // ["Alice",30]

// NonEmptyList
String nel = mapper.writeValueAsString(
    NonEmptyList.of("a", List.of("b", "c")));                  // ["a","b","c"]

// Deserialization (requires type reference for generic types)
Option<String> opt = mapper.readValue("\"hello\"",
    new TypeReference<Option<String>>() {});

Result<Integer, String> result = mapper.readValue("{\"ok\":42}",
    new TypeReference<Result<Integer, String>>() {});
```

Jackson 3 exceptions are unchecked (`tools.jackson.core.JacksonException`), so no
`throws` clauses are needed around these calls.

## Jackson version compatibility

The module is tested against the following Jackson 3 versions in CI:

| Version | Status |
|---------|--------|
| 3.0.x   | tested |
| 3.1.x   | tested |
| 3.2.x   | tested |
| 3.3.x   | tested |

To run the tests locally against a specific version:

```bash
./gradlew :serialization:jackson3:test -Pjackson3Version=3.1.8
```
