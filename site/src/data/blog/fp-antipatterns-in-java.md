---
title: "Common Anti-Patterns When Writing Functional Code in Java"
description: "Functional style in Java is easy to get subtly wrong. This post walks through the most common mistakes — from returning null inside a mapper to leaking shared mutable state into a stream — and shows how to fix each one."
pubDate: 2026-04-03
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Best Practices"
tags: ["Functional Programming", "Java", "Anti-Patterns", "Best Practices", "Code Quality"]
image: "https://images.unsplash.com/photo-1744362030217-ccb2dfff718c?q=80&w=1332&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "LATIKA SARKER"
    authorUrl: "https://unsplash.com/@latikasar"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/cono-de-helado-derramado-en-una-calle-de-la-ciudad-0IrOIk_rcgI"
---

Adopting functional style in Java is usually driven by a good instinct: cleaner pipelines, fewer nulls, composable error handling. The intent is right. The execution, however, is easy to get subtly wrong.

The anti-patterns below are not beginner mistakes — they are the kind of thing that slips into otherwise careful code. Each one looks functional on the surface. Each one undermines the actual benefits of functional style in a way that takes a while to diagnose.

---

## 1. Returning `null` from a Mapper

`map` is a contract: given a non-empty container, apply a transformation and produce a new non-empty container. The moment the transformation returns `null`, that contract is broken.

```java
// Looks functional, hides a null bug
Optional<String> name = Optional.of(user)
    .map(u -> u.getProfile())      // getProfile() can return null...
    .map(p -> p.getDisplayName())  // ...second map is never called;
                                   // name is silently Optional.empty()
    .orElse("no name");
    // No NPE — the null was swallowed.
    // You see "no name" when the real
    // problem is a missing profile.
```

The problem: `Optional.map(f)` in the standard JDK returns `Optional.empty()` if `f` returns `null`, silently turning a bug into an absent value. The caller has no way to distinguish "user has no display name" from "getProfile() returned null unexpectedly." With stricter null-marked types like `Option<T>` from `dmx-fun`, a mapper that returns `null` throws `NullPointerException` immediately — the bug surfaces at the source rather than disappearing downstream.

The fix is to model the possibility of absence in the return type of each step, and chain with `flatMap` instead:

```java
// Each step is honest about what it might not have
String name = Option.some(new User())
        .flatMap(User::getProfile)
        .map(Profile::getDisplayName)
        .getOrElse("no name");

```

If `getProfile()` can return null, its return type should be `Option<Profile>`, not `Profile`. Push the optionality into the type, not into null checks scattered downstream.

```text
flatMap mapper must not return null
java.lang.NullPointerException: flatMap mapper must not return null
	at java.base/java.util.Objects.requireNonNull(Objects.java:246)
	at codes.domix.fun.Option.flatMap(Option.java:237)
	at codes.domix.fun.InteropTest$ValidatedInterop.foo(InteropTest.java:288)
```

---

## 2. Using `isPresent()` + `get()` (The Null Check in Disguise)

The most common `Optional` anti-pattern is using it exactly like a null check:

```java
// This is just a null check with extra steps
Optional<User> maybeUser = userRepository.findById(id);
if (maybeUser.isPresent()) {
    User user = maybeUser.get();
    sendWelcomeEmail(user);
}
```

`Optional` was introduced to be *composed*, not inspected. The moment you call `isPresent()`, you have exited the functional model and re-entered the imperative one. Every `isPresent()` + `get()` pair is semantically equivalent to `!= null` + dereference — with none of the composability that makes `Optional` valuable.

The fix depends on what you actually want to do:

```java
// Compose: transform and extract in one chain
userRepository.findById(id)
    .ifPresent(this::sendWelcomeEmail);

// Or, if you need to return something from both branches
String message = userRepository.findById(id)
    .map(u -> "Welcome back, " + u.name())
    .orElse("User not found");
```

The same applies to `Option<T>` from `dmx-fun`: resist the urge to call `isSome()` before calling `get()`. Use `map`, `flatMap`, `getOrElse`, or `fold` instead. The value of an option type is that you never have to unwrap it prematurely.

---

## 3. Exceptions as Control Flow Inside Lambdas

Checked exceptions do not compose through Java's functional interfaces. The natural reaction is to wrap them in a `try/catch` inside the lambda — which defeats the purpose of writing a pipeline in the first place:

