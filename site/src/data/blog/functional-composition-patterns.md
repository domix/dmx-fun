---
title: "Functional Composition Patterns"
description: "Explore powerful composition patterns to build complex functionality from simple, reusable functions"
pubDate: 2026-02-13
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Best Practices"
tags: ["Composition", "Functions", "Patterns", "Advanced"]
image: "https://images.unsplash.com/photo-1506506931473-341add2fa375?q=80&w=1166&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
---

Function composition is one of the most practical ideas you can adopt from functional programming: build complex behavior by chaining small, focused steps.

In Java, composition appears everywhere—`Function#andThen`, `Function#compose`, stream pipelines, `CompletableFuture#thenCompose`, and even validation flows.

This post shows composition patterns using **only the JDK**.

---

## What is Function Composition?

Function composition combines functions to produce a new function. If you have `f` and `g`, composing them yields a new function that applies them in sequence.

```java
import java.util.function.Function;

Function<Integer, Integer> f = x -> x + 1;
Function<Integer, Integer> g = x -> x * 2;

// g(f(x))
Function<Integer, Integer> composed = f.andThen(g);

int result = composed.apply(5); // (5 + 1) * 2 = 12
```

---

## Basic Composition in Java

### `andThen` (left-to-right)

```java
import java.util.function.Function;

Function<String, String> trim = String::trim;
Function<String, String> lower = String::toLowerCase;
Function<String, Integer> length = String::length;

Function<String, Integer> process =
    trim.andThen(lower).andThen(length);

int n = process.apply("  HELLO  "); // 5
```

### `compose` (right-to-left)

```java
import java.util.function.Function;

Function<Integer, Integer> multiplyBy2 = x -> x * 2;
Function<Integer, Integer> add3 = x -> x + 3;

// Executes add3 first, then multiplyBy2
Function<Integer, Integer> composed = multiplyBy2.compose(add3);

int n = composed.apply(5); // (5 + 3) * 2 = 16
```

---

## Practical Patterns

### 1) Pipeline Pattern (data normalization)

Build a pipeline by composing small, testable functions:

```java
import java.util.function.Function;
import java.util.regex.Pattern;

public final class DataPipeline {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");

    private final Function<String, String> normalize = String::trim;
    private final Function<String, String> lowercase = s -> s.toLowerCase();
    private final Function<String, String> removeSpecial =
        s -> NON_ALNUM.matcher(s).replaceAll("");

    public Function<String, String> createPipeline() {
        return normalize.andThen(lowercase).andThen(removeSpecial);
    }
}

// Usage
// Function<String, String> pipeline = new DataPipeline().createPipeline();
// String result = pipeline.apply("  Hello-World!  "); // "helloworld"
```

> `replaceAll` with regex is fine for many cases. If this is hot-path code, consider a precompiled `Pattern` (as shown) or a manual filter.

---

### 2) Validation Pipeline (fail-fast) with a small `Result`

Java doesn’t ship an `Either`, so here’s a minimal JDK-only `Result<T,E>` you can paste into the post. This enables **typed errors** without exceptions.

```java
import java.util.Objects;
import java.util.function.Function;

public sealed interface Result<T, E> permits Result.Ok, Result.Err {

    record Ok<T, E>(T value) implements Result<T, E> {
        public Ok { Objects.requireNonNull(value); }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        public Err { Objects.requireNonNull(error); }
    }

    static <T, E> Result<T, E> ok(T value) { return new Ok<>(value); }
    static <T, E> Result<T, E> err(E error) { return new Err<>(error); }

    default <U> Result<U, E> map(Function<? super T, ? extends U> f) {
        if (this instanceof Ok<T, E> ok) return Result.ok(f.apply(ok.value()));
        @SuppressWarnings("unchecked")
        Err<T, E> err = (Err<T, E>) this;
        return Result.err(err.error());
    }

    default <U> Result<U, E> flatMap(Function<? super T, Result<U, E>> f) {
        if (this instanceof Ok<T, E> ok) return f.apply(ok.value());
        @SuppressWarnings("unchecked")
        Err<T, E> err = (Err<T, E>) this;
        return Result.err(err.error());
    }
}
```

Now you can implement a fail-fast validator:

```java
import java.util.function.Function;

public final class Validator<T> {

    private final Function<T, Result<T, String>> validate;

    public Validator(Function<T, Result<T, String>> validate) {
        this.validate = validate;
    }

    public Validator<T> and(Validator<T> next) {
        return new Validator<>(input -> validate.apply(input).flatMap(next.validate));
    }

    public Result<T, String> apply(T input) {
        return validate.apply(input);
    }
}

// Usage
// Validator<String> notEmpty = new Validator<>(
//     s -> s.isEmpty() ? Result.err("Must not be empty") : Result.ok(s)
// );
//
// Validator<String> minLength = new Validator<>(
//     s -> s.length() < 3 ? Result.err("Must be at least 3 chars") : Result.ok(s)
// );
//
// Validator<String> validator = notEmpty.and(minLength);
// Result<String, String> result = validator.apply("ab"); // Err("Must be at least 3 chars")
```

