---
title: "Stream Gatherers: Custom Intermediate Operations in Modern Java"
description: "For a decade the Stream API let you write your own terminal operation with Collector — but the middle of the pipeline was a closed shop of filter, map, and a handful of others. Gatherers, final since Java 24, open it up: a standard way to write stateful, short-circuiting, even concurrent intermediate operations. Here is how they work and when to reach for one."
pubDate: 2026-07-17
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Java", "Stream Gatherers", "Streams", "JDK", "Functional Programming", "Java 24"]
image: "https://images.pexels.com/photos/18631424/pexels-photo-18631424.jpeg"
imageCredit:
    author: "Vladimir Srajber"
    authorUrl: "https://www.pexels.com/@vladimirsrajber/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/bottles-of-water-in-factory-18631424/"
---

The Stream API shipped in Java 8 with a quietly asymmetric design. The *terminal* end was
extensible: `Collector` let anyone write a custom way to accumulate a stream into a result, and a
whole ecosystem grew around it. The *intermediate* end was sealed. You got `filter`, `map`,
`flatMap`, `limit`, `distinct`, `sorted`, `peek`, `takeWhile`, `dropWhile` — and that was the
entire vocabulary. If the operation you needed was not on that list, you were out of luck.

And plenty of everyday operations are not on that list. Group consecutive elements into batches of
100. Keep only the first element for each key. Emit a running total. Take elements until a
condition, *including* the one that tripped it. Every one of these is a stateful transformation
that belongs in the middle of a pipeline — and for ten years, every one of them forced you to
break out of the stream into an external loop or a contorted abuse of `Collector`.

