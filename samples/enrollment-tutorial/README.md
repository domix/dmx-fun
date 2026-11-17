# enrollment-tutorial

Companion code for the didactic guide on the site (`guide/tutorial`). The running example is
enrolling a student in a course with limited seats. Each chapter lives in its own package and
rewrites the same `enroll` operation with one more dmx-fun type; the shared domain (`Student`,
`Course`, `Enrollment`, `Catalog`, `PaymentGateway`) stays in `domain` and never changes.

| Package     | Chapter                     | Introduces                              |
|-------------|-----------------------------|-----------------------------------------|
| `chapter00` | The honest signature        | the problem: exceptions, `null`, wrapped causes |
| `chapter01` | Return instead of throw     | `Result<T, E>`, a sealed error type, exhaustive `switch` |
| `chapter02` | Two tracks                  | `map`, `flatMap`, `recoverWith`: one pipeline, first failure wins |
| `chapter03` | Absence is not failure      | `Option<T>` for the discount code and the catalog lookups |
| `chapter04` | Capture at the edge         | `Try.run(...).rethrowFatal().toResult(...)` replaces the last try/catch |
| `chapter05` | All the errors at once      | `Validated` by hand, then `Guard` rules composed with `allOf`; `InvalidForm` bridges into `Result` |

The site snippets under `site/src/data/code/guide/tutorial/` are copied from these sources.
When you change a chapter, update its snippets in the same commit.

```bash
./gradlew :samples:enrollment-tutorial:test
```

This module is not published to Maven Central.
