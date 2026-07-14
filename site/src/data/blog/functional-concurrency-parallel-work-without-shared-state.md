---
title: "Functional Concurrency: Structuring Parallel Work Without Shared State"
description: "Concurrency's bad reputation is really shared mutable state's reputation. Structure parallel work the functional way — immutable inputs, pure tasks, outcomes as values, and a join step that combines results instead of threads fighting over an accumulator — and most of the classic hazards stop being representable."
pubDate: 2026-07-14
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Concurrency", "Functional Programming", "Java", "Virtual Threads", "Immutability", "Try", "Result", "Reactive"]
image: "https://images.pexels.com/photos/8028682/pexels-photo-8028682.jpeg"
imageCredit:
    author: "SHVETS production"
    authorUrl: "https://www.pexels.com/@shvets-production/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/people-swimming-in-the-pool-8028682/"
---

Ask a room of Java developers what makes concurrency hard and you will hear the war stories:
the race that only reproduces under load, the deadlock between two locks acquired in opposite
order, the `HashMap` that corrupted itself silently for months. Then look closely at the stories.
None of them are about *threads*. Every single one is about **shared mutable state** — two
threads touching the same data, at least one of them writing.

That distinction is the whole subject. Concurrency did not earn its reputation; shared mutable
state did, and concurrency took the blame. The functional approach to parallel work starts from
that diagnosis: instead of managing the hazard with locks, structure the work so the hazard
cannot occur.

---

## A race condition needs three ingredients

For a data race to exist you need all three: state that is **shared** between threads, state that
is **mutated**, and **concurrent** access. Remove any one ingredient and the race is gone.

Locks remove the third — they serialize access to the contested spot. It works, but the cost is
an invisible protocol: nothing in the type of a `HashMap` says "hold `latch` before touching me."
The locking discipline lives in comments and code review, which is exactly where the `null` and
unchecked-exception problems lived. It is knowledge the compiler cannot check.

Functional style removes the second ingredient instead. An immutable value — a record, a
`List.copyOf`, a dmx-fun `Result` — can be handed to any number of threads with zero coordination,
because there is nothing to coordinate. No lock, no memory-visibility puzzle, no protocol. Sharing
immutable data is not "safe if you are careful"; it is safe *by construction*.

That single decision reshapes how parallel work looks. If threads cannot communicate by mutating
shared objects, they must communicate the only other way: **by returning values.**

---

## Scatter, compute, gather — like swimmers in lanes

The structure that falls out is the one in the picture above: each swimmer in their own lane, no
one touching anyone else, results compared only at the wall. In code:

1. **Scatter** — split the work into independent tasks, each given an *immutable* input.
2. **Compute** — each task runs a function of its input and returns a value. It writes nothing
   anyone else reads.
3. **Gather** — one place joins the returned values into the final answer.

Virtual threads (standard since Java 21) removed the old excuse for not structuring work this way
— tasks are now cheap enough to fork one per item:

```java
List<CustomerId> ids = ...;                       // immutable input

try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Profile>> futures = ids.stream()
        .map(id -> executor.submit(() -> fetchProfile(id)))   // scatter: one task per id
        .toList();
    // gather happens below
}
```

Notice what is absent: no shared collection the tasks add to, no counter they increment, no
`synchronized`. Each task's entire effect is its return value. And for CPU-bound work over a
collection, the degenerate form of the same pattern is a one-liner — `parallelStream()` is
scatter/gather with the fork and join hidden, and it is exactly as safe as the function you map
is pure:

```java
List<Invoice> invoices = orders.parallelStream()
    .map(this::price)     // safe BECAUSE price is pure — no shared state touched
    .toList();
```

The moment `price` writes to a shared cache or mutates its argument, the one-liner becomes the
war story. Purity is not a stylistic preference here; it is the load-bearing wall.

---

## Failures cross threads badly — make outcomes values

There is a second, quieter problem the scatter/gather shape exposes: exceptions do not travel
well between threads. A `throw` inside a submitted task does not reach your `catch` — it is
stored, wrapped in an `ExecutionException`, and rethrown only when you call `Future.get()`, with
the original context gone and the other forty-nine tasks in unknown states. The stack-unwinding
model assumes one stack; parallel work has many.

Values cross threads perfectly. So apply the same move this blog applies everywhere — make the
outcome data — *inside* the task, before the result crosses the boundary:

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Try<Profile>>> futures = ids.stream()
        .map(id -> executor.submit(() -> Try.of(() -> fetchProfile(id))))  // outcome captured here
        .toList();

    List<Try<Profile>> outcomes = futures.stream()
        .map(f -> Try.of(f::get).flatMap(t -> t))   // Future.get's own throw, folded in too
        .toList();

    Try<List<Profile>> all = Try.sequence(outcomes);   // gather: fail-fast join
}
```

Every task returns a `Try<Profile>` — success or captured failure, as an ordinary value that can
be put in a list, filtered, counted, or logged. Nothing explodes mid-gather.

The join then becomes a *decision you make explicitly*, not an accident of which task threw
first. `Try.sequence` is the fail-fast choice: one failure fails the batch. But batch jobs
usually want the other answer — "process what succeeded, report **everything** that failed" —
and that is accumulation, the same shape as form validation:

```java
// Which of the 50 imports failed? All the answers, not just the first.
List<Validated<NonEmptyList<ImportError>, Row>> outcomes = ...;  // one per parallel task

