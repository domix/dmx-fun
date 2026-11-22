---
title: "Building a Small Rules Engine with Composable Predicates"
description: "The phrase 'rules engine' conjures a vendor product with its own language, console, and invoice. But most systems that need one actually need something much smaller: a dozen eligibility checks that can be combined, tested one at a time, and — crucially — explain which ones failed. Like loose gears, each rule is a small part machined to mesh; the engine is just the composition. Here is one built from plain predicates, upgraded until it explains itself, in well under a hundred lines."
pubDate: 2026-08-16
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Rules Engine", "Business Rules", "Composition", "Guard", "Java", "Functional Programming", "Design Patterns"]
image: "https://images.pexels.com/photos/3785926/pexels-photo-3785926.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "Miguel Á. Padriñán"
    authorUrl: "https://www.pexels.com/@padrinan/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/photo-of-golden-cogwheel-on-black-background-3785926/"
---

Somewhere in your system there is a method like `isEligible(application)` — a page of
nested conditionals that decides who gets the loan, the discount, the upgrade, the refund.
It has three properties: nobody can state the full policy from reading it, every change
requires re-understanding all of it, and when it says `false`, nothing can say *why*.

The industry's reflex answer is "get a rules engine," which usually means a product with
its own authoring language and its own operational surface. This post takes the other
route, the one the loose gears in the photo suggest: machine each rule as a small,
identically shaped part, and let the engine be nothing but composition. Plain Java, a
handful of lines, and by the end the thing most vendor pitches lead with — *the rejection
that explains itself* — falls out of the design.

---

## Start where the JDK already is

`java.util.function.Predicate` is a composable rule language hiding in plain sight —
`and`, `or`, `negate` come with it:

```java
record Application(int age, BigDecimal income, int recentDefaults) {}

static final BigDecimal MIN_INCOME = new BigDecimal("20000");

static final Predicate<Application> ADULT         = app -> app.age() >= 18;
static final Predicate<Application> SOLVENT       = app -> app.income().compareTo(MIN_INCOME) >= 0;
static final Predicate<Application> CLEAN_HISTORY = app -> app.recentDefaults() == 0;

Predicate<Application> eligible = ADULT.and(SOLVENT).and(CLEAN_HISTORY);
```

This is already better than the conditional pile: each check has a name, each is testable
in isolation, and the policy reads as a sentence. For a guard clause deep inside your own
code, stop here — composition without ceremony.

But run it at a real decision boundary and the limitation surfaces immediately:
`eligible.test(application)` returns `false`, and `false` is a dead end. The support agent
asking "why was this customer rejected?", the regulator asking the same with a deadline,
the log line you will grep at 2am — all of them need *which rules failed*, and a composed
`Predicate` cannot tell you. It short-circuits, forgets, and answers in one bit.

---

## The whole engine is a dozen lines

Upgrade the atom: a rule is a predicate *plus its name*. An evaluation is the subject
*plus every rule it failed*. That is the entire design.

```java
record Rule<T>(String name, Predicate<T> passes) {}

record Evaluation<T>(T subject, List<String> failures) {
    Evaluation { failures = List.copyOf(failures); }   // defensive: keep passed() stable
    boolean passed() { return failures.isEmpty(); }
}

static <T> Evaluation<T> evaluate(List<Rule<T>> rules, T subject) {
    List<String> failures = rules.stream()
        .filter(rule -> !rule.passes().test(subject))
        .map(Rule::name)
        .toList();
    return new Evaluation<>(subject, failures);
}
```

Note what `evaluate` does *not* do: short-circuit. It runs every rule and keeps every
failure, because at a decision boundary the complete answer — "underage *and* income below
minimum" — is the difference between one support interaction and three. This is the same
insight that [accumulating validation](/dmx-fun/blog/validated-accumulating-errors) is
built on, applied to policy instead of input shape.

