---
title: "Golden/Approval Tests for Functional Pipelines"
description: "When a pipeline's output is a large structured value, assert-by-assert testing collapses into either spot checks that miss regressions or assertion walls nobody maintains. Golden tests flip the deal: capture the whole output once, approve it, and let every future run diff against the approved version. Functional pipelines are the best possible customer for this technique — here is why, and how to do it honestly."
pubDate: 2026-07-26
author: "domix"
authorImage: "https://gravatar.com/avatar/797a8fc41feef42d4bc41aff8cecb986d6f3fbbc157e49a65b2d5a5b6cd42640?s=200"
category: "Article"
tags: ["Testing", "Golden Tests", "Approval Tests", "Functional Programming", "Java", "Pipelines", "Quality"]
image: "https://images.pexels.com/photos/6358840/pexels-photo-6358840.jpeg?auto=compress&cs=tinysrgb&w=1200"
imageCredit:
    author: "Anna Tarazevich"
    authorUrl: "https://www.pexels.com/@anntarazevich/"
    source: "Pexels"
    sourceUrl: "https://www.pexels.com/photo/a-person-s-hand-using-a-stamp-6358840/"
---

There is a kind of code that ordinary example-based tests handle badly: the multi-stage
pipeline whose output is *big*. A settlement report built from a hundred trades. The JSON body
of a non-trivial API response. The classified, enriched, grouped result of an import job. You
can assert three fields and hope the other forty stayed correct, or you can write forty
assertions and watch nobody update them honestly after the first refactor. Both options are
bad, and teams feel it: coverage that looks green while output quietly drifts.

To be fair about scope: for pipelines with small outputs, the strategy from
[Testing in Functional Programming](/dmx-fun/blog/testing-in-functional-programming) — test each
stage in isolation, spot-check the composition — holds up fine. The trouble starts when the
*composed output itself* is the deliverable: large, structured, and full of details no
per-stage test ever looks at.

**Golden tests** — also called approval tests, snapshot tests, or characterization tests —
take a different deal. Run the pipeline, capture the *entire* output as text, and have a human
approve it once — the stamp in the photo above. From then on, the test is a diff: if today's
output matches the approved golden file, pass; if it differs, fail and *show the diff*. The
assertion is not "field X equals 3"; it is "the output is still exactly what we approved."

This post — part of the testing-and-quality series of this blog — is about why functional
pipelines and golden tests fit each other unusually well, and about the discipline that keeps
the technique honest.

---

## Why functional pipelines are the ideal customer

Golden testing has two hard prerequisites, and they are exactly the properties a functional
pipeline already has.

**Determinism.** A golden test is only as good as the promise that the same input produces the
same output. A [pure pipeline](/dmx-fun/blog/pure-functions-and-side-effects) — data in, data
out, effects at the edges — supplies the hard half of that promise: no hidden clock, no random
IDs, no mutable cache leaking state into the report. Purity is not the whole of it, though —
stable input fixtures and a deterministic representation (collection ordering, number
formatting) are still yours to enforce, which is what the render step below is for. When a
pipeline is *not* pure, the golden test tells you immediately: it flakes, and every flake
points at a nondeterminism you probably wanted to know about anyway.

**Output as a value.** The technique needs the whole result reified as one comparable thing.
Imperative code that writes rows, logs, and mutations as it goes has no such value to capture.
A functional pipeline ends in exactly one: the [typed outcome](/dmx-fun/blog/designing-a-good-error-type)
it returns. And crucially, in a `Result`/`Validated` world the *failure side is part of the
value too* — a golden file can capture "these three rows failed validation with these
messages" just as faithfully as the success report. Error behavior gets regression-tested
without writing a single explicit error assertion.

Records help with the mechanics — their generated `toString` renders a tree of simple record
components consistently enough for a quick first golden — but treat it as a prototype
serializer, not a contract. The exact format is a JDK implementation detail the JLS does not
specify, and each component renders via its own `toString`: an array field prints an identity
hash that changes every run, and an unordered collection prints in whatever order it likes.
The render step below is where a real golden earns its stability.

---

## The mechanics, in plain JUnit

Libraries exist (see the end of this section), but the pattern is small enough to own:

```java
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class Golden {
    private Golden() {}

    /** Compares {@code actual} against the golden file; re-stamps it in approve mode. */
    static void verify(String actual, Path golden) throws IOException {
        if (Boolean.getBoolean("approve")) {
            Path parent = golden.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(golden, actual);
            return;
        }
        if (Files.notExists(golden)) {
            fail("no approved golden at " + golden + " — run in approve mode and review it");
        }
        assertEquals(Files.readString(golden), actual,
            "output drifted from approved golden: " + golden);
    }
}
```

One build-tool wrinkle before using it: with Gradle, a `-D` flag on the command line sets the
property on the *Gradle* JVM, not on the forked JVM that runs your tests. Forward it once in
the build script and the switch works from the CLI:

```groovy
test {
    systemProperty 'approve', System.getProperty('approve')   // ./gradlew test -Dapprove=true
}
```

