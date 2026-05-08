---
title: "Why Avoid Mutable State?"
description: "The case against mutable state is not philosophical. It is a list of bugs that mutable state causes reliably, in every codebase, on every team. This post names them, shows what they look like in Java backend code, and demonstrates the concrete improvement that comes from eliminating each one."
pubDate: 2026-05-08
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Fundamentals"
tags: ["Immutability", "Functional Programming", "Java", "Concurrency", "Best Practices"]
image: "https://images.pexels.com/photos/33855098/pexels-photo-33855098.jpeg"
imageCredit:
    author: "Héctor Berganza"
    authorUrl: "https://www.pexels.com/@hberganza/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/butterflies-emerging-from-chrysalis-in-butterfly-house-33855098/"
---

"Prefer immutability" appears in every Java style guide and FP introduction. It is repeated so often that it starts to sound like a ritual rather than a reason. Engineers hear it, nod, and keep writing `setters`.

This post makes the case from first principles. Not because immutability is aesthetically pleasing or philosophically pure — but because mutable state causes a specific, predictable set of failures. These failures are not rare edge cases. They are the bugs your team debugs every quarter, the incidents your on-call investigates at 2 a.m., the test flakiness that nobody can reproduce consistently.

Name the failures. Show where mutable state caused them. Then the prescription — avoid mutable state — stops sounding like a style preference and starts sounding like a diagnosis.

---

## Failure 1: Aliasing — Two Names for One Thing You Didn't Expect to Change

Aliasing happens when two variables reference the same object. A mutation through one variable changes what the other variable sees, invisibly, with no indication at the call site.

```java
List<String> original = new ArrayList<>(List.of("a", "b", "c"));
List<String> copy      = original;  // not a copy — same object

copy.add("d");
System.out.println(original);  // [a, b, c, d] — original mutated, nobody warned you
```

This is a toy example. The production version is subtler:

```java
class ReportBuilder {
    private final List<String> warnings;

    ReportBuilder(List<String> warnings) {
        this.warnings = warnings;  // stores the reference, not a copy
    }

    void build() {
        if (warnings.isEmpty()) {
            addDefaultWarning();  // adds to warnings — which is the caller's list
        }
    }
}

// Caller
List<String> myWarnings = new ArrayList<>();
ReportBuilder builder = new ReportBuilder(myWarnings);
builder.build();
System.out.println(myWarnings);  // ["default warning"] — the builder modified the caller's list
```

The caller passes `myWarnings` expecting to own it. The builder stores the reference. The builder's internal operation modifies the caller's data. Both parties think they are working with their own state.

**The fix:** copy mutable inputs at the boundary.

```java
ReportBuilder(List<String> warnings) {
    this.warnings = List.copyOf(warnings);  // defensive copy — caller owns their list
}
```

Better still: model `warnings` as an immutable type from the start and the aliasing problem cannot occur. `List.copyOf` returns an unmodifiable list that throws on `add` — the bug surfaces at the mutation site, not downstream.

---

## Failure 2: Unexpected Side Effects from Passed-In State

A method that mutates its argument changes the caller's world without the call site revealing it. The signature says "here is the input"; the behaviour says "I will also modify it."

```java
void applyDiscounts(List<LineItem> items, DiscountPolicy policy) {
    for (int i = 0; i < items.size(); i++) {
        LineItem original = items.get(i);
        items.set(i, policy.apply(original));  // mutates the list in-place
    }
}
```

The caller passes a list expecting to use it afterward:

```java
List<LineItem> order = fetchOrder(id);
applyDiscounts(order, policy);
archiveOriginalOrder(order);  // too late — order now contains discounted prices
```

The `archiveOriginalOrder` call receives modified data. There is no indication at the call site that `applyDiscounts` modified `order`. The name says "apply discounts" — it does not say "destroy the original."

**The fix:** return new state instead of mutating input.

```java
List<LineItem> applyDiscounts(List<LineItem> items, DiscountPolicy policy) {
    return items.stream()
        .map(policy::apply)
        .toList();  // new list — original unchanged
}
```

