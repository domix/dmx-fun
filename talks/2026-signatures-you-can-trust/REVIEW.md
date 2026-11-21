# Deck review, second round

Working notes after #600 landed (theme pass, content pass, railway diagram). The speaker dictates
observations first; the proposal comes once they say they are done.

## Speaker's observations

1. **Open with the library's context, right after the naming slide.** The "Three gaps" slide
   (now slide 14, opening block 4 "Why this library") would move up to the preamble: cover,
   naming, three gaps, then the hook. Block 4 would then be only "Integrations"; the block
   ranges in the headmatter and the timing marks in the speaker notes need to follow.
2. **The documentation gap must mention the blog.** Today the column reads "A guide, a
   ten-chapter tutorial and sample modules in the repository"; add the blog (the site's posts,
   also referenced on the Q&A slide and in "Four ways to contribute").
3. **Slide 2 (naming): larger text on the right-hand side.** The verdicts next to `fun`,
   `domix` and `dmx-fun` are 30px (`.naming .verdict`); the words on the left are 56px. Raise
   the verdicts a step (34–36px) so the two columns read as one sentence.
4. **Slide 3 (hook): the closing line "Three answers, none of them in the signature" looks
   small and too low.** It is the takeaway band (34px, pinned above the footer). On this slide
   it is the punchline of the reveal, not a footnote: larger (44–48px) and higher, closer to the
   chips, as the last click. Either a per-slide variant of the band or a different element
   (`.question`-sized line in amber) under the chips.
5. **Slide 6 ("Return instead of throw"): say why not exceptions.** The slide shows the
   signature returning `Result` and the caption "Each failure is now a value the caller
   receives, not an event the caller has to catch", but never argues against exceptions.
   Material already in the deck: the Q&A answer on checked exceptions (not values; no map, no
   flatMap, no lambdas, no composition; every layer re-declares or wraps them) and the blog post
   "why just use exceptions persists". Candidate: a short "exceptions vs values" contrast on
   this slide or a new one right after it, kept to three or four lines, with the Q&A slide
   remaining the long version.
6. **Slide 10 (railway): liked; animate every step.** Today three click states (happy path,
   one failure, one recovery). Wanted: the stations appearing one by one along the Ok rail
   (findStudent, findCourse, ensureNotEnrolled, charge, save), so the chain is built in front
   of the room before the failure and the recovery scenes. Roughly 5 + 2 clicks; the `clicks`
   frontmatter and the `.stage-N` CSS in the SVG grow accordingly, and the speaker notes list
   the clicks.
7. **Slide 18 (demo table): show a fragment of the controller.** The slide lists the four
   requests and their statuses but never shows the code that produces them. Source:
   `samples/spring-boot-sample/src/main/java/dmx/fun/sample/item/ItemController.java`, which
   has `findById` returning `Option<Item>`, `create` returning `Result<Item, String>` and
   `search` returning `Result<NonEmptyList<Item>, String>` (lines 53–73). Needs `// #region`
   markers in that file and the path added to `snippets.json`, like the tutorial snippets; the
   table could sit next to or under the snippet, or the snippet could become its own slide
   before the table.
8. **Slide 21 ("Four ways to contribute"): say more emphatically that it is open source.**
   The slide lists four asks but assumes the room knows the project is open; state it outright
   (Apache 2.0, public repository, public ADRs, issues open to anyone) as the lead-in to the
   four ways, in the title or a first line.

9. **Consider removing slides 24–26 (the backup section and chapters 6 and 7) and think of a
   better closing.**

## Proposal

### On the backup slides and the ending

Remove 24–26. They exist for a question that may never come, chapters 6 and 7 are one QR away
in the tutorial, and anyone reading the PDF ends on "At least one" instead of the closing. Keep
"Three questions that always come" as the only appendix after the closing: it is shown only when
asked, and its three answers are the long version of the exceptions argument (observation 5).

A two-beat ending instead of one slide:

1. **New recap slide, "One method, five rewrites"**: the `enroll` signature as it evolved
   through the talk, one line per click, each with what it gained:
   `Enrollment enroll(String, String)` (four lies) → `Result<Enrollment, EnrollmentError>`
   (the truth) → the chain (railway) → `enroll(EnrollmentForm)` (every error at once) →
   `Result<Either<Waitlisted, Enrolled>, EnrollmentError>` (two outcomes, no villain). This is
   the title of the talk made visible, and it replaces what the backup slides were for.
2. **The closing** stays as the bookend (signature, ask, QRs, mascot, handle), now with the
   open-source line from observation 8 moved here or shared with "Four ways to contribute".

### Order after the changes

Cover · Naming · Three gaps (with the blog) · Hook · ch0 · errors · return instead of throw
(+ why not exceptions) · switch · [Railway] section · chain · railway (all steps animated) ·
[Validation] byHand · guards · bridge · [Out the door] integrations · status · metrics · demo
(+ controller snippet) · cast · start at one boundary · contribute (open source first) · recap ·
closing · Q&A appendix. Four blocks instead of five: "Why this library" dissolves into the
preamble and "Integrations" joins "Out the door".

### Other improvements seen in this round

- **Slide 6 as a before/after pair** carries observation 5 better than prose: the caller of the
  throwing version (`try` / `catch` of exceptions nobody listed) on the left, the caller of the
  `Result` version (`switch`, exhaustive) on the right; the "why not exceptions" argument is the
  caption: exceptions are not values, they do not compose, every layer re-declares or wraps them.
- **Slide 17 (metrics)** has a six-line snippet and an empty slide: add a small inline bar
  chart (theme diagram, identifiers only) with one bar per outcome in the track colours, so
  "failures with names count themselves" is seen, not read.
- **Q&A appendix**: reveal one question per click.
- **Timing marks** in the speaker notes shift with the reorder; the three-gaps minute moves from
  [23–27] to the first two minutes, and "Why this library" loses its own block.

### Delivery

One issue with this plan; two pull requests with before/after PNGs: D, structure and content
(everything above except the diagram); E, the railway animated step by step.
