---
title: "Pattern Matching and Domain Modeling"
description: "Sealed types make your domain states explicit and exhaustive. Pattern matching forces every caller to handle them all. Together they turn a class of runtime surprises into compile-time errors — and make the business model readable in the type signatures."
pubDate: 2026-05-22
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Domain Modeling", "Pattern Matching", "Sealed Types", "Design"]
image: "https://images.pexels.com/photos/1181271/pexels-photo-1181271.jpeg"
imageCredit:
    author: "Christina Morillo"
    authorUrl: "https://www.pexels.com/@divinetechygirl/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/photo-of-two-people-using-laptops-1181271/"
---

Every backend service has states that the business cares about deeply. An order is pending,
confirmed, shipped, or cancelled. A payment is authorized, captured, refunded, or failed. A
notification is queued, delivered, or bounced.

In most Java codebases these states are represented as strings, enums with `if/else` chains,
or boolean flags scattered across the model. The compiler does not know what states exist. It
cannot tell when a caller fails to handle one. It cannot tell when a new state is added and
old handlers are not updated.

The result is a domain that lives in the developer's head — or in a wiki page — rather than
in the code itself. Adding a state means searching for every place that might need to handle
it and hoping you found them all.

Sealed types and pattern matching change this. The compiler becomes the guardian of the domain
model: it knows every state, it enforces that every caller handles every state, and it breaks
at compile time when a new state is added and a handler is missing.

---

## The Problem with Implicit States

Consider a simplified order processing system. A common first approach:

```java
public class Order {
    private String status; // "PENDING", "CONFIRMED", "SHIPPED", "CANCELLED"
    private String cancellationReason;
    private LocalDateTime shippedAt;
    private String trackingCode;
}
```

The problems are not immediately obvious:

- `cancellationReason` is only meaningful when `status` is `"CANCELLED"`. For all other
  states it is `null`. Nothing in the type prevents reading it on a confirmed order.
- `trackingCode` and `shippedAt` are only meaningful when `status` is `"SHIPPED"`. Same issue.
- Adding a new status — say `"RETURNED"` — requires a text search across every method that
  switches on `status`. The compiler will not tell you where.
- A typo (`"CANCLLED"`) is a runtime bug, not a compile error.

An enum is better — it eliminates the typo problem — but it does not solve the data problem.
An `Order` in the `SHIPPED` state and an `Order` in the `PENDING` state have the same shape.
The data that belongs to a specific state is not co-located with that state.

---

## Sealed Types: Making States First-Class

A sealed interface restricts which classes can implement it. Combined with records, it gives
each state its own data shape — only the data that is meaningful in that state exists in that
state.

```java
public sealed interface OrderStatus
    permits OrderStatus.Pending,
            OrderStatus.Confirmed,
            OrderStatus.Shipped,
            OrderStatus.Cancelled {

    record Pending(LocalDateTime placedAt)
        implements OrderStatus {}

    record Confirmed(LocalDateTime confirmedAt, String warehouseId)
        implements OrderStatus {}

    record Shipped(LocalDateTime shippedAt, String trackingCode, String carrier)
        implements OrderStatus {}

    record Cancelled(LocalDateTime cancelledAt, String reason, boolean refundIssued)
        implements OrderStatus {}
}
```

Now:

- A `Shipped` order always has a `trackingCode` and a `carrier`. They are guaranteed non-null.
- A `Cancelled` order always has a `reason` and a `refundIssued` flag. You cannot access them
  on a `Pending` order because a `Pending` order does not have them.
- The complete list of valid states is in one place, visible in the `permits` clause.
- Adding a new state means adding a new `record` — and the compiler will find every switch
  that is not exhaustive.

The `Order` aggregate becomes cleaner:

```java
public record Order(String id, Customer customer, List<OrderLine> lines, OrderStatus status) {}
```

The status carries its own data. The `Order` record holds no nullable state-specific fields.

---

## Pattern Matching: Exhaustive Handling

Sealed types define the states. Pattern matching forces callers to handle all of them.

