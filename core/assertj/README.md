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

### `Either<L, R>`

| Method                     | Description                                                  |
|----------------------------|--------------------------------------------------------------|
| `isRight()`                | Asserts the either is a Right                                |
| `isLeft()`                 | Asserts the either is a Left                                 |
| `containsRight(expected)`  | Asserts the either is Right and contains the given value     |
| `containsLeft(expected)`   | Asserts the either is Left and contains the given value      |

```java
assertThat(Either.right(42)).isRight().containsRight(42);
assertThat(Either.left("oops")).isLeft().containsLeft("oops");
```

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

### `Resource<T>`

| Method                       | Description                                                          |
|------------------------------|----------------------------------------------------------------------|
| `succeedsWith(expected)`     | Asserts the resource lifecycle completes and yields the given value  |
| `failsWith(exceptionType)`   | Asserts the lifecycle fails with an exception of the given type      |
| `failsWithMessage(message)`  | Asserts the lifecycle fails with an exception containing the message |

The lifecycle (acquire → use → release) runs exactly once and is cached — all
chained assertions on the same instance reuse the same result.

```java
assertThat(Resource.of(() -> "conn", c -> {})).succeedsWith("conn");
assertThat(Resource.of(() -> { throw new IOException("timeout"); }, c -> {}))
    .failsWith(IOException.class);
```

### `Guard<T>`

| Method                                   | Description                                                       |
|------------------------------------------|-------------------------------------------------------------------|
| `accepts(value)`                         | Asserts the guard passes for the given value                      |
| `rejects(value)`                         | Asserts the guard fails for the given value                       |
| `rejectsWithMessage(value, message)`     | Asserts the guard fails and at least one rejection message contains the given string     |
| `rejectsWithMessages(value, messages…)`  | Asserts the guard fails and each given string appears in at least one rejection message  |

```java
Guard<String> notBlank = Guard.of(s -> !s.isBlank(), "must not be blank");
assertThat(notBlank).accepts("alice").rejects("  ");
assertThat(notBlank).rejectsWithMessage("", "must not be blank");
```

### `Accumulator<E, A>`

| Method                          | Description                                                              |
|---------------------------------|--------------------------------------------------------------------------|
| `hasValue(expected)`            | Asserts the primary value equals the expected value                      |
| `hasAccumulation(expected)`     | Asserts the side-channel accumulation equals the expected value          |
| `accumulationContains(element)` | Asserts the accumulation (a `Collection`) contains the given element     |
| `accumulationHasSize(size)`     | Asserts the accumulation (a `Collection`) has the given size             |

```java
Accumulator<List<String>, Integer> acc = Accumulator.of(42, List.of("step1", "step2"));
assertThat(acc).hasValue(42).accumulationContains("step1").accumulationHasSize(2);
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
