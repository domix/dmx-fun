---
title: "Why You Should Upgrade to Java 25"
description: "Java 25 is the next Long-Term Support release and the most significant Java upgrade since Java 21. Here is a practical look at what it delivers, why LTS matters for production codebases, and how a functional-style library like dmx-fun benefits from the new language features."
pubDate: 2026-03-18
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Guide"
tags: ["Java 25", "LTS", "Upgrade", "Structured Concurrency", "Virtual Threads", "Pattern Matching", "Functional Programming"]
image: "https://images.unsplash.com/photo-1763568258672-7f0b6d2aeaf2?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Daniil Komov"
    authorUrl: "https://unsplash.com/@dkomow"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/portatil-abierto-con-codigo-y-taza-de-cafe-sobre-el-escritorio-Qpj1LAgh4bY"
---

Java 25 landed in September 2025 as the first Long-Term Support release since Java 21. That alone makes it special — but the real story is what it finalizes. Features that have been previewing and incubating across Java 22, 23, and 24 graduate to stable APIs in one release. If you are running Java 21 LTS today, the jump to Java 25 delivers meaningful improvements to concurrency, pattern matching, the module system, and performance — without breaking anything you have already written.

> **March 17, 2026:** Java 26 was released. It is a standard, non-LTS release with a six-month support window. It continues maturing several features still in preview — including Structured Concurrency and Scoped Values. If you are choosing a stable foundation for a new project, Java 25 LTS remains the right target.

This post walks through what matters most, why the LTS label changes your calculus, and how functional-style code in particular benefits.

---

## Why LTS Status Matters More Than You Think

Every six months, Java ships a new release. Most teams skip non-LTS versions in production — the support window is too short to justify the migration cost, and their frameworks, build tools, and container images take time to certify. The result is that a large portion of the Java ecosystem accumulates features in preview mode for two to four years before they become safe to build on.

Java 25 breaks that logjam. The features that have been marked "preview" since Java 21 or 22 are now stable, supported, and available in every LTS-following JDK distribution. You are no longer experimenting — you are writing production-grade code.

From a maintenance perspective:

- **Free update support** from Oracle extends for several years after GA.
- **Vendor distributions** (Temurin, Corretto, Liberica, etc.) all publish Java 25 images on release day.
- **Frameworks** (Spring Boot, Quarkus, Micronaut) target their LTS-aligned releases to Java 25, so ecosystem compatibility improves sharply.

The practical message: if you are on Java 21, upgrade now. If you are on an interim release (22, 23, 24), upgrade now. The groundwork is done.

---

## Structured Concurrency — Maturing Preview

Structured concurrency has been in preview since Java 21 and continues to evolve in Java 25. It is not yet a stable API — you still need `--enable-preview` to use it — but each release refines the design based on community feedback, and it is steadily converging. The core idea is simple and powerful: a unit of work that spawns multiple concurrent subtasks should also own their lifetime. When the parent scope exits — normally or via exception — all subtasks are cleaned up automatically.

```java
// Java 25 — still requires --enable-preview
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<User>   user    = scope.fork(() -> fetchUser(id));
    Future<Config> config  = scope.fork(() -> loadConfig());

    scope.join().throwIfFailed();

    return process(user.resultNow(), config.resultNow());
}
// Both tasks are guaranteed to be finished or cancelled here
```

This composability maps naturally onto functional pipelines. Where you would previously write a chain of `CompletableFuture.thenCompose` calls to model parallel steps, structured concurrency gives you lexically scoped concurrency — easier to read, safer to reason about, and impossible to leak.

### Why it matters for functional code

Structured concurrency and `Result`/`Try` compose well together. A parent scope can gather results from forked tasks and then apply functional error handling on the collected values:

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<Result<Invoice, BillingError>> invoice =
        scope.fork(() -> billingService.generate(orderId));
    Future<Result<Receipt, ReceiptError>> receipt =
        scope.fork(() -> receiptService.create(orderId));

    scope.join().throwIfFailed();

    return invoice.resultNow()
        .flatMap(inv -> receipt.resultNow().map(rec -> new Confirmation(inv, rec)));
}
```

No `CompletableFuture` chaining, no thread management, no leaked tasks. The structure of the code reflects the structure of the computation. Even in preview, this pattern is worth understanding now — it will finalize in a future release and the mental model does not change.

---

## Scoped Values — Maturing Preview

Like structured concurrency, scoped values remain a preview feature in Java 25. They replace `ThreadLocal` for passing contextual data down a call chain and continue to be refined alongside structured concurrency (the two are designed to work together). Where `ThreadLocal` survives for the lifetime of a thread (and requires manual cleanup), a `ScopedValue` is bound for a delimited scope and automatically unbound when that scope exits.

```java
private static final ScopedValue<RequestContext> REQUEST_CTX = ScopedValue.newInstance();

// At the entry point (e.g., a servlet filter or gRPC interceptor)
ScopedValue.where(REQUEST_CTX, new RequestContext(traceId, userId))
    .run(() -> orderService.process(request));