---

### 3) Validation that accumulates errors (not fail-fast)

In real systems you often want to return *all* validation problems at once. This version returns `Result<T, List<String>>`.

```java
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class AccumulatingValidator<T> {

    private final Function<T, List<String>> validate; // returns error messages

    public AccumulatingValidator(Function<T, List<String>> validate) {
        this.validate = validate;
    }

    public AccumulatingValidator<T> and(AccumulatingValidator<T> next) {
        return new AccumulatingValidator<>(input -> {
            List<String> errors = new ArrayList<>(this.validate.apply(input));
            errors.addAll(next.validate.apply(input));
            return errors;
        });
    }

    public Result<T, List<String>> apply(T input) {
        List<String> errors = validate.apply(input);
        return errors.isEmpty() ? Result.ok(input) : Result.err(errors);
    }
}
```

---

### 4) Transformation Chain (types must line up)

A clean approach: keep transforms on the same type (`RawUser -> RawUser`), and only at the end convert to your domain type.

```java
import java.util.function.Function;

public final class UserTransformer {

    public Function<RawUser, User> createTransformer() {
        Function<RawUser, RawUser> pipeline =
            this::normalizeEmail
                .andThen(this::capitalizeNames)
                .andThen(this::setDefaults)
                .andThen(this::validate);

        return pipeline.andThen(User::new);
    }

    private RawUser normalizeEmail(RawUser raw) {
        return raw.withEmail(raw.email().toLowerCase().trim());
    }

    private RawUser capitalizeNames(RawUser raw) {
        return raw.withName(capitalize(raw.name()));
    }

    private RawUser setDefaults(RawUser raw) {
        return raw.role() == null ? raw.withRole("USER") : raw;
    }

    private RawUser validate(RawUser raw) {
        if (!isValidEmail(raw.email())) {
            throw new ValidationException("Invalid email");
        }
        return raw;
    }

    // placeholders
    private static String capitalize(String s) { return s; }
    private static boolean isValidEmail(String email) { return email.contains("@"); }
}
```

> This example throws on invalid input (very Java). If you want a no-exceptions pipeline, use `Result` and compose with `flatMap` (next section).

---

## Advanced Composition (JDK-First)

### 1) Lifting functions to work with `Optional`

Lift `T -> R` into `Optional<T> -> Optional<R>`:

```java
import java.util.Optional;
import java.util.function.Function;

public final class FunctionLifter {

    public static <T, R> Function<Optional<T>, Optional<R>> lift(Function<T, R> f) {
        return opt -> opt.map(f);
    }
}

// Usage
// Function<String, Integer> length = String::length;
// Function<Optional<String>, Optional<Integer>> optLength = FunctionLifter.lift(length);
// Optional<Integer> len = optLength.apply(Optional.of("John")); // Optional[4]
```

---

### 2) “Kleisli” composition in the JDK: composing `Result` pipelines

When steps can fail, `andThen` isn’t enough—you want `flatMap`.

```java
import java.util.function.Function;

public final class ResultPipeline {

    static Result<Integer, String> parseInt(String s) {
        try { return Result.ok(Integer.parseInt(s)); }
        catch (NumberFormatException e) { return Result.err("Not a number: " + s); }
    }

    static Result<Double, String> toPercent(Integer i) {
        return Result.ok(i / 100.0);
    }

    public static void main(String[] args) {
        Function<String, Result<Integer, String>> parse = ResultPipeline::parseInt;
        Function<Integer, Result<Double, String>> percent = ResultPipeline::toPercent;

        Result<Double, String> r = parse.apply("50").flatMap(percent); // Ok(0.5)
        Result<Double, String> bad = parse.apply("x").flatMap(percent); // Err(...)
    }
}
```

This is the core idea: **compose “effectful” functions via `flatMap`**.

---

### 3) Async composition with `CompletableFuture` (most common in Java)

If you do async work, you already use composition:

* `thenApply` ≈ map
* `thenCompose` ≈ flatMap

