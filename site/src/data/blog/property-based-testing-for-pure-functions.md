---
title: "Property-Based Testing for Pure Functions"
description: "Example-based tests check the points you thought of; property-based tests check the laws that must hold at every point. Pure functions are the perfect customer for the technique: deterministic, self-contained, and reproducible from a seed. What a property is, the six patterns that cover most real code, how to write them in Java with jqwik, and the pitfalls that make teams give up too early."
pubDate: 2026-09-06
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Testing", "Property-Based Testing", "jqwik", "Pure Functions", "Functional Programming", "Java", "Quality"]
image: "https://images.pexels.com/photos/6990398/pexels-photo-6990398.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "DS stories"
    authorUrl: "https://www.pexels.com/@ds-stories/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/red-dice-in-the-air-6990398/"
---

An example-based test is a claim about one point: *given this exact input, the function returns this exact output*. Write five of them and you have verified five points in an input space that usually holds billions. The bugs, of course, live in the points you did not think to write — the empty list, the negative amount, the string with a surrogate pair, the total that does not divide evenly.

Property-based testing inverts the deal. Instead of picking inputs, you state something that must be true for *every* input — a law — and let the framework generate hundreds of randomized cases trying to break it. When it succeeds, it does not just fail the build: it hands you a simplified counterexample — shrinking reduces the failing input as far as it can within its search bounds, which is usually, but not guaranteed to be, the smallest one that violates the law.

The technique comes from the functional world — QuickCheck, published for Haskell in 2000 by Koen Claessen and John Hughes — and that origin is no accident. Properties need functions they can call thousands of times with arbitrary inputs and judge purely by the return value. In other words: they need [pure functions](/dmx-fun/blog/pure-functions-and-side-effects). If you have been pushing your logic into a pure core, you have already built the ideal test subject.

---

## A Property Is a Law, Not an Example

Take a function every billing system ends up writing: split an amount into *n* installments without losing a cent.

```java
public final class Payments {
    /** Splits total into n installments that differ by at most one cent. */
    public static List<BigDecimal> split(BigDecimal total, int parts) { ... }
}
```

The example-based suite asserts `split(100.00, 3)` is `[33.34, 33.33, 33.33]` and moves on. The property-based suite states the laws that *define* correctness:

1. The installments sum back to the total — no cent created, no cent lost.
2. There are exactly `parts` installments.
3. No two installments differ by more than one cent.

