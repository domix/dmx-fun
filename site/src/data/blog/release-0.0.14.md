---
title: "dmx-fun 0.0.14 Released"
description: "Version 0.0.14 is the ecosystem release: Spring transaction support, Spring Boot autoconfiguration, Micrometer metrics, Resilience4J adapters, Guard, Accumulator, Resource, NonEmptyMap, NonEmptySet, Try.timeout, and a production-like Spring Boot sample — all in one milestone."
pubDate: 2026-04-22
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Release"
tags: ["Release", "Spring", "SpringBoot", "Micrometer", "Resilience4J", "Guard", "Accumulator", "Resource", "NonEmptyMap", "NonEmptySet"]
image: "https://images.unsplash.com/photo-1558494949-ef010cbdcc31?q=80&w=1634&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Jordan Harrison"
    authorUrl: "https://unsplash.com/@jordanharrison"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/photos/blue-and-black-server-racks-40XgDxBfYXM"
---

Version **0.0.14** is the ecosystem release. Where previous milestones built out the core
type system, this one connects `dmx-fun` to the frameworks and infrastructure that
production Java applications actually run on: Spring, Spring Boot, Micrometer, and
Resilience4J. Five new production modules ship alongside new core types, collector façades,
and a full Spring Boot reference application.

Here is everything that changed.

---

## fun-spring — transaction support for Result, Try, and Validated

Spring's `@Transactional` rolls back only when an unchecked exception escapes the method.
`Result`, `Try`, and `Validated` capture failures as return values — so transactions commit
silently even when an operation fails. `fun-spring` fixes this.

### Programmatic: TxResult, TxTry, TxValidated

```java
@Service
public class OrderService {
    private final TxResult tx;
    private final OrderRepository repo;

    public Result<Order, String> place(PlaceOrderCommand cmd) {
        return tx.execute(() -> {
            if (cmd.quantity() <= 0) return Result.err("quantity must be positive");
            Order order = repo.save(new Order(cmd));
            return Result.ok(order);
        });
    }
}
```

`TxResult.execute` commits when the result is `Ok` and rolls back when it is `Err`.
`TxTry` and `TxValidated` follow the same contract for their respective types.
All three accept an explicit `TransactionDefinition` for propagation, isolation, and timeout.

### Declarative: @TransactionalResult, @TransactionalTry, @TransactionalValidated

```java
@Service
public class InventoryService {

    @TransactionalResult
    public Result<Item, String> reserve(long itemId, int qty) {
        // any Err return rolls back; any unchecked exception also rolls back and rethrows
        return repo.findById(itemId)
            .map(item -> item.reserve(qty))
            .toResult(() -> "item not found");
    }
}
```

`fun-spring` declares `spring-tx` and `spring-context` as `compileOnly`. Spring **6.0.x,
6.1.x, 6.2.x, and 7.0.x** are tested in CI.

```kotlin
// Gradle (Kotlin DSL)
implementation("codes.domix:fun-spring:0.0.14")
```

---

## fun-spring-boot — zero-configuration Spring Boot integration

`fun-spring-boot` auto-configures everything `fun-spring` provides — no `@Bean`
declarations, no `@EnableAspectJAutoProxy`. Drop the dependency in and it works.

```kotlin
// Gradle (Kotlin DSL) — replaces fun-spring; brings fun-spring transitively
implementation("codes.domix:fun-spring-boot:0.0.14")
```

Spring Boot picks up `DmxFunSpringAutoConfiguration` automatically and registers:

| Bean | Registered when |
|---|---|
| `TxResult`, `TxTry`, `TxValidated` | `spring-tx` on classpath and a `PlatformTransactionManager` present |
| `DmxTransactionalAspect` | Same, plus `aspectjweaver` on classpath |
| `DmxFunModule` | `jackson-databind` and `fun-jackson` on classpath |
| MVC return value handlers | `spring-webmvc` on classpath |

### Spring MVC return value handlers

Controllers can now return dmx-fun types directly — no manual `fold` or `getOrElse` needed.

```java
@RestController
@RequestMapping("/items")
public class ItemController {

    @GetMapping("/{id}")
    public Option<Item> findById(@PathVariable long id) {
        return service.findById(id);
        // Some(item) → HTTP 200 with item body
        // None()     → HTTP 404 with empty body
    }

    @PostMapping
    public Result<Item, String> create(@RequestBody CreateItemRequest req) {
        return service.create(req.name(), req.description());
        // ok(item)   → HTTP 200 with item body
        // err(msg)   → HTTP 500 with error body
    }

    @PutMapping("/{id}")
    @TransactionalResult
    public Result<Item, String> update(@PathVariable long id, @RequestBody UpdateItemRequest req) {
        return service.update(id, req.name());
        // same HTTP mapping + automatic transaction rollback on err
    }
}
```