```java
// Exception caught and swallowed into Optional.empty()
List<Config> configs = paths.stream()
    .map(path -> {
        try {
            return Optional.of(ConfigLoader.load(path));
        } catch (IOException e) {
            return Optional.<Config>empty(); // error silently discarded
        }
    })
    .filter(Optional::isPresent)
    .map(Optional::get)
    .toList();
```

Two problems here. First, the `IOException` is silently discarded — you have no idea which paths failed or why. Second, the `Optional` wrapping and filtering dance is a sign that the wrong abstraction is being used.

The right tool when a step can fail is a type that preserves the failure:

```java
// Failures are values — nothing is discarded
List<Try<Config>> results = paths.stream()
    .map(path -> Try.of(() -> ConfigLoader.load(path)))
    .toList();

// You can then separate successes from failures:
List<Config> loaded = results.stream()
    .filter(Try::isSuccess)
    .map(Try::get)
    .toList();

List<Throwable> failures = results.stream()
    .filter(Try::isFailure)
    .map(Try::getCause)
    .toList();
```

`Try<T>` is exactly the right container for computations that may throw. It captures both the value *and* the exception as first-class values, without swallowing either.

---

## 4. Shared Mutable State Inside Lambdas

Java's lambda specification requires that captured local variables be effectively final. But nothing prevents a lambda from mutating state that it reaches through an object reference — and that is where subtle bugs live.

```java
// Shared mutable accumulator in a stream
List<String> errors = new ArrayList<>();

List<User> validUsers = users.stream()
    .filter(user -> {
        boolean valid = validator.validate(user);
        if (!valid) errors.add("Invalid: " + user.id()); // side effect inside filter
        return valid;
    })
    .toList();
```

The `errors` list is mutated as a side effect of the `filter` predicate. This is illegal in a parallel stream (race condition on the `ArrayList`), and it is a design smell in a sequential one: `filter` should answer a question, not produce side effects. The intent — collecting errors — is tangled with the intent of filtering valid users.

The functional approach is to separate the concerns. Map each user to a `Result`, then partition:

```java
// Concerns separated; no shared mutable state
List<Result<User, String>> classified = users.stream()
    .map(user -> validator.validate(user)
        ? Result.<User, String>ok(user)
        : Result.err("Invalid: " + user.id()))
    .toList();

List<User> validUsers = classified.stream()
    .filter(Result::isOk)
    .map(Result::get)
    .toList();

List<String> errors = classified.stream()
    .filter(Result::isError)
    .map(Result::getError)
    .toList();
```

No mutation. No shared state. Both outputs are derived from a single pass of immutable values.

---

## 5. Using `forEach` for Transformations

`forEach` is a terminal operation. It exists to produce side effects — logging, writing to a file, calling an external service. Using it to build a new collection means you are already outside the functional model:

```java
// forEach used to accumulate results
List<String> result = new ArrayList<>();
users.stream()
    .filter(User::isActive)
    .forEach(u -> result.add(u.email().toUpperCase())); // mutation inside forEach
```

This is strictly worse than a for-loop: it obscures the intent (it *looks* functional), requires a mutable intermediate variable, and becomes a race condition the moment `.parallel()` is added.

The correct form uses `collect`, which is designed for exactly this:

```java
// Terminal operation that produces a value, not a side effect
List<String> result = users.stream()
    .filter(User::isActive)
    .map(u -> u.email().toUpperCase())
    .toList();
```

The rule of thumb: if you find yourself creating an empty mutable collection before the stream and populating it inside `forEach`, you need `map` + `collect` (or `toList()`), not `forEach`.

---

## 6. Absorbing Errors Instead of Propagating Them

This anti-pattern is the functional equivalent of catching `Exception` and logging it:

```java
// Error silently converted to empty — information lost
public Option<UserProfile> loadProfile(UserId id) {
    try {
        return Option.some(profileService.load(id));
    } catch (ProfileNotFoundException e) {
        return Option.none(); // was this "not found" or a bug?
    } catch (ServiceUnavailableException e) {
        return Option.none(); // callers cannot distinguish these cases
    }
}
```

`Option.none()` means "no value." It does not mean "an error occurred." When you collapse a `ProfileNotFoundException` and a `ServiceUnavailableException` into the same `None`, you lose all information about which one happened and why. Callers cannot react differently to different failure modes, because those modes are gone.

The right carrier depends on what callers need:

