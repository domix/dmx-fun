---
title: "Currying and Partial Application in Practice"
description: "Currying sounds like academic theory. Partial application is something Java developers already do every day — they just don't name it. This post names it, distinguishes the two concepts, and shows five situations where partial application makes real production code cleaner."
pubDate: 2026-06-05
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Currying", "Partial Application", "Higher-Order Functions", "Design Patterns"]
image: "https://images.pexels.com/photos/574071/pexels-photo-574071.jpeg"
imageCredit:
    author: "Lukas"
    authorUrl: "https://www.pexels.com/@goumbik/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/person-encoding-in-laptop-574071/"
---

Currying has a reputation problem. The name sounds like a Haskell textbook. The usual examples — turning `add(a, b)` into `add(a)(b)` — feel like ceremony for its own sake. Most Java developers encounter currying, decide it is an academic curiosity, and move on.

That dismissal is partly fair. Full Haskell-style currying — where every function is automatically a chain of single-argument functions — is genuinely awkward in Java. The language was not designed for it, and forcing it produces unreadable types.

But **partial application** is different. Partial application is the practice of fixing some of a function's arguments now and getting back a new function that accepts the rest later. It is not a transformation of the type system — it is a lambda that closes over some values. Java developers do it constantly. Most just do not name it.

This post names it, distinguishes it from currying, and shows the specific situations where it makes production code cleaner.

---

## Two Concepts, One Source of Confusion

These terms are related but not the same thing.

**Currying** transforms a function that takes multiple arguments into a chain of functions that each take one argument. Given a function `f(a, b, c)`, currying produces `f(a)(b)(c)` — a function that takes `a` and returns a function that takes `b` and returns a function that takes `c` and returns the result.

**Partial application** fixes some of a function's arguments now and returns a new function that accepts the remaining arguments. Given `f(a, b, c)`, partial application of the first argument produces `g(b, c)` — a new function with `a` already bound.

In languages like Haskell, every function is automatically curried, which makes partial application trivial — you just call a function with fewer arguments than it expects. In Java, neither happens automatically. Currying requires explicit scaffolding; partial application requires a lambda.

```java
// Currying (explicit, not idiomatic in Java)
Function<Integer, Function<Integer, Integer>> curriedAdd = a -> b -> a + b;
Function<Integer, Integer> add5 = curriedAdd.apply(5);  // fix 'a'
int result = add5.apply(3);  // 8

// Partial application (idiomatic — just a lambda that closes over a value)
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
Function<Integer, Integer> add5 = b -> add.apply(5, b); // fix 'a' via closure
int result = add5.apply(3);  // 8
```

The curried version chains `apply` calls and produces deeply nested function types as you add parameters. The partial application version is a lambda — readable to any Java developer without knowing the term.

In practice, Java code almost never benefits from full currying. It benefits regularly from partial application, which is just captured arguments in a lambda.

---

## The Java Idiom: Functions That Return Functions

Before the practical use cases, it helps to see the underlying pattern clearly.

A method that returns a `Function<B, R>` and takes an `A` is the natural Java equivalent of a curried function. You call it once with the part you know now, and you get back a function for the part you know later.

```java
static Predicate<String> minLength(int min) {
    return s -> s.length() >= min;
}

Predicate<String> atLeastFive  = minLength(5);
Predicate<String> atLeastEight = minLength(8);

// Both are now reusable, composable predicates
boolean valid = atLeastFive.and(atLeastEight).test("password");
// false — "password" has 8 chars, passes atLeastEight but is checked by and()
```

`minLength` is not exotic. It is just a factory method that returns a lambda. The `min` variable is captured. That capture *is* the partial application.

The same pattern works with `Function`, `BiFunction`, `Consumer`, `Supplier` — any functional interface.

---

## Use Case 1: Configuring Behavior Without Subclasses

The most common real-world use of partial application is creating configured variants of a generic operation without subclassing.

Consider an HTTP client method:

```java
static <T> Result<T, HttpError> fetch(
    HttpClient client,
    Duration timeout,
    Class<T> responseType,
    URI uri
) { ... }
```

