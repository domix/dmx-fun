---
title: "How to Model Data Transformation Pipelines"
description: "Ad-hoc transformation code grows into spaghetti because there is no shared vocabulary for what a stage is, what it produces, or how failures propagate. Functional types give you that vocabulary — typed stage boundaries, composable error handling, and batch pipelines that separate failures from successes without stopping the whole run."
pubDate: 2026-06-06
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Pipelines", "Data Transformation", "Result", "Try", "Architecture", "Patterns"]
image: "https://images.pexels.com/photos/34766652/pexels-photo-34766652.jpeg"
imageCredit:
    author: "Xiang Qi"
    authorUrl: "https://www.pexels.com/@xiang-qi-346904928/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/es-es/foto/34766652/"
---

Every backend service transforms data. A request arrives as raw JSON, becomes a validated command, gets enriched with data from other services, is mapped to a domain model, and is finally persisted or forwarded. The same structure appears in batch imports, event processors, ETL jobs, and report generators.

What changes between projects is not the structure — it is whether the structure is *visible*.

In many codebases, the transformation logic lives in a single service method that grows as requirements change. Parsing, validation, enrichment, and persistence are interleaved. A failure in one stage is caught somewhere in the middle with `try/catch`, and the recovery logic is tangled with the happy path. New stages get stapled on. After enough iterations, nobody can tell what the pipeline actually does without reading the whole method.

Functional types give you a vocabulary for making the pipeline structure explicit: each stage has a declared input type and output type, failures are first-class values, and the composition is visible in the code rather than implied by execution order.

---

## The Core Model: A Stage Is a Typed Function

The smallest unit of a pipeline is a stage — a function that takes one type and produces another.

```java
// A stage: RawEvent → ParsedEvent
static Result<ParsedEvent, ParseError> parse(RawEvent raw) { ... }

// A stage: ParsedEvent → EnrichedEvent
static Result<EnrichedEvent, EnrichError> enrich(ParsedEvent parsed) { ... }

// A stage: EnrichedEvent → PersistedEvent
static Result<PersistedEvent, PersistError> persist(EnrichedEvent enriched) { ... }
```

Each stage has an explicit contract:
- What type it consumes
- What type it produces on success
- What typed error it returns on failure

Nothing is hidden. The compiler verifies that you cannot chain `enrich` after `persist` — the types do not align. Stages that cannot be composed do not compile.

---

## Composing Stages: Single-Item Pipeline

When each stage can fail independently and a failure in any stage should stop the pipeline for that item, compose with `flatMap`:

```java
Result<PersistedEvent, PipelineError> process(RawEvent raw) {
    return parse(raw)
        .mapError(PipelineError::parseFailure)
        .flatMap(parsed  -> enrich(parsed).mapError(PipelineError::enrichFailure))
        .flatMap(enriched -> persist(enriched).mapError(PipelineError::persistFailure));
}
```

`mapError` normalizes each stage's specific error type into the shared `PipelineError` union before composing. The pipeline short-circuits at the first failure — subsequent stages are skipped.

The sealed error type makes the failure reasons explicit:

```java
public sealed interface PipelineError
    permits PipelineError.ParseFailure,
            PipelineError.EnrichFailure,
            PipelineError.PersistFailure {

    record ParseFailure(String field, String reason)  implements PipelineError {}
    record EnrichFailure(String serviceId, String msg) implements PipelineError {}
    record PersistFailure(String detail)               implements PipelineError {}
}
```

The caller at the boundary handles all three cases with an exhaustive switch — the compiler rejects any call site that forgets one.

---

## Stage Design: One Responsibility, One Type Boundary

The most common mistake in pipeline design is a stage that does two things. A stage that validates *and* transforms is harder to test and harder to replace.

The rule: **one stage transforms one type into one other type**. If a stage is doing two things, it is two stages.

```java
// Bad: parse and validate combined — hard to test the parts independently
static Result<ValidatedOrder, OrderError> parseAndValidate(String json) { ... }

// Good: separate stages with a clear type boundary
static Result<RawOrder, ParseError>      parse(String json) { ... }
static Result<ValidatedOrder, ValidationError> validate(RawOrder raw) { ... }
```

The intermediate type `RawOrder` is the contract between the two stages. It does not need to be a rich domain model — it just needs to be precise enough to carry the data from one stage to the next without losing information.

### Naming intermediate types

Intermediate types are not implementation details — they are the vocabulary of the pipeline. Name them after what they represent at that stage:

| Stage output | Suggested name |
|---|---|
| Successfully parsed, not yet validated | `RawOrder`, `ParsedEvent` |
| Validated against domain rules | `ValidatedOrder`, `ValidatedEvent` |
| Enriched with external data | `EnrichedOrder`, `EnrichedEvent` |
| Ready to be persisted | `OrderDraft`, `EventRecord` |

The names make the pipeline stages readable at a glance.