Now:
```java
List<LineItem> order    = fetchOrder(id);
List<LineItem> discounted = applyDiscounts(order, policy);
archiveOriginalOrder(order);        // original intact
processPayment(discounted);         // discounted copy used for payment
```

The caller explicitly chooses which version to use. The function's contract is complete: inputs in, output out, nothing else modified.

---

## Failure 3: Race Conditions from Shared Mutable State

Shared mutable state in a concurrent context is the most costly failure mode. The window for the bug is often microseconds wide; the consequence is data corruption that appears in production months after the code was written.

```java
@Service
class CounterService {
    private int count = 0;  // shared mutable state

    public int increment() {
        return ++count;  // not atomic — read-modify-write, three operations
    }
}
```

Under concurrent load, two threads executing `++count` simultaneously can both read `0`, both add `1`, and both write `1` — producing a count of `1` when it should be `2`. No exception is thrown. The data is silently wrong.

The usual fix is synchronization — `synchronized`, `AtomicInteger`, `volatile`. These work but come with costs: contention, complexity, and the constant risk that a future contributor adds a second field and forgets to synchronize it too.

The immutable fix removes the race condition entirely by removing the shared state:

```java
// No shared mutable state — each call is independent
record CounterResult(int previous, int next) {}

CounterResult increment(int current) {
    return new CounterResult(current, current + 1);
}
```

The caller manages the current count. Two callers working concurrently work on their own copies. Coordination, if needed, happens explicitly at a higher level (a database, a queue, an atomic reference) rather than being silently assumed.

In Spring services, this pattern means **no instance fields that accumulate state across requests**. A `@Service` bean is a singleton; any mutable field is shared across every concurrent request:

```java
// Wrong — currentUser is shared across all concurrent requests
@Service
class OrderService {
    private User currentUser;

    public Order place(PlaceOrderRequest req) {
        currentUser = userRepository.findById(req.userId());
        // if two requests run simultaneously, currentUser may be someone else's by the time
        // the next line reads it
        return orderRepository.save(buildOrder(req, currentUser));
    }
}

// Right — all state is local to the method call
@Service
class OrderService {
    public Order place(PlaceOrderRequest req) {
        User user = userRepository.findById(req.userId());
        return orderRepository.save(buildOrder(req, user));
    }
}
```

The second version has no race condition because there is nothing shared to race on.

---

## Failure 4: Brittle Tests from Hidden Setup

Mutable state in test subjects requires tests to set up the correct internal state before calling the method under test, and to verify — or clean up — mutated state afterward. This creates three failure modes:

**Setup leakage:** a previous test mutates shared state, leaving the object in a different starting condition for the next test. The second test fails not because of a bug in the code it exercises but because of what the previous test did.

**Order sensitivity:** tests that pass when run individually fail when run in certain orderings because later tests depend on state left by earlier ones. The test suite reports green in one order and red in another.

**Hidden assertions:** after calling the method under test, the test must inspect internal state to verify behavior. This couples tests to implementation details, making refactoring break tests that exercise correct behavior.

```java
// Mutable service — tests must manage internal state
class CartService {
    private final List<Item> items = new ArrayList<>();

    void add(Item item) { items.add(item); }
    void remove(Item item) { items.remove(item); }
    BigDecimal total() { return items.stream().map(Item::price).reduce(ZERO, BigDecimal::add); }
}

class CartServiceTest {
    CartService cart = new CartService();  // shared instance across test methods?

    @Test
    void total_withTwoItems_returnsSumOfPrices() {
        cart.add(new Item("a", new BigDecimal("10.00")));
        cart.add(new Item("b", new BigDecimal("5.00")));
        assertThat(cart.total()).isEqualByComparison(new BigDecimal("15.00"));
        // items left in cart — will corrupt the next test if cart is shared
    }
}
```

Replacing mutable state with explicit, passed-in state turns the test subject into a pure function:

```java
// Pure function — takes the cart state as input, returns a new state or result
List<Item> add(List<Item> cart, Item item) {
    var next = new ArrayList<>(cart);
    next.add(item);
    return List.copyOf(next);
}

BigDecimal total(List<Item> cart) {
    return cart.stream().map(Item::price).reduce(ZERO, BigDecimal::add);
}
```

