---
title: "Pure Functions and Side Effects"
description: "A pure function always returns the same output for the same input and changes nothing in the world around it. That single rule — predictable output, no hidden changes — is the foundation that makes functional code easier to test, reason about, and refactor."
pubDate: 2026-06-12
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Fundamentals"
tags: ["Functional Programming", "Java", "Pure Functions", "Side Effects", "Immutability", "Testability", "Design"]
image: "https://images.pexels.com/photos/37911188/pexels-photo-37911188.jpeg"
imageCredit:
    author: "Shubham Dhage"
    authorUrl: "https://www.pexels.com/@shubham-dhage-18137965/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/colorful-geometric-boxes-with-mathematical-symbols-37911188/"
---

Every concept in functional programming eventually traces back to one idea: pure functions.

Not because purity is the goal in itself — most useful programs have to read a database, write a log, or send an HTTP request. But because understanding what purity means, and what it costs to give it up, is the foundation for every practical decision that follows.

This post explains what a pure function is, what a side effect is, how to recognize both in real Java code, and why the distinction matters more than it might first appear.

---

## The Definition

A function is **pure** if it satisfies two conditions:

1. **Deterministic** — it always returns the same output for the same input. Given the same arguments, you always get the same result. No exceptions.
2. **No side effects** — it does not change anything observable outside itself. It reads no external state, modifies no shared data, writes nothing, logs nothing, throws nothing.

Both conditions must hold. A function that is deterministic but writes to a file is not pure. A function that has no observable side effects but returns a random value is not pure.

The simplest example:

```java
// Pure: same input → same output, changes nothing
static int add(int a, int b) {
    return a + b;
}
```

`add(3, 4)` is always `7`. Today, tomorrow, in production, in a test. It has no memory of past calls. It cannot be affected by anything external. It cannot affect anything external.

Now contrast:

```java
// Not pure: reads from an external source — result depends on when it is called
static int currentYear() {
    return LocalDate.now().getYear();
}
```

`currentYear()` returns different values in different years. It has the same signature — zero arguments, one return value — but its output depends on the state of the system clock. It is not deterministic.

---

## What a Side Effect Is

A **side effect** is any observable change that a function makes to the world outside its own stack frame.

The most common side effects in Java code:

**Writing to external state**
```java
// Side effect: modifies a shared list
static void addItem(List<String> items, String item) {
    items.add(item);  // mutates the argument
}
```

**Logging**
```java
// Side effect: writes to a log
static double divide(double a, double b) {
    if (b == 0) {
        log.warn("Division by zero attempted");  // side effect
        return 0.0;
    }
    return a / b;
}
```

**Throwing exceptions**
```java
// Side effect: throws — alters the call stack instead of returning a value
static User findUser(String id) {
    User user = repository.findById(id);
    if (user == null) throw new UserNotFoundException(id);  // side effect
    return user;
}
```

**Calling I/O**
```java
// Side effect: reads from a database
static Order loadOrder(String orderId) {
    return orderRepository.findById(orderId);  // depends on external state
}
```

**Reading mutable shared state**
```java
// Side effect: reads from a shared field that may change
static String currentRegion() {
    return Config.INSTANCE.getRegion();  // depends on when/how Config was set
}
```

The common thread: the function's behavior, or the world's state, is different after the call than before — in a way that is not captured in the return value.

---

## Why Purity Matters

The benefits of pure functions are not abstract. They show up in concrete ways during development.

### Testing becomes trivial

A pure function test needs nothing:

```java
@Test
void add_returnsCorrectSum() {
    assertThat(add(3, 4)).isEqualTo(7);
}
```

No database. No mock. No `@SpringBootTest`. No fixture setup. No teardown. The function has no external dependencies, so the test has no external dependencies.

An impure function test needs all of that:

```java
@Test
void loadOrder_returnsOrder_whenOrderExists() {
    // Need a database or a mock
    when(orderRepository.findById("ord-1")).thenReturn(someOrder);
    // Now test
    Order result = service.loadOrder("ord-1");
    assertThat(result).isNotNull();
}
```

The test has complexity not because the logic is complex, but because the function reaches into external state.

### Reasoning becomes local

With a pure function, you can understand what it does by reading it. The entire behavior is visible in the function body — no hidden dependencies on global state, no behavior that changes based on when it is called.

With an impure function, understanding what it does requires knowing: what is the current state of every system it touches? What did callers do before this? What will callers do after?

This is the essential difference between local reasoning and global reasoning. Pure functions allow local reasoning.

### Refactoring becomes safe

A pure function can be safely moved, extracted, inlined, or memoized. It has no coupling to call order, to shared state, or to external systems. Its only contract is: given these inputs, return this output.

An impure function cannot be moved freely. Changing when it is called — before or after some other function — might change what it does, because both functions may read or write the same state.

---

## A Real Example: Pricing Logic

Consider a discount calculation that starts as a service method:

```java
// Impure: reads from a field, logs a side effect
public class PricingService {
    private final DiscountPolicy policy;  // shared mutable state

    public Money applyDiscount(Money price, Customer customer) {
        BigDecimal rate = policy.rateFor(customer.tier());  // reads external state
        if (rate.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Applying {}% discount to {}", rate, customer.id());  // side effect
        }
        return price.multiply(BigDecimal.ONE.subtract(rate));
    }
}
```

