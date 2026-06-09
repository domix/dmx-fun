---
title: "Algebraic Data Types Explained for Business Software Developers"
description: "Algebraic Data Types sounds like a math lecture. It is actually a simple idea with a large practical payoff: two building blocks — AND and OR — that let you model business domains so precisely that illegal states cannot be represented in code at all."
pubDate: 2026-06-09
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Fundamentals"
tags: ["Functional Programming", "Java", "Algebraic Data Types", "Domain Modeling", "Sealed Types", "Records", "Design"]
image: "https://images.pexels.com/photos/3184292/pexels-photo-3184292.jpeg"
imageCredit:
    author: "fauxels"
    authorUrl: "https://www.pexels.com/@fauxels/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/photo-of-people-doing-handshakes-3184292/"
---

"Algebraic Data Types" sounds like it belongs in a category theory lecture. The phrase is intimidating enough that most developers skip past it and move on.

That is a shame, because the idea itself is simple — and the practical payoff for anyone writing business software is large.

This post strips the jargon. By the end, you will understand what the two building blocks are, how they map to Java features you already know, and how they let you model business domains so precisely that whole categories of bugs become structurally impossible.

---

## The Problem: Classes That Lie About What Is Possible

Consider a typical Java class modeling an order in an e-commerce system:

```java
public class Order {
    private String id;
    private Customer customer;
    private List<OrderLine> lines;
    private String status;          // "PENDING", "CONFIRMED", "SHIPPED", "CANCELLED"
    private String trackingCode;    // only set if status is "SHIPPED"
    private String cancellationReason; // only set if status is "CANCELLED"
    private boolean refundIssued;   // only relevant if status is "CANCELLED"
    // ... setters, getters
}
```

The class says that every `Order` has a `trackingCode`, a `cancellationReason`, and a `refundIssued` flag. None of that is true. A pending order has none of these. A shipped order has a tracking code but no cancellation reason. A cancelled order has a cancellation reason and a refund flag but no tracking code.

The model *allows* an order with status `"CONFIRMED"` and a `trackingCode` set. It allows `refundIssued = true` on a shipped order. These combinations are nonsense — they do not correspond to any real state. But the code cannot prevent them.

Every developer working with `Order` has to remember, mentally, which fields are valid in which state. When they forget, they write a null check. When the null check is missing, there is a `NullPointerException` in production.

The root cause is that the model is *too permissive*. It can represent states that should not exist. Algebraic Data Types are the tool for fixing this.

---

## Two Building Blocks

Algebraic Data Types are built from exactly two operations.

### Product types: AND

A product type combines multiple fields that are *all present at the same time*. If type `P` is a product of `A` and `B`, then every `P` has an `A` **and** a `B` — always, with no exceptions.

Java records are product types:

```java
record Customer(String id, String email, String name) {}
```

Every `Customer` has an `id` **and** an `email` **and** a `name`. There is no "customer without an email" in this model. The constructor enforces it. The fields are final. The type is honest about what a `Customer` is.

The name "product" comes from the number of possible values: a type with two `boolean` fields can represent 2 × 2 = 4 combinations. The count of possible values is the *product* of the counts of each field.

### Sum types: OR

A sum type says a value is *one of* a fixed set of alternatives. If type `S` is a sum of `A` and `B`, then every `S` is either an `A` **or** a `B` — never both, never something else.

Java sealed interfaces are sum types:

```java
sealed interface Shape permits Shape.Circle, Shape.Rectangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
```

Every `Shape` is either a `Circle` **or** a `Rectangle`. There is no third option. There is no `null`. The compiler enforces this — adding a new `permits` entry is the only way to extend the type.

The name "sum" comes from the same counting logic: the total number of possible values is the *sum* of the counts of each variant.

---

## Remodeling the Order

With these two building blocks, the `Order` class from earlier becomes a precise model.

First, extract the state-specific data into separate types — one product type per state:

