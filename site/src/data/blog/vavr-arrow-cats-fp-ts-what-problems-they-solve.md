---
title: "Vavr, Arrow, Cats, fp-ts: What Problems Do They Solve?"
description: "Four libraries, four languages, one recurring motivation. Vavr, Arrow, Cats, and fp-ts exist because mainstream languages gave developers functional building blocks but left out the types that make them safe. This post maps what each library actually provides — and what gap in the host language each one fills."
pubDate: 2026-06-16
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Functional Programming", "Java", "Kotlin", "Scala", "TypeScript", "Vavr", "Arrow", "Cats", "fp-ts", "Libraries"]
image: "https://images.pexels.com/photos/256541/pexels-photo-256541.jpeg"
imageCredit:
    author: "Pixabay"
    authorUrl: "https://www.pexels.com/@pixabay/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/library-256541/"
---

If you have spent any time reading about functional programming in the JVM or JavaScript ecosystems, you have run into the same four names: **Vavr** for Java, **Arrow** for Kotlin, **Cats** for Scala, **fp-ts** for TypeScript.

They get mentioned together so often that it is easy to assume they are interchangeable — four flavors of the same thing. They are not. They differ in scope, in ambition, and in how much of their host language's grain they cut against.

But they do share a common origin story. Each one exists because a mainstream, multi-paradigm language gave developers lambdas and a streams API, and then stopped — leaving out the types that make functional programming actually safe. These libraries fill that gap. This post maps what each one provides, and what specific hole in the host language it was built to fill.

---

## The Common Gap

Start with what every one of these languages already has.

Java, Kotlin, Scala, and TypeScript all support first-class functions. They all have lambdas. They all have a `map`/`filter`/`reduce` story for collections. By the mid-2010s, "functional programming" in the sense of "passing functions around" was solved everywhere.

What none of them shipped with — or shipped incompletely — was the second half of the functional toolkit: the **data types that model outcomes**.

- A type for *absence* that is better than `null`
- A type for *failure* that is better than a thrown exception
- A type for *accumulating* multiple errors instead of stopping at the first
- A type for *deferred* or *asynchronous* computation that composes
- The combinators — `map`, `flatMap`, `fold`, `traverse` — that make those types work together

This is the gap. Every library on this list is, at its core, an implementation of these types plus the operations that connect them. Where they diverge is *how far past that baseline they go*.

---

## Vavr (Java): Backfilling What Java Left Out

Java is the most conservative language of the four, and Vavr is the most conservative library — by necessity. It exists to give Java the data types the JDK never added.

The JDK has `Optional<T>`. It does not have:

- A `Try<T>` for capturing exceptions as values
- An `Either<L, R>` for representing two outcomes
- A `Validated` for accumulating errors
- Persistent, truly immutable collections (`List`, `Map`, `Set`, `Seq`)
- Tuples
- Pattern matching (before Java caught up with sealed types and switch patterns)

Vavr provides all of these:

```java
// Vavr's Try captures the exception as a value
Try<Config> config = Try.of(() -> loadConfig(path));

Try<ServerSettings> settings = config.map(Config::serverSettings);

// Vavr's Either for a typed two-outcome result
Either<ValidationError, Order> result = validate(request)
    .map(this::buildOrder);

// Vavr's persistent collections — immutable, structural sharing
io.vavr.collection.List<Integer> nums = io.vavr.collection.List.of(1, 2, 3);
io.vavr.collection.List<Integer> more = nums.append(4);  // nums unchanged
```

Vavr's scope is deliberately bounded. It does not introduce type classes, higher-kinded types, or an effect system. It cannot — Java's type system will not express them cleanly. Vavr's job is to fill the most painful, most concrete gaps: typed errors, immutable collections, `Try`. It is a *data types* library, not a *category theory* library.

This is worth dwelling on, because it explains a tension Java developers feel. As Java the language has added `record`, sealed interfaces, and pattern-matching `switch`, some of Vavr's original motivation has eroded. You can now model `Either` and `Result` yourself with a sealed interface and two records — which is exactly the approach a JDK-first library like dmx-fun takes. Vavr's persistent collections and its large combinator catalog remain its strongest justification.

