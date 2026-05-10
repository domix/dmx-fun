---
number: 21
title: "Resource<T> as composable managed resource — alternative to try-with-resources"
status: Accepted
date: 2026-05-09
---

## Context

`try-with-resources` works well for a single `AutoCloseable` but becomes awkward when
multiple resources must be acquired sequentially, composed, or conditionally released.
It cannot be stored, passed, or reused across call sites as a value, and it cannot
integrate with the library's typed error model (`Try`, `Result`, `Either`).
Functional pipelines cannot express resource acquisition and release as composable values.

## Decision

Provide `Resource<T>` — a `final` class that pairs an acquisition function with a
guaranteed release action. The resource is only live during the execution of `use(fn)`:
the resource is acquired just before the body runs, and the release function is
**always** called when the body completes, whether it succeeds or throws.

**Factories:**

- `Resource.of(CheckedSupplier<? extends T> acquire, CheckedConsumer<? super T> release)` —
  the primary factory; acquires and releases on every `use()` call.
- `Resource.fromAutoCloseable(CheckedSupplier<? extends T> acquire)` —
  convenience wrapper for `AutoCloseable` types; uses `AutoCloseable::close` as the
  release function.
- `Resource.eval(Try<? extends T> acquired, CheckedConsumer<? super T> release)` —
  wraps a pre-computed `Try<T>`; if the `Try` is already a failure, `use()` returns
  that failure immediately and `release` is never called. One-shot contract: calling
  `use()` more than once releases the same value; prefer `of()` when reuse is required.

**Core operations:**

- `use(CheckedFunction<? super T, ? extends R> body)` → `Try<R>` — acquires, runs the
  body, releases, and returns the result. Both success and failure are captured as values.
- `useAsResult(body, onError)` → `Result<R, E>` — like `use()`, but the body returns a
  `Result` directly; any `Throwable` from acquire, release, or an unexpected body
  exception is mapped to `E` via `onError`, eliminating `Try<Result<R,E>>` nesting.
- `useAsEither(body, onError)` → `Either<E, R>` — symmetric with `useAsResult()`
  for code that models results as neutral `Either` values.

**Transformations (composition without nesting):**

- `map(Function<? super T, ? extends R> fn)` → `Resource<R>` — transforms the resource
  value without changing acquire/release; if `fn` throws, the resource is still released.
- `flatMap(Function<? super T, ? extends Resource<R>> fn)` → `Resource<R>` — sequences
  two resources; both are released in **reverse acquisition order** (inner first, then
  outer), mirroring nested `try-with-resources` semantics.
- `mapTry(Function<? super T, ? extends Try<? extends R>> fn)` → `Resource<R>` — like
  `map()`, but the mapping function returns a `Try<R>`; useful when the transformation
  is itself a fallible operation (e.g., parsing or validation).

**Exception-merging contract** (matches `try-with-resources`):

| Body    | Release | Outcome                                                               |
|---------|---------|-----------------------------------------------------------------------|
| Success | Success | `Try.success(result)`                                                 |
| Success | Throws  | `Try.failure(releaseException)`                                       |
| Throws  | Success | `Try.failure(bodyException)`                                          |
| Throws  | Throws  | `Try.failure(bodyException)` — release exception **suppressed** onto body |

The body exception always takes priority; the release exception is suppressed (via
`Throwable.addSuppressed`), not discarded. This is identical to the JDK behaviour for
`try-with-resources`.

**Internal design:** `Resource<T>` wraps a private `Effect<T>` interface whose single
method is `<R> Try<R> run(CheckedFunction<? super T, ? extends R> body)`. Because this
method carries its own type parameter `<R>`, it cannot be implemented by a lambda
(Java lambdas cannot introduce new type parameters). Anonymous class instances are used
throughout instead. The `CheckedFunction`, `CheckedSupplier`, and `CheckedConsumer`
types are the API surface defined in
[ADR-019 — Checked functional interfaces](https://domix.github.io/dmx-fun/adr/adr-019-checked-functional-interfaces/).

## Consequences

**Positive:**

- Resource acquisition and release are expressed as values — testable, storable, and
  composable across call sites.
- `flatMap` chains resources whose lifetimes overlap without nested `try` blocks; the
  release order is deterministic (reverse acquisition).
- `use()` is independent on each call: the same `Resource<T>` can be reused — each
  invocation goes through a full acquire/run/release cycle (for `of`/`fromAutoCloseable`).
- Typed integration: `useAsResult` and `useAsEither` eliminate the `Try<Result<R,E>>`
  nesting that would otherwise appear at domain service boundaries.
- Exception behaviour is identical to `try-with-resources` — no surprises for teams
  already familiar with JDK resource handling.

**Negative / tradeoffs:**

- Less familiar than `try-with-resources`; requires developers to understand the
  acquire/use/release value model.
- `eval()` has a one-shot contract that is easy to violate: calling `use()` more than
  once on an `eval`-backed resource releases the same underlying value each time.
- The internal `Effect<T>` anonymous-class pattern (required by the generic method) is
  more verbose than a lambda-based design would be.
- `flatMap` uses a `sneakyThrow` to re-propagate inner failures through the outer
  resource's lifecycle without wrapping them — the JVM sees the original throwable,
  but the technique relies on type-erasure and requires a `@SuppressWarnings("unchecked")`.

## Alternatives considered

- **`try-with-resources`:** idiomatic Java, guaranteed release, but not composable as a
  value; cannot be stored, passed, or chained without introducing nesting.
- **`CompletableFuture` with `whenComplete`:** asynchronous semantics overpowers
  synchronous resource management; adds complexity and thread-switching overhead.
- **Loan pattern (callback only):** similar to `Resource.use`, but the resource itself
  cannot be stored and composed as a first-class value before calling `use`.
- **Making `Resource<T>` a record or sealed interface:** `Effect<T>` requires a method
  with its own type parameter `<R>`, which Java lambdas cannot implement; an interface
  or record cannot carry anonymous-class implementations in the same ergonomic way.
