---
title: "Project Valhalla and Value Classes: The Functional Payoff"
description: "Functional Java has been writing checks that the JVM's object model charges a fee to cash: every Option, every Result, every small immutable record is typically a heap allocation carrying an identity nobody uses. Project Valhalla's value classes — previewing in JDK 28 — remove exactly that fee, and the code best positioned to collect is the code that was already immutable and identity-free. Here is what is actually coming, when, and why functional style is the natural beneficiary."
pubDate: 2026-08-04
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Project Valhalla", "Value Classes", "JEP 401", "Java", "Performance", "JVM", "Functional Programming"]
image: "https://images.pexels.com/photos/5845951/pexels-photo-5845951.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "Tima Miroshnichenko"
    authorUrl: "https://www.pexels.com/@tima-miroshnichenko/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/hot-metal-bar-in-close-up-photography-5845951/"
---

Every object in Java carries something most functional code never uses: an **identity**. The
JVM guarantees that each `new` produces a distinct individual — one you can lock on, compare
with `==`, hang a weak reference off, observe mutating over time. That guarantee is not free.
It is why a `Point(3, 4)` typically lives in the heap with a header, why an `Option<Price>` is
usually a pointer to somewhere else, why an array of a million small records is a million
scattered allocations rather than one contiguous block — *typically*, because the JIT's
escape analysis already eliminates some of these allocations when a value provably never
escapes; the identity contract is what forces the pessimistic shape everywhere else.

Functional code pays this fee constantly and collects nothing for it. A value that is
immutable, whose `equals` is structural, that nobody locks or mutates — its identity is dead
weight. **Project Valhalla** exists to let such classes shed it, like the photo above: strip
the ornament, keep the forged metal. This post — from the platform-evolution side of this
blog — covers what is actually arriving, on what schedule, and why the functional style this
blog advocates is the code best positioned to benefit.

---

## What is actually coming, and when