---

## Arrow (Kotlin): Idiomatic FP for a Pragmatic Language

Kotlin sits one step further toward functional expressiveness than Java. It has nullable types built in (`String?`), so the "absence" problem is largely solved at the language level — you rarely need an `Option` type in Kotlin because `T?` already does the job with compiler support.

Arrow, therefore, does not spend much effort on `Option`. It focuses on what Kotlin still lacks:

- `Either<L, R>` and a rich error-handling story
- `Validated` / accumulating errors (via `EitherNel` and friends)
- Typed error handling with `Raise` — a Kotlin-idiomatic effect that uses the language's own control flow
- Coroutine-aware functional composition
- Optics (lenses, prisms) for immutable data updates

What makes Arrow notable is how hard it leans on Kotlin's language features instead of fighting them. The `Raise` DSL is the clearest example:

```kotlin
// Arrow's Raise: typed errors using Kotlin's own syntax, no flatMap chains
fun Raise<OrderError>.placeOrder(request: Request): Order {
    val customer = findCustomer(request.customerId)  // may raise OrderError
    val items    = validateItems(request.items)      // may raise OrderError
    return Order(customer, items)
}

// Run it, get an Either back
val result: Either<OrderError, Order> = either { placeOrder(request) }
```

Notice there is no explicit `flatMap`. Arrow uses Kotlin's context receivers and suspension machinery so that error-prone code reads like ordinary sequential code, while still being typed and short-circuiting. This is Arrow's philosophy: functional *semantics*, imperative-looking *syntax*, riding on Kotlin's grain rather than against it.

---

## Cats (Scala): The Full Type Class Hierarchy

With Cats, the ambition changes category entirely.

Vavr and Arrow are, broadly, "here are some useful data types." Cats is "here is the abstract algebra that all of those data types share, expressed as type classes, so you can write code that is generic over *any* type that satisfies the laws."

Scala has higher-kinded types — the ability to abstract over `F[_]`, a type constructor, not just a concrete type. This is the language feature Java and (until recently) Kotlin lack, and it is what makes Cats possible.

Cats provides the now-canonical hierarchy:

- `Functor` — anything you can `map` over
- `Applicative` — anything you can combine independent values in
- `Monad` — anything you can `flatMap` (sequence dependent steps) in
- `Semigroup` / `Monoid` — anything you can combine associatively
- `Traverse` — turn a structure of effects into an effect of a structure

```scala
import cats.syntax.all._

// This function works for ANY F that has an Applicative instance:
// Option, Either, List, IO, Future, Validated...
def combine[F[_]: Applicative](fa: F[Int], fb: F[Int]): F[Int] =
  (fa, fb).mapN(_ + _)

combine(Option(1), Option(2))          // Some(3)
combine(List(1, 2), List(10, 20))      // List(11, 21, 12, 22)
```

That single `combine` function is genuinely generic over the *shape of the effect*. You cannot write this in Java or vanilla Kotlin — there is no way to say "for any container `F` that supports combining." This is the power Cats unlocks, and it is also its cost: the abstraction is real, and so is the learning curve. Cats (and its sibling Cats Effect for managing side effects) is the most powerful and the most demanding library on this list.

---

## fp-ts (TypeScript): Bringing Typed FP to JavaScript

fp-ts brought the Cats/Scalaz style of programming to TypeScript. It provides `Option`, `Either`, `Task` (async), `TaskEither`, `Reader`, and the same type class vocabulary — `Functor`, `Monad`, `Traversable` — simulated on top of TypeScript's structural type system.

TypeScript does not have higher-kinded types natively, so fp-ts famously emulated them with a clever "URI" encoding. The result was powerful but notoriously verbose, especially around the pipe-based composition style:

```typescript
import { pipe } from 'fp-ts/function'
import * as E from 'fp-ts/Either'

const result = pipe(
  validateInput(raw),                       // Either<Error, Input>
  E.flatMap(parseOrder),                    // Either<Error, Order>
  E.map(order => order.total),              // Either<Error, number>
  E.getOrElse(() => 0)
)
```