---

## Batch Pipelines: Partitioning Failures From Successes

Single-item pipelines short-circuit on the first failure. Batch pipelines usually should not — if one item in a thousand fails, the other 999 should still be processed.

The model changes: instead of `Result<Output, Error>` per pipeline run, you want a collection of results, partitioned into successes and failures.

```java
record BatchResult<O, E>(
    List<O> successes,
    List<E> failures
) {
    static <I, O, E> BatchResult<O, E> run(
        List<I> items,
        Function<I, Result<O, E>> pipeline
    ) {
        List<O> successes = new ArrayList<>();
        List<E> failures  = new ArrayList<>();

        for (I item : items) {
            switch (pipeline.apply(item)) {
                case Result.Ok<O, E>  ok  -> successes.add(ok.value());
                case Result.Err<O, E> err -> failures.add(err.error());
            }
        }
        return new BatchResult<>(
            Collections.unmodifiableList(successes),
            Collections.unmodifiableList(failures)
        );
    }
}
```

The `pipeline` parameter is the single-item pipeline composed earlier — the batch runner does not know or care what the stages are. It runs the pipeline on each item and sorts the results.

Usage:

```java
List<RawEvent> incoming = fetchIncoming();

BatchResult<PersistedEvent, PipelineError> result =
    BatchResult.run(incoming, this::process);

// Handle the two groups separately
result.successes().forEach(this::acknowledgeEvent);
result.failures().forEach(err -> log.warn("pipeline failure: {}", err));
```

This model is the standard shape for ETL pipelines, file importers, and message processors. The business decision — whether to retry failures, dead-letter them, or alert — is made *after* the batch completes, not interleaved with the transformation logic.

---

## Wrapping Stages That Throw

Not every stage can return `Result` from the start — legacy code, standard library methods, and third-party SDKs throw checked or unchecked exceptions. `Try<T>` wraps the boundary:

```java
// Legacy parser that throws
static ParsedEvent parseUnchecked(RawEvent raw) throws ParseException { ... }

// Wrapped stage — the exception becomes a first-class value
static Try<ParsedEvent> parseSafe(RawEvent raw) {
    return Try.of(() -> parseUnchecked(raw));
}
```

`Try<T>` converts to `Result<T, Throwable>` when you need to compose it with `Result`-returning stages:

```java
Result<PersistedEvent, PipelineError> process(RawEvent raw) {
    return parseSafe(raw)
        .toResult()
        .mapError(e -> PipelineError.parseFailure(raw.id(), e.getMessage()))
        .flatMap(parsed   -> enrich(parsed).mapError(PipelineError::enrichFailure))
        .flatMap(enriched -> persist(enriched).mapError(PipelineError::persistFailure));
}
```

The `Try` wraps the boundary; `toResult()` converts it; `mapError` assigns it the right pipeline error variant. From that point on, the composition is uniform.

---

## A Complete Example: Event Import Pipeline

Putting the pieces together — an event import service that processes a batch of raw payloads from an external queue.

### Domain types

```java
record RawPayload(String id, String body, Instant receivedAt) {}

record ParsedEvent(String id, String type, Map<String, String> fields) {}

record EnrichedEvent(ParsedEvent event, UserContext user, String correlationId) {}

record StoredEvent(String id, Instant storedAt) {}
```

### Stage implementations

```java
final class EventPipelineStages {

    static Result<ParsedEvent, PipelineError> parse(RawPayload payload) {
        return Try.of(() -> JsonMapper.parse(payload.body(), ParsedEvent.class))
            .toResult()
            .mapError(e -> PipelineError.parseFailure(payload.id(), e.getMessage()));
    }

    static Result<EnrichedEvent, PipelineError> enrich(
        ParsedEvent event,
        UserContextService svc
    ) {
        return svc.resolve(event.fields().get("userId"))
            .map(user -> new EnrichedEvent(event, user, UUID.randomUUID().toString()))
            .toResult(PipelineError.enrichFailure(event.id(), "user not found"));
    }

    static Result<StoredEvent, PipelineError> store(
        EnrichedEvent enriched,
        EventRepository repo
    ) {
        return Try.of(() -> repo.save(enriched))
            .toResult()
            .mapError(e -> PipelineError.persistFailure(enriched.event().id(), e.getMessage()));
    }
}
```

### Pipeline composition

```java
final class EventImportService {

    private final UserContextService userSvc;
    private final EventRepository    repo;

    EventImportService(UserContextService userSvc, EventRepository repo) {
        this.userSvc = userSvc;
        this.repo    = repo;
    }

    Result<StoredEvent, PipelineError> processOne(RawPayload payload) {
        return EventPipelineStages.parse(payload)
            .flatMap(parsed   -> EventPipelineStages.enrich(parsed, userSvc))
            .flatMap(enriched -> EventPipelineStages.store(enriched, repo));
    }

    BatchResult<StoredEvent, PipelineError> processBatch(List<RawPayload> payloads) {
        return BatchResult.run(payloads, this::processOne);
    }
}
```