**Gatherers**, previewed across Java 22–23 and finalized in **Java 24** ([JEP 485](https://openjdk.org/jeps/485)),
close the gap. `Gatherer` is to intermediate operations what `Collector` is to terminal ones: a
standard, composable way to write your own.

---

## The shape of a gatherer

You plug a gatherer into a pipeline with the new `Stream.gather(Gatherer)` method, which returns a
`Stream` so it chains like any other intermediate op:

```java
List<R> result = source.stream()
    .filter(...)              // built-in intermediate op
    .gather(myGatherer)       // YOUR intermediate op
    .map(...)                 // built-in again
    .toList();                // terminal op
```

A `Gatherer<T, A, R>` transforms a stream of `T` into a stream of `R`, using private mutable state
of type `A`. It has up to four components, and the elegance is that you only supply the ones you
need:

- **initializer** — a `Supplier<A>` that creates the private per-evaluation state.
- **integrator** — the heart: called once per input element. It sees the state, the element, and a
  `Downstream` handle it can `push` results into. It returns a `boolean` — `false` to
  short-circuit the whole stream.
- **combiner** — merges two states for parallel evaluation. Omit it and the gatherer runs
  sequentially.
- **finisher** — runs after the last element, for emitting anything buffered in the state.

The `integrator` is where gatherers get their power. A single input element can push *zero*
results downstream (filtering), *one* (mapping), or *many* (expanding) — and because it holds
state, what it pushes can depend on everything it has seen so far.

---

## A first custom gatherer: `distinctBy`

The JDK gives you `distinct()`, which dedups by `equals`. It has never given you `distinctBy(key)`
— keep the first element for each derived key — despite it being one of the most requested stream
operations of the last decade. With gatherers it is a few lines:

```java
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Gatherer;

static <T, K> Gatherer<T, ?, T> distinctBy(Function<? super T, ? extends K> keyExtractor) {
    return Gatherer.ofSequential(
        HashSet::new,                                    // initializer: the set of seen keys
        (seen, element, downstream) -> {                 // integrator
            if (seen.add(keyExtractor.apply(element))) {
                return downstream.push(element);         // first time we see this key → emit
            }
            return true;                                 // duplicate → skip, keep consuming
        });
}
```

Two things to notice. The state (`HashSet` of seen keys) is created fresh by the initializer and
is private to this pipeline run — no shared mutable state, no thread-safety puzzle. And the
integrator returns `downstream.push(...)`, propagating downstream's own answer about whether it
wants more elements — that is how backpressure and short-circuiting flow back up a pipeline.

Using it reads exactly like a built-in operation:

```java
List<Order> firstPerCustomer = orders.stream()
    .gather(distinctBy(Order::customerId))
    .toList();
```

Because it relies on encounter order to define "first," it is built with `ofSequential` — a
gatherer that declares it cannot be parallelized, so the runtime never tries.

---

## Short-circuiting: `takeUntilInclusive`

The built-in `takeWhile` stops *before* the element that fails the predicate. A common need is the
opposite: stop *after* it — include the boundary. The `boolean` return of the integrator is the
short-circuit switch, and here we do not even need state:

```java
import java.util.function.Predicate;
import java.util.stream.Gatherer;

static <T> Gatherer<T, ?, T> takeUntilInclusive(Predicate<? super T> stop) {
    return Gatherer.ofSequential(
        (_, element, downstream) -> {                    // no state → unnamed with '_'
            boolean more = downstream.push(element);     // always emit this element
            return more && !stop.test(element);          // then halt if it tripped the predicate
        });
}
```

```java
// Read log lines up to and including the first ERROR, then stop the whole stream.
List<String> throughFirstError = logLines.stream()
    .gather(takeUntilInclusive(line -> line.contains("ERROR")))
    .toList();
```

Returning `false` tells the stream to stop pulling from the source entirely — so on a lazy or
infinite source, everything after the boundary is never even evaluated. Note the `_` for the
unused state parameter: unnamed variables (final since Java 22) pair naturally with the stateless
`Gatherer` overloads.

---

## You often do not need to write one at all

Before you build a gatherer, check `java.util.stream.Gatherers` — the JDK ships several
battle-tested ones, and they cover a surprising fraction of real needs:

```java
import java.util.stream.Gatherers;

// Fixed-size batching — Stream<Trade> to Stream<List<Trade>> of up to 100
// (the final window is smaller when the count is not a multiple of 100).
List<List<Trade>> batches = trades.stream()
    .gather(Gatherers.windowFixed(100))
    .toList();

// Sliding windows of 3 — [1,2,3], [2,3,4], [3,4,5], ...
List<List<Integer>> windows = Stream.of(1, 2, 3, 4, 5)
    .gather(Gatherers.windowSliding(3))
    .toList();

// Running total — scan emits each intermediate accumulation: 1, 3, 6, 10.
List<Integer> runningTotals = Stream.of(1, 2, 3, 4)
    .gather(Gatherers.scan(() -> 0, Integer::sum))
    .toList();
```

`windowFixed`, `windowSliding`, `fold`, and `scan` are the ones you will reach for most. `fold` is
a stateful reduction to a single result; `scan` is its cousin that emits every step along the way —
neither was expressible as a plain intermediate op before.

---

## The one that changes architecture: `mapConcurrent`

`Gatherers.mapConcurrent(maxConcurrency, mapper)` deserves its own mention, because it quietly
solves a problem that used to require an executor, a list of futures, and careful cleanup. It maps
each element by running the mapper on a **virtual thread**, with a hard cap on how many run at
once, and it preserves encounter order in the output:

```java
// Fetch 10-at-a-time, on virtual threads, order preserved. No executor, no futures.
List<Profile> profiles = customerIds.stream()
    .gather(Gatherers.mapConcurrent(10, this::fetchProfile))
    .toList();
```

This is exactly the bounded fan-out that hand-rolled `newVirtualThreadPerTaskExecutor()` code
reaches for — one task per item is cheap, but an unbounded fan-out will flatten a downstream
service. `mapConcurrent` gives you the concurrency cap declaratively, as a number in the pipeline,
and tears the virtual threads down for you when the stream ends or short-circuits. If you have been
structuring parallel work by hand, this is often the whole thing in one line.

---

## When to reach for a gatherer (and when not to)

Gatherers are powerful enough to be over-used, so a lazy rule of thumb:

- **Use a built-in `Gatherers.*` first.** Windowing, scanning, folding, bounded concurrency — if
  one of the shipped gatherers fits, you are done.
- **Write a custom gatherer when the operation is stateful *and* reusable.** `distinctBy`, a
  chunk-by-predicate, a de-duplicate-consecutive — things you will use in more than one pipeline
  and want to name once. The payoff is a pipeline that reads as a sequence of named steps instead
  of a loop with a mutable variable smuggled alongside it.
- **Do not reach for one for a plain `map` or `filter`.** If a built-in intermediate op already
  says it, a gatherer is just ceremony. And if the logic is a one-off with no state, an ordinary
  loop is still allowed to exist.

The deeper point is that Java finally treats the middle of a stream as an open, user-extensible
space — the same courtesy `Collector` extended to the end of the pipeline in 2014. Stateful stream
logic that used to leak out into imperative loops now has a first-class home, where it composes,
short-circuits correctly, and can be tested in isolation.

Gatherers are pure JDK — no dependency required, which is exactly the [JDK-first](/dmx-fun/blog/jdk-first-functional-programming)
instinct worth having. Where a custom gatherer emits outcomes rather than plain values, the
[dmx-fun](/dmx-fun/) types — `Result`, `Option`, `Validated` — flow through a `gather` step like
any other value, so a stateful intermediate operation stays on the functional track. The
[Developer Guide](/dmx-fun/guide/) has the details.

---

## Further reading

- [Why Upgrade to Java 25?](/dmx-fun/blog/why-upgrade-to-java-25) — the broader case for the
  platform version gatherers ship in.
- [JDK-First Functional Programming](/dmx-fun/blog/jdk-first-functional-programming) — how much of
  the functional toolkit lives in the standard library already.
- [Modeling Data Transformation Pipelines](/dmx-fun/blog/modeling-data-transformation-pipelines) —
  the sequential shape gatherers slot into.
- [Functional Composition Patterns](/dmx-fun/blog/functional-composition-patterns) — composing
  small, named steps into larger transformations.
- [Functional Concurrency: Structuring Parallel Work Without Shared State](/dmx-fun/blog/functional-concurrency-parallel-work-without-shared-state)
  — the fan-out problem `mapConcurrent` solves declaratively.
- [Lazy Evaluation: When It Helps](/dmx-fun/blog/lazy-evaluation-when-it-helps) — why
  short-circuiting through `downstream.push` matters on large or infinite sources.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