And because rules are values in a `List`, the policy is inspectable: the checklist of
rule names can be logged straight off the list, and the policy reads — and diffs in code
review — as declarative source instead of control flow. (Inspectability stops at each
rule's name: the predicate inside is opaque at runtime, so anything richer — rendering
the full structure for documentation — works from the source that builds the rules, not
from the objects.) Adding a rule is appending an entry with its own unit tests — the
property that made
[case 5 of the real-world post](/dmx-fun/blog/real-world-cases-where-fp-adds-value) a
schedule-visible win.

---

## Combinators: the gears mesh

AND-of-a-list is what `evaluate` gives you. Real policies need two more shapes, and
because a composite rule is just another `Rule`, they nest arbitrarily:

```java
static <T> Rule<T> anyOf(String name, List<Rule<T>> alternatives) {
    List<Rule<T>> options = List.copyOf(alternatives);   // snapshot: later list edits change nothing
    return new Rule<>(name, subject ->
        options.stream().anyMatch(rule -> rule.passes().test(subject)));
}

static <T> Rule<T> atLeast(String name, int n, List<Rule<T>> rules) {
    List<Rule<T>> checks = List.copyOf(rules);
    return new Rule<>(name, subject ->
        checks.stream().filter(rule -> rule.passes().test(subject)).count() >= n);
}
```