```java
String summary = switch (order.status()) {
    case OrderStatus.Pending    p -> "Placed at " + p.placedAt();
    case OrderStatus.Confirmed  c -> "Confirmed by warehouse " + c.warehouseId();
    case OrderStatus.Shipped    s -> "In transit — tracking: " + s.trackingCode();
    case OrderStatus.Cancelled  x -> "Cancelled: " + x.reason()
                                     + (x.refundIssued() ? " (refunded)" : " (no refund)");
};
```

If you add `Returned` to the sealed interface and forget to add it to this switch, the code
does not compile. The compiler enforces the exhaustiveness guarantee.

Each branch binds the matched record to a local variable (`p`, `c`, `s`, `x`) with the
specific type of that state. Inside the `Shipped` branch, `s` is a `Shipped` — you can call
`s.trackingCode()` directly, with no cast.

---

## Guarded Patterns: Business Rules Inside the Switch

Pattern matching in Java supports guards — conditions that narrow a match further without
nesting `if` statements inside the branch.

```java
BigDecimal shippingCost = switch (order.status()) {
    case OrderStatus.Shipped s when s.carrier().equals("EXPRESS") -> new BigDecimal("9.99");
    case OrderStatus.Shipped s                                     -> new BigDecimal("4.99");
    case OrderStatus.Confirmed c when c.warehouseId().startsWith("MX") -> BigDecimal.ZERO;
    default -> BigDecimal.ZERO;
};
```

Guards let you express business conditions as part of the dispatch rather than as nested logic
after the dispatch. The compiler still checks exhaustiveness across all guarded and unguarded
cases combined.

---

## Modeling Outcomes, Not Just States

The same pattern applies to operation results. Instead of returning a boolean and setting
fields on a shared object, return a sealed type that represents the outcome:

```java
public sealed interface PaymentOutcome
    permits PaymentOutcome.Authorized,
            PaymentOutcome.Declined,
            PaymentOutcome.Failed {

    record Authorized(String authorizationCode, BigDecimal amount)
        implements PaymentOutcome {}

    record Declined(String reason, boolean retryable)
        implements PaymentOutcome {}

    record Failed(String gatewayError, boolean shouldAlert)
        implements PaymentOutcome {}
}
```

The payment service signature becomes:

```java
public PaymentOutcome authorize(PaymentRequest request) { ... }
```

The caller cannot ignore the declined or failed cases — they have to handle them to get any
value from the result. And each case carries exactly the data needed to act on it:

```java
return switch (paymentService.authorize(request)) {
    case PaymentOutcome.Authorized a ->
        orderService.confirm(order, a.authorizationCode());

    case PaymentOutcome.Declined d when d.retryable() ->
        scheduleRetry(order, request);

    case PaymentOutcome.Declined d ->
        orderService.cancel(order, "Payment declined: " + d.reason());

    case PaymentOutcome.Failed f -> {
        if (f.shouldAlert()) alertService.notify(f.gatewayError());
        yield orderService.cancel(order, "Payment gateway error");
    }
};
```

Every outcome is handled. The logic for each outcome is co-located with the outcome itself.
Adding `Disputed` to `PaymentOutcome` makes this switch a compile error until it is handled.

---

## The Progression: From Strings to Types

It helps to see the full progression to understand what each step gains.

**Step 1 — String flags** (where most code starts):

```java
if (order.getStatus().equals("SHIPPED")) {
    System.out.println(order.getTrackingCode()); // may be null
}
```

Problems: typos compile, null fields, no exhaustiveness.

**Step 2 — Enum**:

```java
if (order.getStatus() == OrderStatus.SHIPPED) {
    System.out.println(order.getTrackingCode()); // still may be null
}
```

Improvement: no typos. Still no exhaustiveness guarantee in `if/else` chains, still nullable
state-specific fields.

**Step 3 — Sealed type with records**:

```java
if (order.status() instanceof OrderStatus.Shipped s) {
    System.out.println(s.trackingCode()); // guaranteed non-null
}
```