The concrete state as of this writing (August 2026): [JEP 401 — Value Objects (Preview)](https://openjdk.org/jeps/401) was integrated into the OpenJDK mainline in July
2026, targeting **JDK 28 (March 2027) as a preview feature** — a ~197,000-line change, the
largest language change in a decade. Preview means opt-in (`--enable-preview`), disabled by
default, and Brian Goetz has signaled it will likely *remain* in preview past the next LTS.
You can experiment today with the [Valhalla early-access builds](https://jdk.java.net/valhalla/).
Plan learning now, production later.

The programming model is one modifier, on a record or a plain class (which becomes implicitly
final, with implicitly final fields):

```java
value record Money(BigDecimal amount, Currency currency) {}

value record Range(int low, int high) {}
```

A `value` class renounces identity. In exchange for giving up what identity enables —
`synchronized` on instances, identity-`==`, mutation — its instances become pure carriers of
their field values: `==` compares state, and the JVM gains the *freedom* (not the obligation)
to represent them without object headers or pointers — flattened into containing objects and
arrays, scalar-replaced in registers, re-materialized at will.

The two examples are chosen deliberately, because the payoff differs by shape. Small value
classes can flatten today: the JEP's own examples show `Integer[]` and `LocalDate[]` being
laid out as contiguous data in the preview — an implementation decision the JVM is free to
make, not a layout the spec promises — nulls included, via a flag bit encoded into the
flattened element. Anything whose layout exceeds what the JVM can read and write atomically (roughly 64
bits in mutable storage, null flag included) stays referenced for now — that is where the
null-restricted and relaxed-atomicity work still in progress picks up, and it is why a
`Range[]` may or may not flatten depending on that budget. And `Money` is still a fine value
class — identity gone, `==` structural — but its *fields* are reference types: `BigDecimal`
and `Currency` keep their own identity, so inside even a maximally flattened `Money` they
remain pointers. Value classes remove the wrapper's cost, not its fields' nature.

Two boundaries worth stating precisely, because Valhalla claims get inflated easily. First,
flattening is a JVM optimization decision, not a guarantee — the real blockers in the preview
are the atomicity budget above and *supertype-typed variables*: a value stored in an
`Object`-typed field or an erased generic (`T`) stays a reference regardless of how small it
is. (Nullability, contrary to intuition, does not block flattening — the null gets a flag
bit; the separate null-restricted-types work, still in draft on the
[project page](https://openjdk.org/projects/valhalla/value-objects), buys *denser* layouts.)
Second, specialized generics — `List<int>` without boxing — are a *later* phase of Valhalla,
not part of JEP 401. What previews in JDK 28 is the object model.

---

## Why functional code is first in line

Here is the part that matters for this blog: **the eligibility conditions for `value` are the
functional style's existing habits.** A class can renounce identity only if it is immutable,
final, and nobody depends on `==`-as-identity or per-instance locking. Mutable builders,
entity classes with lifecycle, lock targets — disqualified. But look at what this blog has
been building all along:

- [Records modeling domain data](/dmx-fun/blog/algebraic-data-types-for-business-developers) —
  immutable, structural equality, identity never used.
- The container types — `Option`, `Result`, `Try`, tuples — small immutable wrappers whose
  entire cost *is* the wrapper.
- [Immutable values shared freely](/dmx-fun/blog/immutability-in-java-an-oop-foundation)
  across threads, where nobody could observe identity anyway.

Functional Java has spent a decade being told its style allocates too much. The honest answer
was always twofold: mostly it [does not matter](/dmx-fun/blog/streams-immutable-collections-efficient-data-processing),
and the JIT already elides much of it through escape analysis — an *opportunistic* rescue
that gives up when a value escapes a compilation unit. Valhalla turns that opportunism into a
declared property: the class itself says "I have no identity," and the freedom to flatten
follows the value everywhere — into fields, across calls, into arrays.

The wrapper tax this blog has called "real but localized" is precisely the fee being
abolished — headers and pointers for values whose identity nobody ever used.

What about the container types themselves — `Option<Price>` as "nothing at all," a sentinel
plus the payload's fields? That full story is honestly a *later-phase* one, twice over: an
`Option`-typed variable is interface-typed (the `value` keyword on `Some`/`None` does not
un-box a variable declared as the supertype), and the `Price` inside is an erased generic —
exactly the two blockers stated above. Making the leaf records `value` classes is still
worthwhile (scalar replacement gets a declared license instead of an escape-analysis guess),
but the flattened-wrapper endgame for generic containers arrives with null-restriction and
specialized generics, not with the JDK 28 preview. The honest near-term wins are concrete
value types — your domain records — in fields and arrays.

---

## What this means for a functional codebase today

**Nothing to rewrite — that is the point.** If your domain types are records, your absence is
`Option`, your failures are [typed values](/dmx-fun/blog/designing-a-good-error-type), and
your state lives in [immutable collections](/dmx-fun/blog/streams-immutable-collections-efficient-data-processing),
your codebase is already *value-shaped*, and for simple value-shaped records the migration is
largely one keyword per class. "Largely" is doing honest work there: adding `value` is safe
only for types that genuinely meet the constraints — final, fully immutable state, no
identity-dependent semantics, no per-instance locking, constructors that fit the value-class
initialization rules — so audit the identity-sensitive paths (`==` comparisons, locks, weak
references) before flipping the keyword on anything less obviously value-shaped.

The JDK itself is the proof, and it already happened: JEP 401 declares some thirty platform
classes — `Optional`, `LocalDate`, `LocalDateTime`, `Duration`, `Integer`, and friends, the
long-documented "value-based classes" whose contracts always forbade identity-dependence —
as value classes *in the preview itself*. Run JDK 28 EA with `--enable-preview` and `==` on
a `LocalDate` compares state; `synchronized` on a statically-typed `LocalDate` is rejected at
compile time, and a value object that reaches a monitor through a general reference (an
`Object`-typed lock) fails at runtime with `IdentityException`. (That is also the migration
hazard in miniature: code that leaned on those classes' identity breaks — which is why their
docs warned against it for a decade.)

The dmx-fun types share the shape: `Option`, `Result`, and `Try` are sealed hierarchies of
small immutable records, the tuples are plain records, and none of them has identity
semantics anywhere in its contract. One honest exception proves the eligibility rule:
`Lazy` *does* depend on per-instance locking by design (ADR-012) — memoization is identity-ful
work — so it is exactly the kind of type that stays an identity class. And to be clear about
timelines: the library's Java 25 baseline (ADR-001) makes any `value` adoption a
future-major-version story once the feature leaves preview, not a roadmap item.

Meanwhile, the practical guidance inverts the usual performance conversation:

- **Do not contort code today to dodge small-object allocation** — the workarounds (primitive
  unpacking, parallel arrays, hand-rolled flyweights) are exactly the code Valhalla obsoletes,
  and they trade away the clarity that made the functional version worth having.
- **Do keep identity out of your value types' contracts** — no reliance on *identity*-`==`
  (structural `==` is exactly what value classes give you), no locking on domain values, no
  mutable "value" objects. Every such leak is a future migration blocker.
- **Treat it as preview.** Experiment on the EA builds, follow the
  [project page](https://openjdk.org/projects/valhalla/value-objects), and let the model
  stabilize before betting production code on flattening behavior.

The forge metaphor holds: Valhalla does not ask you to build different things — it burns away
what your things were never using. The code that wrote down its values as *values* gets the
payoff for free.

---

## Further reading

- [JEP 401 — Value Objects (Preview)](https://openjdk.org/jeps/401) — the
  authoritative spec for what previews in JDK 28.
- [Valhalla Early-Access Builds](https://jdk.java.net/valhalla/) — try value classes today.
- [Value Classes and Objects — Project Valhalla](https://openjdk.org/projects/valhalla/value-objects)
  — the project's own explainer on identity and flattening.
- [Algebraic Data Types for Business Developers](/dmx-fun/blog/algebraic-data-types-for-business-developers)
  — the record-based modeling that turns out to be value-class-ready.
- [Streams, Immutable Collections, and Efficient Data Processing](/dmx-fun/blog/streams-immutable-collections-efficient-data-processing)
  — where the allocation costs live today, and how the JIT already helps.
- [Immutability in Java: An OOP Foundation](/dmx-fun/blog/immutability-in-java-an-oop-foundation)
  — the discipline that doubles as Valhalla eligibility.
- [Why Upgrade to Java 25?](/dmx-fun/blog/why-upgrade-to-java-25) — the platform-evolution
  case for staying current while features like this mature.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