Both handlers are enabled by default and can be toggled independently:

```properties
dmx.fun.mvc.option-handler.enabled=false  # disable Option → 200/404 handler
dmx.fun.mvc.result-handler.enabled=false  # disable Result/Try/Validated → 200/500 handler
```

`DmxFunModule` is registered as a Spring bean so `JacksonAutoConfiguration` picks it up
automatically — all dmx-fun types serialize to JSON with no extra wiring
(`dmx.fun.jackson.enabled=false` to opt out).

Spring Boot **3.3.x, 3.4.x, 3.5.x, and 4.0.x** are tested in CI.

---

## fun-micrometer — automatic metrics for Try and Result

`fun-micrometer` records counters, timers, and failure metrics for any `Try`- or
`Result`-returning operation.

```java
MeterRegistry registry = ...; // your Micrometer registry
DmxMicrometer micrometer = DmxMicrometer.of(registry);

DmxMetered<PaymentResponse> metered = micrometer.metered(
    "payment.charge",
    () -> gateway.charge(cmd)   // returns Try<PaymentResponse>
);

Try<PaymentResponse> result = metered.executeTry();
// Records: payment.charge.count (tags: outcome=success or outcome=failure)
//          payment.charge.timer
```

Micrometer is declared `compileOnly`. Versions **1.5.x through 1.16.x** are tested in CI.

```kotlin
implementation("codes.domix:fun-micrometer:0.0.14")
```

---

## fun-resilience4j — Resilience4J adapters that return Try and Result

`fun-resilience4j` wraps Resilience4J's `Retry`, `CircuitBreaker`, `RateLimiter`, and
`Bulkhead` so that executions return `Try<V>` or `Result<V, E>` instead of throwing.

```java
DmxRetry retry = DmxRetry.of("payment",
    RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.ofMillis(200))
        .retryOnException(IOException.class::isInstance)
        .build());

DmxCircuitBreaker cb = DmxCircuitBreaker.of("payment",
    CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .build());

// Compose: retry inside circuit breaker
Result<Receipt, Throwable> result = cb.executeResult(() ->
    retry.executeTry(() -> gateway.charge(cmd)).getOrThrow()
);
```

Each adapter exposes three execution methods:

| Method | Returns | Policy rejection | Call failure |
|---|---|---|---|
| `executeTry(supplier)` | `Try<V>` | `Failure(policyEx)` | `Failure(cause)` |
| `executeResult(supplier)` | `Result<V, Throwable>` | `Err(policyEx)` | `Err(cause)` |
| `executeResultTyped(supplier)` | `Result<V, PolicyEx>` | `Err(policyEx)` | rethrows unchecked |

Resilience4J is declared `compileOnly`. Versions **2.0.2, 2.1.0, 2.2.0, 2.3.0, and 2.4.0**
are tested in CI via a dedicated compatibility matrix.

```kotlin
implementation("codes.domix:fun-resilience4j:0.0.14")
// Add only the Resilience4J artifacts you use:
implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
```

---

## Guard\<T\> — composable validation predicates

`Guard<T>` is a composable predicate designed to work naturally with `Validated` for
error-accumulating validation.

```java
Guard<String> notBlank  = Guard.of(s -> !s.isBlank(),  "must not be blank");
Guard<String> maxLen100 = Guard.of(s -> s.length() <= 100, "max 100 characters");
Guard<String> nameGuard = notBlank.and(maxLen100);

// Short-circuit: skip maxLen100 when notBlank already fails
Guard<String> safe = notBlank.andThen(maxLen100);

Validated<String, String> result = nameGuard.validate("  ");
// Invalid("must not be blank")
```

`Guard.nonNull()` provides a built-in null check. `and`, `or`, and `negate` combine guards
with the usual boolean semantics.

---

## Accumulator\<E, A\> — value with an accumulated side-channel

`Accumulator<E, A>` pairs a primary value with a side-channel of accumulated entries.
Think of it as `Writer` from functional programming: run a computation and collect
warnings, audit entries, or debug logs alongside the result.

```java
Accumulator<String, Order> result = Accumulator.pure(order)
    .tell("address normalized")
    .tell("duplicate check passed")
    .map(o -> o.withStatus(Status.CONFIRMED));

result.value();       // Order(status=CONFIRMED)
result.accumulated(); // ["address normalized", "duplicate check passed"]
```

`flatMap` threads both the value and the accumulated channel through a chain of operations.

---

## Resource\<T\> — functional managed resources

`Resource<T>` pairs resource acquisition with guaranteed release, composable via `map` and
`flatMap` without breaking the acquire/release contract.