// Deep in the call stack — no parameter threading required
RequestContext ctx = REQUEST_CTX.get();
```

For functional code, scoped values are the right answer to the "how do I pass logging context / tenant ID / trace ID through a pure function pipeline without polluting every method signature?" problem. The answer is: you don't. You bind it once at the edge and let the scope carry it.

---

## Primitive Types in Patterns — A Functional Developer's Win

Pattern matching for `switch` became stable in Java 21. Java 25 extends it to primitive types, closing the last awkward gap where you had to box `int`, `long`, or `double` to participate in pattern-matching expressions.

```java
// Java 25 — primitive patterns in switch
String describe(Object value) {
    return switch (value) {
        case int i when i < 0    -> "negative integer";
        case int i               -> "non-negative integer: " + i;
        case double d            -> "double: " + d;
        case String s            -> "string: " + s;
        case null                -> "null";
        default                  -> "other";
    };
}
```

In a functional codebase where you frequently fold over sealed types with mixed primitive and object components, this removes the last class of implicit boxing that polluted otherwise clean algebraic-style code.

Combined with record patterns, you can now destructure deeply nested records in a single `switch` expression:

```java
sealed interface Shape permits Circle, Rectangle {}
record Circle(double radius)          implements Shape {}
record Rectangle(double width, double height) implements Shape {}

double area(Shape s) {
    return switch (s) {
        case Circle(double r)           -> Math.PI * r * r;
        case Rectangle(double w, double h) -> w * h;
    };
}
```

---

## Module Import Declarations

Java 25 finalizes module import declarations, which let you import all the exported packages of a module in a single line:

```java
import module java.base;
import module dmx.fun;

// All exported types from both modules are now directly available
Option<String> value = Option.some("hello");
Result<Integer, String> result = Result.ok(42);
```

For library authors, this is a quality-of-life improvement that makes demo code, examples, and blog posts significantly less cluttered. For users of `dmx-fun`, it means the full API surface of the library is available with a single import statement instead of a dozen type-specific ones.

---

## Virtual Threads — Now the Default

Virtual threads were introduced as a preview in Java 19 and stabilized in Java 21. In Java 25, the JVM infrastructure around them has matured significantly:

- The JDK's internal thread pinning — which caused virtual threads to block the carrier thread during `synchronized` blocks — has been substantially reduced.
- Carrier thread counts are managed more aggressively, reducing overhead in high-concurrency scenarios.
- Profiling and monitoring tools now surface virtual thread information cleanly.

For most applications, the recommendation in Java 25 is straightforward: use virtual threads for all I/O-bound work. The thread-per-request model scales to millions of concurrent operations without tuning a thread pool.

```java
// Java 25 — virtual threads are the right default for I/O
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

Combined with structured concurrency (still in preview but steadily maturing), virtual threads represent the most significant shift in Java concurrency since the introduction of `java.util.concurrent` in Java 5.

---

## Performance Improvements

Beyond the API surface, Java 25 includes several JVM-level improvements that reduce overhead in common patterns:

- **ZGC generational mode** is now the default GC for low-latency workloads. Pause times under 1 ms at any heap size, with significantly improved throughput for short-lived objects — exactly the profile of functional-style code that favors value creation over mutation.
- **Compact object headers** (finalized from experimental status) reduce object size by 8 bytes in most cases, improving cache density in record-heavy codebases.
- **JIT improvements** continue to optimize pattern matching, switch expressions, and record accessor calls — common operations in functional pipelines.

---

## Upgrading in Practice

For a Gradle project, the change is three lines:

```kotlin
// lib/build.gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
```

```yaml
# .github/workflows/gradle.yml
- uses: actions/setup-java@v4
  with:
    java-version: '25'
    distribution: 'temurin'
```

For Maven:

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
</properties>
```

In most cases, existing code compiled on Java 21 or 24 recompiles on Java 25 without changes. The removals and deprecations in this release cycle affect internal APIs (`sun.misc.Unsafe` memory-access methods are now deprecated for removal) and Windows 32-bit x86 support (removed), neither of which affects typical application code.

---

## What This Means for dmx-fun

**dmx-fun** is already compiled and tested on Java 25. The library itself benefits directly:

- **Structured concurrency integration** will inform the planned `CompletableFuture` adapters (issue [#49](https://github.com/domix/dmx-fun/issues/49)) — the API can be designed from the start to compose with `StructuredTaskScope` rather than treating `CompletableFuture` as the primary concurrency primitive.
- **Primitive patterns** simplify internal switch expressions and make example code cleaner.
- **Module import declarations** make the library more ergonomic to use in exploratory or example code.
- **ZGC generational mode** improves the performance profile of functional pipelines that create many short-lived `Result`, `Option`, and `Try` wrapper objects.

---

## Conclusion

Java 25 is not just an incremental release — it is the culmination of four years of careful evolution since Java 21. Primitive patterns and module import declarations land stable, virtual threads mature further, and performance improvements in the GC and JIT reduce the overhead that historically made functional-style code feel expensive. Structured concurrency and scoped values continue as preview features, steadily converging toward finalization in a future release.

If you are on Java 21 and have been waiting for a clear signal to move: this is it.

If you are on a non-LTS release: Java 25 is the stable foundation your next project deserves.

---

*The dmx-fun library requires Java 25+. Add it to your build:*

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun:0.0.15")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun</artifactId>
  <version>0.0.15</version>
</dependency>
```

*Full Javadoc at [/dmx-fun/javadoc/](/dmx-fun/javadoc/index.html).*
