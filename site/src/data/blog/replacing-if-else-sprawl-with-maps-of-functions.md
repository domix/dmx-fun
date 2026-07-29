---
title: "Replacing if/else and switch Sprawl with Maps of Functions"
description: "A long if/else-if chain over the same discriminator is a lookup table wearing control flow as a disguise. Make the table explicit — a Map from key to function — and dispatch collapses to one line, handlers become values you can register, decorate, and test in isolation. Here is the pattern, the JDK mechanics, and the honest line where an exhaustive switch over a sealed type is the better tool."
pubDate: 2026-07-28
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Patterns", "Higher-Order Functions", "Java", "Dispatch", "Refactoring", "Functional Programming", "Design"]
image: "https://images.pexels.com/photos/12032762/pexels-photo-12032762.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "David McElwee"
    authorUrl: "https://www.pexels.com/@davidmcelwee/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/room-with-lockers-12032762/"
---

Every backend codebase has one: the method that started with two branches and now scrolls.
A webhook processor with fifteen `else if (type.equals(...))` arms. A command handler whose
`switch` cases each call a differently-shaped private method. The same discriminator checked
again in three other files — once to process, once to validate, once to describe. Adding a
case means finding every sprawl site, and the compiler helps with none of them.

Look at what that chain actually *does*: it takes a key and finds the code that handles it.
That is not control flow — that is a **lookup**, the post-office wall in the photo above: one
labeled box per key, and delivery is "find the box, put the thing in." Java has a first-class
data structure for lookups, and since functions are values, the thing in the box can be the
handler itself. This post — part of the functional-patterns-in-practice thread — is about
making the table explicit, and about the equally important line where you should *not*.

---

## The move: branches become entries

The sprawl version, condensed from the real thing:

```java
Result<Ack, EventError> handle(Event event) {
    if (event.type().equals("payment.settled")) {
        return settle(event);
    } else if (event.type().equals("payment.refunded")) {
        return refund(event);
    } else if (event.type().equals("payment.disputed")) {
        return dispute(event);
    }
    return Result.err(new EventError.UnknownType(event.type()));
}
```

Each branch has identical shape — compare the key, call a handler with the same signature.
Whenever every arm looks alike, the chain is a table row-by-row. Write the table:

```java
private static final Map<String, Function<Event, Result<Ack, EventError>>> HANDLERS = Map.of(
    "payment.settled",  Payments::settle,
    "payment.refunded", Payments::refund,
    "payment.disputed", Payments::dispute);
```

And dispatch is a lookup plus an application. The JDK-only version handles the unknown key
with a default entry:

```java
Result<Ack, EventError> handle(Event event) {
    return HANDLERS
        .getOrDefault(event.type(), e -> Result.err(new EventError.UnknownType(e.type())))
        .apply(event);
}
```

The second variant earns its extra lines when dispatch is one step in a longer `Result`
pipeline — validate, dispatch, persist — or when you want to *do* something with the absent
case (log the unknown type, count it) before converting it. Lift the lookup into an
[`Option`](/dmx-fun/guide/option) and the whole flow stays one chain:

```java
Result<Ack, EventError> handle(Event event) {
    return Option.ofNullable(HANDLERS.get(event.type()))
        .<EventError>toResult(new EventError.UnknownType(event.type()))
        .flatMap(handler -> handler.apply(event));
}
```

(The explicit `<EventError>` witness keeps inference from fixing the error type to the
`UnknownType` record — the chained-`toResult` wrinkle described in
[Designing a Good Error Type](/dmx-fun/blog/designing-a-good-error-type). Note also that
`toResult` takes the error *eagerly*: it is constructed on every call, hit or miss, so keep it
a cheap record. And if you reach for the library anyway, `NonEmptyMap` makes the lookup
Option-native — its `get(key)` returns `Option<V>` directly, and a dispatch table is non-empty
by construction; see the [NonEmptyMap guide](/dmx-fun/guide/non-empty-map).)

What changed is more than line count. The mapping is now *data*: you can print it to answer
"what events do we handle?", assert on it in a test, and hand it to code that has no idea
what the handlers do. Each handler is a named method testable without constructing the
dispatcher at all. And the unknown-key case — the branch the `else` chain was quietly
mishandling or forgetting — is forced into the open, because a `Map` lookup makes absence
explicit instead of falling off the end of a chain.

---

## Where the pattern earns its keep

**Open, external key sets.** The webhook example's keys are strings minted by someone else's
system. No sealed type can close that set; new event types appear without your compiler's
consent. A map mirrors the reality: known keys handled, everything else is data-driven
rejection.

**Registration over enumeration.** Because the table is a value, it does not have to be a
literal. Modules can contribute entries — a plugin registry, feature-flagged handlers, a
test double swapped in for one key — and the dispatcher never changes:

```java
Map<String, Function<Event, Result<Ack, EventError>>> handlers = new HashMap<>();
handlers.putAll(PaymentHandlers.TABLE);
handlers.putAll(ShippingHandlers.TABLE);
this.handlers = Map.copyOf(handlers);   // frozen at wiring time, immutable at dispatch time
```

(In a wired dispatcher like this, the table becomes a constructor-set field and `handle`
reads `this.handlers` instead of a static `HANDLERS` constant — same lookup, different home.)