In one corner of the application, the client and timeout are always the same — only the URI and response type change. Instead of passing three fixed arguments everywhere, fix them once:

```java
HttpClient client  = HttpClient.newHttpClient();
Duration   timeout = Duration.ofSeconds(5);

// Partially apply the stable arguments; get back a two-arg function
BiFunction<Class<?>, URI, Result<?, HttpError>> get =
    (type, uri) -> fetch(client, timeout, type, uri);

// Usage throughout the module — only the variable parts remain
Result<UserDto, HttpError>    user    = get.apply(UserDto.class,    userUri);
Result<ProductDto, HttpError> product = get.apply(ProductDto.class, productUri);
```

This is not a class. There is no inheritance. There is no strategy interface. There is a lambda that remembers two values, producing a cleaner call site for every downstream caller.

---

## Use Case 2: Fitting Multi-Argument Methods Into Pipelines

Java streams and `Optional` chains expect single-argument functions (`Function<T, R>`, `Predicate<T>`). When the operation you want to apply has two or more arguments, you need to reduce it to one.

Partial application is the reduction mechanism.

```java
static String formatLine(String prefix, int lineNumber, String content) {
    return "[%s:%03d] %s".formatted(prefix, lineNumber, content);
}

List<String> lines = List.of("first line", "second line", "third line");

// Fix the prefix now; the line number and content vary per element
AtomicInteger counter = new AtomicInteger(1);
List<String> formatted = lines.stream()
    .map(line -> formatLine("INFO", counter.getAndIncrement(), line))
    .toList();
```

Or, if the counter is not needed, fix both stable arguments upfront:

```java
// Fix two arguments; adapt to Function<String, String> for map
String prefix = "INFO";
int    base   = 1;
Function<String, String> formatter = line -> formatLine(prefix, base, line);

List<String> formatted = lines.stream().map(formatter).toList();
```

This pattern appears often when integrating third-party APIs into a stream pipeline — the API method has more parameters than `map` can accept, and a partially applied wrapper reduces it cleanly.

---

## Use Case 3: Injecting Dependencies Without a Class

Dependency injection frameworks use objects and interfaces. When a unit of behavior is small enough to be a single function, the framework is overhead. Partial application gives you the same result — a function bound to its dependencies — without the wiring ceremony.

```java
// A pure function that needs a repository and a clock
static Result<Order, OrderError> place(
    OrderRepository repo,
    Clock clock,
    PlaceOrderRequest request
) {
    Instant now   = clock.instant();
    Order   draft = Order.draft(request, now);
    return repo.save(draft);
}
```

At the composition root (the application entry point, or a Spring `@Bean` method):

```java
OrderRepository repo  = ...;
Clock           clock = Clock.systemUTC();

// Fix the dependencies; expose a one-arg function to the rest of the app
Function<PlaceOrderRequest, Result<Order, OrderError>> placeOrder =
    request -> place(repo, clock, request);
```

The rest of the application receives a `Function<PlaceOrderRequest, Result<Order, OrderError>>`. It does not know about `OrderRepository` or `Clock`. Those dependencies are already applied. The function is fully self-contained.

This is especially useful in tests — you can inject a fake repository and a fixed clock in two lines, without a mocking framework:

```java
OrderRepository fakeRepo  = new InMemoryOrderRepository();
Clock           fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

Function<PlaceOrderRequest, Result<Order, OrderError>> placeOrder =
    request -> place(fakeRepo, fixedClock, request);

// The test drives a plain function — no @Mock, no @InjectMocks
Result<Order, OrderError> result = placeOrder.apply(validRequest);
```

---

## Use Case 4: Adapting Checked-Exception Methods

Java's `Function<T, R>` and `BiFunction<T, U, R>` cannot declare `throws`. Any method that throws a checked exception cannot be used directly in `map` or `flatMap` — the compiler refuses it.

dmx-fun provides `CheckedFunction<T, R, E>`, `TriFunction<A, B, C, R>`, `QuadFunction<A, B, C, D, R>`, and their checked variants precisely for this situation.

When you need to partially apply a method that throws, these interfaces let you do it without the `try/catch` noise:

