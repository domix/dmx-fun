---
title: "From Optional to Option: Migrating Null-Handling in an Existing Codebase"
description: "You do not migrate a codebase's null-handling with a big-bang rewrite — you migrate it the way flocks migrate: in formation, one leg at a time, everyone knowing the direction. Here is the mechanical playbook for moving a working codebase from Optional (and raw null) to Option: where to start, the conversion seams at every border, the API mapping, and the rules that keep a half-migrated codebase coherent while you finish."
pubDate: 2026-08-01
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Option", "Optional", "Migration", "Null Safety", "Java", "Refactoring", "Core Types"]
image: "https://images.pexels.com/photos/35709739/pexels-photo-35709739.jpeg?auto=compress&cs=tinysrgb&w=800"
imageCredit:
    author: "Veronika Andrews"
    authorUrl: "https://www.pexels.com/@veronika-andrews-2153322013/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/massive-flock-of-snow-geese-in-flight-35709739/"
---

This post is deliberately not about *whether* `Option` beats `Optional` — the trade-offs are
recorded in [ADR-015](https://domix.github.io/dmx-fun/adr/adr-015-option-vs-optional/), and a
future post in this series will weigh the three absence strategies side by side. This one —
continuing the working-with-the-core-types series — assumes you have decided, and answers the
question that actually stops teams: **how do you move a working codebase without breaking it
or living in a half-converted mess for a year?**

The answer has the same shape as every successful adoption story on this blog: no big bang,
boundary first, one seam at a time — and a few mechanical rules that keep the two types from
tangling while both are alive in the codebase.

---

## Step 0: agree on the target picture

A migration without a target picture converges on chaos. The one that works:

- **`Option` is the internal vocabulary.** Repositories, services, domain logic — anything
  your team owns returns `Option<T>` for absence, composing with the rest of the
  [typed-outcome toolkit](/dmx-fun/guide/).
- **`Optional` survives at the edges you do not own.** JPA's `findById`, third-party SDKs,
  and any public API your external consumers already depend on keep speaking `Optional`.
  You convert at the border, not in their code.
- **Raw `null` retreats to nowhere.** The migration's real payoff is that `null`-returning
  internal methods disappear entirely — `Option` is the replacement for both.

That picture gives every pull request a direction: conversions happen at borders, and each
migrated seam moves the border outward.

---

## The conversion seams

`Option` ships bidirectional bridges, so the two worlds interoperate wherever they meet:

```java
// Inbound: foreign Optional (JPA, SDKs) becomes Option at your adapter
Option<Customer> customer = Option.fromOptional(jpaRepository.findById(id));

// Inbound: legacy null-returning APIs become Option the same way
Option<String> header = Option.ofNullable(request.getHeader("X-Tenant"));

// Outbound: your Option becomes Optional where a framework demands it
Optional<Customer> forSpring = customer.toOptional();
```

These three bridges — `fromOptional`, `ofNullable`, `toOptional` — cover most borders. (The
hand-rolled `opt.map(Option::some).orElseGet(Option::none)` you may have seen in older code —
including this blog's earlier legacy-codebase post — is exactly what `fromOptional` replaces.)
A few more exist for specific edges: `getOrNull()` hands a value back to a null-expecting
legacy API without an `Optional` detour, `fromTryOptional` collapses the common
`Try<Optional<V>>` shape from wrapped repository calls, and when the destination is another
core type, `Result.fromOptional`, `Try.fromOptional`, and `Validated.fromOptional` bridge in
one step with no `Option` hop.

The discipline that keeps a mixed codebase sane is **one conversion per border, pointing
inward**: a foreign value is converted to `Option` exactly once, at the adapter where it
enters your code, and converted back exactly once if it must leave through a framework edge.
Any `toOptional()` or `getOrNull()` outside a named adapter is a review flag — it usually
means someone is unwinding the migration instead of extending it. And if you find
`fromOptional(x.toOptional())` chains mid-service, a border was drawn in the wrong place.

---

## The migration order that does not hurt

**1. Start with one repository interface.** Data access is where `Optional` (and `null`)
enters most codebases, the signatures are few, and the callers are localized:

```java
interface CustomerRepository {
    Option<Customer> findByEmail(EmailAddress email);   // was Optional<Customer>
}
```

The implementation converts at its own border (`Option.fromOptional(jpa.findByEmail(...))`),
and only this interface's callers need to change with it — a reviewable PR, not a campaign.

**2. Let the compiler walk you outward.** Changing the return type breaks every caller —
deliberately. Each red call site is one mechanical translation (the table below), and when
the build is green, that seam is *done*: no half-migrated file, no "we'll come back to it."

**3. New code starts on `Option` from day one.** The border only moves one direction.

**4. Public APIs migrate last or never.** A published *Java* API with external consumers
keeps `Optional` in its signatures as long as compatibility demands; internally it converts
at its own boundary like any other foreign edge. REST DTOs are softer than they look: with
the [fun-jackson](/dmx-fun/guide/jackson) module registered, `Option.some(v)` serializes as
the value and `none()` as `null` — wire-identical to `Optional` handling — so a DTO's
migration is invisible to JSON consumers.

---

## The mechanical translation table

Most call sites are one-to-one renames. The full API is in the
[Option guide](/dmx-fun/guide/option); these are the ones migration actually meets:

| `Optional` call site            | `Option` equivalent                  |
|---------------------------------|--------------------------------------|
| `Optional.of(v)` / `empty()`    | `Option.some(v)` / `Option.none()`   |
| `Optional.ofNullable(v)`        | `Option.ofNullable(v)`               |
| `opt.map(f)` / `flatMap(f)` / `filter(p)` | same names, same semantics |
| `opt.orElse(fallback)`          | `opt.getOrElse(fallback)`            |
| `opt.orElseGet(supplier)`       | `opt.getOrElseGet(supplier)`         |
| `opt.orElseThrow(supplier)`     | `opt.getOrThrow(supplier)` — see caveat below |
| `opt.or(() -> other)`           | `opt.orElse(() -> other)` — same lazy shape, returns `Option` |
| `opt.isPresent()` / `isEmpty()` | `opt.isDefined()` / `isEmpty()`      |
| `opt.ifPresent(action)`         | `opt.peek(action)` or `match(onNone, onSome)` |
| `opt.stream()`                  | `opt.stream()`                       |

Two spots deserve a highlight because they are the classic mixed-codebase stumbles. First,
`Option.orElse` has two overloads — an eager one taking an alternative `Option` and a lazy
one taking a `Supplier` — while the *value* fallback is `getOrElse`; when migrating
`Optional.or`, keep the supplier form so a lazy fallback stays lazy (rewriting
`cached.or(() -> loadFromDb(id))` to the eager overload would run the query on every cache
hit). Second, `getOrThrow` accepts a `Supplier<? extends RuntimeException>` only — call
sites whose `orElseThrow` supplier produces a *checked* exception cannot be renamed
mechanically; wrap the checked exception or, better, take the exit below to `toResult`.

Where migration pays beyond renames is the API `Optional` never had: `fold(onNone, onSome)`
collapses both cases in one expression, `sequence` turns `List<Option<T>>` into
`Option<List<T>>`, the `Options.presentToList()` collector gathers the present values from a
stream of options, and — the real point — `toResult(error)` upgrades absence into a
[typed failure](/dmx-fun/blog/designing-a-good-error-type) the moment "not found" stops being
a normal case and starts being an error:

```java
Result<Receipt, LookupError> receipt =
    customers.findByEmail(email)
        .<LookupError>toResult(new LookupError.UnknownEmail(email))
        .flatMap(customer -> billing.latestReceipt(customer));
```

(The explicit `<LookupError>` witness is required exactly here — in a chain that continues
with `flatMap`, inference would otherwise fix the error type to the `UnknownEmail` record;
the wrinkle is [described in full here](/dmx-fun/blog/designing-a-good-error-type). On a
plain assignment with no chain, target-typing infers it and the witness can be dropped.)

---

## Rules for the in-between months

A real migration lives months with both types present. Three rules keep that period boring:

- **Direction is law** — the one-conversion-per-border rule from above, held for months, not
  just drawn once.
- **Migrate seams, not files.** The unit of work is "this interface plus its callers," ended
  by a green build. Half-migrated *seams* are the mess; coexisting *seams at different
  stages* are fine and invisible.
- **Enforce mechanically, not socially.** For the `Optional` leg, an ArchUnit-style rule
  banning `Optional` returns outside the adapter packages. For the raw-`null` leg, the tool
  the library itself uses ([ADR-008](https://domix.github.io/dmx-fun/adr/adr-008-jspecify-null-safety/)):
  jspecify's `@NullMarked` on migrated packages plus a checker like NullAway, which turns
  "no null-returning internals" from a review hope into a build failure.

The pattern is the same incremental adoption story this blog keeps returning to — one return
type at a time, [legacy-codebase edition](/dmx-fun/blog/introducing-fp-into-legacy-codebase):
value per pull request, reversible steps, no paradigm purchase required. The flock gets there
because every bird flies the same heading, not because anyone teleports.

---

## Further reading

- [How to Introduce Functional Programming into a Legacy Codebase](/dmx-fun/blog/introducing-fp-into-legacy-codebase)
  — the general incremental-adoption playbook this migration instantiates.
- [Designing a Good Error Type](/dmx-fun/blog/designing-a-good-error-type) — where
  `toResult` sends you when absence becomes a typed failure.
- [Where to Put Validation: At the Boundary, Not in the Core](/dmx-fun/blog/validation-at-the-boundary-not-in-the-core)
  — the same border-drawing discipline, applied to input checking.
- [Functional Programming in Java Without Losing Pragmatism](/dmx-fun/blog/functional-programming-in-java-without-losing-pragmatism)
  — the one-return-type-at-a-time stance in full.
- [Option — Developer Guide](/dmx-fun/guide/option) — the complete API these translations
  target.
- [ADR-015 — Option vs Optional](https://domix.github.io/dmx-fun/adr/adr-015-option-vs-optional/)
  — the recorded rationale for having a separate Option type at all.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