**Uniform decoration.** Handlers are functions, so cross-cutting behavior composes once
instead of being pasted into every branch — the [higher-order-function](/dmx-fun/blog/higher-order-functions-real-examples)
payoff in one line per concern:

```java
static <A, B> Function<A, B> timed(String name, Metrics metrics, Function<A, B> handler) {
    return input -> {
        var start = System.nanoTime();
        try {
            return handler.apply(input);
        } finally {
            metrics.record(name, System.nanoTime() - start);
        }
    };
}

// at wiring time: decorate the fully composed table (registered modules included),
// then freeze — same discipline as any other table
Map<String, Function<Event, Result<Ack, EventError>>> decorated = new HashMap<>();
handlers.forEach((key, handler) -> decorated.put(key, timed(key, metrics, handler)));
this.handlers = Map.copyOf(decorated);
```

**Many homogeneous cases.** Fifty pricing rules keyed by product category, each a
`Function<Order, Money>`: as a `switch` this is a wall; as a map it is a table you could even
load from configuration.

---

## Where the switch is the better tool

Here is the honest half, because this pattern gets over-sold. A map of functions buys
openness by giving up the compiler.

When the discriminator is a **closed set your domain owns** — a sealed hierarchy of order
states, the cases of a [well-designed error type](/dmx-fun/blog/designing-a-good-error-type)
— an exhaustive `switch` is strictly stronger:

```java
String describe(PaymentResult result) {
    return switch (result) {                        // compiler enforces every case
        case PaymentResult.Captured c -> "captured " + c.reference();
        case PaymentResult.Declined d -> "declined: " + d.reason();
        case PaymentResult.Pending p  -> "retry after " + p.retryAfter();
    };
}
```

Add a case to the sealed type and every such `switch` fails to compile until handled — the
maintenance mechanism [pattern matching](/dmx-fun/blog/pattern-matching-domain-modeling)
exists to provide. The map gives you the opposite: add a key and nothing complains until a
request arrives at runtime and misses the table. Deconstruction is also lost — a `switch`
binds each case's typed payload; a `Map<K, Function<Object, R>>` forced over heterogeneous
cases degenerates into casts, which is the pattern telling you it is the wrong shape here.

The decision rule in one breath: **switch over sealed types for closed domain alternatives,
where exhaustiveness is the point; maps of functions for open or data-driven key sets, where
registration is the point.** String keys from the outside world, plugin points, and large
homogeneous tables go in maps. Domain states, error cases, and anything you would model as a
[sum type](/dmx-fun/blog/algebraic-data-types-for-business-developers) go in a `switch`.

There is a middle ground the binary hides: a *closed* key set whose handler *bindings* are
chosen at runtime — per-tenant wiring, feature-flagged handlers over a fixed enum. A `switch`
cannot express that; the right tool is an `EnumMap`, which keeps near-switch lookup cost and
lets a wiring-time check fail fast before any request arrives —

```java
if (!table.keySet().containsAll(EnumSet.allOf(EventKind.class))) {
    throw new IllegalStateException("unhandled event kinds: missing handlers at wiring time");
}
```

— recovering the completeness guarantee the plain map gave up.

And below both thresholds: an `if` with two arms is fine. Replacing it with a map is
ceremony, not design — the same judgment call as
[every functional make-over](/dmx-fun/blog/when-making-it-functional-makes-it-worse).

---

## Keeping the table honest

Three habits keep the pattern from decaying into its own sprawl:

- **Freeze it.** Build with `Map.of` or seal with `Map.copyOf` at wiring time. A mutable
  dispatch table that code can edit at runtime is shared mutable state with extra steps, and
  debugging "who replaced this handler" is worse than any `else` chain.
- **Type the whole signature.** The value type `Function<Event, Result<Ack, EventError>>`
  states what every handler receives *and how it fails*. Handlers that throw instead of
  returning the typed outcome reintroduce the invisible error channel the signature just
  eliminated.
- **Treat the missing key as a first-class case.** `getOrDefault` with a rejection handler,
  or `Option` + `toResult` — either way the unknown key produces a value your caller can act
  on, not a `NullPointerException` thrown later, when something calls `apply` on the `null`
  an unchecked `get` handed back.

The [dmx-fun](/dmx-fun/) types slot in on the value side: handlers returning
[`Result`](/dmx-fun/guide/result) keep failures typed across the table,
[`Option`](/dmx-fun/guide/option) makes the lookup's absence explicit, and the
[Developer Guide](/dmx-fun/guide/) covers both.

---

## Further reading

- [Higher-Order Functions Explained with Real Examples](/dmx-fun/blog/higher-order-functions-real-examples)
  — functions as values, the capability this whole pattern stands on.
- [Pattern Matching and Domain Modeling](/dmx-fun/blog/pattern-matching-domain-modeling) — the
  exhaustive-switch side of the decision rule, in depth.
- [Designing a Good Error Type](/dmx-fun/blog/designing-a-good-error-type) — closed sealed
  hierarchies: the cases that belong in a `switch`, not a map.
- [Functional Composition Patterns](/dmx-fun/blog/functional-composition-patterns) — composing
  handler pipelines out of small functions.
- [Algebraic Data Types for Business Developers](/dmx-fun/blog/algebraic-data-types-for-business-developers)
  — recognizing closed sum types when you see them.
- [When "Making It Functional" Makes It Worse](/dmx-fun/blog/when-making-it-functional-makes-it-worse)
  — the two-branch `if` that should stay an `if`.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
