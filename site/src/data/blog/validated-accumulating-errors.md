---
title: "Validated: Accumulating Errors in a Functional Way"
description: "Most error handling reports the first failure and stops. A registration form, an API request, a config file — these need every problem at once, not one round-trip per mistake. Validated is the type that collects all errors instead of short-circuiting, and the reason it works comes down to one deep idea: independent checks compose differently from dependent ones."
pubDate: 2026-06-30
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Validated", "Error Handling", "Validation", "Applicative"]
image: "https://images.pexels.com/photos/590022/pexels-photo-590022.jpeg"
imageCredit:
    author: "Pixabay"
    authorUrl: "https://www.pexels.com/@pixabay/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/590022/"
---

Fill in a sign-up form, leave three fields wrong, hit submit — and get told only that
your password is too short. You fix it, submit again, and *now* learn the email is
invalid. Fix that, submit, and the username is taken. Three round-trips for three
problems you could have seen at once.

That experience is the visible symptom of **fail-fast error handling**. Most of the
tools we reach for — exceptions, and even a `Result` type — stop at the first failure.
That is exactly what you want for a sequential pipeline, and exactly what you do *not*
want for validation. `Validated` is the type built for the second case: it runs
independent checks and **accumulates every error**, returning all of them together.

This post explains what `Validated` is, why it accumulates where `Result` short-circuits,
and the one deep reason the two behave differently.

---

## Two kinds of "and then"

Consider validating a registration request with three fields. There are two ways to
combine the checks, and they are not the same.

**Sequential / dependent:** each step needs the previous one's result.

> Parse the user ID → load that user → check the user's permissions.

You cannot load a user you failed to parse. If step one fails, steps two and three are
*meaningless*, so stopping at the first error is correct. This is what `flatMap` does,
and it is the heart of [railway-oriented programming](/dmx-fun/blog/railway-oriented-programming-in-java).

**Parallel / independent:** each check stands on its own.

> The username must be non-blank **and** the email must contain `@` **and** the age must
> be positive.

None of these depends on the others. The email's validity has nothing to say about the
age. When they fail, you want *all* the failures, because reporting them one at a time is
just making the user pay for your control flow.

`Result` models the first kind. `Validated` models the second.

---

## What `Validated` is

`Validated<E, A>` is a sealed type with two cases, mirroring `Result`:

- `Valid(A)` — the value passed every check
- `Invalid(E)` — one or more checks failed, with the accumulated errors in `E`

```java
Validated<NonEmptyList<String>, String> ok      = Validated.valid("alice");
Validated<NonEmptyList<String>, String> bad      = Validated.invalidNel("must not be blank");
```

