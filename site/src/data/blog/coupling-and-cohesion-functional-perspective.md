---
title: "Coupling and Cohesion from a Functional Perspective"
description: "Coupling and cohesion predate objects — and the strongest form of cohesion in the original 1979 taxonomy is literally called functional. Seen through a functional lens, the two classic design forces stop being vague virtues: coupling becomes what your signatures hide, cohesion becomes what your types gather. Here is how pure functions, immutable data, and typed outcomes move code toward the good end of both scales."
pubDate: 2026-07-24
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Coupling", "Cohesion", "Software Design", "Architecture", "Functional Programming", "Java", "Immutability"]
image: "https://images.pexels.com/photos/18500074/pexels-photo-18500074.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "Tom Fisk"
    authorUrl: "https://www.pexels.com/@tomfisk/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/hitch-connecting-two-train-cars-18500074/"
---

Every design conversation eventually lands on the same two words: *low coupling, high cohesion*.
They are older than objects — Larry Constantine worked them out in the late 1960s, and Yourdon and
Constantine's [*Structured Design*](https://archive.org/details/Structured_Design_Edward_Yourdon_Larry_Constantine)
(1979) turned them into a ranked taxonomy of how modules connect
and how their insides hold together. Here is the detail that should make any functional programmer
smile: in that original ranking, the **best** form of cohesion — the top of the scale — is named
**functional cohesion**. The vocabulary was pointing somewhere all along.

The trouble with both words today is that they have gone soft. "Loosely coupled" appears in every
architecture slide and commits nobody to anything. The functional perspective sharpens them back
into something you can check in a code review: **coupling is everything two pieces of code share
that the signature does not show; cohesion is how much of a unit's inside serves one
transformation.** This post — part of the design-and-architecture side of this blog — works
through the ends of both scales: the forms functional code moves you away from, and the ones it
moves you toward.

---

## The coupling ladder, and where hidden state sits on it

