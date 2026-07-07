---
title: "Why 'Just Use Exceptions' Persists, and When It Is Actually Right"
description: "Every functional programming pitch eventually collides with a senior engineer saying 'just use exceptions.' That advice is not ignorance — exceptions win real trade-offs, and there are cases where they are genuinely the right tool. Here is an honest map of where each belongs."
pubDate: 2026-07-05
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Exceptions", "Error Handling", "Result", "Architecture"]
image: "https://images.pexels.com/photos/22775934/pexels-photo-22775934.jpeg"
imageCredit:
    author: "Han-Chieh Lee"
    authorUrl: "https://www.pexels.com/@han-chieh-lee-1234591373/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/sidewalk-in-park-22775934/"
---

Bring `Result<V, E>` to a code review and, sooner or later, someone senior will say it: *"Why not
just use exceptions?"* It is easy to hear that as resistance to change. It is usually the opposite —
it is someone who has shipped a lot of software and knows that exceptions buy something real.

The functional case for typed errors is strong, and this blog has made it more than once. But a
case you can only make by pretending the other side has no arguments is a weak case. So let us do
the harder thing: take "just use exceptions" seriously, understand *why* it persists, and then draw
the line where it is genuinely the right call — and where it quietly costs you.

---

## Why the advice persists

Exceptions did not survive fifty years by accident. They persist because they are genuinely good at
several things.

**They keep the happy path clean.** With exceptions, the code that does the work is uninterrupted
by the code that handles failure. You write the three steps that matter, and the `catch` sits off
to the side. A `Result`-threaded pipeline asks you to acknowledge failure at every hop; exceptions
let you ignore it until you choose not to.

```java
// The exceptional style reads as a straight line.
var user    = repo.load(id);         // throws if missing
var account = user.primaryAccount(); // throws if none
return account.balance();
```

**They propagate for free.** An exception thrown ten frames down arrives at your handler without a
single intermediate function mentioning it. A typed error has to be *carried* — every function in
the chain has to name it in its return type and thread it through. For deep call stacks where the
middle has nothing useful to say about the failure, exceptions are less ceremony.

**They are the language's native idiom.** The JDK throws. Every library you depend on throws. Your
team already knows the mechanics, your tooling shows stack traces, your APM groups by exception
class. Choosing typed errors means living in two worlds at once and paying the bilingual tax at
every boundary.

None of these are illusions. Anyone who tells you exceptions have no advantages has not maintained
a large codebase. The question is never "are exceptions bad" — it is "for *which failures* is each
mechanism the right fit."

---

## The distinction that actually matters

The useful line is not functional-versus-exceptions. It is **expected outcomes versus genuine
surprises.**

- An *expected outcome* is a result your business logic has an opinion about: a transfer declined
  for insufficient funds, a coupon that has expired, a username already taken. These are not
  errors in any real sense — they are answers. The domain *knows* they happen and has decided what
  to do about them.
- A *genuine surprise* is a condition that means the world is not as your code assumed: the
  database is unreachable, a config value is malformed at startup, an invariant you believed
  impossible has been violated, you are out of memory.

Exceptions are an excellent fit for surprises and a poor fit for outcomes. The trouble in most
codebases is that they use one mechanism — `throw` — for both, and so the two categories become
indistinguishable in the code. `InsufficientFundsException` and `SQLException` unwind the same
stack, get caught by the same `catch (Exception e)`, and get logged at the same severity. The type
system stops helping you the moment you conflate them.

Typed errors are how you make the first category visible:

```java
// An expected outcome: the domain has opinions about this. Make it a value.
Result<Receipt, TransferError> transfer(Account from, Account to, Money amount);
```

`TransferError` — `InsufficientFunds`, `AccountFrozen`, `LimitExceeded` — is now part of the
contract. A caller cannot forget to handle it, because the compiler will not let them reach the
`Receipt` without deciding. That is the whole benefit, and it applies precisely to the failures
your domain *expects*.

---

## When exceptions are actually right

Here is the part the functional pitch usually skips. There are cases where reaching for `Result` is
the wrong call, and an exception is the correct, idiomatic, lazy-in-the-good-sense answer.

**1. Programming errors and broken invariants.** If a condition means *your code has a bug* — a null
where you proved there could be none, an enum case that "cannot happen," an index past the end —
throw. You do not want the caller to handle these gracefully; you want them loud, fatal, and
attached to a stack trace. Wrapping a bug in a `Result` just teaches callers to pattern-match on
your mistakes.

```java
return switch (status) {
    case PENDING   -> confirm();
    case CONFIRMED -> ship();
    // Not a domain outcome — a contradiction. Fail loud.
    case CANCELLED -> throw new IllegalStateException("cancelled order reached dispatch: " + id);
};
```