Improvement: state-specific data is co-located and non-null. Still no exhaustiveness in
`instanceof` chains.

**Step 4 — Sealed type + exhaustive switch** (the full model):

```java
String label = switch (order.status()) {
    case OrderStatus.Pending    p -> "Pending since " + p.placedAt().toLocalDate();
    case OrderStatus.Confirmed  c -> "Confirmed";
    case OrderStatus.Shipped    s -> "Shipped via " + s.carrier();
    case OrderStatus.Cancelled  x -> "Cancelled";
};
```

All benefits: no nulls, no typos, no missing cases, no searching for handlers when a state
is added.

---

## Connecting to dmx-fun

The types in dmx-fun are sealed types. `Result<V, E>` has exactly two states: `Ok` and `Err`.
`Option<T>` has exactly two: `Some` and `None`. `Try<V>` has exactly two: `Success` and
`Failure`. `Either<L, R>` has exactly two: `Left` and `Right`.

Every `fold`, `map`, `flatMap`, and `match` call on these types is pattern matching under the
hood — each one takes a function per branch and handles all states explicitly.

```java
Result<Order, OrderError> result = orderService.place(request);

// fold is exhaustive pattern matching over Result's two states
String response = result.fold(
    order -> "Order " + order.id() + " confirmed",
    error -> switch (error) {
        case OrderError.InsufficientStock e -> "Out of stock: " + e.sku();
        case OrderError.InvalidAddress    e -> "Invalid address: " + e.field();
        case OrderError.PaymentFailed     e -> "Payment failed: " + e.reason();
    }
);
```

The inner `switch` on `OrderError` is itself exhaustive — if you add a new error variant, the
compiler finds the switch. The outer `fold` guarantees both the success and failure branches
are handled. Two levels of exhaustiveness, no `if/else`, no unchecked casts.

When you model domain outcomes as sealed types and handle them with pattern matching, `fold`
becomes the natural termination point: collapse the type into whatever the caller needs —
an HTTP response, a log line, a downstream event — exhaustively, at the boundary.

---

## When to Use Sealed Types for Domain Modeling

The right candidates are states or outcomes where:

- **The set of variants is closed** — you define them all and they do not grow arbitrarily
  at runtime. Order statuses, payment outcomes, notification results, validation decisions.
- **Each variant has different data** — if every variant has the same fields, a plain record
  with an enum field is simpler.
- **Callers need to react differently** — if every caller would do the same thing regardless
  of the variant, the distinction is not worth the type.
- **Missing a case is a bug** — if forgetting to handle `Cancelled` in a billing report
  would silently produce wrong output, exhaustiveness checking is worth the investment.

The wrong candidates are:

- Open-ended hierarchies that external code must extend — sealed types cannot be extended
  outside the module.
- Simple flags that are always handled the same way — `boolean active` does not need a
  sealed type.
- Configuration variants that users define at runtime — those belong in a database, not in
  a sealed interface.

---

## Conclusion

Sealed types are a modeling tool before they are a language feature. They answer the question
"what are the valid states of this value?" at the type level — not in a comment, not in a wiki,
not in a developer's head.

Pattern matching forces every caller to answer the question "what do I do in each state?" at
the call site — not optionally, not with a default that silently absorbs new cases, but
exhaustively.

Together they produce a domain model where the compiler enforces what the business requires:
every state exists, every state carries the right data, and every state is handled everywhere
it appears. Adding a state is a refactoring exercise guided by compile errors, not a search
for forgotten handlers.

That is not a style preference. It is a structural reduction in the category of bugs that
come from implicit, untested, and undocumented states.

---

## Further reading

- [Functional Design of Business Rules](/dmx-fun/blog/functional-design-of-business-rules) — composable predicates and typed outcomes for business logic
- [Railway-Oriented Programming in Java](/dmx-fun/blog/railway-oriented-programming-in-java) — `Result` as a sealed two-state type for error handling pipelines
- [Developer Guide](/dmx-fun/guide/) — `Result`, `Option`, `Try`, and `Either` — sealed types with exhaustive fold/match APIs