(Maven's Surefire forwards command-line `-D` properties to the test JVM by default.)

And a pipeline test becomes three lines that exercise the *whole* flow:

```java
@Test
void settlementReport_matchesApprovedGolden() throws IOException {
    var trades = TestFixtures.tradesFromCsv("fixtures/trades-2026-07.csv");

    Result<SettlementReport, ReportError> outcome = SettlementPipeline.run(trades);

    Golden.verify(render(outcome), Path.of("src/test/resources/golden/settlement-2026-07.txt"));
}
```

The first run fails with a pointed message (no golden yet); you re-run in approve mode,
*read the file*, commit it.
Every run after that is a full-output regression test. When a legitimate change alters the
output, the failure is the review artifact — though how readable it is depends on the runner:
IDEs render `assertEquals` string mismatches as a proper expected/actual diff, while a bare
CLI report prints the two strings, so on CI you may prefer a diff-reporting tool. Either way,
re-approving updates the golden, and the commit carries both the code change and the golden
change — reviewable side by side.

If you would rather not own even that much, [ApprovalTests.Java](https://github.com/approvals/ApprovalTests.Java)
packages the same idea with reporters that open your diff tool on failure, and most JVM
snapshot libraries follow the same approve-then-diff cycle. The pattern matters more than the
tool.

---

## The render step is where the engineering lives

Notice the `render(outcome)` call — it is not an afterthought. The golden file is a *report
for humans*, and its format decides whether the technique helps or hurts:

- **Render deterministically.** Sort map entries and any collection whose order is
  incidental; format numbers with a fixed locale; render a `Result` as an explicit
  `OK: ...` / `ERR: ...` line so both channels are visible. Nondeterminism belongs in the
  render step's contract, not in the diff noise.
- **Exclude what you do not promise.** Timestamps, host names, version strings — if it is not
  part of the pipeline's observable contract, it does not belong in the golden. Either inject
  it as fixed input (a `Clock` parameter, like any other
  [effect pushed to the edge](/dmx-fun/blog/pure-functions-and-side-effects)) or normalize it
  away when rendering.
- **Prefer line-oriented text.** Diffs are the failure UX. One fact per line diffs beautifully;
  a single 4,000-character JSON line does not. Pretty-print with stable key order, or render a
  purpose-built plain-text report.

A golden file that follows those rules reads like a report — line-oriented, both channels
visible, nothing incidental:

```text
SettlementReport 2026-07 (fixtures/trades-2026-07.csv)
OK: 97 trades settled
ERR trade-0042: counterparty account frozen
ERR trade-0057: currency mismatch (EUR vs USD)
ERR trade-0090: notional exceeds limit
total settled: 14250304.00 EUR
```

This is the same lesson as [validation at the boundary](/dmx-fun/blog/validation-at-the-boundary-not-in-the-core)
wearing a test harness: the core stays pure and typed; the edge — here, the renderer — owns
formatting, ordering, and everything audience-facing.

---

## The honest failure modes

Golden tests have a bad reputation in some circles, and the criticisms are real — they are
just aimed at undisciplined use.

**Rubber-stamping.** The failure mode is cultural, not technical: output drifts, the test
fails, someone re-approves without reading, and the golden file becomes a mirror of whatever
the code currently does — a tautology with excellent coverage numbers. The countermeasures are
process: golden-file changes are *review objects* (a PR that touches a golden without
explaining why should not merge), and goldens are kept small enough to actually read.

**Brittleness by over-capture.** If the golden contains incidental detail — ordering you never
promised, floating-point noise, that timestamp — every unrelated change breaks it and trains
the team to ignore failures. The fix is the render step above: capture the contract, not the
accident. A golden test that fails only for reasons someone should look at is one people keep
looking at.

**The oracle problem.** Approving output verifies that the output *looks right to a human
today* — it does not prove correctness against a specification. Golden tests are
characterization, not proof. Use them to pin the behavior of a whole pipeline cheaply, and
keep targeted example tests for the business rules where a wrong answer must fail loudly —
`assertThat(confirm(cancelledOrder)).isErr()`, with the
[fun-assertj](/dmx-fun/guide/assertj) assertions — plus property-based tests where laws exist.
The three layers answer different questions; the golden layer's question is "did anything
change that nobody meant to change?"

---

## Where this lands

The sweet spot is precise: **a deterministic pipeline with a large, structured, human-readable
output, where regressions are more likely than spec changes.** Report generation, data
transformations, serialization layers, migration outputs, error-message catalogs — pin them
with a golden, and refactor the pipeline's internals with the confidence that any observable
drift will surface as a readable diff.

Functional style is what makes the space large. Purity buys the determinism, values buy the
capturability, typed errors put failures inside the captured value, and the
[functional core / imperative shell](/dmx-fun/blog/should-all-business-logic-be-pure) split
means the golden test exercises the entire core without a mock in sight. The
[dmx-fun](/dmx-fun/) types — [`Result`](/dmx-fun/guide/result),
[`Validated`](/dmx-fun/guide/validated), [`Try`](/dmx-fun/guide/try) — are the value side of that bargain; the
[Developer Guide](/dmx-fun/guide/) covers them in depth.

---

## Further reading

- [Testing in Functional Programming: Why It Is Often Simpler](/dmx-fun/blog/testing-in-functional-programming)
  — the broader case: pure functions need less test machinery.
- [Pure Functions and Side Effects](/dmx-fun/blog/pure-functions-and-side-effects) — where the
  determinism that golden tests depend on comes from.
- [Modeling Data Transformation Pipelines](/dmx-fun/blog/modeling-data-transformation-pipelines)
  — the pipeline shape this post pins with goldens.
- [Designing a Good Error Type](/dmx-fun/blog/designing-a-good-error-type) — typed failures as
  part of the capturable output.
- [How to Write More Predictable Code with Functional Programming](/dmx-fun/blog/predictable-code-with-fp)
  — predictability as a testability property.
- [ApprovalTests.Java](https://github.com/approvals/ApprovalTests.Java) — the approve-then-diff
  cycle as a ready-made library, with diff-tool reporters.

---

*Found a bug or have a suggestion? Open an issue on [GitHub](https://github.com/domix/dmx-fun).*