With [jqwik](https://jqwik.net/) — a property-based testing engine that runs on the JUnit 5 platform — the first law reads:

```java
@Property
void installmentsSumBackToTotal(
        @ForAll @BigRange(min = "0.01", max = "10000000.00") @Scale(2) BigDecimal total,
        @ForAll @IntRange(min = 1, max = 360) int parts) {

    var installments = Payments.split(total, parts);

    var sum = installments.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum).isEqualByComparingTo(total);
}
```

One property, hundreds of generated `(total, parts)` pairs per run — including the adversarial ones a human rarely writes down: one part, 360 parts, totals one cent above a clean division. If the implementation rounds carelessly, this test finds out.

And when it does, the second half of the machinery kicks in: **shrinking**. The framework does not lead with the ugly random input that first failed; it repeatedly simplifies it — smaller numbers, fewer parts — while the failure persists, and reports the simplest failing case it reached (alongside the original sample), plus the random seed so the run can be reproduced exactly. A failure report of `total = 0.01, parts = 2` tells you the bug at a glance; the raw eight-figure original would not.

---

## Why Pure Functions Are the Perfect Customer

Every requirement of the technique is a property purity gives you for free:

- **Callable in isolation, thousands of times.** No container, no database, no fixtures to reset between the hundreds of generated cases. Input in, output out.
- **Judged by the return value alone.** A property asserts on what the function *returns*. A function whose interesting behavior is a side effect has nothing for the property to inspect.
- **Deterministic replay.** When a property fails, the framework reports the seed; rerunning with that seed regenerates the same inputs. That is only a *reproduction* if the function returns the same output for the same input — which is exactly the definition of purity. An impure function turns the shrunk counterexample into a flaky rumor.
- **Laws actually exist.** Pure functions over well-modeled data tend to *have* algebraic properties — round-trips, invariants, idempotence. A method that reads three fields from a context object and fires two events has no laws to state.

This is why property-based testing slots so naturally into a functional architecture: the pure core gets properties, the [imperative shell gets a thin layer of integration tests](/dmx-fun/blog/testing-in-functional-programming), and the two barely overlap.

---

## The Six Patterns That Cover Most Real Code

Coming up with properties is the skill, and it is learnable. Six patterns cover the overwhelming majority of practical cases.

**1. Round-trip.** Encode then decode, and you must get the original back. Serialization, parsing, encryption, unit conversion:

```java
@Property
void renderedOrderIdsParseBack(@ForAll("orderIds") OrderId id) {
    assertThat(OrderId.parse(id.render())).isOk().containsValue(id);
}
```

(The string in `@ForAll("orderIds")` names a `@Provide` method on the test class that supplies the generator — the same wiring the `money()` provider below uses.)

This single property has ended more parser bugs than any assertion wall, and it pairs naturally with a `Result`-returning parse: the [error channel](/dmx-fun/blog/error-handling-without-exceptions) is part of the contract being tested.

**2. Invariants.** Something about the output holds regardless of input: a sort returns the same elements; a filter never grows the list; the split above never produces a negative installment.

**3. Idempotence.** Applying the function twice equals applying it once: `normalize(normalize(s)) = normalize(s)`. The law that catches "cleanup" functions that keep cleaning.

**4. Commutativity and associativity.** Two distinct laws, worth stating separately. Commutativity says argument *order* must not matter: `merge(a, b) = merge(b, a)`. Associativity says *grouping* must not: `merge(a, merge(b, c)) = merge(merge(a, b), c)`. Some operations legitimately have only one — a last-write-wins config merge is non-commutative by design — and it is associativity, not commutativity, that a parallel or chunked fold needs to stay correct. State the law the operation actually promises.

**5. Test against an oracle.** A simple, obviously-correct implementation checks a fast, clever one: the O(n²) brute force validates the optimized version on small inputs. Ideal when [refactoring for performance](/dmx-fun/blog/performance-cost-of-immutability-when-it-matters) — the old code becomes the oracle for the new.

**6. Hard to compute, easy to check.** Finding the answer may be expensive, but verifying it is cheap: whatever `solve` returns, `isValidSolution(input, solution)` must say yes. Factoring is hard; multiplying the factors back is not.

A useful smell in the other direction: if the only property you can state is a restatement of the implementation — *the discount is `price * 0.1` because the code multiplies by 0.1* — you have written the code twice, not a property. Reach for one of the six shapes instead.

---

## Generators Are Half the Craft

The default generators are deliberately adversarial — empty strings, `Integer.MIN_VALUE`, unpaired UTF-16 surrogates — and that is where much of the value lives. But domain types need domain generators, and jqwik lets you build them compositionally:

```java
@Provide
Arbitrary<Money> money() {
    return Arbitraries.bigDecimals()
        .between(BigDecimal.ZERO, new BigDecimal("10000000"))
        .ofScale(2)
        .map(Money::of);
}
```

Any property then consumes it by name — `@ForAll("money") Money amount` — exactly like the `orderIds` provider earlier.

Two honest warnings from the field:

- **Random does not mean representative.** A uniform generator over `int` almost never produces the small values where your off-by-one bugs live. Good engines bias toward edge cases (zero, one, bounds), but *you* know your domain's dangerous values — encode them into the generator rather than hoping.
- **Constrain with generators, not filters.** Generating arbitrary strings and discarding the ones that are not valid ISBNs throws away essentially every sample — and the engine does not quietly persist: jqwik fails the property once tried runs exceed checked runs by more than five to one (the default `maxDiscardRatio`, where discarded samples count toward the tried side). Build valid values constructively (`map`, `combine`) and keep the discard rate near zero.

The investment compounds: a good `Arbitrary<Order>` written once feeds every property you write about orders afterward — the same compositional payoff your [algebraic data types](/dmx-fun/blog/algebraic-data-types-for-business-developers) already gave the production code.

---

## Where It Fits in the Suite

Property-based tests do not replace example-based tests; they cover different failure modes, and mature suites use the whole toolbox:

- **Examples** pin concrete, communicative cases — the spec by illustration, and the regression test for the specific bug ticket.
- **Properties** patrol the input space for the cases nobody illustrated, and document the *laws* of the function in executable form.
- **[Golden tests](/dmx-fun/blog/golden-approval-tests-functional-pipelines)** guard large structured outputs where the law is "same as the approved version."

A practical adoption path: next time a bug report arrives for a pure function, do not just add the failing example. Ask *which law did this bug break?* — then write that law as a property. The example proves this bug is fixed; the property patrols for its whole family. One important caveat on what you get in return: a passing property is evidence from a few hundred sampled points, not a proof — the run is a search for counterexamples that happened to come up empty.

And if a function resists this treatment — it needs the clock, the database, a mock — that is not a property-based-testing problem. It is the function telling you it is not pure yet, which is [precisely the signal](/dmx-fun/blog/should-all-business-logic-be-pure) worth acting on.

---

## Conclusion

Property-based testing is the testing style pure functions were born for: state a law, generate the inputs, let shrinking hand you a minimized counterexample. Six patterns — round-trip, invariant, idempotence, commutativity and associativity, oracle, easy-to-check — cover most code you will ever write, and the discipline of asking *"what is always true here?"* improves the design even before the first test runs.

The dice in the header are loaded, and that is the point: hundreds of adversarial rolls per build, every one reproducible from a seed. Your examples check the answers you knew. Properties check the ones you didn't.

---

## Further reading

- [Testing in Functional Programming: Why It Is Often Simpler](/dmx-fun/blog/testing-in-functional-programming) — why the pure core needs no scaffolding to test at all
- [Pure Functions and Side Effects](/dmx-fun/blog/pure-functions-and-side-effects) — the prerequisite: what purity is and how to get it
- [Golden/Approval Tests for Functional Pipelines](/dmx-fun/blog/golden-approval-tests-functional-pipelines) — the complementary technique for large structured outputs
- [The Substitution Model: Evaluating Code in Your Head](/dmx-fun/blog/substitution-model-evaluating-code-in-your-head) — the reasoning discipline that makes laws statable in the first place
- [Error Handling Without Exceptions: A Functional Approach](/dmx-fun/blog/error-handling-without-exceptions) — `Result`-returning functions, the round-trip property's natural partner
- [Algebraic Data Types Explained for Business Software Developers](/dmx-fun/blog/algebraic-data-types-for-business-developers) — data modeled tightly enough that generators and laws follow
- [Should All Business Logic Be Pure?](/dmx-fun/blog/should-all-business-logic-be-pure) — what to do with the functions that resist properties
