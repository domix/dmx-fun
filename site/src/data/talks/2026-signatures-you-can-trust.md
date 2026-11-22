---
title: "From exceptions to signatures you can trust"
description: "A 45-minute session for Java user groups: one method, rewritten until its signature stops lying about failure. Result, sealed errors and an exhaustive switch; the two tracks; all the errors at once; a Spring Boot controller that returns the Result."
duration: 45
languages: [en, es]
status: upcoming
deck: talks/2026-signatures-you-can-trust/
pdf:
  en: talks/2026-signatures-you-can-trust/deck-en.pdf
  es: talks/2026-signatures-you-can-trust/deck-es.pdf
source: https://github.com/domix/dmx-fun/tree/main/talks/2026-signatures-you-can-trust
related:
  - label: "The tutorial, same example, ten chapters"
    href: guide/tutorial
  - label: "Good first issues"
    href: https://github.com/domix/dmx-fun/labels/good%20first%20issue
---

The talk is the trailer; the [tutorial](../guide/tutorial) is the film. Both follow the same
example, enrolling a student in a course with limited seats, and the talk stops at chapter 5
and points at the rest.

| Minutes | Block |
|---|---|
| 0–4 | The signature that lies: `User findUser(String id)`, and what happens when it fails |
| 4–10 | Return instead of throw: a sealed error type and a `switch` the compiler checks |
| 10–17 | Two tracks: five steps chained with `flatMap`, the railway on one slide |
| 17–23 | All the errors at once: `Validated`, then rules as `Guard` values |
| 23–27 | Why this library: Java 25 baseline, peers not dependencies, tested matrices, public ADRs |
| 27–32 | Demo: a Spring Boot controller returns the `Result`; the client gets the right status with no mapping code |
| 32–35 | Start at one boundary, and four concrete ways to contribute |
| 35–45 | Questions |

One deck serves both languages: open it with `?lang=es` for Spanish, press `L` to switch at any
time. Every code snippet on the slides is imported from compiled, tested sources in the repository.