```java
// Failure is a first-class typed value
public Result<UserProfile, ProfileError> loadProfile(UserId id) {
    return Try.of(() -> profileService.load(id))
        .fold(
            profile -> Result.<UserProfile, ProfileError>ok(profile),
            cause -> switch (cause) {
                case ProfileNotFoundException e   -> Result.err(ProfileError.notFound(id));
                case ServiceUnavailableException e -> Result.err(ProfileError.serviceDown());
                default                           -> Result.err(ProfileError.unexpected(cause));
            }
        );
}
```

Reserve `Option` for genuine optionality — "this value may or may not be present by design." Use `Result` (or `Try`) when a computation can fail, and the failure reason matters to the caller.

---

## 7. `Optional<Optional<T>>` — Forgetting `flatMap`

This one is almost always a sign that `map` was used where `flatMap` was needed:

```java
// Double wrapping — map returns Optional<Optional<T>>
Optional<Optional<String>> wrapped =
    Optional.of(user).map(u -> userRepository.findEmail(u.id()));
//                                             ^^^^^^^^^^^^^^^^^^^
//                                             This already returns Optional<String>
```

`map` wraps whatever the function returns in a new `Optional`. If the function already returns `Optional<String>`, the result is `Optional<Optional<String>>` — and you cannot compose further without unwrapping twice.

```java
// flatMap flattens the nesting
Optional<String> email =
    Optional.of(user).flatMap(u -> userRepository.findEmail(u.id()));
```

The same rule applies to `Option`, `Result`, `Try`, and `Stream`:
- `map` is for transforming `T` → `R`.
- `flatMap` is for transforming `T` → `Container<R>`, where the function itself may return absence or failure.

If you ever see `Container<Container<T>>` in your code, reach for `flatMap`.

---

## 8. Treating Every Method as a Candidate for `Result`

The inverse anti-pattern — over-engineering in the functional direction — deserves equal attention.

```java
// Result adds zero value here
public Result<Integer, String> add(int a, int b) {
    return Result.ok(a + b);
}

// Or wrapping a method that can never fail
public Result<List<User>, String> getEmptyList() {
    return Result.ok(List.of());
}
```

`Result<T, E>` is valuable when a computation has two meaningful outcomes: a success value and a typed failure. If a function cannot meaningfully fail — no network calls, no parsing, no domain invariants to violate — wrapping it in `Result` adds ceremony without benefit. Callers must now unwrap a result that is always `Ok`, and the type gives a false impression that failure is possible.

The right heuristic:

| Situation                                                   | Right type                                         |
|-------------------------------------------------------------|----------------------------------------------------|
| Value may or may not be present by design                   | `Option<T>` / `Optional<T>`                        |
| Computation may fail; caller needs to know why              | `Result<V, E>`                                     |
| Computation may throw; wrapping legacy API                  | `Try<T>`                                           |
| Can only succeed; pure transformation                       | Plain return type                                  |
| Can only succeed; pure transformation that may be expensive | Plain return type (laziness is a separate concern) |

Functional types are tools, not religion. Use them where they carry their weight.

---

## 9. Impure Functions Masquerading as `map`

The reason `map` is composable is that it is supposed to be a pure transformation: same input, same output, no side effects. When `map` is used to trigger side effects, the pipeline becomes unpredictable and harder to test.

```java
// Side effects buried in a map call
List<User> processed = users.stream()
    .map(user -> {
        auditLog.record("processing " + user.id()); // side effect
        metricsCollector.increment("users.processed"); // another side effect
        return user.withStatus(Status.PROCESSED);
    })
    .toList();
```

The problems compound if the stream ever becomes parallel, lazy, or composed with other operations. The audit log and metrics calls may fire in an unexpected order, or multiple times if the stream is replayed.

Side effects belong at the boundary, not in the middle of a pipeline. If you need to run a side effect for each element, use `peek` explicitly (which signals "this is a side effect step"), or move the side-effecting code out of the pipeline:

```java
// Side effects declared at the boundary, pipeline remains pure
List<User> processed = users.stream()
    .map(user -> user.withStatus(Status.PROCESSED))
    .toList();

// Side effects run after the pure transformation is complete
processed.forEach(user -> {
    auditLog.record("processed " + user.id());
    metricsCollector.increment("users.processed");
});
```

Or, if you need to keep it in one pass:

```java
// peek makes the side-effectful step explicit
List<User> processed = users.stream()
    .map(user -> user.withStatus(Status.PROCESSED))
    .peek(user -> auditLog.record("processed " + user.id()))
    .peek(user -> metricsCollector.increment("users.processed"))
    .toList();
```