Testing this requires constructing a `PricingService` with a `DiscountPolicy`, controlling what `policy.rateFor()` returns, and potentially suppressing log output.

Extract the pure core:

```java
// Pure: given a price and a rate, return the discounted price
static Money discountedPrice(Money price, BigDecimal discountRate) {
    return price.multiply(BigDecimal.ONE.subtract(discountRate));
}
```

The service method becomes a thin shell that reads state, calls the pure core, and handles the side effects:

```java
public Money applyDiscount(Money price, Customer customer) {
    BigDecimal rate = policy.rateFor(customer.tier());      // impure: reads external
    Money result = discountedPrice(price, rate);            // pure: the actual logic
    if (rate.compareTo(BigDecimal.ZERO) > 0) {
        log.info("Applying {}% discount to {}", rate, customer.id()); // impure: I/O
    }
    return result;
}
```

Now the business logic — "how do you apply a discount to a price?" — is a one-line pure function that can be tested with three lines of code, reused anywhere, and documented by its signature alone.

---

## The Purity Spectrum

Real programs are not purely pure or purely impure. They live on a spectrum, and the practical goal is to push as much logic as possible toward the pure end while keeping side effects at the edges.

```
Pure ◀────────────────────────────────────────────────────▶ Impure

discountedPrice()     applyDiscount()     loadOrder()     log.info()
(pure math)           (reads policy)      (database)      (I/O)
```

The useful heuristic: **pure core, impure shell**. Keep your business logic in pure functions. Push your I/O, your database access, your logging, and your exception handling to the outer layer that orchestrates those pure functions.

This is not a new idea — it is the organizing principle behind hexagonal architecture, clean architecture, and the functional core / imperative shell pattern. What makes it functional is making it explicit: not just a convention, but a structural distinction visible in the code.

---

## Identifying Side Effects in Code Review

When reviewing a method for purity, ask these questions:

1. Does the method read anything not passed as an argument? (Fields, static state, clocks, random number generators, environment variables)
2. Does the method write anything outside its local scope? (Arguments that are collections or mutable objects, static fields, files, databases, logs)
3. Does the method throw an exception? (Exceptions are control-flow side effects — they bypass the return type)
4. Does the method call any method that answers yes to 1–3?

If the answer to any of these is yes, the method has a side effect. That is not a bug — it is a fact about the method. The question is whether the side effect is intentional, controlled, and at the right layer.

---

## When Purity Is Not the Goal

Pure functions are a tool, not a religion.

A function that sends an email *should* send an email. Making it pure would mean not sending the email — which defeats the purpose. The goal is not to eliminate side effects, but to separate them from the logic that computes what should happen.

```java
// Pure: decide what email should be sent
static EmailDraft buildWelcomeEmail(User user, LocalDate signUpDate) {
    return new EmailDraft(
        user.email(),
        "Welcome, " + user.firstName() + "!",
        renderTemplate("welcome", user, signUpDate)
    );
}

// Impure: execute the decision
void sendWelcomeEmail(User user) {
    EmailDraft draft = buildWelcomeEmail(user, LocalDate.now());  // pure
    mailer.send(draft);                                            // side effect
}
```

The pure function is tested exhaustively — every edge case, every template rendering. The impure function is tested with an integration test or a simple check that `mailer.send` was called with the right draft.

The division of responsibility makes both easier to test and easier to change independently.

---

## Referential Transparency

Pure functions have a property called **referential transparency**: any call to the function can be replaced with its return value without changing the program's behavior.

```java
// If add is pure, these two programs are equivalent
int x = add(3, 4) + add(3, 4);

// You can always replace a pure call with its result
int x = 7 + 7;
```

This sounds trivial, but it is a powerful guarantee. It means you can:
- Cache the return value and reuse it safely (memoization)
- Evaluate the function in any order (parallelism)
- Move the call to a different point in the program without changing behavior (refactoring)

None of these are safe with impure functions. A function that writes to a database twice produces different results than a function that writes once and reuses the value. Evaluating in a different order changes which write happens first.

Referential transparency is the property that makes pure functions composable — you can combine them freely because each one is an isolated, self-contained computation.

---

## Summary

A pure function:
- Returns the same output for the same input, every time
- Changes nothing outside itself

A side effect is any change to the world outside the function's own stack frame: mutation, I/O, logging, thrown exceptions, reads from shared mutable state.

Pure functions are easy to test, easy to reason about, and safe to refactor. They earn those properties precisely because they are isolated.

The practical goal in most programs is not full purity — it is the *separation* of pure logic from impure operations. The business rules go in the pure core. The database access, the I/O, the logging go in the impure shell that orchestrates the core. The compiler cannot enforce this separation, but the discipline pays off in every test you write, every refactor you make, and every bug you do not have to chase through global state.

---

## Further reading

- [What Referential Transparency Really Means](/dmx-fun/blog/referential-transparency) — the formal property that makes pure functions composable and memoizable
- [Functional Core, Imperative Shell](/dmx-fun/blog/functional-core-imperative-shell) — the architecture pattern that puts purity to work in real systems
- [Immutability: The Foundation of Functional Programming](/dmx-fun/blog/immutability-foundation-functional-programming) — how immutable data and pure functions reinforce each other
- [How to Write More Predictable Code with Functional Programming](/dmx-fun/blog/predictable-code-with-functional-programming) — purity in practice across a real module
