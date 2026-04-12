---
title: "Should All Business Logic Be Pure?"
description: "Purity is one of the most valuable ideas functional programming offers — but insisting on it everywhere leads to a different kind of mess. Here is how to draw the line in real Java codebases."
pubDate: 2026-04-12
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Design Philosophy", "Architecture", "Best Practices"]
image: "https://images.unsplash.com/39/lIZrwvbeRuuzqOoWJUEn_Photoaday_CSD%20%281%20of%201%29-5.jpg?q=80&w=1470&auto=format&fit=crop&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8fA%3D%3D"
imageCredit:
    author: "Charles Forerunner"
    authorUrl: "https://unsplash.com/@charles_forerunner"
    source: "Unsplash"
    sourceUrl: "https://unsplash.com/es/fotos/personas-de-pie-dentro-del-edificio-de-la-ciudad-3fPXt37X6UQ"
---

The question comes up every time a team starts adopting functional programming ideas in Java:

> "Should we make all our business logic pure?"

The short answer is: as much as possible — but not all of it, and the distinction between what *should* be pure and what *cannot* be is one of the most important design decisions you will make.

This post is about where that line falls, why it matters, and what patterns help you stay on the right side of it.

---

## What "Pure" Actually Means

A function is pure if:

1. **It always returns the same output for the same input.** No hidden state, no reading from a database, no checking the current time.
2. **It produces no observable side effects.** No writing to disk, no sending emails, no mutating a shared variable.

```java
// Pure — the result depends only on the arguments
public static Money calculateTax(Money amount, TaxRate rate) {
    return amount.multiply(rate.value());
}

// Impure — the result depends on the current time, and it writes to a log
public Money calculateTax(Money amount) {
    log.info("Calculating tax at {}", LocalDateTime.now()); // side effect + external state
    return amount.multiply(taxConfig.getCurrentRate());     // reads shared config
}
```

The first version is a fact about your domain. The second is a procedure. The difference sounds subtle until you try to test them.

---

## Why Purity Matters in Business Logic

### Testing Without Infrastructure

Pure functions are trivially testable. No mocks, no database, no in-memory HTTP server. You call the function with an input and assert the output:

```java
@Test
void appliesProportionalDiscountToEligibleOrders() {
    var order = Order.of(Money.of(200, EUR), CustomerTier.GOLD);
    var result = DiscountPolicy.apply(order);
    assertThat(result).isEqualTo(Money.of(180, EUR));
}
```

That test runs in microseconds. It never flakes. It documents the domain rule precisely. Compare this to a test that needs a `@SpringBootTest` context, a mock repository, and a stub clock just to verify a discount calculation.

### Reasoning in Isolation

When business logic is pure, you can understand any function by reading its inputs and outputs. There is no "and also it updates this counter over there" or "unless the feature flag is toggled." The function *is* its body.

This matters especially for domain-critical code: pricing logic, eligibility rules, compliance calculations. When an auditor asks "why did this invoice calculate to this amount?", a pure function gives you a reproducible, inspectable answer. An impure one gives you a log file and hope.

### Composition Without Ceremony

Pure functions compose. If `validateOrder` returns a `Result<ValidOrder, ValidationError>` and `applyPricing` takes a `ValidOrder`, you can chain them without worrying about what state each step is secretly reading or writing:

```java
Result<PricedOrder, OrderError> process(Order order) {
    return validateOrder(order)
        .flatMap(pricingService::applyPricing)
        .flatMap(discountPolicy::apply)
        .flatMap(taxCalculator::calculate);
}
```

Each step is a transformation. The whole pipeline reads like the business process. None of it touches the database — yet.

---

## The Honest Answer: Not All Business Logic Can Be Pure

Here is where the nuance lives. A realistic business process almost always involves:

- Reading the current time (`LocalDateTime.now()`)
- Querying the database for existing records
- Calling an external service (payment gateway, inventory system)
- Generating random identifiers
- Sending notifications

None of these are pure. The question is not "how do we make them pure" but "how do we isolate them from the logic that *can* be pure."

### The Functional Core, Imperative Shell Pattern

The most productive architectural answer is to split your code into two zones:

**Functional core** — pure domain logic. Takes values, returns values. No I/O, no external dependencies, no mutable state.

**Imperative shell** — orchestrates I/O. Reads from the database, calls external services, writes results, handles infrastructure failures. Delegates all decision-making to the core.