The structured-design school ranked coupling from tightest to loosest. The six-level version most
textbooks teach — content, common, external, control, stamp, data — is Glenford Myers' refinement
([*Reliable Software Through Composite Design*](https://archive.org/details/reliablesoftware00myer),
1975) of Constantine's original factors. Nearly
half a century later the ranking still explains most production incidents — and the functional
reading of it is blunt: **the worst forms are exactly the ones a signature cannot show.** This
post walks the rungs where that reading bites hardest — common, control, data — and one everyday
hazard the ladder never named.

**Common coupling** — two modules sharing writable state — is the classic. A train coupler, like
the one in the photo, is honest: you can see exactly where the cars connect, and nowhere else. A
shared mutable cache is the opposite — every reader is invisibly hitched to every writer:

```java
class PricingService {
    static Map<String, BigDecimal> rates = new HashMap<>();   // common coupling

    BigDecimal price(Order order) {
        return order.amount().multiply(rates.get(order.currency()));  // hitched to every writer
    }
}
```

Nothing in `price(Order)` tells you it depends on whoever last touched `rates`, on *when* they
touched it, or on which thread. The dependency is real; the signature denies it. Make the data
immutable and pass it in, and the coupling does not disappear — it becomes **data coupling**, the
weakest rung on the ladder, and it moves into the signature where it belongs:

```java
BigDecimal price(Order order, RateTable rates)   // same dependency, now visible and inert
```

That is the general move, and it is worth stating as a rule: **functional style does not remove
coupling — it converts hidden coupling into visible data coupling.** A pure function is coupled
to exactly its parameter types and its return type. You can read its entire dependency surface
without opening the body.

Two more forms dissolve the same way — the first a rung of the ladder, the second the everyday
hazard the ladder never named:

- **Control coupling** — a caller passing a flag that tells the callee *which* behavior to run
  (`process(order, /* validate = */ true)`) — inverts into passing the behavior itself. A
  higher-order function does not ask for a flag and branch; it asks for the function:
  `process(order, validator)`. The caller decides, the callee composes.
- **Temporal coupling** — "call `init()` before `execute()`, or else" — is a protocol that lives
  in Javadoc and tribal memory. When steps are functions that *return values consumed by the next
  step*, the order is not a convention; it is the only way the code compiles. A
  `flatMap` chain is sequencing made structural: you cannot confirm an order you have not yet
  parsed, because the unparsed thing has the wrong type.

---

## Exceptions are coupling to a stranger

There is one more hidden hitch worth naming, because it hides in plain sight. A method that
throws a domain exception is coupled to *whichever caller up the stack knows to catch it* — a
module it cannot name, at a distance it cannot see, through a channel the signature does not
declare (unchecked) or declares in a way nobody composes (checked). That is control flow shared
between two pieces of code with no visible connection: coupling of the "action at a distance"
kind, the same genus as the shared cache.

Typed outcomes convert this one too. A `Result<Order, OrderError>` return moves the entire
failure contract into the signature: the caller is coupled to a *value it receives*, not to a
protocol it must remember. And when layers need different failure vocabularies, one `mapError`
at the seam keeps each layer hitched only to its neighbor — persistence errors do not leak
upward to couple the HTTP handler to the database schema. Deep error hierarchies shared across a
whole application are common coupling wearing a type-safe costume; per-operation error types,
translated at boundaries, are the data-coupled version.

---

## Cohesion: the type plus its operations

Cohesion got the same soft treatment as coupling — "things that belong together" convinces
nobody. The functional reading is more concrete: **a cohesive unit is a data type plus the
operations that preserve its meaning.** Algebraic data types make this almost mechanical:

```java
public sealed interface Subscription {
    record Trial(Instant startedAt, Duration length)      implements Subscription {}
    record Active(PlanId plan, Instant renewsAt)          implements Subscription {}
    record Lapsed(Instant since)                          implements Subscription {}
}
```

The operations are exhaustive `switch`es over the type — no default branch, because the sealed
hierarchy closes the case set:

```java
String renewalNotice(Subscription s) {
    return switch (s) {                       // compiler enforces every case
        case Subscription.Trial t  -> "Trial ends " + t.startedAt().plus(t.length());
        case Subscription.Active a -> "Renews " + a.renewsAt();
        case Subscription.Lapsed l -> "Lapsed since " + l.since();
    };
}
```

The sealed hierarchy plus its `switch`-based operations is one self-contained cluster: every
case of the domain concept, every rule that consumes it, nothing else. This is what the 1979
taxonomy called *functional cohesion* — every element contributes to a single, nameable task —
and it falls out of modeling with types rather than being enforced by discipline. Compare the
classic low-cohesion utility class: a `SubscriptionUtils` holding a date formatter, a
plan-price lookup, and a null-safe equals is *coincidental cohesion* — things sharing a file
because nobody knew where else to put them. The functional habit of grouping code by the data it
transforms, instead of by the layer it runs in, is a cohesion strategy with a very old name.

Purity feeds the same scale from the other side. A function that computes a price *and* writes
an audit row *and* increments a metric has three reasons to change — the pricing rule, the audit
schema, the metrics backend. Split the effects out (compute the decision purely, let the shell
execute it) and each piece collapses to one reason to change. "Do one thing" was always a
cohesion claim; a pure function is the version of it the compiler can help you keep, because the
moment it needs a second thing it needs a second parameter or a different return type, and the
signature tattles.

---

## The same forces, one level up

None of this stops at the function boundary. The functional-core / imperative-shell architecture
is coupling-and-cohesion applied at module scale: a core cohesive around domain decisions (pure,
data-coupled, testable by calling), a shell cohesive around effects (I/O, retries, transactions),
and a seam between them made of *values* — parsed inputs going in, decisions and typed outcomes
coming out. The [validation-at-the-boundary](/dmx-fun/blog/validation-at-the-boundary-not-in-the-core)
pattern is the same idea for inputs: parse once at the edge into proof-carrying types, and the
core is no longer coupled to the wire format at all.

Two honest caveats, so the perspective stays a perspective and not a religion. First, functional
code can absolutely recreate tight coupling — a codebase where every internal API traffics in
deeply nested generic containers has stamp-coupled itself to its plumbing, and a ten-step
pipeline threading one growing tuple through every stage is common coupling with extra steps.
Second, data coupling being the *weakest* form does not make it *free*: a function taking nine
parameters is loosely coupled to each and hard to call anyway. The scales measure visibility and
strength, not virtue; judgment still decides how much surface a signature should have.

But the direction of the force is constant. Immutability converts shared fate into shared data.
Purity converts side channels into parameters and returns. Typed outcomes convert distant
handlers into local values. Sum types gather a concept's cases and operations into one nameable
unit. Every one of these moves is mechanical, checkable in review, and pushes in the same
direction the 1979 taxonomy called *best* — which is perhaps the least surprising naming
coincidence in software design.

The [dmx-fun](/dmx-fun/) library supplies the working parts for the coupling side —
[`Result`](/dmx-fun/guide/result) and [`Option`](/dmx-fun/guide/option) for signature-visible outcomes, `mapError` for
seam translation, [`Try`](/dmx-fun/guide/try) for fencing exception-throwing calls into values,
[`Validated`](/dmx-fun/guide/validated) and [`Guard`](/dmx-fun/guide/guard) for boundary parsing — as plain Java types. The [Developer Guide](/dmx-fun/guide/) walks through each.

---

## Further reading

- [Pure Functions and Side Effects](/dmx-fun/blog/pure-functions-and-side-effects) — the purity
  half of the cohesion argument, in full.
- [Why Avoid Mutable State?](/dmx-fun/blog/why-avoid-mutable-state) — the case against the
  tightest rung of the coupling ladder.
- [Designing a Good Error Type: Sealed Hierarchies Callers Can Act On](/dmx-fun/blog/designing-a-good-error-type)
  — per-operation error types and `mapError` at the seams, the decoupled failure contract.
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types)
  — signatures as the full statement of a function's dependency surface.
- [Where to Put Validation: At the Boundary, Not in the Core](/dmx-fun/blog/validation-at-the-boundary-not-in-the-core)
  — decoupling the core from the wire format with proof-carrying types.
- [Refactoring OO Toward a Functional Style](/dmx-fun/blog/refactoring-oo-toward-functional-style)
  — the incremental path from hidden coupling to visible data flow in existing code.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
