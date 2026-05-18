---
title: "Lazy Evaluation: When It Helps and When It Complicates Things"
description: "Lazy evaluation defers a computation until its result is actually needed. That single idea eliminates wasted work, enables safe default values, and defers heavy initialization — but it also introduces traps around side effects, debugging, and error timing that you need to know before reaching for it."
pubDate: 2026-05-19
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Lazy", "Performance", "Best Practices"]
image: "https://images.pexels.com/photos/3729557/pexels-photo-3729557.jpeg"
imageCredit:
    author: "Nothing Ahead"
    authorUrl: "https://www.pexels.com/@ian-panelo/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/photo-of-woman-leaning-on-wooden-table-3729557/"
---

Lazy evaluation means exactly what it sounds like: do not compute a value until someone asks
for it.

That single idea has practical consequences that go beyond performance. Lazy evaluation lets
you define what a computation *is* without triggering it, compose computations that might never
run, and defer expensive initialization until the moment it is actually needed — not a
millisecond earlier.

It also introduces a set of traps that catch developers by surprise: side effects that fire at
unpredictable times, errors that surface far from their origin, and memoization behavior that
defies the caller's expectations.

This post works through both sides — when lazy evaluation is the right tool, and when it is not.
It ends with a look at how dmx-fun's `Lazy<T>` type implements these guarantees in a form
that is safe to use in production Java backends.

---

## What Eager Evaluation Costs You

Before arguing for lazy evaluation, it helps to be specific about what eager evaluation costs.

```java
public Report generateReport(DateRange range, boolean includeCharts) {
    ReportData   data   = fetchData(range);          // always runs — DB call
    List<Chart>  charts = buildCharts(data);          // always runs — CPU intensive
    String       pdf    = renderPdf(data, charts);    // always runs — serialization

    if (!includeCharts) {
        return Report.textOnly(data);                 // charts were wasted
    }
    return Report.full(data, charts, pdf);
}
```

`buildCharts` and `renderPdf` always execute, even when the caller passes `includeCharts =
false`. The work is done regardless of whether the result is used. In a hot path this wastes
CPU; in a service with variable load it wastes both CPU and memory; in a test suite it wastes
time on every invocation.

The eager version is simple to read. But it encodes an assumption — that every intermediate
result will be needed — that may not be true.

---

## When Lazy Evaluation Helps

### 1. Avoiding work that may never be needed

The canonical case. Wrap a computation in a supplier and evaluate it only when the branch that
needs it is reached.

```java
public Report generateReport(DateRange range, boolean includeCharts) {
    ReportData data = fetchData(range);

    Supplier<List<Chart>> charts = () -> buildCharts(data);   // not called yet
    Supplier<String>      pdf    = () -> renderPdf(data, charts.get()); // not called yet

    if (!includeCharts) {
        return Report.textOnly(data);   // charts and pdf never run
    }
    return Report.full(data, charts.get(), pdf.get());
}
```

`buildCharts` and `renderPdf` only run when the full report is actually requested. The
text-only path pays only for `fetchData`.

### 2. Default values that are expensive to produce

This is one of the most common lazy evaluation pitfalls in Java, and it appears in a form that
looks harmless.

```java
// Eager default — loadFromDisk() always runs, even if config is present
Config effective = optional.orElse(Config.loadFromDisk());

// Lazy default — loadFromDisk() runs only when optional is empty
Config effective = optional.orElseGet(() -> Config.loadFromDisk());
```

`Optional.orElse(T)` takes a value — the `Config.loadFromDisk()` call happens unconditionally
before `orElse` is even invoked. `Optional.orElseGet(Supplier<T>)` takes a supplier — the
load only happens if the `Optional` is empty. The difference is invisible in the method
signature but significant at runtime.

The same pattern applies to `Option.getOrElse` vs `Option.getOrElseGet` in dmx-fun:

```java
// Eager — always calls the fallback, even when the value is present
String name = option.getOrElse(computeExpensiveDefault());

// Lazy — calls the supplier only when the Option is None
String name = option.getOrElseGet(() -> computeExpensiveDefault());
```

If you are passing an expression (not a literal) to `getOrElse`, `orElse`, or any similar
fallback method, question whether that expression is cheap. If it is not, use the supplier
overload.

### 3. Deferring application startup cost

Heavy initialization — loading config files, establishing database connections, warming caches —
blocks application startup even when only a fraction of the initialized resources will be used
during a given run. Lazy initialization moves that cost to first use.

```java
public class AppContext {

    private static final Lazy<AppConfig> CONFIG =
        Lazy.of(() -> AppConfig.loadFromDisk(Paths.get("config/app.yaml")));

    private static final Lazy<DataSource> DATA_SOURCE =
        CONFIG.flatMap(cfg -> Lazy.of(() -> DataSource.connect(cfg.dbUrl())));

    private static final Lazy<FeatureFlags> FLAGS =
        CONFIG.map(cfg -> FeatureFlags.from(cfg.featureSection()));

    public static AppConfig    config()     { return CONFIG.get(); }
    public static DataSource   dataSource() { return DATA_SOURCE.get(); }
    public static FeatureFlags flags()      { return FLAGS.get(); }
}
```