```java
// Functional core — pure, no dependencies, fully testable in isolation
public final class OrderProcessor {

    public static Result<ProcessedOrder, OrderError> process(
            Order order,
            List<Discount> applicableDiscounts,
            TaxRate taxRate) {

        return validateOrder(order)
            .flatMap(valid -> applyDiscounts(valid, applicableDiscounts))
            .flatMap(priced -> applyTax(priced, taxRate));
    }

    private static Result<ValidOrder, OrderError> validateOrder(Order order) {
        if (order.items().isEmpty()) {
            return Result.err(OrderError.EMPTY_ORDER);
        }
        if (order.customer() == null) {
            return Result.err(OrderError.MISSING_CUSTOMER);
        }
        return Result.ok(new ValidOrder(order));
    }

    // ... other pure helpers
}

// Imperative shell — orchestrates I/O, delegates decisions to the core
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final DiscountRepository discountRepository;
    private final TaxRateService taxRateService;
    private final EventPublisher eventPublisher;

    public Result<ProcessedOrder, OrderError> submitOrder(Order order) {
        // Gather inputs from the world
        var discounts = discountRepository.findApplicable(order.customer());
        var taxRate   = taxRateService.currentRate(order.shippingAddress());

        // Delegate all logic to the pure core
        var result = OrderProcessor.process(order, discounts, taxRate);

        // Act on the result in the world
        result.onSuccess(processed -> {
            orderRepository.save(processed);
            eventPublisher.publish(new OrderProcessed(processed));
        });

        return result;
    }
}
```

The `OrderProcessor` class is 100% testable without any Spring context, database, or mocked services. The `OrderService` is mostly glue — its tests focus on that glue (did it call the repository? did it publish the event?), not on the business rules.

---

## Where the Line Falls

The pattern above implies a clear principle: **any code that makes a decision should be pure**. Any code that takes action on that decision is allowed to be impure.

| Layer | Pure? | What it does |
|---|---|---|
| Domain model (records, value objects) | Yes | Represents facts about the domain |
| Validation logic | Yes | Checks constraints, returns `Result` or `Validated` |
| Calculation / transformation | Yes | Applies business rules to values |
| Eligibility / policy checks | Yes | Returns booleans or sum types |
| Repository interfaces | No — I/O | Reads and writes from the database |
| External service clients | No — I/O | HTTP calls, queue publishing |
| Event listeners | No — I/O | React to external state changes |
| Scheduled jobs | No — I/O | Trigger orchestration at a point in time |

The closer to the center of your hexagon (domain), the purer the code should be. The closer to the edge (infrastructure), the more impure it necessarily is.

---

## The Time and Randomness Problem

Two frequent challenges to domain purity deserve special attention: **time** and **random identifiers**.

Business logic constantly needs these:
- "Is this coupon still valid?" — requires the current date.
- "Generate a correlation ID." — requires randomness.

The naive approach embeds them directly:

```java
// Impure — logic depends on the system clock
public boolean isCouponValid(Coupon coupon) {
    return coupon.expiresAt().isAfter(LocalDateTime.now()); // hidden dependency
}
```

The pure approach passes them as arguments:

```java
// Pure — the caller provides the clock
public boolean isCouponValid(Coupon coupon, LocalDateTime now) {
    return coupon.expiresAt().isAfter(now);
}
```

This may feel like it just pushes the problem one level up — and it does, deliberately. The imperative shell reads the clock once and passes it in. The domain logic remains a pure predicate over values. Your tests can now pass any timestamp without patching the system clock:

```java
@Test
void expiredCouponIsNotValid() {
    var coupon = Coupon.expiringAt(LocalDate.of(2025, 1, 1).atStartOfDay());
    assertThat(CouponPolicy.isCouponValid(coupon, LocalDate.of(2025, 6, 1).atStartOfDay()))
        .isFalse();
}
```

No `Mockito.mockStatic(LocalDateTime.class)`. No `@TestConfiguration Clock`. Just a function call.

---

## Errors Are Not Side Effects

One common mistake is treating error handling as inherently impure. It is not. A function that returns `Result<V, E>` is perfectly pure — it maps an input to either a success value or a typed error, deterministically.

The impure version is the function that *throws* an exception. Exceptions are control-flow side effects: they jump across the call stack, potentially skip cleanup code, and make the failure invisible in the method signature.