`peek` is not hidden — it is a declared side-effect stage. Anyone reading the pipeline knows exactly where state escapes.

---

## 10. Confusing Fail-Fast `Result` with Accumulating `Validated`

The last anti-pattern is choosing the wrong container for the semantics you need.

```java
// Reporting one error at a time when you need all of them
public Result<RegistrationRequest, String> validate(RegistrationRequest req) {
    if (req.email().isBlank())       return Result.err("email is required");
    if (!isValidEmail(req.email()))  return Result.err("email is invalid");
    if (req.password().length() < 8) return Result.err("password too short");
    if (req.name().isBlank())        return Result.err("name is required");
    return Result.ok(req);
}
```

This returns the *first* error. A user who submits a form with a blank name, an invalid email, and a short password will see one error message, fix it, resubmit, see another, fix that, resubmit, and so on. That is a poor UX — and a sign that fail-fast `Result` is the wrong abstraction for this use case.

`Result` is fail-fast by design: the first `Err` short-circuits the chain. When you need to collect *all* errors, use `Validated`:

```java
// All validation errors collected in one pass
public Validated<List<String>, RegistrationRequest> validate(RegistrationRequest req) {
    // Each field validated independently — each can be Valid or Invalid
    Validated<List<String>, String> emailV = req.email().isBlank()
        ? Validated.invalid(List.of("email is required"))
        : Validated.valid(req.email());

    Validated<List<String>, String> passwordV = req.password().length() < 8
        ? Validated.invalid(List.of("password too short"))
        : Validated.valid(req.password());

    Validated<List<String>, String> nameV = req.name().isBlank()
        ? Validated.invalid(List.of("name is required"))
        : Validated.valid(req.name());

    BinaryOperator<List<String>> merge =
        (a, b) -> Stream.concat(a.stream(), b.stream()).toList();

    // combine(other, errMerge, valueMerge): errors accumulate, values compose
    return emailV
        .combine(passwordV, merge, (e, p) -> e)
        .combine(nameV,     merge, (ep, n) -> req);
}
// Submit with blank email + short password + blank name:
// → Invalid(["email is required", "password too short", "name is required"])
// All three errors at once — no resubmit loop.
```

The distinction matters:

| Semantics                                        | Right container   |
|--------------------------------------------------|-------------------|
| Stop at the first failure, use its error         | `Result<V, E>`    |
| Collect all failures, report them all            | `Validated<E, A>` |
| Stop at the first failure, capture the exception | `Try<V>`          |

Using `Result` for form validation or `Validated` for a sequential pipeline are both correct in isolation and subtly wrong in context.

---

## Putting It Together

Most of these anti-patterns share a common root: **treating functional types as syntax sugar for null checks and try/catch**, rather than as a different model for composition and error handling.

The shift in thinking is:

- **Containers carry contracts.** `Option<T>` means "no value is a valid outcome." `Result<V,E>` means "failure is a typed value." `Try<V>` means "this computation might throw." Do not use one where another's semantics apply.
- **Transformations are pure.** `map` answers a question: "given this value, produce that value." Side effects belong at the edges of the pipeline, not hidden inside a mapper.
- **Errors are not silenced.** Absorbing an exception into `Optional.empty()` or `null` is not error handling. It is deferring the bug to whoever calls you next.
- **Composition requires honesty.** If a step can fail, its return type must say so. If it cannot fail, wrapping it in `Result` is noise. Precision in types is what makes pipelines composable.

None of this requires category theory. It requires the discipline to mean what your types say — and the same care with functional code that good engineers bring to any other design decision.

---

## A Note on dmx-fun

The `Option<T>`, `Result<V, E>`, `Try<V>`, and `Validated<E, A>` types in **dmx-fun** are designed to make these patterns natural:

- `@NullMarked` throughout — returning `null` from a mapper is a compile-time warning.
- `flatMap` on all container types — no accidental double-wrapping.
- `Try.of()` and `Try.run()` — the idiomatic way to capture throwing computations without hiding the exception.
- `Validated` for accumulating errors — the right tool is available so you do not have to reach for the wrong one.

The library will not prevent every anti-pattern listed here — some of them, like impure lambdas and `isPresent()` abuse, are habits that no type system fully enforces. But it does make the correct patterns the path of least resistance.