`anyOf` models alternative qualification paths ("salaried income *or* two years of
invoices"). `atLeast` models scorecards ("any three of these five signals") — a shape that
turns into remarkably tangled boolean logic when written by hand, and stays one readable
line here. (One asymmetry to know: `anyOf` stops at the first passing alternative —
`anyMatch` short-circuits — while `atLeast` as written evaluates every rule; if the rules
are expensive, `.filter(...).limit(n).count() == n` is the short-circuiting form.) The
tree of composites is your policy's actual structure, written down in source instead
of implied by nesting depth — the same shift as
[replacing branch sprawl with data](/dmx-fun/blog/replacing-if-else-sprawl-with-maps-of-functions).

Four honest caveats before you ship these combinators, because sharp edges in a rules
engine reject real people. First, a failing composite reports only *its own* name — which
inner alternative was the near-miss is discarded, the same one-bit answer this post held
against plain `Predicate`; the remedy within this design is to name composites by what
the user must fix ("proof of income"). Second, `anyOf` of an *empty* list always
fails (`anyMatch` on nothing is `false`) — deadly when alternatives are filtered
dynamically by product or region. Third, `atLeast` accepts thresholds that make it vacuous
(`n <= 0` passes everyone) or unsatisfiable (`n` greater than the rule count rejects
everyone), both silently; validate `n` where the threshold is computed or configured.
Fourth, a rule that *throws* — `SOLVENT` on an application whose `income` is `null`, say —
aborts the entire evaluation from inside the stream, producing no `Evaluation` at all: a
stack trace where the caller was promised a list of reasons, strictly worse than the
one-bit `false`. Either establish non-null inputs at the boundary before rules run, or
fold the exception into a failure the way `Guard.ofCatching` does below.

Two disciplines keep the engine honest at a higher level. Rules should be **pure** — a
rule that queries a repository mid-evaluation reintroduces ordering, latency, and partial
failure into what should be a calculation, so fetch first, decide second, keeping the
decision in the [pure core and the fetching in the shell](/dmx-fun/blog/should-all-business-logic-be-pure).
And rule *evaluation* must be separate from rule *consequence* — the engine reports which
rules failed; what to do about it (reject, price up, route to manual review) is the
caller's `switch`, not the rule's side effect.

---

## The same shape, already in the library

If the dozen-line engine looks familiar to readers of this blog, it should: a named check
that accumulates failures is exactly what dmx-fun's
[`Guard`](/dmx-fun/guide/guard) is, with the outcome made explicit in the type — either
the validated value, or a provably non-empty list of failure messages, rather than a bare
list you must remember to check. The atoms carry their message; `allOf` runs every guard
and accumulates:

```java
Guard<Application> adult   = Guard.of(app -> app.age() >= 18, "applicant must be an adult");
Guard<Application> solvent = Guard.of(
    app -> app.income().compareTo(MIN_INCOME) >= 0,
    app -> "income below minimum: " + app.income());
Guard<Application> cleanHistory =
    Guard.of(app -> app.recentDefaults() == 0, "recent defaults on file");

Guard<Application> eligibility = Guard.allOf(adult, solvent, cleanHistory);

Validated<NonEmptyList<String>, Application> outcome = eligibility.check(application);
```

The result is a [`Validated`](/dmx-fun/blog/validated-accumulating-errors): either the
application, or a non-empty list of every reason it failed. Consequences live in one
exhaustive `switch` at the boundary:

```java
String response = switch (outcome) {
    case Validated.Valid<NonEmptyList<String>, Application> ok ->
        approve(ok.value());                        // String approve(Application)
    case Validated.Invalid<NonEmptyList<String>, Application> rejected ->
        "rejected: " + String.join("; ", rejected.error().toList());
};
```

`Guard` also ships the engine niceties you would otherwise hand-roll next sprint —
including fixes for the caveats above. Its `anyOf`, when every alternative fails,
accumulates the messages from *all* of them (the near-miss stays visible), and its
mandatory first argument makes the empty composition unrepresentable. For throwing
predicates there is `Guard.ofCatching`, which folds a thrown `RuntimeException` into the
guard's failure message instead of letting it abort the evaluation. Beyond that: `or`
and `negate(message)` on individual guards, `withMessage` to rename a composite, and
`contramap` with a field label to aim a small reusable guard at part of a bigger object —
given a guard of your own like `notBlank = Guard.of(s -> !s.isBlank(), "must not be blank")`,
the composition `notBlank.contramap(User::email, "email")` reports failures as
`email: must not be blank`. Small gears, machined once, meshing across every policy in
the codebase.

---

## When you actually need the big engine

The honest boundary, so the title's "small" stays truthful. Reach for a real rules product
(Drools, a DMN engine) when the *organizational* requirements appear: business analysts
authoring rules without deployments, rule changes on a different release cadence than
code, thousands of interdependent rules where inference actually pays, an audit UI for
non-engineers. Those are real needs — and they are the product's price of admission, paid
in authoring tooling, runtime surface, and a second language nobody on the team reviews
fluently. Martin Fowler's [classic note on rules engines](https://martinfowler.com/bliki/RulesEngine.html)
makes the same point from the consulting trenches: much of what reaches for the product
needed the list of predicates.

Below that threshold — which is most eligibility, routing, and pricing logic in most
services — the composable version wins on every axis stakeholders can see: rules live in
git and are reviewed like code, each rule is a pure function with a unit test, the policy
is data you can print, and every rejection arrives with its reasons attached. The engine
was never the interesting part. The gears were.

---

## Further reading

- [Guard — the library guide](/dmx-fun/guide/guard) — the full API this post's engine is
  a distillation of: variance, catching guards, message mapping, field-context contramap.
- [Functional Design of Business Rules](/dmx-fun/blog/functional-design-of-business-rules)
  — rules as composable functions at production scale, including where database-backed
  checks fit.
- [Should All Business Logic Be Pure?](/dmx-fun/blog/should-all-business-logic-be-pure) —
  the fetch-first, decide-second boundary this post's purity discipline leans on.
- [Validated: Accumulating Errors in a Functional Way](/dmx-fun/blog/validated-accumulating-errors)
  — the error channel underneath `Guard.check`.
- [Replacing if/else and switch Sprawl with Maps of Functions](/dmx-fun/blog/replacing-if-else-sprawl-with-maps-of-functions)
  — the sibling move: dispatch as data, as rules are here.
- [RulesEngine — Martin Fowler](https://martinfowler.com/bliki/RulesEngine.html) — the
  build-vs-buy caution, from the source.
- [Real-World Cases Where Functional Programming Actually Adds Value](/dmx-fun/blog/real-world-cases-where-fp-adds-value)
  — case 5 is this post's pattern, measured by its schedule impact.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