```java
import codes.domix.fun.function.CheckedBiFunction;

// Parses a file into a config — throws IOException
static Config parse(ConfigSchema schema, Path path) throws IOException { ... }

ConfigSchema schema = ConfigSchema.load("default.schema");

// Fix the schema; produce a CheckedFunction<Path, Config, IOException>
CheckedFunction<Path, Config, IOException> parser = path -> parse(schema, path);

// Use Try.of to wrap each call
List<Try<Config>> configs = paths.stream()
    .map(p -> Try.of(() -> parser.apply(p)))
    .toList();
```

Without `CheckedFunction`, you would need an inline `try/catch` inside every lambda, or a helper `sneakyThrow` utility that is frowned upon in reviewed code. The checked interface names the pattern and makes the intent clear.

---

## Use Case 5: Building Reusable Validation Rules

Partial application is the natural model for parameterized validation rules — rules that share a structure but differ only in their configuration:

```java
static Predicate<String> matches(Pattern pattern) {
    return s -> pattern.matcher(s).matches();
}

static Predicate<Integer> between(int min, int max) {
    return n -> n >= min && n <= max;
}

static <T> Predicate<T> notNull() {
    return Objects::nonNull;
}

// Build specific rules by partial application
Predicate<String> validEmail    = matches(EMAIL_PATTERN);
Predicate<String> validUsername = matches(USERNAME_PATTERN).and(minLength(3)).and(maxLength(32));
Predicate<Integer> validAge     = between(18, 120);
```

Each rule is a first-class value. They compose with `.and()`, `.or()`, `.negate()`. They can be stored in a list and evaluated together. No class hierarchy. No `implements Validator<T>`.

---

## When Partial Application Makes Things Worse

Partial application improves code when the fixed arguments are genuinely stable and the variable arguments are genuinely variable. It makes things worse in several situations.

**When the fixed arguments are not actually stable.** If the value you are capturing changes between calls, a partially applied function is an implicit dependency — the behavior differs depending on *when* the lambda was created. This is confusing. Keep the argument explicit.

**When there are more than two or three captured values.** A lambda that closes over five variables is a class with five fields in disguise — but without a name, without visibility, and without the ability to inspect it with a debugger. At that point, a named record and a method are clearer:

```java
// Hard to follow: five captured values
Function<Request, Response> handler =
    req -> handle(logger, config, rateLimiter, cache, timeout, req);

// Clear: a named class that makes the dependencies explicit
record RequestHandler(Logger logger, Config config, RateLimiter rl, Cache cache, Duration timeout) {
    Response handle(Request req) { ... }
}
```

**When the composition point is not obvious.** Partial application moves logic from the call site to wherever the lambda was constructed. If the construction is far from the use, readers must trace back to understand what the function does. A direct method call is always traceable.

**When tests already have the full context.** In unit tests, partial application of dependencies offers no benefit if you can call the method directly. Apply it at module boundaries; do not apply it inside test helpers.

---

## Summary

| Technique | Java idiom | Best for |
|---|---|---|
| Full currying | `Function<A, Function<B, R>>` | Rare — only when callers always need single-arg composition |
| Partial application | Lambda closing over stable arguments | Adapting multi-arg functions to pipelines, injecting stable dependencies, building configured rule factories |
| Method reference | `ClassName::method` | When no argument is being fixed — the simplest form |

Currying as a language-wide transformation is not idiomatic in Java, and you do not need it to be. What matters is partial application: fixing what you know, deferring what you do not. In Java, that is always just a lambda.

The interesting design question is not "should I curry this function?" but "which arguments are stable and which are variable?" When you answer that question, the lambda writes itself.

---

## Further reading

- [Higher-Order Functions Explained with Real Examples](/dmx-fun/blog/higher-order-functions-real-examples) — the foundation: functions as values, Strategy, Decorator, and pipeline patterns
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types) — making method signatures tell the truth with Option, Result, and Validated
- [Functional Composition Patterns](/dmx-fun/blog/functional-composition-patterns) — composing functions, predicates, and pipelines at a higher level
- [Do You Need a Functional Library or Just Better Habits?](/dmx-fun/blog/library-vs-habits) — when the patterns above warrant a library and when discipline suffices
