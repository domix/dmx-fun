---
title: "dmx-fun 0.0.11 Released"
description: "Version 0.0.11 is the biggest release yet: Validated for applicative error accumulation, four checked functional interfaces, Tuple3 and Tuple4 with combinators, and an upgrade to the Java 25 LTS toolchain."
pubDate: 2026-03-20
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "Validated", "Tuple3", "Tuple4", "TriFunction", "QuadFunction", "CheckedFunction", "Java 25"]
image: "https://plus.unsplash.com/premium_photo-1661963874418-df1110ee39c1?q=80&w=1086&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Getty Images"
    authorUrl: "https://unsplash.com/@gettyimages"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/fondo-abstracto-del-numero-de-codigo-digital-representa-la-tecnologia-de-codificacion-y-los-lenguajes-de-programacion-KDMsC1xglWs"
---

Version **0.0.11** is out. This is the largest release to date, landing four new types, a new
functional interfaces layer, and the upgrade to Java 25 LTS. Here is everything that is new.

---

## Validated — applicative error accumulation

`Validated<E, A>` is a new type for scenarios where you want to collect *all* errors rather than
stopping at the first one. Unlike `Result`, which is fail-fast, `Validated` accumulates every
failure encountered across independent validations.

```java
Validated<List<String>, String> email =
    validateEmail(input.email());      // Valid("user@example.com") or Invalid(["bad email"])

Validated<List<String>, Integer> age =
    validateAge(input.age());          // Valid(30) or Invalid(["age must be positive"])

Validated<List<String>, RegistrationForm> form =
    email.combine(
        age,
        RegistrationForm::new,
        (e1, e2) -> { var merged = new ArrayList<>(e1); merged.addAll(e2); return merged; }
    );
// If both fail → Invalid(["bad email", "age must be positive"])
// If both pass → Valid(RegistrationForm("user@example.com", 30))
```

The key method is `combine` — it merges two `Validated` values applicatively. When both are
`Invalid`, the provided `BinaryOperator` merges the two error values. When both are `Valid`, the
provided `BiFunction` combines the two success values into the result type.

### Available operations

| Method | Description |
|---|---|
| `Validated.valid(a)` / `Validated.invalid(e)` | Factory methods |
| `map(Function)` | Transform the success value |
| `mapError(Function)` | Transform the error value |
| `flatMap(Function)` | Chain a dependent validation |
| `combine(other, merger, errorMerge)` | Applicative combination |
| `fold(onInvalid, onValid)` | Collapse to a single value |
| `getOrElse(a)` / `getOrElseGet(Supplier)` | Extract with fallback |
| `getOrThrow(Function)` | Extract or throw a mapped exception |
| `peek(Consumer)` / `peekError(Consumer)` | Side-effect hooks |
| `toOption()` / `toResult(Supplier)` | Interop with `Option` and `Result` |
| `sequence(Iterable)` / `traverse(Iterable, Function)` | Collect, accumulating errors |
| `fromOption(Option, Supplier)` | Lift an `Option` into `Validated` |

### sequence and traverse

When you have a list of independent values to validate, `sequence` and `traverse` accumulate
every error across the entire list rather than stopping at the first failure:

```java
List<String> inputs = List.of("alice@example.com", "not-an-email", "bob@example.com", "also-bad");

Validated<List<String>, List<String>> result =
    Validated.traverse(inputs, email -> validateEmail(email));

// Invalid(["not-an-email is invalid", "also-bad is invalid"])
// — both failures reported, not just the first
```

---

## Checked functional interfaces

Four `@FunctionalInterface` types have been promoted to first-class, top-level types to make
working with legacy checked-exception APIs less painful. They mirror the standard `java.util.function`
interfaces but declare `throws Exception` on their abstract method.

| Interface | Mirrors |
|---|---|
| `CheckedFunction<T, R>` | `Function<T, R>` |
| `CheckedSupplier<T>` | `Supplier<T>` |
| `CheckedConsumer<T>` | `Consumer<T>` |
| `CheckedRunnable` | `Runnable` |

The primary use case is bridging into `Try.of()` and `Try.run()`:

```java
// Without CheckedSupplier — anonymous lambda, exception leaks
Try<Config> config = Try.of(() -> ConfigLoader.load(path));

// With CheckedSupplier — reusable, named, testable
CheckedSupplier<Config> loader = () -> ConfigLoader.load(path);
Try<Config> config = Try.of(loader);

// CheckedFunction inside a pipeline
CheckedFunction<String, Integer> parse = Integer::parseInt;
Try<Integer> result = Try.of(() -> parse.apply("42"));
```

All four interfaces are `@NullMarked` and participate in the module's null-safety contract.

---

## Tuple3 and Tuple4

`Tuple2<A, B>` gets two siblings. Both are immutable `@NullMarked` records with null-guarded
constructors and a full set of mapping combinators.

### Tuple3

```java
Tuple3<String, Integer, Boolean> t = Tuple3.of("hello", 42, true);

t._1();  // "hello"
t._2();  // 42
t._3();  // true

// Map a single slot — others are unchanged
Tuple3<String, Integer, Boolean> upper = t.mapFirst(String::toUpperCase);
// ("HELLO", 42, true)

Tuple3<String, Integer, Boolean> doubled = t.mapSecond(n -> n * 2);
// ("hello", 84, true)

// Collapse to a single value
String summary = t.map((s, n, b) -> s + "/" + n + "/" + b);
// "hello/42/true"
```

### Tuple4

```java
Tuple4<String, Integer, Boolean, Double> t = Tuple4.of("x", 1, true, 3.14);

Tuple4<String, Integer, Boolean, Double> t2 = t.mapFourth(d -> d * 2);
// ("x", 1, true, 6.28)

String result = t.map((s, n, b, d) -> s + n + b + d);
```

### TriFunction and QuadFunction

`Tuple3.map` and `Tuple4.map` are powered by two new functional interfaces:

```java
TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;
int total = Tuple3.of(1, 2, 3).map(sum);  // 6

QuadFunction<Integer, Integer, Integer, Integer, Integer> sumFour = (a, b, c, d) -> a + b + c + d;
int total4 = Tuple4.of(1, 2, 3, 4).map(sumFour);  // 10
```

Both interfaces will also serve as the building blocks for the upcoming `zip3` and `zip4`
combinators on `Option`, `Result`, and `Try` (#69, #70).

---

## Java 25 LTS

The library now targets **Java 25**, the first Long-Term Support release since Java 21.
The toolchain in `lib/build.gradle` and all three CI workflows have been updated. See the
[Why You Should Upgrade to Java 25](/dmx-fun/blog/why-upgrade-to-java-25) post for the full story.

---

## Getting the release

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun:0.0.11")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun</artifactId>
  <version>0.0.11</version>
</dependency>
```

Full Javadoc is available at [/dmx-fun/javadoc/](/dmx-fun/javadoc/index.html).

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
