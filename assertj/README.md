# fun-assertj

Fluent AssertJ custom assertions for all [dmx-fun](https://github.com/domix/dmx-fun) types.

## Dependency

```xml
<!-- Maven -->
<dependency>
    <groupId>codes.domix</groupId>
    <artifactId>fun-assertj</artifactId>
    <version>${dmx-fun.version}</version>
    <scope>test</scope>
</dependency>
```

```groovy
// Gradle
testImplementation("codes.domix:fun-assertj:${dmxFunVersion}")
```

AssertJ itself is declared `compileOnly` in this module — you must provide
`org.assertj:assertj-core` on your own classpath.
Compatible versions: **3.21.x – 3.27.x**.

## Usage

Import the entry point and use `assertThat` with any dmx-fun type:

```java
import static dmx.fun.assertj.DmxFunAssertions.assertThat;
```

`DmxFunAssertions.assertThat` is overloaded for every supported type.
It coexists with the standard `org.assertj.core.api.Assertions.assertThat` — both
can be statically imported in the same test class without conflict as long as
the argument types differ.

## Assertions reference

### `Option<T>`

| Method                         | Description                                                               |
|--------------------------------|---------------------------------------------------------------------------|
| `isSome()`                     | Asserts the option is present                                             |
| `isNone()`                     | Asserts the option is absent                                              |
| `containsValue(expected)`      | Asserts the option is present and contains the given value                |
| `hasValueSatisfying(consumer)` | Asserts the option is present and the value satisfies the given condition |

```java
assertThat(Option.some(42)).isSome().containsValue(42);
assertThat(Option.none()).isNone();
assertThat(Option.some("hello")).hasValueSatisfying(v -> assertThat(v).startsWith("hel"));
```

### `Result<V, E>`

| Method                    | Description                                            |
|---------------------------|--------------------------------------------------------|
| `isOk()`                  | Asserts the result is a success                        |
| `isErr()`                 | Asserts the result is a failure                        |
| `containsValue(expected)` | Asserts the result is Ok and contains the given value  |
| `containsError(expected)` | Asserts the result is Err and contains the given error |

```java
assertThat(Result.ok("hello")).isOk().containsValue("hello");
assertThat(Result.err("oops")).isErr().containsError("oops");
```

### `Try<V>`

| Method                     | Description                                                |
|----------------------------|------------------------------------------------------------|
| `isSuccess()`              | Asserts the try is a success                               |
| `isFailure()`              | Asserts the try is a failure                               |
| `containsValue(expected)`  | Asserts the try is successful and contains the given value |
| `failsWith(exceptionType)` | Asserts the try failed with an exception of the given type |

```java
assertThat(Try.success(1)).isSuccess().containsValue(1);
assertThat(Try.failure(new IOException("boom"))).isFailure().failsWith(IOException.class);
```

### `Validated<E, A>`

| Method                    | Description                                                   |
|---------------------------|---------------------------------------------------------------|
| `isValid()`               | Asserts the validated is valid                                |
| `isInvalid()`             | Asserts the validated is invalid                              |
| `containsValue(expected)` | Asserts the validated is valid and contains the given value   |
| `hasError(expected)`      | Asserts the validated is invalid and contains the given error |

```java
assertThat(Validated.valid("ok")).isValid().containsValue("ok");
assertThat(Validated.invalid("bad")).isInvalid().hasError("bad");
```

### `Tuple2<A, B>` / `Tuple3<A, B, C>` / `Tuple4<A, B, C, D>`

| Method                | Applies to             |
|-----------------------|------------------------|
| `hasFirst(expected)`  | Tuple2, Tuple3, Tuple4 |
| `hasSecond(expected)` | Tuple2, Tuple3, Tuple4 |
| `hasThird(expected)`  | Tuple3, Tuple4         |
| `hasFourth(expected)` | Tuple4                 |

```java
assertThat(new Tuple2<>("Alice", 30)).hasFirst("Alice").hasSecond(30);
assertThat(Tuple3.of("Alice", 30, true)).hasFirst("Alice").hasSecond(30).hasThird(true);
assertThat(Tuple4.of("Alice", 30, true, 1.75)).hasFirst("Alice").hasFourth(1.75);
```

## AssertJ version compatibility

The module is tested in CI against the following AssertJ versions on every pull
request that touches the `assertj/` module:

| AssertJ version | Status  |
|-----------------|---------|
| 3.21.x          | tested  |
| 3.22.x          | tested  |
| 3.23.x          | tested  |
| 3.24.x          | tested  |
| 3.25.x          | tested  |
| 3.26.x          | tested  |
| 3.27.x          | tested  |

To run the tests locally against a specific version:

```bash
./gradlew :assertj:test -PassertjVersion=3.25.3
```
