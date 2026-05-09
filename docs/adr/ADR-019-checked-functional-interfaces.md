---
number: 19
title: "CheckedFunction, CheckedSupplier, CheckedRunnable, CheckedConsumer as first-class interfaces"
status: Accepted
date: 2026-05-09
---

## Context

Java's standard functional interfaces — `Function`, `Supplier`, `Runnable`,
`Consumer` — do not declare checked exceptions. Code that calls checked-exception
APIs (JDBC, I/O, JNDI) cannot be used directly as lambdas or method references
in `Try.of`, `map`, or `flatMap` without a manual `try/catch` wrapper at each
call site, which obscures intent.

## Decision

Provide `CheckedFunction<T, R>`, `CheckedSupplier<T>`, `CheckedRunnable`, and
`CheckedConsumer<T>` as `@FunctionalInterface` types whose single abstract method
declares `throws Exception`:

| Interface               | Checked equivalent of | Signature                           |
|-------------------------|-----------------------|-------------------------------------|
| `CheckedFunction<T, R>` | `Function<T, R>`      | `R apply(T t) throws Exception`     |
| `CheckedSupplier<T>`    | `Supplier<T>`         | `T get() throws Exception`          |
| `CheckedRunnable`       | `Runnable`            | `void run() throws Exception`       |
| `CheckedConsumer<T>`    | `Consumer<T>`         | `void accept(T t) throws Exception` |

The SAM is `throws Exception` rather than `throws Throwable`. This allows checked
exceptions and `RuntimeException` to propagate through lambda boundaries without
compilation errors, while `Error` (which signals JVM-level failures outside normal
error handling) is not part of the declared surface. In practice, `Try.of` and
`Try.run` catch `Throwable` internally, so an `Error` thrown inside the supplier
or runnable is still captured as a `Failure` — but this is a `Try` implementation
detail, not a contract of the `Checked*` interfaces themselves.

`CheckedSupplier<T>` and `CheckedRunnable` are the primary entry points for `Try`:
- `Try.of(CheckedSupplier)` executes the supplier and wraps any thrown exception
  as a `Failure`.
- `Try.run(CheckedRunnable)` executes the runnable and wraps any thrown exception
  as a `Failure(Throwable)`, returning `Success(null)` on completion.
- `Try.withTimeout(Duration, CheckedSupplier)` runs the supplier on a virtual
  thread with a deadline (documented in ADR-013).

`CheckedFunction<T, R>` and `CheckedConsumer<T>` are the primary API for
`Resource<T>`:
- `Resource.use(CheckedFunction)` opens the resource, passes it to the function
  body, and closes it — all within a single `Try<R>`.
- The `release` parameter of `Resource.of(CheckedSupplier, CheckedConsumer)`
  accepts a `CheckedConsumer` for the teardown action.

All four interfaces are `@NullMarked` (jspecify), consistent with the rest of
the library's null safety policy.

## Consequences

**Positive:**

- Throwing method references and lambdas can be passed directly to `Try.of`,
  `Try.run`, `Resource.use`, and other functional APIs without manual
  `try/catch` wrappers at every call site.
- Consistent with the library's goal of capturing exceptions as values rather
  than propagating them through imperative control flow.
- Each interface is `@FunctionalInterface` — lambdas and method references work
  directly without additional adapters.

**Negative / tradeoffs:**

- Four additional public interfaces increase the API surface.
- `throws Exception` is broad — the signature alone does not reveal which specific
  checked exceptions the implementation may throw. Callers relying only on the
  interface type cannot statically determine the exception domain.
- Introducing `CheckedConsumer` into `Resource.of` means a release action that
  throws is captured as a `Failure`, which may be surprising if callers expect
  teardown to be infallible.

## Alternatives considered

- **Sneaky-throws (Lombok `@SneakyThrows`):** hides checked exceptions from
  callers at the bytecode level; breaks caller assumptions and requires an
  external annotation processor dependency.
- **Wrapper lambda at each call site:** boilerplate at every use of `Try.of` or
  `map`; defeats the purpose of ergonomic API design.
- **`UncheckedIOException` and siblings:** only covers specific exception families;
  not a general solution.
- **`throws Throwable` on the SAM:** wider than needed for the checked-exception
  use case; `Error` subclasses signal unrecoverable JVM conditions and are not
  part of the normal exception-as-value pattern. `throws Exception` is the
  conventional boundary.