fp-ts is worth understanding for a reason beyond TypeScript itself: it is the clearest illustration of what happens when you push a full type-class-based FP style into a language whose type system was not designed for it. It works, it is type-safe, and it is undeniably heavier than the language's native idioms. That tension is exactly why its successor, **Effect**, has been gaining ground — it keeps the typed-error and typed-effect benefits while presenting a more approachable, less category-theory-forward API. The lesson echoes Arrow's: the libraries that thrive are the ones that bend toward the host language's grain.

---

## Side by Side

| | **Vavr** | **Arrow** | **Cats** | **fp-ts** |
|---|---|---|---|---|
| Language | Java | Kotlin | Scala | TypeScript |
| Higher-kinded types? | No (language limit) | Emulated/limited | Yes (native) | Emulated (URI trick) |
| Core offering | Data types + immutable collections | Typed errors + optics, on Kotlin's grain | Full type class hierarchy | Type classes ported to TS |
| `Option` emphasis | Yes (JDK gap) | Low (Kotlin has `T?`) | Yes | Yes |
| Effect system | No | Partial (coroutines) | Yes (Cats Effect) | Yes (Task; Effect successor) |
| Learning curve | Low | Moderate | High | High |
| Fills which gap | Missing JDK data types | Composable typed errors | Generic abstraction over effects | Typed FP for JS |

---

## What This Tells You About the Java Decision

Reading across all four, a pattern emerges that is directly useful when deciding what to do in Java.

The libraries split into two philosophies:

1. **Data-types-first** (Vavr, mostly Arrow): "Here are `Option`, `Either`, `Try`, `Validated` and the combinators to use them. We will not ask you to learn category theory." These libraries are easy to adopt incrementally and easy to leave.

2. **Type-classes-first** (Cats, fp-ts): "Here is an abstract algebra. Once you learn it, you can write code generic over any effect." These deliver more power and demand more investment, and they tend to take over a codebase's style rather than sit at its edges.

Java, with its lack of higher-kinded types, simply cannot host the second philosophy comfortably — and that is fine. The realistic, valuable target for Java is the *first* one: typed absence, typed failure, typed accumulation, and the combinators that compose them. That is exactly the niche Vavr occupies and the niche a smaller, JDK-first library like dmx-fun aims at with `Option`, `Result`, `Try`, `Either`, and `Validated` built directly on sealed interfaces and records.

The arrival of records, sealed types, and pattern-matching `switch` means a Java team can now get a large fraction of the data-types-first benefit either from a focused library or, for the simplest cases, from a handful of their own sealed types. The type-classes-first power of Cats remains genuinely out of reach — and for most business software, genuinely unnecessary.

---

## So: What Problems Do They Solve?

All four solve the same root problem — **mainstream languages shipped the function-passing half of functional programming and skipped the typed-outcome half** — but they aim at different points on the power/cost curve:

- **Vavr** restores the data types Java's standard library never added.
- **Arrow** gives Kotlin composable typed errors that read like ordinary code.
- **Cats** gives Scala a complete, lawful abstraction over effects via higher-kinded types.
- **fp-ts** ported that abstraction into TypeScript, proving both its power and its friction in a language not built for it.

Knowing which gap each fills is what lets you choose deliberately — and, in Java specifically, recognize how much of that gap you can now close with the language alone plus a focused set of types.

---

## Further reading

- [Do You Need a Functional Library or Just Better Habits?](/dmx-fun/blog/library-vs-habits) — when a library earns its place and when discipline is enough
- [JDK-First Functional Programming: How Far Can You Go Without Dependencies?](/dmx-fun/blog/jdk-first-functional-programming) — how much of the data-types-first toolkit the modern JDK gives you for free
- [Designing More Expressive APIs with Functional Types](/dmx-fun/blog/expressive-apis-with-functional-types) — putting Option, Result, and Validated to work in real signatures
- [Monads Without the Smoke and Mirrors](/dmx-fun/blog/monads-without-smoke-and-mirrors) — the abstraction that Cats and fp-ts build their hierarchy around, explained plainly
