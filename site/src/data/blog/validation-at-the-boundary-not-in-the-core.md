---
title: "Where to Put Validation: At the Boundary, Not in the Core"
description: "Scattered validation is a symptom of a missing decision: nobody agreed where checking ends and trust begins. Validate once, at the edge of the system, and convert raw input into types that carry the proof — so the core never has to check again. Here is the architecture, and the line between input validation and domain rules that makes it work."
pubDate: 2026-07-12
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Validation", "Architecture", "Functional Programming", "Java", "Validated", "Guard", "Domain Modeling"]
image: "https://images.pexels.com/photos/20862314/pexels-photo-20862314.jpeg"
imageCredit:
    author: "Brett Bennett"
    authorUrl: "https://www.pexels.com/@bgbennett/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/helpoort-castle-in-maastricht-netherlands-20862314/"
---

Walk through any mature codebase and count where a single email address gets validated. The
controller checks it is not blank. The service checks it again — defensively, because who knows
what other callers do. The domain object has a `validate()` method someone added after an
incident. The repository null-checks it before the insert. Four checks, four slightly different
rules, and still nobody can answer the only question that matters: *past this point, can I trust
this value?*

That scatter is not carelessness. It is the absence of a decision. Nobody agreed **where**
validation lives, so everyone validates everywhere, which in practice means nobody can rely on
anyone else having done it. The fix is architectural, and it is one sentence: **validate at the
boundary, and make the core impossible to call with unvalidated data.**

---

## The boundary is where the outside world gets in

Every service has a handful of doors: the HTTP endpoint, the message consumer, the file or config
reader, the row mapper that turns database columns back into objects. Everything that crosses one
of those doors is untrusted by definition — it came from a network, a user, a queue, another team.

Medieval cities understood this. The wall had a gate, the gate had a guard, and the guard checked
you *once* — at the gate. Nobody re-inspected your papers at the bakery. The entire value of the
wall was that being inside it *meant something*.

Your service should work the same way. Validation is the gate: raw, untrusted input is inspected
exactly once, at the door it came through. Everything inside the wall operates on the assumption
that the gate did its job — and the way to make that assumption safe is the next step.

---

## Parsing beats checking: make the type carry the proof

Here is where most codebases lose the thread. They validate at the boundary — and then pass the
*same raw type* inward:

```java
// The check happened... somewhere. The String cannot prove it.
void register(String email, String username) { ... }
```

A `String` that has been validated is indistinguishable from one that has not. So the core, unable
to tell the difference, checks again. The scatter is back — not because anyone forgot the
architecture, but because the type system was never told about it.

The fix is to make validation *produce something*: convert the raw input into a type that can only
exist if the rules passed. Validation stops being a yes/no inspection and becomes a
**transformation** — from data that might be wrong into data that cannot be:

```java
public record EmailAddress(String value) {
    public EmailAddress {   // every construction path enforces the invariant
        if (!isWellFormed(value)) {
            throw new IllegalArgumentException("invalid email address");
        }
    }

    public static Result<EmailAddress, String> parse(String raw) {
        return raw != null && isWellFormed(raw)
            ? Result.ok(new EmailAddress(raw))
            : Result.err("invalid email address");
    }

    private static boolean isWellFormed(String s) {
        int at = s.indexOf('@');
        return at > 0 && at < s.length() - 1;   // non-empty local and domain parts
    }
}
```

Now the core signature changes, and the whole game changes with it:

```java
// No String in sight. This method CANNOT receive an unvalidated email —
// there is no way to construct an EmailAddress without passing the gate.
void register(EmailAddress email, Username username) { ... }
```

The core needs zero defensive checks, not out of discipline but out of *impossibility*. This is
the idea Alexis King named "parse, don't validate": a check that only returns a boolean throws its
knowledge away; a parse that returns a new type keeps the knowledge where the compiler can see it.

---

## Accumulate at the gate — the user gets one answer

A boundary has one more job the core should never inherit: reporting *everything* that is wrong in
a single pass. A form with three bad fields deserves three errors, not a fail-fast drip of one per
round-trip. This is exactly what `Validated` is for:

```java
Validated<NonEmptyList<String>, Registration> parse(SignupForm form) {
    return Validated.combine3(
        EmailAddress.parseValidated(form.email()),
        Username.parseValidated(form.username()),
        Password.parseValidated(form.password()),
        NonEmptyList::concat,      // merge every violation
        Registration::new);        // built only if all three pass
}
```

