---
title: "Functional Composition Patterns"
description: "Explore powerful composition patterns to build complex functionality from simple, reusable functions"
pubDate: 2024-02-05
author: "dmx-fun Team"
category: "Best Practices"
tags: ["Composition", "Functions", "Patterns", "Advanced"]
---

Function composition is the heart of functional programming. By combining simple functions into more complex ones, you can build powerful, maintainable systems with minimal code.

## What is Function Composition?

Function composition is the process of combining two or more functions to produce a new function. If you have functions `f` and `g`, composing them gives you a new function that applies `g` to the output of `f`.

```java
Function<Integer, Integer> f = x -> x + 1;
Function<Integer, Integer> g = x -> x * 2;

// Composition: g(f(x))
Function<Integer, Integer> composed = f.andThen(g);
composed.apply(5); // (5 + 1) * 2 = 12
```

## Basic Composition

### andThen

Applies functions left-to-right:

```java
Function<String, String> trim = String::trim;
Function<String, String> lower = String::toLowerCase;
Function<String, Integer> length = String::length;

Function<String, Integer> process = trim
    .andThen(lower)
    .andThen(length);

process.apply("  HELLO  "); // 5
```

### compose

Applies functions right-to-left:

```java
Function<Integer, Integer> multiplyBy2 = x -> x * 2;
Function<Integer, Integer> add3 = x -> x + 3;

// Executes add3 first, then multiplyBy2
Function<Integer, Integer> composed = multiplyBy2.compose(add3);
composed.apply(5); // (5 + 3) * 2 = 16
```

## Practical Patterns

### Pipeline Pattern

Create data processing pipelines:

```java
public class DataPipeline {
    private final Function<String, String> normalize = String::trim;
    private final Function<String, String> lowercase = String::toLowerCase;
    private final Function<String, String> removeSpecial =
        s -> s.replaceAll("[^a-z0-9]", "");

    public Function<String, String> createPipeline() {
        return normalize
            .andThen(lowercase)
            .andThen(removeSpecial);
    }
}

// Usage
Function<String, String> pipeline = new DataPipeline().createPipeline();
String result = pipeline.apply("  Hello-World!  "); // "helloworld"
```

### Validation Pipeline

Chain validators:

```java
public class Validator<T> {
    private final Function<T, Either<String, T>> validate;

    public Validator(Function<T, Either<String, T>> validate) {
        this.validate = validate;
    }

    public Validator<T> and(Validator<T> next) {
        return new Validator<>(
            input -> validate.apply(input)
                .flatMap(next.validate)
        );
    }

    public Either<String, T> apply(T input) {
        return validate.apply(input);
    }
}

// Usage
Validator<String> notEmpty = new Validator<>(
    s -> s.isEmpty() ? Either.left("Must not be empty") : Either.right(s)
);

Validator<String> minLength = new Validator<>(
    s -> s.length() < 3 ? Either.left("Must be at least 3 chars") : Either.right(s)
);

Validator<String> validator = notEmpty.and(minLength);
Either<String, String> result = validator.apply("ab"); // Left("Must be at least 3 chars")
```

### Transformation Chain

Build complex transformations:

```java
public class UserTransformer {
    public Function<RawUser, User> createTransformer() {
        return this::normalizeEmail
            .andThen(this::capitalizeNames)
            .andThen(this::setDefaults)
            .andThen(this::validate);
    }

    private User normalizeEmail(RawUser raw) {
        return raw.withEmail(raw.email().toLowerCase().trim());
    }

    private User capitalizeNames(RawUser raw) {
        return raw.withName(capitalize(raw.name()));
    }

    private User setDefaults(RawUser raw) {
        return raw.role() == null ? raw.withRole("USER") : raw;
    }

    private User validate(RawUser raw) {
        if (!isValidEmail(raw.email())) {
            throw new ValidationException("Invalid email");
        }
        return new User(raw);
    }
}
```

## Advanced Composition

### Lifting Functions

Lift regular functions to work with Option:

```java
public class FunctionLifter {
    public static <T, R> Function<Option<T>, Option<R>> lift(
        Function<T, R> f
    ) {
        return opt -> opt.map(f);
    }
}

// Usage
Function<String, Integer> length = String::length;
Function<Option<String>, Option<Integer>> optLength =
    FunctionLifter.lift(length);

Option<String> name = Option.of("John");
Option<Integer> len = optLength.apply(name); // Some(4)
```

### Kleisli Composition

Compose functions that return monadic values:

```java
public class Kleisli<T, R> {
    private final Function<T, Try<R>> f;

    public Kleisli(Function<T, Try<R>> f) {
        this.f = f;
    }

    public <V> Kleisli<T, V> andThen(Kleisli<R, V> next) {
        return new Kleisli<>(
            input -> f.apply(input).flatMap(next.f)
        );
    }

    public Try<R> apply(T input) {
        return f.apply(input);
    }
}

// Usage
Kleisli<String, Integer> parse = new Kleisli<>(
    s -> Try.of(() -> Integer.parseInt(s))
);

Kleisli<Integer, Double> toPercent = new Kleisli<>(
    i -> Try.of(() -> i / 100.0)
);

Kleisli<String, Double> combined = parse.andThen(toPercent);
Try<Double> result = combined.apply("50"); // Success(0.5)
```

### Function Caching

Add memoization through composition:

```java
public class Memoizer<T, R> {
    private final Map<T, R> cache = new ConcurrentHashMap<>();

    public Function<T, R> memoize(Function<T, R> f) {
        return input -> cache.computeIfAbsent(input, f);
    }
}

// Usage
Function<Integer, Integer> expensive = n -> {
    // Expensive computation
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    return n * n;
};

Function<Integer, Integer> cached = new Memoizer<Integer, Integer>()
    .memoize(expensive);

cached.apply(5); // Takes 1 second
cached.apply(5); // Instant (cached)
```

## Real-World Example

Here's a complete example of a data processing system using composition:

```java
public class OrderProcessor {
    // Individual functions
    private final Function<String, Try<Order>> parseOrder =
        json -> Try.of(() -> jsonParser.parse(json, Order.class));

    private final Function<Order, Try<Order>> validateOrder =
        order -> Try.of(() -> validator.validate(order));

    private final Function<Order, Try<Order>> enrichOrder =
        order -> Try.of(() -> enrichmentService.enrich(order));

    private final Function<Order, Try<Order>> saveOrder =
        order -> Try.of(() -> database.save(order));

    // Composed pipeline
    public Try<Order> processOrder(String orderJson) {
        return parseOrder.apply(orderJson)
            .flatMap(validateOrder)
            .flatMap(enrichOrder)
            .flatMap(saveOrder)
            .recover(ValidationException.class, e -> Order.rejected())
            .recover(DatabaseException.class, e -> {
                logger.error("Database error", e);
                return Order.pending();
            });
    }
}
```

## Best Practices

1. **Keep functions small**: Each function should do one thing well
2. **Make functions pure**: No side effects makes composition reliable
3. **Use descriptive names**: Function names should describe what they do
4. **Compose incrementally**: Build complex behavior from simple pieces
5. **Test individually**: Test each function before composing

## Conclusion

Function composition is a powerful technique that allows you to build complex systems from simple, reusable parts. By thinking in terms of composition, you can create more maintainable and testable code.

Start identifying opportunities to replace complex methods with composed functions in your codebase!
