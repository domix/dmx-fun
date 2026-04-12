---
title: "dmx-fun 0.0.13 Released"
description: "Version 0.0.13 adds Either<L,R>, NonEmptyList<T>, typed Try.recover overloads, the fun-jackson and fun-assertj modules, a runnable samples subproject, and a full Developer Guide — plus a breaking package rename to dmx.fun."
pubDate: 2026-04-13
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "Either", "NonEmptyList", "Jackson", "AssertJ", "Samples", "DeveloperGuide"]
image: "https://images.unsplash.com/photo-1556075798-4825dfaaf498?q=80&w=1752&auto=format&fit=crop&ixlib=rb-4.1.0"
imageCredit:
    author: "Yancy Min"
    authorUrl: "https://unsplash.com/@yancymin"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/photos/a-close-up-of-a-text-description-on-a-computer-screen-842ofHC6MaI"
---

Version **0.0.13** is the largest release to date. The core library gains two new types
(`Either<L, R>` and `NonEmptyList<T>`), typed recovery on `Try`, and `orElse` / collector
additions across the board. Two new optional modules ship for the first time — `fun-jackson`
and `fun-assertj`. A `samples/` subproject gives every type a runnable example. The
Developer Guide is now complete. And the Java package has been renamed to `dmx.fun`.

> **Breaking change:** the package has changed from `codes.domix.fun` to `dmx.fun`.
> Update all import statements. Maven/Gradle coordinates (`codes.domix:fun`) are unchanged.

---

## Either\<L, R\> — typed disjoint union

`Either<L, R>` represents a value that is one of two possible types — a `Left<L>` or a
`Right<R>`. By convention the right side carries the success value and the left side carries
the error or alternative.

```java
Either<Integer, String> classify(String input) {
    try {
        return Either.left(Integer.parseInt(input));
    } catch (NumberFormatException e) {
        return Either.right(input.toUpperCase());
    }
}

Either<Integer, String> result = classify("hello");
result.fold(
    n -> "number: " + n,
    s -> "text: "   + s
); // "text: HELLO"
```

`map` operates on the right side; `mapLeft` on the left side. `flatMap` chains computations
on the right track. `swap()` exchanges the two channels.

```java
// Map the right side only
Either<Integer, String> shouted = classify("world").map(s -> s + "!");
// Right("WORLD!")

// Interop — Right becomes Some, Left becomes None
Option<String> opt = classify("hello").toOption(); // Some("HELLO")
```

---

## NonEmptyList\<T\> — non-empty list at compile time

`NonEmptyList<T>` is a list guaranteed to have at least one element. It is the standard
companion for `Validated` error accumulation.

```java
// Construction
NonEmptyList<String> tags = NonEmptyList.of("java", List.of("fp", "dmx-fun"));
tags.head(); // "java"
tags.tail(); // ["fp", "dmx-fun"]
tags.size(); // 3

// Singleton
NonEmptyList<String> single = NonEmptyList.singleton("only");

// fromList — safe construction from a plain List
Option<NonEmptyList<String>> nel = NonEmptyList.fromList(List.of("a", "b"));
// Some([a, b])

Option<NonEmptyList<String>> empty = NonEmptyList.fromList(List.of());
// None

// Concatenation
NonEmptyList<String> more = NonEmptyList.of("quarkus", List.of("spring"));
NonEmptyList<String> all  = tags.concat(more); // size 5
```

### Validated error accumulation

```java
Validated<NonEmptyList<String>, Integer> v1 = Validated.invalidNel("Must be positive");
Validated<NonEmptyList<String>, Integer> v2 = Validated.invalidNel("Must be a number");

v1.combine(v2, NonEmptyList::concat, Integer::sum)
  .peekError(errors -> errors.toList().forEach(System.out::println));
// Must be positive
// Must be a number
```

---

## Try.recover and recoverWith — typed exception overloads

The existing `recover` / `recoverWith` methods now have typed overloads that target a
specific exception class. The overload is a no-op when the failure holds a different
exception type.

```java
Try<Config> config = Try.of(() -> loadFromFile(path))
    .recover(FileNotFoundException.class, ex -> Config.defaults())
    .recover(IOException.class,           ex -> Config.minimal());

// Only the matching handler is applied; others are skipped
```

```java
Try<Connection> conn = Try.of(() -> connect(primary))
    .recoverWith(TimeoutException.class, ex -> Try.of(() -> connect(fallback)));
```

---

## fun-jackson — Jackson serialization for all dmx-fun types

