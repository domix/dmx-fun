# fun-jackson

Jackson serializers and deserializers for all [dmx-fun](https://github.com/domix/dmx-fun) types.

## Dependency

```xml
<!-- Maven -->
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-jackson</artifactId>
    <version>${dmx-fun.version}</version>
</dependency>
```

```groovy
// Gradle
implementation("codes.domix:fun-jackson:${dmxFunVersion}")
```

Jackson itself is declared `compileOnly` in this module — you must provide
`com.fasterxml.jackson.core:jackson-databind` on your own classpath.
Compatible versions: **2.13.x – 2.21.x**.

## Registration

### Manual

```java
ObjectMapper mapper = new ObjectMapper()
    .registerModule(new DmxFunModule());
```

### Auto-discovery (recommended)

`DmxFunModule` is registered as a `com.fasterxml.jackson.databind.Module`
service provider. Jackson's `findAndRegisterModules()` picks it up automatically:

```java
ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
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

## Usage examples

```java
ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

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

## Jackson version compatibility

The module is tested against the following Jackson versions in CI:

| Version | Status |
|---------|--------|
| 2.13.x  | tested |
| 2.14.x  | tested |
| 2.15.x  | tested |
| 2.16.x  | tested |
| 2.17.x  | tested |
| 2.18.x  | tested |
| 2.19.x  | tested |
| 2.20.x  | tested |
| 2.21.x  | tested |

To run the tests locally against a specific version:

```bash
./gradlew :jackson:test -PjacksonVersion=2.15.4
```