```java
// Impure error handling — throws escape from the type system
public Invoice generateInvoice(OrderId id) throws InvoiceGenerationException {
    var order = orderRepository.findById(id)
        .orElseThrow(() -> new InvoiceGenerationException("order not found: " + id));
    // ...
}

// Pure error handling — failures are values, visible in the signature
public Result<Invoice, InvoiceError> generateInvoice(Order order) {
    if (!order.isEligibleForInvoicing()) {
        return Result.err(InvoiceError.notEligible(order.id()));
    }
    return Result.ok(buildInvoice(order));
}
```

The second version takes a fully-resolved `Order` (no I/O inside), returns a typed result (no exceptions), and is easy to test. The database call that resolves the `Order` belongs in the imperative shell.

---

## A Practical Smell Test

When you are reviewing code and asking "is this too impure?", these are the signs that business logic has leaked into the infrastructure:

**A service method that calls the repository *and* makes the decision:**

```java
// Mixed concerns — the conditional belongs in the domain
public void processRefund(String orderId) {
    var order = orderRepository.findById(orderId).orElseThrow();
    if (order.paidAt().isBefore(LocalDate.now().minusDays(30).atStartOfDay())) {
        throw new RefundWindowExpiredException();
    }
    // ...
}
```

**A domain object that knows about its persistence:**

```java
// Domain model should not touch repositories
public record Order(OrderId id, ...) {
    public void cancel() {
        if (this.status != OrderStatus.PENDING) throw new IllegalStateException();
        orderRepository.save(this.withStatus(OrderStatus.CANCELLED)); // wrong
    }
}
```

**A calculation method that reads configuration from a static global:**

```java
public Money applyMarkup(Money cost) {
    double markup = ConfigStore.get("pricing.markup"); // hidden I/O
    return cost.multiply(1 + markup);
}
```

In each case, the fix follows the same pattern: extract the decision into a pure function, pass the external data as an argument, and leave the I/O in the shell.

---

## How dmx-fun Helps You Stay Pure

The types in `dmx-fun` are designed to make the pure-core pattern feel natural rather than ceremonious.

**`Result<V, E>`** lets domain functions communicate failure without touching the exception mechanism. The failure is a value — composable, returnable, testable:

```java
Result<ShippingLabel, ShippingError> prepareLabel(ValidOrder order, Address destination) {
    if (!carrierRules.supports(destination.country())) {
        return Result.err(ShippingError.unsupportedRegion(destination.country()));
    }
    return Result.ok(ShippingLabel.of(order, destination, labelTemplate));
}
```

**`Option<T>`** replaces nullable return types in lookups that are legitimately empty, keeping absence explicit and safe:

```java
Option<DiscountCode> findActiveCode(String code, LocalDate today) {
    return discountCodes.stream()
        .filter(d -> d.code().equals(code) && d.validThrough().isAfter(today))
        .findFirst()
        .map(Option::some)
        .orElse(Option.none());
}
```

**`Validated<E, A>`** accumulates all domain rule violations instead of failing on the first:

```java
Validated<NonEmptyList<String>, RegistrationRequest> validate(RegistrationRequest req) {
    return Validated.valid(req)
        .combine(validateEmail(req.email()),     (r, _) -> r)
        .combine(validatePassword(req.password()), (r, _) -> r)
        .combine(validateAge(req.birthDate()),   (r, _) -> r);
}
```

None of these types touch a database or an HTTP client. They model facts and failures about the domain. The I/O stays in the shell.

---

## Conclusion

The question "should all business logic be pure?" has a practical answer: **yes, as a goal — and the constraint that prevents 100% purity is I/O, not complexity**.

The useful rule of thumb:

> If a function makes a decision, it should be pure.
> If a function takes action, it is allowed to be impure.

Separate these two concerns consistently and you get a codebase where domain logic is fast to test, easy to audit, and safe to refactor. The imperative shell is thinner than you expect — mostly wiring between data sources and the pure core — and its tests focus on integration, not correctness.

You do not need a Haskell type system to achieve this. You need Java records, sealed interfaces, `Result`, `Option`, and the discipline to pass values in rather than reading global state out. Start with a single service method, extract the decisions into a pure function, and notice how much simpler the tests become. That is the feedback loop that builds the habit.