The new `fun-jackson` module provides Jackson serializers and deserializers for every
dmx-fun type. Register `DmxFunModule` once and all types work transparently with
`ObjectMapper`.

```java
ObjectMapper mapper = new ObjectMapper()
    .registerModule(new DmxFunModule());
```

| Type | JSON shape (present/success) | JSON shape (absent/failure) |
|---|---|---|
| `Option<T>` | `{"value": ...}` | `{}` |
| `Result<V, E>` | `{"ok": ...}` | `{"err": ...}` |
| `Try<V>` | `{"value": ...}` | `{"error": "..."}` |
| `Either<L, R>` | `{"right": ...}` | `{"left": ...}` |

```java
// Round-trip Option
String json = mapper.writeValueAsString(Option.some("alice"));
// {"value":"alice"}

Option<String> parsed = mapper.readValue(json, new TypeReference<>() {});
// Some("alice")

// Record with dmx-fun fields
record UserDto(String name, Option<String> nick, Result<Integer, String> age) {}

UserDto user = new UserDto("Alice", Option.some("ali"), Result.ok(30));
String dto = mapper.writeValueAsString(user);
// {"name":"Alice","nick":{"value":"ali"},"age":{"ok":30}}
```

Add the dependency:

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun-jackson:0.0.13")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun-jackson</artifactId>
  <version>0.0.13</version>
</dependency>
```

Tested against Jackson **2.17.x through 2.21.x**.

---

## fun-assertj — fluent assertions for all dmx-fun types

The new `fun-assertj` module provides custom AssertJ assertions via a single entry point:
`DmxFunAssertions.assertThat(...)`.

```java
import static dmx.fun.assertj.DmxFunAssertions.assertThat;

// Option
assertThat(Option.some("alice")).isSome().hasSomeValue("alice");
assertThat(Option.<String>none()).isNone();

// Result
assertThat(Result.ok(42)).isOk().hasOkValue(42);
assertThat(Result.err("oops")).isError();

// Try
assertThat(Try.success(99)).isSuccess().hasSuccessValue(99);
assertThat(Try.failure(new RuntimeException())).isFailure();

// Validated
assertThat(Validated.valid("ok")).isValid();
assertThat(Validated.invalidNel("bad")).isInvalid();

// Tuples
assertThat(new Tuple2<>("a", 1)).hasFirst("a").hasSecond(1);
```

Add as a test-only dependency:

```kotlin
// Gradle (Kotlin DSL)
testImplementation("codes.domix:fun-assertj:0.0.13")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun-assertj</artifactId>
  <version>0.0.13</version>
  <scope>test</scope>
</dependency>
```

Tested against AssertJ **3.21.x through 3.27.x**.

---

## Runnable samples subproject

The new `samples/` module in the repository contains one executable class per type plus an
AssertJ test class, so you can clone the repo and run any example directly.

| Class | What it shows |
|---|---|
| `OptionSample` | `some`, `none`, `map`, `flatMap`, pattern match |
| `ResultSample` | `ok`, `err`, `flatMap`, `peek`, `peekError`, pattern match |
| `TrySample` | `of`, typed `recover`, `toResult`, `onSuccess` |
| `EitherSample` | `left`, `right`, `map`, `fold`, `toOption` |
| `ValidatedSample` | `combine`, error accumulation, pattern match |
| `LazySample` | deferred evaluation, memoization, `map` |
| `TupleSample` | `_1()`…`_4()`, `mapThird`, `map(TriFunction)` |
| `NonEmptyListSample` | `of`, `fromList`, `concat`, Validated integration |
| `CheckedInterfacesSample` | `CheckedFunction`, `CheckedSupplier`, `TriFunction`, `QuadFunction` |
| `JacksonSample` | `DmxFunModule`, round-trip for every type |
| `AssertJSampleTest` | `DmxFunAssertions.assertThat` for all types |

Each Developer Guide page now links directly to its corresponding sample at the top.

---

## Complete Developer Guide

The [Developer Guide](/dmx-fun/guide/) now covers every type and both extension modules —
11 content pages in total, each with detailed sections on construction, extraction,
transformation, composition, interop, pitfalls, and a real-world example.

Four new maintainer pages document the contribution workflow, CI/CD pipelines, release
process, and module conventions.

---

## Getting the release

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun:0.0.13")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun</artifactId>
  <version>0.0.13</version>
</dependency>
```

See the [full changelog](https://github.com/domix/dmx-fun/blob/main/CHANGELOG.md) for
the complete list of changes, fixes, and build improvements in this release.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun/issues).*
