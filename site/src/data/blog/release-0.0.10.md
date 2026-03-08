---
title: "dmx-fun 0.0.10 Released"
description: "Version 0.0.10 brings sequence and traverse combinators to Try, Optional interop for both Result and Try, and a full set of Stream collectors for Result including stream(), toList(), and partitioningBy()."
pubDate: 2026-03-08
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "Result", "Try", "Collectors", "Optional", "Sequence", "Traverse"]
image: "https://images.unsplash.com/photo-1712300506253-7c5c23528449?q=80&w=1469&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Liana S"
    authorUrl: "https://unsplash.com/@cherstve_pechivo"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/una-persona-con-la-mano-extendida-frente-al-sol-oFddapylkKQ"
---

Version **0.0.10** is out. This release focuses on making it easier to work with collections of
`Result` and `Try` values: you can now fold a stream of results into a single result, partition
them into two typed lists, or bridge from `Optional` — all without writing the boilerplate yourself.

---

## Try — sequence & traverse

When you have multiple independent `Try` computations, you no longer need to iterate and check
each one manually. The new combinators handle the fail-fast pattern for you.

```java
// Collect a list of Try values — stops at the first Failure
List<Try<Integer>> attempts = List.of(
    Try.of(() -> Integer.parseInt("1")),
    Try.of(() -> Integer.parseInt("2")),
    Try.of(() -> Integer.parseInt("oops"))
);

Try<List<Integer>> result = Try.sequence(attempts);
// result.isFailure() == true  (NumberFormatException from "oops")
```

`traverse` combines the mapping and collecting steps:

```java
List<String> inputs = List.of("1", "2", "3");

Try<List<Integer>> parsed = Try.traverse(inputs, s -> Try.of(() -> Integer.parseInt(s)));
// parsed.isSuccess() == true  →  [1, 2, 3]
```

Both combinators have overloads for `Iterable` and `Stream`, and both fail fast — as soon as a
`Failure` is encountered the remaining elements are not evaluated.

---

## Optional interop

Bridging from `Optional` is now a one-liner on both `Result` and `Try`.

### `Result.fromOptional`

```java
Optional<User> maybeUser = userRepository.findById(id);

Result<User, NoSuchElementException> result = Result.fromOptional(maybeUser);
// Ok(user)  or  Err(NoSuchElementException("Optional is empty"))
```

### `Try.fromOptional`

```java
Try<User> tryUser = Try.fromOptional(maybeUser, () -> new UserNotFoundException(id));
// Success(user)  or  Failure(UserNotFoundException)
```

The exception supplier is lazy — it is only called when the `Optional` is empty.

---

## Result — Stream collectors

Three new additions let you plug `Result` values directly into the Stream API.

### `stream()`

Turns any `Result` into a `Stream` — one element for `Ok`, empty for `Err`. Useful for
flat-mapping inside a stream pipeline:

```java
List<Integer> values = Stream.of(Result.ok(1), Result.err("bad"), Result.ok(3))
    .flatMap(Result::stream)
    .toList();
// [1, 3]
```

### `toList()` collector

Accumulates `Stream<Result<V, E>>` into a single `Result<List<V>, E>`. If all elements are `Ok`
the collector returns `Ok` with an unmodifiable list; the first `Err` encountered (in encounter
order) is returned otherwise.

```java
Result<List<Integer>, String> r =
    Stream.of(Result.ok(1), Result.ok(2), Result.ok(3))
          .collect(Result.toList());
// Ok([1, 2, 3])
```

> **Note:** `toList()` is **not fail-fast**. Because the Java `Collector` API always feeds every
> element to the accumulator before the finisher runs, all stream elements are always consumed.
> Use `Result.sequence(Stream)` when you need true short-circuit behaviour.

### `partitioningBy()` collector

Separates a mixed stream into two typed, unmodifiable lists:

```java
Result.Partition<Integer, String> p =
    Stream.of(Result.ok(1), Result.err("a"), Result.ok(3), Result.err("b"))
          .collect(Result.partitioningBy());

p.oks();    // [1, 3]
p.errors(); // ["a", "b"]
```

`Result.Partition<V, E>` is a plain record. Its compact constructor defensively copies both lists
via `List.copyOf`, so neither the source lists nor the lists returned by `oks()` and `errors()`
can be mutated after construction.

---

## Null-safety improvements

Several null-safety gaps were closed across the release:

- `Try` is now `@NullMarked`; null guards were added throughout.
- `Try.recoverWith` validates that its callback does not return `null`.
- `Result.toList()` and `Result.partitioningBy()` now explicitly reject `null` stream
  elements with `NullPointerException`, matching the contract already established by
  `sequence()` and `traverse()`.
- `Try.sequence` / `Try.traverse` use `Collections.unmodifiableList` instead of
  `List.copyOf` so that `Success(null)` values — which can arise from `Try.run()` — survive
  the collection step.

---

## Getting the release

Add the dependency to your build:

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:dmx-fun:0.0.10")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>dmx-fun</artifactId>
  <version>0.0.10</version>
</dependency>
```

Full Javadoc is available at [/dmx-fun/javadoc/](/dmx-fun/javadoc/index.html).

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