Validated<NonEmptyList<ImportError>, List<Row>> report =
    Validated.sequenceNel(outcomes);   // Valid(all rows) or Invalid(every failure)
```

Fail-fast versus accumulate is a one-word change at the gather step — because the failures were
values all along. Try getting that flexibility from a `try/catch` around `Future.get()` in a loop.

---

## Never share the accumulator

The classic concurrency bug factory is the shared accumulator: threads adding to a synchronized
list, incrementing a shared counter, merging into a `ConcurrentHashMap`. Even when the collection
itself is thread-safe, the *logic* around it rarely is — check-then-act races, iteration during
mutation, partial results observed mid-update. Thread-safe collections make individual operations
atomic; they do not make your algorithm correct.

The functional answer is that the accumulator should not exist while the parallel work runs. Each
task returns its piece; accumulation happens **after** the join, sequentially, where it is just a
fold over a list:

```java
// Not: tasks incrementing shared counters.
// Instead: tasks return values; the join folds them.
Money total = outcomes.stream()
    .flatMap(t -> t.toOptional().stream())    // keep the successes
    .map(Profile::balance)
    .reduce(Money.ZERO, Money::plus);          // sequential, trivial, race-free
```

This looks like it gives up parallelism, but it does not — the expensive part (the fifty network
calls, the heavy computation) ran in parallel. The fold over fifty in-memory results costs
nothing. You parallelized the work and kept the combination sequential, which is the split that
map-reduce systems have used at datacenter scale for twenty years. Your service is a small
datacenter.

---

## The same shape, reactive

If your stack is reactive, the pattern translates directly — `Flux.flatMap` runs the inner calls
concurrently, and [`fun-reactor`](/dmx-fun/guide/reactor) provides the same two joins for a
stream of outcomes:

```java
Flux<Result<Quote, QuoteError>> quotes =
    Flux.fromIterable(symbols)
        .flatMap(this::fetchQuote);            // concurrent, each returns an outcome

Mono<Result<List<Quote>, QuoteError>> failFast   = ReactorFlux.sequence(quotes);
Mono<Validated<NonEmptyList<QuoteError>, List<Quote>>> everything
                                                  = ReactorFlux.collectValidated(quotes);
```

Same decision, same one-word change: stop at the first failure, or collect them all. The
concurrency machinery differs; the functional structure — immutable inputs, outcomes as values,
an explicit join — is identical.

---

## The recipe

Structuring parallel work functionally comes down to four rules:

1. **Inputs are immutable.** What you hand a task, nobody mutates — starting with the task itself.
2. **Tasks are functions.** Input in, value out, no effects on anything shared. Purity is what
   makes the parallelism safe rather than lucky.
3. **Outcomes are values.** `Try` or `Result` captured inside the task, so failure crosses the
   thread boundary as data, not as a delayed explosion.
4. **The join is explicit.** One place gathers the values and decides: fail fast, accumulate
   everything, fold into a total. The decision is visible in the code, not emergent from timing.

This is the functional core / imperative shell idea applied to *time*: the coordination — forking,
awaiting, joining — is a thin shell you can read in one screen, and everything that runs in
parallel is pure and therefore cannot interfere. (Java's structured concurrency API, still in
preview as of Java 25, is pushing the platform the same direction: explicit fork/join scopes whose
natural inhabitants are value-returning tasks.) The threads were never the problem. Give them
nothing to fight over, and they have nothing to fight with.

The [dmx-fun](/dmx-fun/) library provides the value side — [`Try`](/dmx-fun/guide/try),
`Result`, [`Validated`](/dmx-fun/guide/validated) and their `sequence`/`traverse` joins, plus
[`fun-reactor`](/dmx-fun/guide/reactor) for reactive pipelines. The
[Developer Guide](/dmx-fun/guide/) walks through each.

---

## Further reading

- [Why Avoid Mutable State?](/dmx-fun/blog/why-avoid-mutable-state) — the deeper case against the
  ingredient this whole post removes.
- [Immutability in Java: An OOP Foundation](/dmx-fun/blog/immutability-in-java-an-oop-foundation)
  — building the immutable values that make sharing free.
- [Pure Functions and Side Effects](/dmx-fun/blog/pure-functions-and-side-effects) — why purity is
  the load-bearing wall under `parallelStream` and friends.
- [Validated: Accumulating Errors in a Functional Way](/dmx-fun/blog/validated-accumulating-errors)
  — the accumulate-everything join, in full.
- [Modeling Data Transformation Pipelines](/dmx-fun/blog/modeling-data-transformation-pipelines) —
  the scatter/compute/gather shape in its sequential form.
- [Functional Thinking for Backend Engineers](/dmx-fun/blog/functional-thinking-for-backend-engineers)
  — the habit of modeling outcomes as values that this post extends across threads.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