`CONFIG` is evaluated only when first accessed. `DATA_SOURCE` depends on `CONFIG` but neither
is evaluated until `dataSource()` is called. A startup path that only checks feature flags
never touches the database.

### 4. Short-circuit evaluation — the case you already use every day

Java's `&&` and `||` operators are lazy. The right-hand side is not evaluated if the result is
already determined by the left-hand side.

```java
if (user != null && user.isActive()) {
    // user.isActive() is only called if user != null
}

if (cache.contains(key) || expensiveLoad(key)) {
    // expensiveLoad is only called when the key is not in the cache
}
```

This is lazy evaluation built into the language. Every conditional short-circuit is a form of
deferred computation. The explicit lazy patterns described elsewhere in this post generalize
this idea to arbitrary computations, not just boolean expressions.

### 5. Composing computations before committing to them

Lazy evaluation lets you build a pipeline of transformations without triggering any of them.
The pipeline is a description of what will happen, not the execution of it.

```java
Lazy<Config> config = Lazy.of(() -> Config.loadFromDisk());

// Neither of these calls loadFromDisk or extracts the port
Lazy<String> host = config.map(Config::host);
Lazy<Integer> port = config.map(Config::port);

boolean stillInert = !config.isEvaluated(); // true

// Only here does the supplier run — once, and the result is cached
String h = host.get();   // evaluates config, extracts host
Integer p = port.get();  // reuses the already-evaluated config
```

This is useful when the same expensive computation feeds multiple downstream values. The
supplier runs exactly once regardless of how many `map`/`flatMap` chains reference it.

---

## When Lazy Evaluation Complicates Things

### 1. Side effects at unpredictable times

A lazy computation that has a side effect will fire that side effect at a time determined by
the caller, not by the code that defined the computation. That disconnect is dangerous.

```java
// The email is sent when .get() is called — not when this line is reached
Lazy<Void> notification = Lazy.of(() -> {
    emailService.sendWelcome(user);
    return null;
});

// ...fifty lines later, in a different context...
notification.get();   // email fires here — and only here, and only once
notification.get();   // nothing happens — memoized
```

Two problems: first, the email fires at a point that may be far from the intent. Second, it
fires exactly once — a caller who expects it to re-send on every `.get()` call will be
surprised. Lazy memoization is a feature for pure computations. It is a trap for side effects.

**Rule:** if the supplier has observable side effects (writes, network calls, logging), do not
put it in a `Lazy`. Use a `Supplier` and call it directly, or use a method reference and call
that directly. Reserve `Lazy` for pure computations that produce a value.

### 2. Errors surface far from their origin

If the supplier throws, the exception is raised at the point of `.get()`, not at the point
where `Lazy.of(...)` was called. In a stack trace, the origin of the `Lazy` may be invisible.

```java
// Defined here — no exception yet
Lazy<Config> config = Lazy.of(() -> Config.loadFromDisk("missing.yaml"));

// Many frames later...
String host = config.get();   // FileNotFoundException thrown here
```

The stack trace points to the `.get()` call site. The definition of the `Lazy` — where the
decision to load from `"missing.yaml"` was made — is absent from the trace. In a large
codebase this makes debugging slower.

dmx-fun's `Lazy<T>` addresses this partially through exception memoization: if the supplier
throws, the exception is captured and rethrown on every subsequent `.get()` call, so the
computation is not retried silently. But the origin gap in the stack trace is inherent to the
pattern.

When debugging is a concern, prefer conversions to `Try` or `Result` at the point of access:

```java
// Forces evaluation and captures the exception as a Failure — no surprise NPE or unchecked throw
Try<Config> config = Lazy.of(() -> Config.loadFromDisk("app.yaml")).toTry();

Result<Config, AppError> safeConfig = Lazy.of(() -> Config.loadFromDisk("app.yaml"))
    .toResult(ex -> new AppError("Config load failed: " + ex.getMessage(), ex));
```

Now the failure is a typed value that the caller must handle explicitly, and the error message
can include enough context to compensate for the stack trace gap.

### 3. Thread safety is your responsibility with plain `Supplier`

A bare `Supplier<T>` used as a lazy initializer is not thread-safe. Two threads can both find
the value uninitialized and both execute the supplier, producing two instances, with neither
thread seeing the other's result.

```java
// Not thread-safe
private Supplier<HeavyResource> resource = () -> {
    HeavyResource r = new HeavyResource();
    resource = () -> r;   // replace with constant — but this is a race
    return r;
};
```

This kind of hand-rolled lazy initialization needs explicit synchronization, double-checked
locking, or `AtomicReference` to be safe in a concurrent environment. Getting it wrong is
easy and silent.