```java
Resource<Connection> connResource = Resource.of(
    () -> dataSource.getConnection(),
    Connection::close
);

Resource<PreparedStatement> stmtResource = connResource.flatMap(conn ->
    Resource.of(
        () -> conn.prepareStatement("SELECT * FROM items WHERE id = ?"),
        PreparedStatement::close
    )
);

// Both connection and statement are closed even if the function throws
Result<Item, Throwable> item = stmtResource.useAsResult(stmt -> {
    stmt.setLong(1, id);
    ResultSet rs = stmt.executeQuery();
    return Item.from(rs);
});
```

---

## NonEmptyMap\<K, V\> and NonEmptySet\<T\>

Two new non-empty collection types join `NonEmptyList`:

```java
// NonEmptyMap — at least one entry guaranteed
NonEmptyMap<String, Integer> scores = NonEmptyMap.of("alice", 10,
    Map.of("bob", 8, "carol", 9));

scores.get("alice");          // 10
scores.toMap();               // regular Map
scores.mapValues(n -> n * 2); // NonEmptyMap<String, Integer>

// NonEmptySet — at least one element guaranteed
NonEmptySet<String> roles = NonEmptySet.of("admin", Set.of("user"));
roles.contains("admin");      // true
roles.union(NonEmptySet.of("guest", Set.of())); // NonEmptySet with all elements
```

---

## More additions to the core library

### Try.timeout(Duration)

```java
Try<Report> report = Try.timeout(Duration.ofSeconds(5), () -> generateReport(params));
// Failure(TimeoutException) if deadline exceeded; virtual thread is interrupted cleanly
```

### Validated.combine3 and combine4

```java
Validated<String, Address> address = Validated.combine3(
    validateStreet(street),
    validateCity(city),
    validatePostalCode(zip),
    Address::new,
    (e1, e2) -> e1 + "; " + e2
);
```

### Option.zipWith and Option.flatZip

```java
Option<String> full = firstName.zipWith(lastName, (f, l) -> f + " " + l);
// None if either is absent; Some("Alice Smith") if both present

Option<Tuple2<User, Profile>> pair = userOption.flatZip(u -> profileRepo.find(u.id()));
// keeps both the user and the profile in a Tuple2
```

### NonEmptyList.first() and last()

`NonEmptyList` now implements `SequencedCollection<T>`:

```java
NonEmptyList<Integer> nums = NonEmptyList.of(1, List.of(2, 3, 4));
nums.first(); // 1  — always non-null
nums.last();  // 4  — always non-null
```

### Results, Options, Tries collector façades

```java
List<Result<Item, String>> results = items.stream()
    .map(service::process)
    .toList();

// Collect into a single Result — fails fast on first Err
Result<List<Item>, String> combined = results.stream()
    .collect(Results.collector());

// Group by outcome in one pass
Map<Boolean, List<Result<Item, String>>> grouped = results.stream()
    .collect(Result.groupingBy(Result::isOk));
```

### fun-assertj additions

`DmxFunAssertions` gains assertions for `Resource`, `Guard`, and `Accumulator`:

```java
assertThat(guard).accepts("valid input");
assertThat(guard).rejects("  ");

assertThat(accumulator).hasValue(expectedOrder);
assertThat(accumulator).accumulatedContains("address normalized");
```

---

## Spring Boot reference application

The new `spring-boot-sample` module is a complete end-to-end Spring Boot application that
demonstrates all four patterns working together:

- `ItemController` — returns `Option<Item>` (GET) and `Result<Item, String>` (POST, PUT)
  directly from controller methods, handled by the MVC return value handlers
- `ItemService` — uses `@TransactionalResult` for all mutating operations
- Full Testcontainers integration tests verifying the HTTP → service → database round-trip

It serves as the definitive reference for integrating `dmx-fun` into a real Spring Boot
application.

---

## Getting the release

```kotlin
// Gradle (Kotlin DSL) — core library
implementation("codes.domix:fun:0.0.14")

// Spring Boot integration (brings fun-spring transitively)
implementation("codes.domix:fun-spring-boot:0.0.14")

// Optional modules
implementation("codes.domix:fun-micrometer:0.0.14")
implementation("codes.domix:fun-resilience4j:0.0.14")
implementation("codes.domix:fun-jackson:0.0.14")
testImplementation("codes.domix:fun-assertj:0.0.14")
```

```xml
<!-- Maven -->
<dependency>
  <groupId>codes.domix</groupId>
  <artifactId>fun</artifactId>
  <version>0.0.14</version>
</dependency>
```

See the [full changelog](https://github.com/domix/dmx-fun/blob/main/CHANGELOG.md) for the
complete list of changes, fixes, and build improvements in this release.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun/issues).*