```java
// Each state carries exactly the data that exists in that state — no more, no less
record PendingOrder(String id, Customer customer, List<OrderLine> lines, Instant placedAt) {}

record ConfirmedOrder(String id, Customer customer, List<OrderLine> lines,
                      Instant placedAt, Instant confirmedAt, String warehouseId) {}

record ShippedOrder(String id, Customer customer, List<OrderLine> lines,
                    Instant placedAt, Instant confirmedAt, Instant shippedAt,
                    String trackingCode, String carrier) {}

record CancelledOrder(String id, Customer customer, List<OrderLine> lines,
                      Instant placedAt, Instant cancelledAt,
                      String reason, boolean refundIssued) {}
```

Then combine them with a sum type:

```java
sealed interface Order
    permits PendingOrder, ConfirmedOrder, ShippedOrder, CancelledOrder {}
```

Now consider what changed:

- A `ShippedOrder` always has a `trackingCode`. Not sometimes. Always. You cannot create a `ShippedOrder` without one.
- A `PendingOrder` has no `trackingCode` field at all. It cannot be set. It cannot be accidentally read.
- `refundIssued` only exists on `CancelledOrder`. Accessing it on any other state is a compile error.

The type model has become a faithful mirror of the business domain. The compiler now knows what a shipped order is — and it will reject any code that treats a pending order like a shipped one.

---

## The Payoff: Illegal States Are Unrepresentable

This phrase — *make illegal states unrepresentable* — is the practical summary of what ADTs give you.

Before the remodel, this was possible and would compile:

```java
Order order = new Order();
order.setStatus("PENDING");
order.setRefundIssued(true);  // nonsense — but compiles
```

After the remodel, this is impossible — not by convention, not by a runtime check, but by the structure of the types. The compiler enforces it.

The benefit compounds over time. New developers cannot accidentally read `trackingCode` on a pending order — there is no `trackingCode` to read. A method that takes `ShippedOrder` cannot receive a `CancelledOrder` without an explicit cast. The business rules are encoded in the type system, not in comments or assertions.

---

## Working With Sum Types: Exhaustive Pattern Matching

A sum type is only useful if you can branch on which variant you have. Java's exhaustive `switch` expression is the tool for this:

```java
String summary(Order order) {
    return switch (order) {
        case PendingOrder   p -> "Pending since %s".formatted(p.placedAt());
        case ConfirmedOrder c -> "Confirmed, warehouse: %s".formatted(c.warehouseId());
        case ShippedOrder   s -> "Shipped via %s, tracking: %s".formatted(s.carrier(), s.trackingCode());
        case CancelledOrder x -> "Cancelled: %s".formatted(x.reason());
    };
}
```

This switch is **exhaustive** — the compiler requires that every variant of `Order` is handled. If you add `ReturnedOrder` to the sealed interface and forget to add a case here, the code does not compile. There is no runtime surprise. No null check. No `default` branch that silently absorbs new states.

This is qualitatively different from a string or enum switch. A `switch` on `String status` does not fail when you add `"RETURN_REQUESTED"` without updating every handler. An exhaustive sealed switch does.

---

## Nested Composition: Building Complex Domains

Product and sum types compose freely. A product type can contain a sum type; a sum type variant can itself be a product type. This is how real business domains get modeled.

Consider a payment processing domain:

```java
// Sum type: what happened when we tried to charge?
sealed interface ChargeResult
    permits ChargeResult.Authorized, ChargeResult.Declined, ChargeResult.NetworkError {

    // Product types: each outcome carries exactly the data it needs
    record Authorized(String authCode, BigDecimal amount, Instant authorizedAt)
        implements ChargeResult {}

    record Declined(String reason, boolean retryable, DeclineCode code)
        implements ChargeResult {}

    record NetworkError(String endpoint, Duration timeout, int attemptNumber)
        implements ChargeResult {}
}

// Sum type: what is the current state of this invoice?
sealed interface InvoiceStatus
    permits InvoiceStatus.Draft, InvoiceStatus.Issued, InvoiceStatus.Paid,
            InvoiceStatus.Overdue, InvoiceStatus.Voided {

    record Draft(Instant createdAt)                                       implements InvoiceStatus {}
    record Issued(Instant issuedAt, Instant dueDate)                      implements InvoiceStatus {}
    record Paid(Instant paidAt, ChargeResult.Authorized payment)          implements InvoiceStatus {}
    record Overdue(Instant dueDate, int remindersSent)                    implements InvoiceStatus {}
    record Voided(Instant voidedAt, String reason)                        implements InvoiceStatus {}
}

// Product type: the invoice itself
record Invoice(
    String id,
    Customer billTo,
    List<LineItem> lines,
    Money total,
    InvoiceStatus status
) {}
```

Look at `InvoiceStatus.Paid` — it embeds a `ChargeResult.Authorized` directly. The type says: a paid invoice carries the authorization record. Not a string reference. Not a foreign key you might forget to load. The full `Authorized` value, inline.

And `InvoiceStatus.Paid` cannot accidentally embed a `ChargeResult.Declined` — the type won't allow it. A paid invoice, by definition, has an authorization.

---

## ADTs and dmx-fun Types

The functional types in dmx-fun are themselves algebraic data types.

`Option<T>` is a sum type:
- `Option.Some<T>` — a product type with one field: the value
- `Option.None` — a product type with no fields

`Result<V, E>` is a sum type:
- `Result.Ok<V, E>` — a product type carrying the success value
- `Result.Err<V, E>` — a product type carrying the error value

`Either<L, R>`, `Try<V>` — the same pattern.

When you use `Result<Invoice, BillingError>`, you are using an ADT. The `BillingError` is itself typically a sealed interface — another sum type. The `Invoice` is a record — a product type. You are already building with ADTs; the vocabulary just makes the structure explicit.

---

## A Practical Catalog: When to Use Each

### Use product types (records) for:

- **Value objects**: `Money(BigDecimal amount, Currency currency)`, `Address(String street, String city, String country)`
- **Commands and events**: `PlaceOrderCommand(CustomerId, List<CartItem>)`, `OrderPlacedEvent(OrderId, Instant)`
- **Entities with stable structure**: fields that are always present together

### Use sum types (sealed interfaces) for:

- **State machines**: the current state of an order, subscription, payment, or document
- **Discriminated outcomes**: what happened when an operation ran — success, one of several failure modes
- **Multi-variant data**: a notification that is either an email or an SMS; a rule that is either a fixed amount or a percentage

### The combination

Most real domain models are a combination. An entity (product type) holds a status (sum type). Each status variant (product type) holds its state-specific data. A service method returns an outcome (sum type) whose success variant holds the entity (product type).

---

## Why This Matters for Business Software Specifically

General-purpose systems can absorb modeling ambiguity more easily. Business software cannot. The rules are precise: a shipped order has a carrier, a pending one does not. A paid invoice has an authorization code, an overdue one does not. A declined payment has a retry flag, a network error does not.

When those rules live in comments and developer memory, they degrade. When they live in the type system, they are enforced automatically — in every method that handles the type, in every test that creates one, in every refactor that changes the model.

ADTs are the mechanism for encoding business rules into types instead of runtime checks. The compiler becomes the domain expert: it knows what states exist, what data each state carries, and whether every caller handles every case.

That is a different category of safety than unit tests and code reviews — it is structural, and it never sleeps.

---

## Further reading

- [Pattern Matching and Domain Modeling](/dmx-fun/blog/pattern-matching-domain-modeling) — sealed types and exhaustive switches applied to real domain state machines
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types) — making method signatures honest using Option, Result, and Validated
- [Functional Design of Business Rules](/dmx-fun/blog/functional-design-of-business-rules) — encoding domain rules as composable, testable values
- [Monads Without the Smoke and Mirrors](/dmx-fun/blog/monads-without-smoke-and-mirrors) — the same "demystifying" approach applied to another intimidating FP concept
