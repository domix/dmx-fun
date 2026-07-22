---
title: "Designing a Good Error Type: Sealed Hierarchies Callers Can Act On"
description: "Result<V, E> makes the failure part of the signature — but that only pays off if E is worth reading. A String is not an error type, and neither is a raw Throwable. The errors worth modeling are a sealed hierarchy whose cases are carved by what the caller can do about them. Here is how to design one."
pubDate: 2026-07-21
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Error Handling", "Result", "Sealed Interfaces", "Pattern Matching", "Domain Modeling", "Java", "Functional Programming"]
image: "https://images.pexels.com/photos/28588774/pexels-photo-28588774.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "Jan van der Wolf"
    authorUrl: "https://www.pexels.com/@jan-van-der-wolf-11680885/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/colorful-directional-signpost-against-clear-sky-28588774/"
---

When a team adopts `Result<V, E>`, all the attention goes to the mechanics — `map`, `flatMap`,
the pipeline, the missing `try/catch`. The second type parameter gets whatever was lying around:
a `String`, an exception, sometimes an enum somebody started and never finished. And then the
payoff feels smaller than promised, because the signature now admits *that* the operation can
fail but still cannot say *how* — or what anyone should do about it.

The error type is where the design actually happens. `E` is not a slot to fill; it is an API the
caller will program against. This post — the first in a series on working with the library's core
types — is about designing that API well, and the answer has a shape: a **sealed hierarchy whose
cases are carved by what the caller can act on.**

---

## The error type is an API, so `String` is not one

The test for a good error type is a single question: **when this comes back, what does the
calling code do next?** Every design decision follows from taking that question seriously.

`Result<Transfer, String>` fails the test immediately. A `String` supports exactly one operation
— show it to someone — so the only caller it serves is a log file. The moment code needs to
*branch* on the failure, you get the pattern everyone has scrolled past in a legacy codebase:

```java
// The compiler is a bystander here. Rename the message, this silently stops matching.
if (error.contains("insufficient")) { ... }
```

`Result<Transfer, Throwable>` fails it more subtly. It looks typed, but `Throwable` is an *open*
set — the caller cannot enumerate the cases, the compiler cannot check that handling is complete,
and you have reproduced the worst property of unchecked exceptions inside the value that was
supposed to fix them. A raw `Throwable` in the error channel is a fine *intermediate* state at
the boundary where a `Try` captured it — and a poor final vocabulary for a domain operation.

What the question demands is a **closed, enumerable set of alternatives, each carrying its own
data**. Java has a construct for exactly that.

---

## The shape: one sealed interface, one record per outcome

```java
public sealed interface TransferError {
    record InsufficientFunds(Money shortfall)              implements TransferError {}
    record AccountFrozen(AccountId account)                implements TransferError {}
    record LimitExceeded(Money limit, Money attempted)     implements TransferError {}
    record RecipientNotFound(AccountId recipient)          implements TransferError {}
}
```

Four lines of vocabulary, and the operation's signature now tells the whole story:

```java
Result<Receipt, TransferError> transfer(TransferRequest request);
```

The caller handles it with an exhaustive `switch` — no default branch, because the set is closed:

```java
String respond(TransferError error) {
    return switch (error) {
        case TransferError.InsufficientFunds e -> "You need " + e.shortfall() + " more.";
        case TransferError.AccountFrozen e     -> "Account " + e.account() + " is frozen — contact support.";
        case TransferError.LimitExceeded e     -> "Exceeds your " + e.limit() + " limit. Split the transfer?";
        case TransferError.RecipientNotFound e -> "No account " + e.recipient() + ". Check the number.";
    };
}
```

Each case leads to a *different next step* — top up, escalate, split, re-enter. That is the
signpost property: every arm points somewhere. And exhaustiveness is not a style point; it is the
maintenance mechanism. Add `TransferError.CurrencyMismatch` next quarter and the compiler walks
you to every `switch` in the codebase that now has an unhandled case. Compare that to adding a
new exception type, where discovering the missed handlers is the production incident's job.

---

## Carve cases by caller action, not by cause

The subtle mistake is to enumerate what went wrong *internally* — one case per throwing line, one
per failing subsystem. That produces taxonomies like `DatabaseTimeout`, `ConnectionPoolExhausted`,
`ReplicaLagExceeded`: faithful to the implementation, useless to the caller, who collapses all
three into the same branch anyway.

Carve from the other side. Group outcomes the caller treats identically into one case; split
outcomes the caller treats differently into separate cases — even if they share a cause. The
number of cases should converge on the number of *distinct reactions*, not the number of distinct
failures.

When one reaction genuinely spans several cases — "anything transient gets retried" — encode the
grouping in the hierarchy itself instead of asking every caller to rebuild it:

```java
public sealed interface FetchError {
    sealed interface Transient extends FetchError {}
    record Timeout(Duration elapsed)        implements Transient {}
    record RateLimited(Duration retryAfter) implements Transient {}

    record NotFound(String key)             implements FetchError {}
    record Unauthorized()                   implements FetchError {}
}
```

Callers that care about the split match the group; callers that need the detail match the leaf —
and both switches stay exhaustive:

```java
return switch (error) {
    case FetchError.Transient t    -> retry(t);       // Timeout and RateLimited, one branch
    case FetchError.NotFound e     -> fallback(e.key());
    case FetchError.Unauthorized _ -> reauthenticate();
};
```