```java
import java.util.concurrent.CompletableFuture;

public final class AsyncCompositionExample {

    CompletableFuture<User> fetchUser(String id) { /* ... */ return CompletableFuture.completedFuture(null); }
    CompletableFuture<Account> fetchAccount(User user) { /* ... */ return CompletableFuture.completedFuture(null); }
    CompletableFuture<ResultDto> enrich(Account account) { /* ... */ return CompletableFuture.completedFuture(null); }

    public CompletableFuture<ResultDto> pipeline(String id) {
        return fetchUser(id)
            .thenCompose(this::fetchAccount)
            .thenCompose(this::enrich);
    }
}
```

---

### 4) Function caching / memoization

Memoization is composition-friendly, but be clear about tradeoffs (unbounded memory, TTL, etc.).

```java
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Memoizer<T, R> {
    private final Map<T, R> cache = new ConcurrentHashMap<>();

    public Function<T, R> memoize(Function<T, R> f) {
        return input -> cache.computeIfAbsent(input, f);
    }
}

// Usage
// Function<Integer, Integer> expensive = n -> {
//     try { Thread.sleep(1000); }
//     catch (InterruptedException e) { Thread.currentThread().interrupt(); }
//     return n * n;
// };
//
// Function<Integer, Integer> cached = new Memoizer<Integer, Integer>().memoize(expensive);
```

> For production, use a cache with bounds/TTL instead of an unbounded map.

---

## Real-World Example: A JDK-Only Order Processing Pipeline with typed failures

This version keeps the pipeline linear and avoids exceptions for expected failures.

```java
import java.util.function.Function;

public final class OrderProcessor {

    private final Function<String, Result<Order, Failure>> parseOrder =
        json -> Json.safeParse(json, Order.class)
            .mapError(msg -> Failure.validation("Invalid JSON: " + msg));

    private final Function<Order, Result<Order, Failure>> validateOrder =
        order -> Validators.validate(order);

    private final Function<Order, Result<Order, Failure>> enrichOrder =
        order -> Enrichment.enrich(order);

    private final Function<Order, Result<Order, Failure>> saveOrder =
        order -> Database.save(order);

    public Result<Order, Failure> processOrder(String orderJson) {
        return parseOrder.apply(orderJson)
            .flatMap(validateOrder)
            .flatMap(enrichOrder)
            .flatMap(saveOrder);
    }

    // Example “recover” style:
    public Order processOrderWithFallbacks(String orderJson) {
        Result<Order, Failure> r = processOrder(orderJson);

        if (r instanceof Result.Ok<Order, Failure> ok) return ok.value();
        Failure f = ((Result.Err<Order, Failure>) r).error();

        return switch (f.kind()) {
            case VALIDATION -> Order.rejected();
            case DATABASE -> Order.pending();
        };
    }

    public record Failure(Kind kind, String message) {
        enum Kind { VALIDATION, DATABASE }
        static Failure validation(String m) { return new Failure(Kind.VALIDATION, m); }
        static Failure database(String m) { return new Failure(Kind.DATABASE, m); }
    }
}
```


---

## Best Practices (aligned with JDK usage)

1. **Keep functions small**: one responsibility per step.
2. **Make types do the work**: composition becomes safer as types get tighter.
3. **Encapsulate failure**: use `Result`/`Optional` for expected problems; reserve exceptions for truly exceptional cases.
4. **Compose incrementally**: build pipelines from tested pieces.
5. **Test steps, then test the pipeline**: unit tests first, then integration tests.
6. **Name steps like a story**: `parse → validate → enrich → save`.

---

## Closing note: when to use a library (Vavr, dmx-fun, etc.)

If you find yourself enjoying these patterns, you’ll quickly run into a common tradeoff with “JDK-only FP”: you can absolutely build the primitives you need, but you’ll end up re-creating a lot of plumbing—`Either/Result`, richer combinators (`zip`, `recover`, `fold`), immutable collections, better pattern matching helpers, and a more consistent “standard vocabulary” across your codebase.

That’s where libraries like **Vavr** shine: they provide battle-tested types (`Try`, `Either`, `Option`) and a coherent API surface designed specifically for composition. You get clearer intent, fewer footguns, and much less custom code to maintain—especially when you start composing error handling, validation, async workflows, or complex transformations.

And if you’re building your own functional toolkit (for example, something like **`dmx-fun`**), the biggest win is consistency: a shared `Result`/`Option` vocabulary, predictable combinators, and a style that your team can recognize instantly. The goal isn’t to be “more FP for FP’s sake”—it’s to make business pipelines easier to read, safer to refactor, and cheaper to test. Once your domain logic starts to look like composable pipelines, a dedicated library can turn that from “nice idea” into a scalable, repeatable engineering practice.

---

## Conclusion

Function composition is not an academic trick—it’s a way to make Java systems easier to test, extend, and reason about using tools you already have in the JDK.

Start small: extract tiny steps, tighten types, and compose pipelines. Over time, “giant methods” turn into readable, modular flows.