Each test provides its own input and asserts on the output. No shared state. No setup. No cleanup. No order sensitivity.

```java
@Test
void total_withTwoItems_returnsSumOfPrices() {
    var cart = List.of(new Item("a", new BigDecimal("10.00")),
                       new Item("b", new BigDecimal("5.00")));
    assertThat(total(cart)).isEqualByComparison(new BigDecimal("15.00"));
}
```

---

## Failure 5: Reasoning Gaps — Reading Code Without Knowing When It Ran

The most insidious cost of mutable state is cognitive. A piece of code that reads a field may be reading the initial value, a value set by a previous call, or a value set by a concurrent thread. The reader cannot know by looking at the code — they must trace every write to that field in the entire codebase to understand what any given read might produce.

```java
class ReservationService {
    private LocalDate reservationDate;  // set somewhere, read somewhere else

    public void setDate(LocalDate date) { this.reservationDate = date; }

    public boolean isAvailable(Room room) {
        // what is reservationDate here?
        // it depends on when setDate was called, by whom, and whether any other
        // concurrent call modified it since then
        return room.isAvailableOn(reservationDate);
    }
}
```

The reader of `isAvailable` cannot understand what it does without reading every call site that invokes `setDate`. In a codebase of any size, that is a significant search. If the class is used in multiple contexts, each with different calling conventions, the search becomes exhaustive.

Immutable, parameter-based design makes the code readable in place:

```java
class ReservationService {
    public boolean isAvailable(Room room, LocalDate date) {
        return room.isAvailableOn(date);
    }
}
```

Every piece of state the method uses is visible at the call site. A reader of `isAvailable(room, LocalDate.now())` knows exactly what date is being checked without examining anything beyond the signature.

---

## Where to Draw the Line

Avoiding mutable state does not mean treating all mutation as equally harmful. There are three categories:

**Local, bounded mutation** — a variable modified within a single method call, with no reference escaping the method — is fine. The loop variable in a `for` loop, the builder assembling a complex object, the `StringBuilder` in a formatting method: these are invisible to callers and carry none of the costs above.

**Controlled, explicit shared state** — a database, a cache backed by an atomic reference, a queue — is necessary for persistence and coordination. The key is that the shared nature is explicit: the type (`AtomicReference`, `ConcurrentHashMap`, a repository interface) signals that concurrent access is expected, and the access patterns are designed accordingly.

**Hidden, implicit shared state** — ordinary instance fields in singletons, static fields, objects passed between callers without copying — is the category that causes the failures above. This is the state worth eliminating.

The guideline is not "never mutate anything." It is: **if two callers can observe the same mutation, make that sharing explicit in the type**. If only one caller can observe the state, the scope is small enough that mutation carries no systemic risk.

---

## The Practical Prescription

In a Java backend service:

1. **Make fields `final` by default.** If you cannot make a field `final`, ask why it needs to change after construction.

2. **Make `@Service` and `@Component` beans stateless.** All per-request data belongs in method parameters, not instance fields.

3. **Copy mutable collections at the boundary.** Any constructor or method that accepts a `List`, `Map`, or `Set` should call `List.copyOf` (or equivalent) immediately. Never store the caller's reference.

4. **Return new values instead of mutating arguments.** A method that computes a transformation should return the result; the caller decides whether to replace their variable.

5. **Prefer records over mutable classes for domain objects.** `record` enforces immutability by construction. `with`-style builders (a record plus a static factory that copies with one field changed) make transformations explicit.

---

## The Connection to Functional Types

Immutable state and functional types reinforce each other. `Result<V, E>`, `Option<T>`, and `Try<V>` are inherently immutable — once created, they do not change. A pipeline built on them is a chain of value transformations, not a sequence of mutations.

The absence of mutable state is what makes these pipelines safe to compose: calling `.flatMap(f)` on a `Result` never modifies the `Result` — it produces a new one. You can pass the same `Result` to two different pipelines and both will see the original value, with no aliasing risk and no synchronization needed.

Immutability is not an FP tax. It is the condition under which composition becomes reliable.
