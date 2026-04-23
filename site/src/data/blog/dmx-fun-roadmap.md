---
title: "The dmx-fun Roadmap: What's Coming Next"
description: "A personal project born from curiosity about new Java features has grown into a functional programming library worth sharing. Here is where it is heading."
pubDate: 2026-04-06
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Roadmap", "Spring", "Quarkus", "Documentation", "FunctionalProgramming", "Java"]
image: "https://images.unsplash.com/photo-1507925921958-8a62f3d1a50d?q=80&w=1632&auto=format&fit=crop"
imageCredit:
    author: "Kelly Sikkema"
    authorUrl: "https://unsplash.com/@kellysikkema"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com"
---

It started with a simple question: *what can I do with the new features shipping in Java 25?*

I have always loved functional programming. The way it pushes you to think in terms of
transformations, immutability, and composition just makes code easier to reason about. So when
modern Java started shipping features that genuinely enable functional idioms — record patterns,
Stream Gatherers, sealed types, module import declarations — I saw an opportunity I could not
ignore: take the principles I care about and express them using the best of what the language
now offers.

I wanted a playground — something concrete enough to exercise those ideas in practice. I was
not planning to build a library. I was planning to experiment for a weekend.

A few weeks later I had `Option<T>`, `Result<V,E>`, `Try<V>`, `Validated<E,A>`, `Tuple2`,
`Tuple3`, `Tuple4`, `Lazy<T>`, zip combinators, checked functional interfaces, and a test suite
with more than 500 tests. At some point it stopped being a playground and became something I
was actually reaching for in my personal projects. That felt like a signal worth following —
if it was useful to me it might be useful to others.

So here we are. `dmx-fun` is a small, opinionated functional programming library for Java,
built to be read and understood, not just consumed. And this post is about where it is going.

---

## The three milestones ahead

### 0.0.13 — Completeness and documentation (due April 2026)

The immediate priority is making the existing API feel complete and making it
**easy to learn**.

On the API side:

- **`Either<L,R>`** — a proper disjoint union type for representing one of two possible values.
- **`NonEmptyList<T>`** — a list guaranteed to have at least one element, useful in validation
  pipelines.
- **`recover` and `recoverWith` on `Try`** — ergonomic fallback combinators.
- **`orElse(Container)`** on `Option` and `Result` — fallback chaining between containers.
- **Custom AssertJ assertions** for all `dmx-fun` types, so tests read as clearly as the
  production code they verify.
- **`Collectors` for `Validated`** — `partition` and `sequence` as standard stream collectors.
- **Structured Concurrency adapters** — bridging virtual threads and `Try`/`Result`.
- **Jackson module** — first-class JSON serialization for all library types.

On the **documentation** side, this milestone is the biggest investment yet.
A full **Developer Guide** is being written, with dedicated pages for every type in the library:

- `Option<T>` — representing the absence of a value without null
- `Result<V,E>` — typed error handling without exceptions
- `Try<V>` — wrapping computations that may throw
- `Validated<E,A>` — error-accumulating validation
- `Tuple2`, `Tuple3`, `Tuple4` — immutable product types
- `Lazy<T>` — deferred, memoized evaluation
- Checked functional interfaces and higher-arity functions
- Cross-type composition patterns

The goal is that someone new to functional programming in Java can open the guide and understand
not just *how* to use each type but *why* it exists and *when* to reach for it.

---

### 0.0.14 — Spring integration (due May 2026)

The single most-requested integration from my own projects: **making `Result<V,E>` and `Try<V>`
work naturally with Spring's transaction management**.

The plan is a new subproject, `dmx-fun-spring`, that provides:

- **Programmatic transaction support** — utilities for running `Result`- and `Try`-returning
  operations inside a transaction boundary, with automatic rollback on `Err` or `Failure`.
- **Declarative AOP support** — `@TransactionalResult` and `@TransactionalTry` annotations that
  work like Spring's own `@Transactional` but are aware of the functional return types.

The idea is simple: if a method returns `Result.err(...)`, the transaction should roll back —
just as it would for an unchecked exception. Today you have to wire that manually. After 0.0.14
you should not have to think about it.

---

### 0.0.15 — Quarkus integration (due May 2026)

Everything in `dmx-fun-spring` has a Quarkus equivalent. The `dmx-fun-quarkus` subproject will
provide the same two layers:

- **Programmatic transaction support** for `Result` and `Try` using Quarkus's
  `QuarkusTransaction` API.
- **CDI interceptor-based declarative support** via `@TransactionalResult` and
  `@TransactionalTry`, built on top of Quarkus's interceptor model instead of Spring AOP.

Having both integrations in the same repository means the contracts stay aligned and the
experience is consistent regardless of which framework you are using.

---

## The bigger picture

These three milestones are not just a feature list — they represent a shift in what `dmx-fun`
is trying to be.

Until now it has been a core library: types, combinators, and tests. Starting with 0.0.13 it
becomes something you can **learn from**. Starting with 0.0.14 it becomes something you can
drop into a real Spring or Quarkus project **without friction**.

The library started as an experiment. The roadmap is about making it robust enough to be
trusted in production — not by abandoning the simplicity that made it useful to begin with,
but by extending it carefully, one well-scoped milestone at a time.

---

## Getting involved

The library, all open issues, and the milestone tracker are public on
[GitHub](https://github.com/domix/dmx-fun). If any of this sounds useful — or if you have
ideas for what should come in 0.0.16 and beyond — issues and pull requests are very welcome.

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun:0.0.14")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun</artifactId>
  <version>0.0.14</version>
</dependency>
```