`Valid(Registration)` — a fully parsed, trustworthy value — or `Invalid([...])` with the complete
list. And for the individual rules, a reusable `Guard` keeps the gate declarative instead of a
pile of `if`s:

```java
Guard<String> notBlank   = Guard.of(s -> !s.isBlank(),      "must not be blank");
Guard<String> minLength3 = Guard.of(s -> s.length() >= 3,   "must be at least 3 chars");

Guard<String> username = notBlank.and(minLength3);   // both checked, errors accumulated
```

Note where all of this lives: at the edge. HTTP status codes, error lists, field names, messages
for humans — that vocabulary belongs to the boundary. The core never sees a `SignupForm`, and the
boundary never makes a business decision.

---

## The line that makes this honest: input validation is not domain rules

"Validate at the boundary" gets misread as "the core never rejects anything," and that is wrong.
There are two different kinds of *no*, and the architecture only works if you keep them apart.

**Input validation** answers: *is this data well-formed?* Is the email shaped like an email, is
the amount a positive number, is the required field present. These are questions about the
*data*, answerable with no context — and they belong at the boundary, once.

**Domain rules** answer: *is this operation allowed?* Can this account transfer this amount today,
can a cancelled order be confirmed, has this coupon expired. These are questions about *state and
policy* — and they belong in the core, because only the core has the context to answer them:

```java
// Core. Not "is the data valid" — it is. This is "does the business allow it."
Result<Order, OrderError> confirm(Order order) {
    if (order.status() != Order.Status.PENDING) {
        return Result.err(new OrderError.NotPending());
    }
    if (order.items().isEmpty()) {
        return Result.err(new OrderError.Empty());
    }
    return Result.ok(order.withStatus(Order.Status.CONFIRMED));
}
```

The test for which side a check belongs on: **could you answer it with the value alone, before
touching any state?** If yes — format, presence, range, shape — it is input validation; do it at
the gate. If you need to load an aggregate, consult a policy, or look at the clock — it is a
domain rule; it lives in the core, expressed as a typed outcome like everything else there.

Confuse the two and both suffer. Domain rules at the boundary turn your controller into a second
business layer that drifts out of sync with the first. Format checks in the core mean the core
handles data that might be garbage, and every function inherits the paranoia.

---

## What each side looks like when it works

The shape that falls out of this decision is simple enough to draw:

```text
 untrusted world │ gate (boundary)                │ trusted core
─────────────────┼────────────────────────────────┼───────────────────────────
 SignupForm      │ parse → Validated/Result       │ register(EmailAddress, …)
 JSON, headers   │ accumulate errors, map to HTTP │ pure decisions, domain rules
 queue message   │ raw types stop here            │ proof-carrying types only
```

- **The boundary** parses, accumulates, and translates: raw types in, proof-carrying types or a
  complete error report out. It knows about HTTP and JSON and field names. It makes no business
  decisions.
- **The core** computes: validated types in, typed outcomes out. It never sees a raw `String`
  from the wire, never re-checks a format, and rejects things only for reasons the *business*
  understands.

The payoff compounds. Defensive checks disappear from the core because they are unrepresentable,
not because a code review caught them. Tests for the core stop needing malformed-input cases —
the type system already proved those impossible. And when validation rules change, there is
exactly one place to change them, because there was only ever one gate.

The [dmx-fun](/dmx-fun/) library provides the gate-side toolkit — [`Validated`](/dmx-fun/guide/validated)
for accumulation, [`Guard`](/dmx-fun/guide/guard) for reusable rules, `Result` for smart
constructors — as plain Java types. The [Developer Guide](/dmx-fun/guide/) walks through each.

---

## Further reading

- [Validated: Accumulating Errors in a Functional Way](/dmx-fun/blog/validated-accumulating-errors)
  — the full story on collecting every violation at the gate.
- [Functional Design of Business Rules](/dmx-fun/blog/functional-design-of-business-rules) — the
  other side of the line: encoding domain rules in the core.
- [Domain-Driven Design and Functional Programming: Allies or Rivals?](/dmx-fun/blog/ddd-and-functional-programming-allies-or-rivals)
  — smart constructors and invariants as part of a larger modeling picture.
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types)
  — signatures that tell the truth, including proof-carrying parameter types.
- [Pure Functions and Side Effects](/dmx-fun/blog/pure-functions-and-side-effects) — why the core
  stays pure and the edges do the messy work.
- ["Parse, Don't Validate"](https://lexi-lambda.github.io/blog/2019/11/05/parse-don-t-validate/),
  Alexis King — the canonical essay behind the boundary-parsing idea.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
