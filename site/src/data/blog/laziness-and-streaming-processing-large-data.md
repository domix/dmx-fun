---
title: "Laziness and Streaming: Processing Large Data Without Loading It All"
description: "A ten-gigabyte file does not fit in a two-gigabyte heap — but a pipeline that holds one element at a time never needs it to. Like an irrigation canal, streaming moves the water without building the reservoir: lazy sources, fused stages, and aggregating sinks keep memory proportional to a batch, not the dataset. Here are the real JDK tools, the stages that silently rebuild the reservoir, and the honest limits."
pubDate: 2026-08-09
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Streaming", "Laziness", "Large Data", "Java", "Streams", "Performance", "Functional Programming"]
image: "https://images.pexels.com/photos/17608610/pexels-photo-17608610.jpeg?auto=compress&cs=tinysrgb&w=800"
imageCredit:
    author: "Khalil Ahmad Mazari"
    authorUrl: "https://www.pexels.com/@khalilmazari/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/an-irrigation-system-in-a-field-17608610/"
---

The naive version reads the whole file first:

```java
List<String> lines = Files.readAllLines(file);   // the entire dataset, resident in heap
```

That line is fine at ten thousand rows and fatal at a hundred million: `readAllLines` builds
the reservoir before you process a drop. The streaming alternative changes one method and
the entire memory story — the canal in the photo above: water moves through it continuously,
and at no point does the canal *contain* the river.

```java
long countRejected(Path file) throws IOException {
    try (Stream<String> lines = Files.lines(file)) {   // lazy: reads as the pipeline pulls
        return lines
            .filter(line -> !line.isBlank())
            .map(Event::parse)
            .filter(Event::isRejected)
            .count();                                   // aggregates without retaining
    }
}
```

`Files.lines` does not read the file when called — it hands the pipeline a lazy source, and
each line is read, examined, and becomes garbage as the terminal operation pulls the next.
Heap usage is one line plus pipeline overhead, whether the file is a megabyte or a terabyte.
This post — item by item, with the sharp edges included — is about that discipline:
processing data whose size is none of your heap's business.

---

## The lazy sources the JDK actually ships

Streaming starts at the source; everything downstream inherits its laziness:

- **`Files.lines(path)` / `BufferedReader.lines()`** — line-at-a-time file and reader
  streams. Two sharp edges: the stream holds the file handle, so it *must* be closed —
  hence the try-with-resources above, easily forgotten because most streams need no closing
  (the library's [`Resource.fromAutoCloseable`](/dmx-fun/guide/resource) composes the same
  acquire-use-release discipline as a value, if you prefer it over the block) — and read
  failures after opening surface as `UncheckedIOException` from inside the pipeline, not
  from the `Files.lines` call itself.
- **`Stream.iterate` / `Stream.generate`** — computed sources: unbounded sequences of pages,
  IDs, retries-with-timestamps. Infinite by construction, usable because
  [short-circuiting](/dmx-fun/blog/streams-immutable-collections-efficient-data-processing)
  (`limit`, `takeWhile`, `findFirst`) stops the pull.
- **Your own sources** — anything that can produce elements on demand (a paginated API, a
  cursor, a message poll) becomes a stream via an `Iterator` or `Spliterator` handed to
  `StreamSupport.stream(...)`. The database counterpart is driver-level: a forward-only
  `ResultSet` with a bounded `fetchSize`, wrapped the same way, streams rows a batch at a
  time — *when the driver honors it*: `fetchSize` is a JDBC hint, and the common drivers
  need convincing (PostgreSQL streams only with autocommit off; MySQL needs its streaming
  mode). Verify with your driver before betting the heap on it.

The unifying property: the source answers "give me the next one," never "give me everything."

---

## The pipeline is only as streaming as its greediest stage

A lazy source buys nothing if a downstream stage rebuilds the reservoir. The stages divide
cleanly:

**Flow-preserving** — hold one element (or one bounded window) at a time: `filter`, `map`,
`flatMap`, `takeWhile`, `limit`, and bounded
[gatherers](/dmx-fun/blog/stream-gatherers-custom-intermediate-operations). Aggregating
terminals that fold without retaining — `count`, `reduce`, `sum`, a running statistics
collector — also keep the flow.

**Reservoir-building** — `sorted()` buffers the entire upstream before emitting anything
(sorting a terabyte through a stream is still sorting a terabyte), and `distinct()` carries
a seen-set that in the worst case is the dataset again. `toList()` materializes the output
outright. `Collectors.groupingBy` deserves precision: the two-arg form with a folding
downstream — `groupingBy(Event::category, counting())` — produces a small summary per
category, but plain `groupingBy(classifier)` builds a `Map` holding *every element*,
regrouped: the reservoir with extra keys.

The practical test before running a pipeline over big data: for each stage, ask what it must
*remember*. The canonical large-data shape remembers only a window — batching a huge import
into bounded inserts:

```java
try (Stream<String> lines = Files.lines(file)) {
    lines.map(Event::parse)
         .gather(Gatherers.windowFixed(500))    // one 500-element batch in memory at a time
         .forEach(batch -> repository.insertAll(batch));
}
```