### 4. Memoization means one evaluation — forever

Lazy memoization means the value is computed once and cached. If the supplier reads from a
mutable source, the cached value may become stale.

```java
Lazy<FeatureFlags> flags = Lazy.of(() -> featureFlagService.load());

flags.get().isEnabled("new-checkout"); // reads flags from service — cached
// ... someone updates the feature flag in production ...
flags.get().isEnabled("new-checkout"); // still reads the cached, now-stale flags
```

If a value changes at runtime and callers need to see the latest version, `Lazy` is the wrong
tool. Use a method that re-fetches on every call, or a `Supplier` that re-evaluates.

---

## `Lazy<T>` in dmx-fun

dmx-fun's `Lazy<T>` is a production-ready implementation of the memoized lazy value with the
guarantees you need to use it safely in a backend service.

**Thread safety.** The supplier runs exactly once, even under concurrent access. The
implementation uses double-checked locking internally — you do not need to add
synchronization at the call site.

**Exception safety.** If the supplier throws, the exception is captured and rethrown on every
subsequent `.get()` call. The computation is never retried silently, and partial initialization
is not possible.

**`map` and `flatMap` without forcing evaluation.** Transformations are themselves lazy — they
compose without triggering the supplier.

```java
Lazy<Config>     config  = Lazy.of(() -> Config.loadFromDisk());
Lazy<String>     host    = config.map(Config::host);         // supplier not called
Lazy<DataSource> ds      = config.flatMap(                   // supplier not called
    cfg -> Lazy.of(() -> DataSource.connect(cfg.dbUrl()))
);

// Nothing has run yet
String h = host.get();   // supplier runs here — once
DataSource live = ds.get(); // reuses cached config result
```

**`isEvaluated()`.** Lets you inspect whether the value has been computed without triggering
computation.

**Null safety.** `Lazy<T>` is `@NullMarked`. The supplier must not return `null` — doing so
produces a `NullPointerException` on `.get()`. For nullable results, use `Lazy<Option<T>>`:

```java
Lazy<Option<Config>> maybeConfig = Lazy.of(() -> Option.ofNullable(tryLoadConfig()));
```

**Interoperability.** `Lazy<T>` converts to `Try`, `Result`, `Either`, and `Option` — each
conversion forces evaluation and handles exceptions in the way appropriate for that type.

```java
Lazy<Config> config = Lazy.of(() -> Config.loadFromDisk("app.yaml"));

Try<Config>              asTry    = config.toTry();
Result<Config, Throwable> asResult = config.toResult();
Either<Throwable, Config> asEither = config.toEither();

// With error mapping
Result<Config, AppError> safeResult =
    config.toResult(ex -> new AppError("Config unavailable", ex));
```

---

## Decision Guide: When to Use `Lazy<T>`

| Situation | Use `Lazy<T>`? |
|---|---|
| Expensive computation that may not be needed | Yes |
| Shared expensive resource initialized once per JVM | Yes |
| Chain of computations depending on the same root value | Yes |
| Value that changes at runtime | No — use a method or `Supplier` |
| Computation with side effects | No — side effects do not compose with memoization |
| Value that is always needed on the happy path | Probably not — eager is simpler |
| Fallback default in `getOrElse` | Use `getOrElseGet` with a supplier instead |

The underlying question is: is this computation *pure* (same inputs always produce the same
output, no observable side effects) and *potentially unnecessary*? If both answers are yes,
`Lazy<T>` is a natural fit. If the computation has side effects or is always needed,
memoization is an obstacle, not a benefit.

---

## Conclusion

Lazy evaluation is not a performance trick. It is a way of expressing that a computation is
defined separately from its execution — and that the execution should happen at most once, only
when it is actually needed.

That idea helps when:
- Work might be skipped entirely depending on a branch.
- Default values are expensive to produce but rarely needed.
- Startup cost can be deferred to first use.
- Multiple downstream values share a single expensive root computation.

It complicates things when:
- The supplier has side effects — memoization and side effects are a bad combination.
- The value can change at runtime — a cached value goes stale.
- Errors need to be understood quickly — the stack trace points to `.get()`, not to the definition.
- Concurrency is involved and you are not using a thread-safe implementation.

dmx-fun's `Lazy<T>` handles thread safety, exception safety, and interoperability with the
rest of the library. If you reach for lazy evaluation in a Java backend, it is the form that
covers the gaps you would otherwise have to handle yourself.

---

## Further reading

- [Lazy type — Developer Guide](/dmx-fun/guide/lazy) — full API reference with runnable examples
- [Predictable Code with Functional Programming](/dmx-fun/blog/predictable-code-with-fp) — pure functions and immutability as the prerequisites for safe lazy evaluation
- [Higher-Order Functions Explained with Real Examples](/dmx-fun/blog/higher-order-functions-real-examples) — `Supplier<T>` as a minimal form of lazy computation
