---
title: "Streams, Immutable Collections, and Efficient Data Processing"
description: "The reflex objection to immutable data is cost: surely all that copying is slow. The JDK's answer is a pairing — lazy streams that fuse a whole pipeline into one pass, and immutable collections engineered so that sharing replaces copying. Here is how the two halves work together, where the real costs live, and when mutation is still the right tool inside a pure boundary."
pubDate: 2026-07-31
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Streams", "Immutability", "Performance", "Java", "Collections", "Functional Programming", "Data Processing"]
image: "https://images.pexels.com/photos/36423815/pexels-photo-36423815.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "Keegan Checks"
    authorUrl: "https://www.pexels.com/@keeganjchecks/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/efficient-glass-bottle-production-line-in-factory-36423815/"
---

Bring up immutable data with a performance-minded Java developer and the objection arrives on
schedule: *all that copying must be slow*. It sounds airtight — if nothing can be modified,
every change must be a copy, so a pipeline of five transformations must allocate five
collections. A codebase built that way would indeed crawl.

The JDK's functional toolkit was designed so that this is not how it works. The two halves —
**streams** for processing and **immutable collections** for the results — attack the copying
problem from opposite ends: streams make the *intermediate* copies never exist, and immutable
collections make the *final* result shareable so defensive copies stop being necessary. Like
the bottling line in the photo, elements flow through every station one at a time — there is
no warehouse of half-processed bottles between machines. This post — from the advanced side of
this blog — walks the machinery, and then the honest part: where costs remain and when
mutation is still the right tool.

---

## Streams: the pipeline is one pass, not five

The naive reading of this code sees four collections:

```java
List<Invoice> overdue = invoices.stream()
    .filter(Invoice::isUnpaid)          // "a filtered list"?
    .filter(inv -> inv.age().toDays() > 30)
    .map(Invoice::withLateFee)
    .toList();                          // the only collection that ever exists
```