### Calling code

```java
List<RawPayload> batch = queue.poll(500);

BatchResult<StoredEvent, PipelineError> result = service.processBatch(batch);

log.info("processed {}/{} events", result.successes().size(), batch.size());

result.failures().forEach(err -> switch (err) {
    case PipelineError.ParseFailure   e -> deadLetter.send(e.id(), "parse",   e.reason());
    case PipelineError.EnrichFailure  e -> deadLetter.send(e.id(), "enrich",  e.msg());
    case PipelineError.PersistFailure e -> retryQueue.add(e.id(),  e.detail());
});
```

The calling code makes decisions — retry, dead-letter, alert. The pipeline stages are unaware of those decisions; they only produce `Result` values.

---

## Testing Pipeline Stages

Because each stage is a pure function (or close to it), testing requires no framework-level wiring.

### Unit test a single stage

```java
@Test
void parse_returnsErr_whenBodyIsNotValidJson() {
    RawPayload bad = new RawPayload("evt-1", "not json", Instant.now());

    Result<ParsedEvent, PipelineError> result = EventPipelineStages.parse(bad);

    assertThat(result).isInstanceOf(Result.Err.class);
    assertThat(((Result.Err<?, PipelineError>) result).error())
        .isInstanceOf(PipelineError.ParseFailure.class);
}
```

### Unit test the full single-item pipeline

```java
@Test
void processOne_shortCircuits_atEnrichStage() {
    UserContextService noUsers = userId -> Option.none(); // fake — never finds a user
    EventRepository    noop    = e -> throw new AssertionError("should not reach store");

    EventImportService svc    = new EventImportService(noUsers, noop);
    RawPayload         payload = new RawPayload("evt-2", validJson, Instant.now());

    Result<StoredEvent, PipelineError> result = svc.processOne(payload);

    assertThat(result).isInstanceOf(Result.Err.class);
    assertThat(((Result.Err<?, PipelineError>) result).error())
        .isInstanceOf(PipelineError.EnrichFailure.class);
    // noop repository was never called — AssertionError would have fired
}
```

### Unit test the batch runner

```java
@Test
void processBatch_partitionsSuccessesAndFailures() {
    // 3 payloads: first and third valid, second has bad JSON
    List<RawPayload> batch = List.of(valid1, badJson, valid3);

    BatchResult<StoredEvent, PipelineError> result = service.processBatch(batch);

    assertThat(result.successes()).hasSize(2);
    assertThat(result.failures()).hasSize(1);
    assertThat(result.failures().get(0)).isInstanceOf(PipelineError.ParseFailure.class);
}
```

No mocking framework required. Fakes are inline lambdas or anonymous implementations. Each test covers one concern.

---

## Common Pipeline Mistakes

### Stages that know about other stages

A stage that reads from the pipeline context or calls the next stage directly is not a stage — it is a service method with a confusing name. Stages should be unaware of their position in the pipeline.

### Using exceptions for expected failures

If a stage fails for a business reason (invalid format, user not found, duplicate key), that failure belongs in `Result.Err`. Exceptions in a pipeline stage are unrecoverable surprises; they should propagate, not be caught and returned.

### A pipeline with no intermediate types

A pipeline where every stage operates on the same type (e.g., `Function<Order, Order>` all the way through) is a signal that the stages are not really separated — they are just methods that mutate the same object. If validation does not produce a new type that proves validation passed, the compiler cannot prevent enrichment from running on an unvalidated order.

### Enriching too early

Enrichment (calling external services, joining with other data) should happen only on data that has already been validated. Enriching raw input before validation means you pay the cost of enrichment even for items that will be rejected.

---

## Summary

A well-modeled data transformation pipeline has four characteristics:

1. **Named intermediate types** — each stage boundary has a type that describes what was true when it was produced.
2. **Result-returning stages** — each stage declares its failure mode; failures are values, not exceptions.
3. **Typed composition** — `flatMap` connects stages; the compiler prevents connecting incompatible ones.
4. **Batch separation** — a batch runner partitions results; callers decide what to do with failures after the run, not during.

The pipeline structure is the architecture. When it is visible in the code, new stages can be added, tested, and removed without changing anything else.

---

## Further reading

- [Railway-Oriented Programming in Java (Without Frameworks)](/dmx-fun/blog/railway-oriented-programming-in-java) — the two-track model that underlies single-item pipeline composition
- [Functional Composition Patterns](/dmx-fun/blog/functional-composition-patterns) — `andThen`, `compose`, and Kleisli composition mechanics
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types) — making stage interfaces self-documenting
- [Functional Design of Business Rules](/dmx-fun/blog/functional-design-of-business-rules) — encoding the validation stage as composable, testable predicates