(The `_` for the unused binding is an unnamed pattern variable, final since Java 22 — on Java 21,
give it a name.)

One caution: the retryable/permanent split belongs in the hierarchy only when it is a property of
the *domain*, stable across callers. When it is one caller's policy, keep it in that caller.

---

## Carry data, not prose

Notice what the records hold: a `Money shortfall`, a `Duration retryAfter`, an `AccountId`. Facts,
not sentences. A formatted message inside the error type is a decision made too early — it fixes
the audience (a log? an API response? a UI in which language?) at the point of failure, the one
place that cannot know the audience.

The rule that falls out: **the core produces data; the boundary produces sentences.** Each record
carries the facts that make its case actionable, and the `switch` at the edge turns those facts
into an HTTP problem detail, a log line, or a UI string — choosing words at the only layer that
knows who is reading. It also makes tests honest: asserting on `new InsufficientFunds(Money.of(50))`
is precise; asserting on a substring of a message is a rename away from a false positive.

---

## One operation, one error type — translate at the seams

The remaining question is scope: one big `AppError` for everything, or a type per operation? The
signpost answers it. A caller of `transfer` forced to handle `EmailDeliveryFailed` because it
shares an error enum with the notification module is handling impossibilities — the mirror image
of the `String` problem. Keep the type scoped to what the operation can actually produce, and let
`mapError` translate at layer boundaries:

```java
public sealed interface OrderError {
    record NotFound(OrderId id)                implements OrderError {}
    record NotPending(Order.Status was)        implements OrderError {}
    record Persistence(PersistenceError cause) implements OrderError {}

    static OrderError persistence(PersistenceError cause) {
        return new Persistence(cause);
    }
}

Result<Order, OrderError> confirmOrder(OrderId id) {
    return orders.findById(id)
        .<OrderError>toResult(new OrderError.NotFound(id))   // witness: infer the interface, not NotFound
        .flatMap(Order::confirm)
        .flatMap(order -> orders.save(order)
            .mapError(OrderError::persistence));   // PersistenceError -> OrderError, at the seam
}
```

The repository speaks `PersistenceError`; the domain speaks `OrderError`; one `mapError` at the
call site is the exchange rate between them. (The explicit `<OrderError>` witness matters in a
chain: without it, Java infers the error type from the argument — the *record* `NotFound` — and
the next `flatMap` fails to compile.) Note that `Persistence` wrapping a *typed* lower-layer
error at a seam is not the cause-based taxonomy warned against above — the cause set stays
closed, and the caller's reaction ("the store misbehaved") is genuinely its own case. Each
layer's vocabulary stays closed and exhaustive — which is precisely what a shared mega-error
type destroys.

And when the lower layer *throws* instead of returning values — a JDBC call, an HTTP client —
the seam is a `Try` that captures the throw and maps it into a case of your hierarchy in the
same breath:

```java
Result<Config, ConfigError> parsed =
    Try.of(() -> objectMapper.readValue(raw, Config.class))   // may throw
        .toResult(ConfigError::malformed);                     // Throwable -> a case of YOUR type
```

One line, and the open world of exceptions becomes the closed world your callers can switch on.

Two final anti-patterns, both of which quietly reopen the set you worked to close:

- **The `Other(String message)` case.** One catch-all arm and exhaustiveness is decorative —
  every real failure migrates into `Other` because it is the path of least resistance. If a case
  keeps absorbing new meanings, that is the compiler telling you a real case is missing.
- **The `Unexpected(Throwable cause)` case for bugs.** A `NullPointerException` is not an outcome
  of `transfer`; it is a defect. Model the outcomes callers must handle; let genuine surprises
  throw and reach the crash handler — the [expected-versus-surprise line](/dmx-fun/blog/why-just-use-exceptions-persists)
  this blog has drawn before.

Design the type by the signpost question, keep the set closed, put facts in the cases, translate
at the seams — and `Result<V, E>` stops being a stylistic preference and becomes what it was
meant to be: a signature that hands the caller a map of everything that can happen next.

The [dmx-fun](/dmx-fun/) library supplies the machinery around your error types —
[`Result`](/dmx-fun/guide/result) with `mapError`, `recover`, and friends — while the hierarchy
itself is plain Java: sealed interfaces and records, no library required. The
[Developer Guide](/dmx-fun/guide/) covers the rest of the core types this series builds on.

---

## Further reading

- [Why "Just Use Exceptions" Persists](/dmx-fun/blog/why-just-use-exceptions-persists) — the
  expected-outcomes-versus-surprises line that decides what belongs in an error type at all.
- [Algebraic Data Types for Business Developers](/dmx-fun/blog/algebraic-data-types-for-business-developers)
  — the sum-type foundation sealed hierarchies are built on.
- [Pattern Matching for Domain Modeling](/dmx-fun/blog/pattern-matching-domain-modeling) — the
  `switch` side of the contract, in depth.
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types)
  — error types as one part of signatures that tell the truth.
- [Railway-Oriented Programming in Java](/dmx-fun/blog/railway-oriented-programming-in-java) — how
  well-typed errors flow through a `flatMap` pipeline.
- [Validated: Accumulating Errors in a Functional Way](/dmx-fun/blog/validated-accumulating-errors)
  — when the caller needs *all* the failures, not the first one.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