The error channel is a `NonEmptyList<String>` — *non-empty* because if something is
`Invalid`, there is by definition at least one error, and modeling that in the type means
you never handle an "invalid with zero errors" case that cannot happen. (dmx-fun fixes the
error type as `NonEmptyList<String>` deliberately; the reasoning is in
[ADR-006](https://domix.github.io/dmx-fun/adr/adr-006-validated-vs-result/).)

So far this looks like `Result` with a different name. The difference is entirely in
*how you combine values*.

---

## Combining: where the magic is

Say you have a `Guard` per field — a named, composable predicate that produces a
`Validated` when checked:

```java
Guard<String>  username = Guard.of(s -> !s.isBlank(),      "username must not be blank");
Guard<String>  email    = Guard.of(s -> s.contains("@"),   "email must contain @");
Guard<Integer> age      = Guard.of(n -> n > 0,             "age must be positive");
```

Each `check` yields a `Validated`. Now combine them. With `Result` and `flatMap`, the
first `Err` ends the story:

```java
// Fail-fast: stops at the first failing field
Result<User, String> user = parseUsername(form).toResult()
    .flatMap(u -> parseEmail(form).toResult().map(e -> ...))
    .flatMap(...);
// Bad form -> ONE error, whichever field flatMap reached first
```

With `Validated`, you combine the independent results and the errors **pile up** instead
of cancelling the computation:

```java
Validated<NonEmptyList<String>, User> user =
    username.check(form.username())
        .product(email.check(form.email()))   // combine two Validated
        .product(age.check(form.age()))
        .map(tuple -> buildUser(tuple));

// Bad form -> Invalid(["username must not be blank",
//                       "email must contain @",
//                       "age must be positive"])  — ALL of them
```

When two `Validated` values are combined and both are `Invalid`, their error lists are
concatenated. When both are `Valid`, their values are paired up. That concatenation —
running every check and merging the failures — is the whole point.

---

## Why `Validated` is not a monad (and why that matters)

Here is the part worth slowing down for, because it explains *why* you need a separate
type at all instead of just teaching `Result` to accumulate.

A monad's defining operation is `flatMap`: `A -> F<B>`. The crucial detail is that the
function producing the next step **receives the previous value**. That means the second
step literally cannot run until the first has produced a value — so if the first fails,
there is no value, and `flatMap` *must* short-circuit. Accumulation is impossible by
construction: you can't run step two to collect its error when step two needs step one's
result that never arrived.

Accumulation needs a weaker, more permissive operation — combining values that **do not
depend on each other**. In functional vocabulary that operation is *applicative*
(`product` / `zip` / `mapN`): it takes `F<A>` and `F<B>` that were each computed
independently and combines them. Because neither waited on the other, both have already
run by the time you combine, so both errors are available to merge.

That is the real reason `Validated` exists alongside `Result`:

- `Result` is a **monad** → sequential, short-circuiting, "stop at first error."
- `Validated` is an **applicative** → parallel, accumulating, "report every error."

It is not that `Validated` is a better `Result`. It deliberately gives up `flatMap` —
you cannot make a later check depend on an earlier one — in exchange for the ability to
collect all failures. Trade dependency for accumulation.

---

## The error type has to be combinable

There is a small but important constraint hiding in "the errors pile up." To merge two
error channels into one, the error type needs a notion of *combination* — a way to take
two `E` values and produce one. For a list of messages that is just concatenation; for
richer types it is whatever "merge" means for them. (In the abstract this combinable
property is called a *semigroup*, but you do not need the word to use it.)

dmx-fun sidesteps the ceremony by fixing the error type as `NonEmptyList<String>`:
concatenation is always available, no merge function required, and there is always at
least one message. The trade-off is that if you want typed domain error *objects* rather
than strings, you work with `Validated<E, A>` directly and supply the merge yourself.
For the overwhelmingly common case — collecting human-readable validation messages —
the fixed type is exactly right.

---

## When to reach for which

A simple test: **does any check depend on the result of another?**

- **No, they are independent** (form fields, config keys, the parts of a request body) →
  `Validated`. Run them all, report them all. This is the better user experience and the
  honest model of the problem.
- **Yes, they are sequential** (parse, then look up, then authorize) → `Result` with
  `flatMap`. Short-circuiting is correct; running later steps on a failed earlier one is
  meaningless or unsafe.

Real systems mix both. A common shape is: accumulate the independent field validations
with `Validated`, then, once you have a well-formed value, switch to `Result` for the
dependent steps that follow. The two types are designed to interoperate — a `Validated`
converts to a `Result` when you are ready to leave the accumulating world and re-enter the
sequential one:

```java
Result<User, NonEmptyList<String>> result = validatedUser.toResult();
// now flatMap into the dependent pipeline
```

---

## Summary

`Validated` is the error-handling type for **independent** checks:

- It has two cases, `Valid(A)` and `Invalid(E)`, like `Result` — but it **accumulates**
  errors instead of stopping at the first.
- It accumulates because it is an **applicative**, not a monad: it combines values that
  were computed independently, so every check has already run and every error is available
  to merge. A monad's `flatMap` makes each step depend on the last, which forces
  short-circuiting.
- The error channel must be combinable; dmx-fun fixes it as `NonEmptyList<String>` so
  message-collecting validation works with zero ceremony.
- Use `Validated` for parallel validation (forms, requests, config), `Result` for
  sequential pipelines, and convert between them at the boundary where independent
  validation hands off to dependent processing.

The next time a form makes you submit three times to discover three mistakes, you will
know exactly which type its authors reached for — and which one they should have.

---

## Further reading

- [Railway-Oriented Programming in Java (Without Frameworks)](/dmx-fun/blog/railway-oriented-programming-in-java) — the fail-fast `Result` model that `Validated` complements
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types) — putting `Validated` in method signatures so accumulation is part of the contract
- [Functional Design of Business Rules](/dmx-fun/blog/functional-design-of-business-rules) — where independent validation rules naturally live
- [Algebraic Data Types Explained for Business Software Developers](/dmx-fun/blog/algebraic-data-types-for-business-developers) — the sealed `Valid`/`Invalid` modeling underneath