**2. Unrecoverable, top-of-stack failures.** A malformed config at boot, a missing required
environment variable, a failed schema migration — when the only sane response is "stop the process
and page a human," an exception that propagates straight to the top is exactly right. Threading a
`Result` up thirty frames so `main` can `System.exit(1)` is ceremony with no payoff.

**3. Deep stacks where the middle has nothing to say.** If a failure originates deep and is handled
high, and none of the twenty functions in between can do anything useful about it, forcing each to
name and re-wrap the error is pure friction. Let it throw and catch it where the decision lives.

**4. Framework and interop boundaries that expect exceptions.** Spring's `@Transactional` rolls
back on a thrown exception. JAX-RS mappers key off exception types. When you are speaking a
framework's native protocol, throwing *is* the API. (This is exactly the gap
[`fun-spring-boot`](/dmx-fun/guide/spring-boot) exists to bridge — making a `Result.err(...)` roll
back a transaction — precisely because the two models do not line up for free.)

The honest rule: **exceptions for the exceptional, values for the expected.** If a failure is a
normal part of what your domain does, model it as a value. If it means something has gone wrong at
a level your business logic has no opinion about, throw.

---

## Where "just use exceptions" quietly costs you

So why not stop there and use exceptions for everything? Because the advice hides three real costs
when it is applied to *expected* outcomes.

**They are invisible in the type.** `Receipt transfer(...)` does not tell you it can fail, or how.
The knowledge that a transfer can be declined lives in a Javadoc line, tribal memory, or the
`catch` block a maintainer forgot to write. Unchecked exceptions are, by design, absent from the
signature — which is convenient until the failure you did not know about reaches production.

**They lose exhaustiveness.** A sealed `TransferError` with three cases means a `switch` over it is
checked by the compiler; add a fourth case and every handler that forgot it fails to compile.
`catch (TransferException e)` and a chain of `instanceof` gives you none of that. The domain's set
of failure modes stops being something the type system tracks.

**They collapse control flow you may want to keep.** Validation is the sharpest example. Exceptions
are fail-fast by nature — the first `throw` ends the story — so an endpoint that throws on the first
bad field can only ever report one error at a time. When you want to accumulate and return *all* of
them, you need a value that collects:

```java
Validated<NonEmptyList<String>, Customer> validate(SignupForm form) {
    return Validated.combine3(
        validateName(form.name()),
        validateEmail(form.email()),
        validatePassword(form.password()),
        NonEmptyList::concat,   // merge every failure, not just the first
        Customer::new);
}
```

There is no clean exception-based version of this that returns all three problems in one pass. The
mechanism you chose for error handling dictated the user experience — and fail-fast lost.

---

## You do not have to pick a side

The framing that causes all the trouble is that this is a binary — that a codebase is either
exception-based or functional. It is not, and the good ones never are.

A healthy service uses both, deliberately: **typed values for the failures the domain expects, and
exceptions for the surprises it does not.** The two meet at well-defined seams, and the tools for
crossing them are ordinary:

```java
// Exception-throwing world -> value world, at the boundary you choose.
Result<Config, ConfigError> parse(String raw) {
    return Try.of(() -> objectMapper.readValue(raw, Config.class))   // may throw
              .toResult(ConfigError::malformed);                      // Throwable -> domain error
}
```

`Try.of` captures a thrown checked exception as a failure at exactly one seam — the
boundary where an infrastructure surprise becomes a domain outcome you have decided to handle.
Inside that boundary, failures are values you compose. Outside it, exceptions do what they are best
at. Neither side has to pretend the other does not exist.

So the next time someone says "just use exceptions," the right answer is not to argue. It is:
*"Agreed — for the surprises. And for the outcomes the domain expects, let us make them values, so
the compiler helps us handle them."* That is not a compromise between two camps. It is what using
each tool for its actual job looks like.

The [dmx-fun](/dmx-fun/) library gives you the value side of that split — `Result`, `Option`,
`Validated`, and the seams (`mapCatching`, `Try`) for crossing between the two worlds. The
[Developer Guide](/dmx-fun/guide/) walks through each with backend-shaped examples.

---

## Further reading

- [Pragmatic FP vs. Academic Purism](/dmx-fun/blog/pragmatic-fp-vs-academic-purism) — choosing the
  right tool instead of the ideologically pure one.
- [When Making It Functional Makes It Worse](/dmx-fun/blog/when-making-it-functional-makes-it-worse)
  — the other half of this honesty: where typed errors are the wrong call.
- [Railway-Oriented Programming in Java](/dmx-fun/blog/railway-oriented-programming-in-java) — how
  the value side composes once you commit to it.
- [Validated: Accumulating Errors in a Functional Way](/dmx-fun/blog/validated-accumulating-errors)
  — the full story on the fail-fast limitation exceptions cannot escape.
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types)
  — signatures that tell the truth about the failures they expect.
- [Functional Thinking for Backend Engineers](/dmx-fun/blog/functional-thinking-for-backend-engineers)
  — the broader habit of modeling absence, failure, and validation as values.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
