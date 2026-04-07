---
title: "dmx-fun 0.0.12 Released"
description: "Version 0.0.12 brings CompletableFuture adapters, Lazy<T>, zip3/zip4, Try.flatMapError, and a sweeping set of internal refactors powered by Stream Gatherers and Java record patterns."
pubDate: 2026-04-05
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "Lazy", "CompletableFuture", "zip3", "zip4", "flatMapError", "Gatherer", "RecordPatterns"]
image: "https://images.unsplash.com/photo-1484417894907-623942c8ee29?q=80&w=1632&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Emile Perron"
    authorUrl: "https://unsplash.com/@emilep"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/macbook-pro-showing-programming-language-xrVDYZRGdw4"
---

Version **0.0.12** is out. This release rounds out the core API with `Lazy<T>`,
`CompletableFuture` adapters, zip combinators for three and four containers, and
`Try.flatMapError`. Under the hood, `sequence` / `traverse` have been rewritten with
Stream Gatherers and the internal architecture was unified through a shared `Bicontainer`
interface. Here is everything that changed.

---

## Lazy\<T\> — deferred, memoized evaluation

`Lazy<T>` wraps a computation that is evaluated at most once, on first access, and then
cached for all subsequent calls.

```java
Lazy<Config> config = Lazy.of(() -> Config.loadFromDisk(path));

// Nothing runs yet.
Config c = config.get();   // evaluates now, result cached
Config c2 = config.get();  // returns cached result — supplier NOT called again
```

If the supplier throws, the exception is captured and rethrown on every call — the
at-most-once contract is honoured even for failures.

```java
Lazy<Connection> conn = Lazy.of(() -> {
    throw new IOException("unreachable host");
});

conn.get(); // throws IOException
conn.get(); // throws the same IOException — supplier was never called a second time
```

### map without forcing evaluation

```java
Lazy<String> appName = Lazy.of(() -> Config.load()).map(Config::appName);
// Config.load() has NOT run yet
```

### CompletableFuture bridge

```java
CompletableFuture<User> future = userService.fetchAsync(id);
Lazy<Try<User>> lazyUser = Lazy.fromFuture(future);
// future result wrapped in Try<User> on first access
```

---

## CompletableFuture adapters for Try and Result

Bridging between async code and functional types is now first-class.

### Try adapters

```java
// Wrap an in-flight future as Try<V>
CompletableFuture<Order> future = orderService.placeAsync(req);
Try<Order> result = Try.fromFuture(future);

// CancellationException and CompletionException are unwrapped to their cause
result.fold(
    order -> "Placed: " + order.id(),
    ex    -> "Failed: " + ex.getMessage()
);

// Convert Try back to a CompletableFuture
CompletableFuture<Order> f = result.toFuture();
// — already completed; Failure becomes exceptionally-completed
```

### Result adapters

```java
Result<Order, Throwable> result = Result.fromFuture(orderService.placeAsync(req));

result.fold(
    order -> render(order),
    err   -> renderError(err)
);
```

---

## zip3 and zip4

Combine three or four independent containers into a single result with `zip3` / `zip4`.
All containers must be present / successful; otherwise the first absent or failed value is
propagated.

### zip3

```java
Option<String>  name    = Option.some("Alice");
Option<Integer> age     = Option.some(30);
Option<String>  country = Option.some("MX");

// Combine into a Tuple3
Option<Tuple3<String, Integer, String>> t = Option.zip3(name, age, country);
// Some(("Alice", 30, "MX"))

// Combine with a custom function
Option<String> label = Option.zipWith3(name, age, country,
    (n, a, c) -> n + " (" + a + ") from " + c);
// Some("Alice (30) from MX")
```

The same API is available on `Result` and `Try`:

```java
Result<Tuple3<User, Profile, Settings>, String> data =
    Result.zip3(loadUser(id), loadProfile(id), loadSettings(id));
```

### zip4

```java
Try<Tuple4<String, Integer, Boolean, Double>> t =
    Try.zip4(
        Try.of(() -> name()),
        Try.of(() -> age()),
        Try.of(() -> active()),
        Try.of(() -> score())
    );

// Collapse with QuadFunction
Try<String> summary = Try.zipWith4(
    Try.of(() -> name()), Try.of(() -> age()),
    Try.of(() -> active()), Try.of(() -> score()),
    (n, a, act, s) -> n + " | " + a + " | " + act + " | " + s
);
```

---

## Try.flatMapError — recovery on the failure channel

`flatMapError` is the dual of `flatMap`: it operates on `Failure` values, allowing you to
attempt recovery with another fallible computation.

```java
Try<Config> config = Try.of(() -> loadFromFile(path))
    .flatMapError(ex -> Try.of(() -> loadFromClasspath(path)))
    .flatMapError(ex -> Try.success(Config.defaults()));
```

- If this is a `Success`, it is returned unchanged — the mapper is never called.
- If the mapper throws or returns `null`, the exception is captured as a new `Failure`
  (same behaviour as `recoverWith`).

| Method | Returns | Mapper receives | Mapper returns |
|---|---|---|---|
| `flatMap` | `Try<B>` | `Value` | `Try<B>` |
| `flatMapError` | `Try<Value>` | `Throwable` | `Try<Value>` |
| `recoverWith` | `Try<Value>` | `Throwable` | `Try<Value>` |
| `recover` | `Try<Value>` | `Throwable` | `Value` |

> `flatMapError` and `recoverWith` are equivalent in most cases. Prefer `flatMapError`
> when you think of the operation as "chaining on the error track"; prefer `recoverWith`
> when you think of it as "providing a fallback".

---

## Internal improvements

### Bicontainer — shared interface

Common combinators (`fold`, `getOrElse`, `getOrElseGet`, `getOrThrow`, `peek`,
`peekError`, `toOption`, `toResult`) have been extracted from both `Result` and
`Validated` into the `Bicontainer<Value, Error>` shared interface. This eliminates
duplicate implementations and ensures both types honour exactly the same contracts.

### sequence / traverse rewritten with Stream Gatherers

All `sequence` and `traverse` methods across `Option`, `Result`, and `Try` now use
`Gatherer.ofSequential()` instead of `Collector.of()` or manual iterator loops.

```
Stream<Result<V, E>> ──► Gatherer ──► Result<List<V>, E>
                          stops at first Err (short-circuit)
```

`Iterable` overloads delegate to their `Stream` counterparts via
`StreamSupport.stream(iterable.spliterator(), false)` — no more duplicated iteration logic.

### Validated — record patterns and stream pipelines

Two internal implementations were modernised:

- **`Validated.product()`**: the double-nested `switch` was replaced with a local
  `record Pair<X, Y>` and a single exhaustive pattern-matching `switch`, reducing
  indentation and improving readability.

- **`Validated.traverse(Iterable)`**: the anonymous `Iterator` inner class was replaced
  with a `StreamSupport.stream(...).map(...).iterator()` pipeline.

---

## Getting the release

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun:0.0.12")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun</artifactId>
  <version>0.0.12</version>
</dependency>
```

Full Javadoc is available at [/dmx-fun/javadoc/](/dmx-fun/javadoc/index.html).

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