The runtime builds one. Intermediate operations are **lazy**: `filter` and `map` do not run
when declared — they compose a chain of operations, and only the terminal `toList()` pulls
elements through it. Each invoice traverses the *whole* chain before the next starts, so no
intermediate collection is ever *required* — the "filtered list" and the "mapped list" simply
never need to exist. In practice (this is how current JDK implementations behave, not a
spec-level allocation guarantee), a stateless pipeline like this does its work in a single
traversal, much like a hand-written loop. (The exceptions are the *stateful* stages —
`sorted()` buffers the entire upstream before emitting anything, `distinct()` carries a
seen-set — so a pipeline containing one pays for that stage's buffer like any loop would.)

Laziness also buys **short-circuiting**. When the answer does not need the whole source, the
pipeline stops pulling:

```java
static final BigDecimal LIMIT = new BigDecimal("10000");

Option<Invoice> firstBig = Option.fromOptional(
    invoices.stream()
        .filter(inv -> inv.total().compareTo(LIMIT) > 0)
        .findFirst());                  // stops at the first hit — rest never evaluated
```

`findFirst`, `anyMatch`, `limit`, `takeWhile` — on a large or lazily generated source
(`Stream.iterate`, `Stream.generate`), the unprocessed tail costs nothing. And when the middle
of the pipeline needs an operation the JDK does not ship — windowing, dedup-by-key, running
totals — [gatherers](/dmx-fun/blog/stream-gatherers-custom-intermediate-operations) stay
inside the lazy pipeline instead of forcing an exit into loops: short-circuiting still flows
through them, and the one-pass shape is preserved as long as the gatherer itself does not
buffer (a windowing gatherer holds its window, just as `sorted()` holds its buffer).

For CPU-bound work over big in-memory collections, the same pipeline parallelizes by changing
one word (`parallelStream()`) — safe when the functions are pure (*non-interfering* and
*stateless*, in the Stream javadoc's terms) and, for `reduce`/`collect`, the combining
functions associative — with a valid identity for the identity-taking `reduce` overloads, and
accumulator/combiner agreeing for custom collectors; the
[shared-state story](/dmx-fun/blog/functional-concurrency-parallel-work-without-shared-state)
covers the rest. And when boxing shows up in a profile, the primitive specializations
(`IntStream`, `LongStream`, `mapToInt`) run the same fused pipelines over unboxed values.

---

## Immutable collections: sharing replaces copying

The stream half kills intermediate copies. The collection half attacks a subtler waste: the
**defensive copy**. In a mutable-collection codebase, every trust boundary pays a copy tax —
the constructor copies the list it stores, the getter copies the list it returns, the cache
copies what it hands out — because any caller might mutate what it received. Three copies of
data nobody ever modifies, made out of fear.

An immutable list needs none of that. Handing the same reference to ten consumers — or ten
threads — is safe *by construction*, so sharing becomes free where copying used to be
mandatory. Two precisions keep that claim honest. First, an unmodifiable list freezes the
*structure* only — sharing is fully safe when the elements are immutable too, which is why
the pattern pairs with records like the `Charge` below; an unmodifiable list of mutable
objects just shares the mutability more efficiently. Second, this is whole-instance sharing;
the per-*update* structural sharing of persistent collections is a different mechanism the
JDK still lacks — [the immutability post](/dmx-fun/blog/immutability-in-java-an-oop-foundation)
covers that half.

```java
public record Statement(NonEmptyList<Charge> charges) {
    // no defensive copy in, no defensive copy out — the reference IS the value
}
```

The JDK's factories are engineered around this — with one sharp edge worth knowing exactly.
In current OpenJDK (this is observed implementation behavior; the spec's only *promise* is an
unmodifiable result), `List.copyOf` skips the copy and returns its argument only when that
argument is one of the JDK's own null-rejecting immutable lists: the `List.of` family,
`List.copyOf` itself, or `Collectors.toUnmodifiableList`. Passing *those* through layers that
each "seal" their input costs one copy total, not one per layer:

```java
List<Charge> charges = raw.stream()
    .map(this::toCharge)
    .collect(Collectors.toUnmodifiableList());   // one materialization, frozen

return List.copyOf(charges);   // no-op here: copyOf recognizes its own immutable lists
```

The sharp edge: `Stream.toList()` guarantees only an unmodifiable list, and today's
implementation returns a *null-tolerant* one — since `List.copyOf` must reject nulls, it
cannot trust that list and copies it in full. The same goes for
`Collections.unmodifiableList` wrappers: unmodifiable to callers, opaque to `copyOf`, fully
copied. These reuse behaviors are implementation details that could shift in a future JDK —
the durable takeaway is the shape: `toList()` is the right default terminal, and following it
with `copyOf` expecting a free seal buys a real O(n) copy today, not a no-op.

The [dmx-fun](/dmx-fun/) collections follow the same discipline —
[`NonEmptyList`](/dmx-fun/guide/non-empty-list) is immutable and its operations return new
instances — and the container types (`Result`, `Option`, `Try`) are immutable values, which
is precisely why they can flow through streams and across threads without ceremony. The
library also ships the terminal half for typed-outcome pipelines: collectors like
`Result.toList()` and `Result.partitioningBy()` that materialize a stream of outcomes in one
traversal, without a hand-rolled two-pass split:

```java
Result<List<Charge>, ChargeError> all = raw.stream()
    .map(this::toCharge)          // Stream<Result<Charge, ChargeError>>
    .collect(Result.toList());    // consumes the stream, then Ok(all charges)
                                  // or the first Err in encounter order
```

What these types cost — and where they vanish into JIT noise — is documented in the library's
own [performance guide](/dmx-fun/guide/performance).

---

## Mutation as an implementation detail

Here is the part dogma gets wrong: efficient functional code mutates *constantly* — inside
boundaries nobody can observe. In current OpenJDK (accumulator types are implementation
details, not public promises), `Collectors.toList()` appends into a mutable `ArrayList`
while the stream runs; `Collectors.joining` builds through mutable buffers — a
`StringBuilder` for the no-arg form, a `StringJoiner` for the delimiter overloads; sorting
copies into an array and mutates it in place. The JDK's own functional machinery is
imperative on the inside, and that is the design, not a compromise.

The rule this implies for your own code: **immutability is a property of boundaries, not of
every statement.** A pure function that fills a local `ArrayList` in a loop and returns
`List.copyOf(result)` is exactly as functional, from the caller's perspective, as a stream
pipeline — nobody can observe the mutation, so it never happened. What matters is what
crosses the boundary: [share nothing anyone can change](/dmx-fun/blog/why-avoid-mutable-state).

That is also the honest answer to hot paths. When a profiler says a stream pipeline in a
tight loop is too slow — allocation pressure, megamorphic lambda dispatch, boxing — the fix
is not to abandon immutability at the API; it is a local, encapsulated imperative core: a
primitive array, an indexed loop, one frozen result out. Measured, contained, invisible.

---

## Where the costs actually are

A checklist for the performance conversation, in the order the costs usually matter:

- **I/O dominates.** In a typical service, the database round-trip is thousands of times the
  cost of any collection copy. Optimizing the copy before the query is inverted effort.
- **The final materialization is the honest cost.** One `toList()` per pipeline. If even that
  hurts, ask whether the consumer needs a collection at all — passing the `Stream` onward
  defers the cost to a consumer that might short-circuit it away.
- **`copyOf` is free only for its own kind — today.** In current OpenJDK, `List.of`-family
  and `toUnmodifiableList` results pass through untouched, while `Stream.toList()` results
  and `Collections.unmodifiableList` wrappers get fully copied (observed behavior, not spec).
  `copyOf` of mutable input is always a real copy — the tax paid once, at the boundary where
  trust begins, the same place
  [validation](/dmx-fun/blog/validation-at-the-boundary-not-in-the-core) already lives.
- **Boxing is the silent one.** `Stream<Integer>` in a numeric hot loop allocates per
  element; `IntStream` does not. This is the most common "streams are slow" diagnosis.
- **Measure before believing any of this matters.** The JIT inlines, escape analysis
  scalar-replaces, and most pipelines run over dozens of elements, not millions. The default
  posture — immutable at boundaries, streams for transformation — is the right one until a
  profiler, not a hunch, says otherwise.

The functional payoffs — no defensive copies, free sharing across threads, fused single-pass
pipelines — are efficiency *gains* the mutable style quietly forfeits. The costs are real but
localized, and the JDK has spent a decade engineering them down. Copying is not the price of
immutability; copying is what immutability makes unnecessary.

---

## Further reading

- [Why Avoid Mutable State?](/dmx-fun/blog/why-avoid-mutable-state) — the correctness case
  that makes the sharing economy possible.
- [Immutability in Java: An OOP Foundation](/dmx-fun/blog/immutability-in-java-an-oop-foundation)
  — building the immutable values these pipelines produce.
- [Lazy Evaluation: When It Helps](/dmx-fun/blog/lazy-evaluation-when-it-helps) — the same
  pay-only-when-consumed economics applied to single expensive values with `Lazy<T>`.
- [Stream Gatherers: Custom Intermediate Operations](/dmx-fun/blog/stream-gatherers-custom-intermediate-operations)
  — extending the fused pipeline with your own stateful stages.
- [Functional Concurrency: Parallel Work Without Shared State](/dmx-fun/blog/functional-concurrency-parallel-work-without-shared-state)
  — why immutable inputs make `parallelStream` and friends safe.
- [Modeling Data Transformation Pipelines](/dmx-fun/blog/modeling-data-transformation-pipelines)
  — the design shape these mechanics implement.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