Memory holds one batch, not one dataset — the [gatherers post](/dmx-fun/blog/stream-gatherers-custom-intermediate-operations)
covers `windowFixed` and friends in depth.

---

## Failures per element, without stopping the river

At a hundred million rows, some rows are garbage — and one bad line must not abort hour six
of the import. The [typed-outcome move](/dmx-fun/blog/designing-a-good-error-type) applies
per element: capture the failure as a value, keep flowing.

```java
var rejected = new LongAdder();          // contained effect: count what we drop

try (Stream<String> lines = Files.lines(file)) {
    lines.map(line -> Try.of(() -> Event.parse(line)))  // failure becomes a value, not an abort
         .filter(t -> {
             if (t.isFailure()) { rejected.increment(); return false; }
             return true;
         })
         .map(Try::get)
         .gather(Gatherers.windowFixed(500))
         .forEach(repository::insertAll);
}
log.info("import done, {} malformed lines rejected", rejected.sum());
```

Each line's outcome is data, handled and *accounted for* without holding more than the
current element. (When you genuinely do not need the count, the terse keep-successes form is
`.flatMap(Try::stream)` — the library's one-method bridge from a `Try` to a zero-or-one
element stream.) A caution worth appending to the
[streams post's](/dmx-fun/blog/streams-immutable-collections-efficient-data-processing)
`Result.toList` example: collectors that gather *all* outcomes are aggregation tools for
bounded data — on an unbounded stream they are the reservoir again. Per-element handling
stays per-element.

One honest boundary on the abort-proofing itself: `Try.of` wraps *your parse*, not the
source. The `UncheckedIOException` edge from the sources section lives upstream of it — a
single malformed byte in a UTF-8 file aborts the pull from inside `Files.lines`, before any
`Try` sees it. If the input's encoding is untrusted, read bytes and decode per line, so the
decode failure becomes one more per-element outcome.

---

## The honest limits

- **A stream is one pass.** Consumed is consumed. If two computations need the data, either
  fuse them into one pass (a combined fold, a
  [gatherer](/dmx-fun/blog/stream-gatherers-custom-intermediate-operations)) or accept
  reading the source twice.
- **`parallelStream` is the wrong tool for IO-bound streaming** — not because the source
  splits poorly (`Files.lines` has split efficiently since JDK 9) but because per-element
  *blocking* IO runs on the shared `ForkJoinPool.commonPool`, starving every other parallel
  stream in the JVM while threads sit in waits. For concurrent per-element IO inside a
  streaming pipeline, `Gatherers.mapConcurrent(n, ...)` bounds the fan-out on virtual
  threads while preserving the flow — the
  [gatherers post](/dmx-fun/blog/stream-gatherers-custom-intermediate-operations) has the
  fuller `mapConcurrent` story, and the
  [concurrency post](/dmx-fun/blog/functional-concurrency-parallel-work-without-shared-state)
  the fan-out problem it solves.
- **Backpressure is the async version of this discipline.** When the producer is a network
  peer rather than a file you pull from, reactive streams (`Flux`, with
  [`fun-reactor`](/dmx-fun/guide/reactor) for typed outcomes) make "don't send more than I
  can hold" an explicit protocol; the pull-based `Stream` gets the same effect for free by
  only ever asking for the next element.
- **Laziness here is about sequences.** Its sibling — deferring and memoizing a single
  expensive *value* with `Lazy<T>` — is the
  [lazy-evaluation post's](/dmx-fun/blog/lazy-evaluation-when-it-helps) territory. (Do not
  wrap a stream itself in a `Lazy`: memoization would hand every caller the same one-shot,
  handle-holding stream — defer the *inputs* to a pipeline, not the pipeline.)

The through-line with the rest of this blog: streaming is the
[pipeline shape](/dmx-fun/blog/modeling-data-transformation-pipelines) under a memory
constraint. Pure per-element functions, outcomes as values, one materialization — at the
sink, sized to the *answer* rather than the input. Build the canal, not the reservoir.

---

## Further reading

- [Streams, Immutable Collections, and Efficient Data Processing](/dmx-fun/blog/streams-immutable-collections-efficient-data-processing)
  — fusion and short-circuiting, the machinery this post runs at scale.
- [Stream Gatherers: Custom Intermediate Operations](/dmx-fun/blog/stream-gatherers-custom-intermediate-operations)
  — `windowFixed`, `mapConcurrent`, and building your own bounded stages.
- [Lazy Evaluation: When It Helps](/dmx-fun/blog/lazy-evaluation-when-it-helps) — the
  single-value side of laziness, with `Lazy<T>`.
- [Modeling Data Transformation Pipelines](/dmx-fun/blog/modeling-data-transformation-pipelines)
  — the pipeline shape, before the memory constraint.
- [Functional Concurrency: Parallel Work Without Shared State](/dmx-fun/blog/functional-concurrency-parallel-work-without-shared-state)
  — bounded fan-out when per-element work is slow.
- [Designing a Good Error Type](/dmx-fun/blog/designing-a-good-error-type) — the typed
  failures that let bad rows become data instead of aborts.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
