# Visual and didactic review

Review of the rendered deck (PNG export, English, 25 slides) and the shared theme in `talks/theme`.
Working notes; items move to issues or disappear as they are resolved.

## Findings

### High impact

1. **The lower half of most slides is empty.** Code, table and caption slides (2, 4, 8, 9, 10, 13,
   14, 17, 19, 22) use the top 45–55% of the canvas. Code is set at 24px, which the theme defines
   as the floor, not the target; with that room it fits at 28–30px.
2. **The most important sentence is the least visible.** The caption carries the takeaway of each
   slide but is grey, 28px, almost the body size. Candidate: a fixed takeaway band at the bottom,
   larger, with an accent bar.
3. **The two deck colours do no work.** Blue = Ok track, amber = Err track is declared in the
   tokens and only appears in the railway diagram. Meanwhile the Shiki theme paints keywords red
   (`public`, `record`, `case`), which reads as "error" everywhere. Two moves: a calmer code theme
   (`github-dark-dimmed` or `vitesse-dark`), and deck-coloured marks where they matter (demo
   statuses 200/201 blue, 404/500 amber; `Result.ok` / `Result.err` in the switches).
4. **"Twenty lines, four lies" never shows the four.** The clicks walk them, the final state is
   uniform. Four numbered amber markers in the margin, persisting after the clicks. Mirror it on
   "One chain": a side-by-side 20 lines / 4 lies vs 10 lines / 0 is the before-and-after the deck
   does not show yet.
5. **The hook slide (2) is the weakest.** `User findUser(String id)` is one 24px line on an empty
   canvas. It should be the largest thing on the slide (64–72px, centred), the question below, the
   three answers appearing as three amber chips.

### Rhythm and orientation

6. **Fifteen slides share one structure** (title, block, caption); the room cannot tell which of
   the five blocks it is in. A small kicker above the title ("Chapter 1 · Name the failures") and a
   block progress indicator in the footer; section slides can say "Block 2 of 5" and what comes.
7. **Tables read like a document** (13, 14, 17, 19, 20, 22). "Start at one boundary" wants three
   large horizontal steps; "Integrations" groups into four families (JSON; Spring and Quarkus;
   observability; validation and tests) with module names as chips instead of eight rows.
8. **The railway diagram** takes a third of the height. Full width and height, `charge` dimmed to
   show it is skipped, `flatMap` on the connectors, two click states: happy path and failure.

### Minor

- Slide 21 repeats its title in the body ("Try it on one endpoint" twice).
- Slide 22 (questions) would benefit from revealing one question per click.
- Test the deck in light mode (D) on the real projector; the grid texture and the grey caption wash
  out on weak projectors.

## Speaker's observations

1. **Cover: show the library's logo/mascot.** The robot holding the green lambda, transparent
   background. It is the same artwork as `site/public/logo.png` (1536×1024, different bytes), so the
   deck can reuse the site's file. The green lambda is a third colour outside the deck's two accents;
   worth deciding how it sits next to the blue/indigo title gradient.
2. **Spanish wording: say "Railway", not "Dos vías".** The translation sounds odd; "Railway" is the
   term the Spanish-speaking room knows. Affects the section title (slide 7, `es="Dos vías"`), the
   diagram title (slide 9, `es="La vía del tren, en una slide"`), "De vuelta a las vías" (slide 12),
   and the captions in `i18n/es.json` that speak of "vía del éxito" / "vía del fallo"
   (`tracks.caption`, `bridge.caption`, `either.caption`); the speaker notes use the same wording.
   Decide the pair for the two tracks themselves (e.g. "carril Ok" / "carril Err", or keep "vía"
   only for the tracks and "Railway" for the pattern).
3. **Slide 9: a nicer railway graphic.** The current `theme/diagrams/two-tracks.svg` (three boxes,
   two straight lines, one curve) is functional but plain. Try a more attractive version: see
   finding 8 for the structural ideas (full canvas, dimmed skipped step, `flatMap` on the
   connectors, click states); the visual quality itself is the ask here.
4. **Opening: a joke about the library's name.** Background from the speaker: when building it they
   did not know what to call it; "fun" was the fun idea but too generic on its own, so they prefixed
   their nickname, "domix", and got dmx-fun. The joke hooks on Phil Karlton's line: "there are only
   two hard things in computer science: cache invalidation and naming things." Goes in the preamble
   or introduction, before the hook slide; today the deck deliberately leaves the project and speaker
   intro for the closing slide (speaker notes on slide 21), so this changes that choice for one beat
   of self-deprecation at the start. Fits naturally next to the mascot on the cover (observation 1).
5. **The three gaps the library was built to fill; the deck must communicate them.** The speaker's
   reasons for creating it, in their order:
   1. **Use the most modern features of the language**, visible in the codebase itself (sealed
      interfaces, records, pattern matching, JSpecify, JPMS).
   2. **Good documentation**: not only the API, but the guide, the new tutorial, and reference
      examples that live in the code (the sample modules).
   3. **An ecosystem**: the library used from the everyday tools, through the Spring, Jackson,
      observability and other integration modules.
   Today "Why this library" (slide 13) covers 1 and part of 3 as a five-row table and "Integrations"
   (slide 14) covers 3; documentation as a deliberate goal appears nowhere. Candidate: one slide with
   the three gaps as three columns, early (after the hook or as the "why" opener), with 13 and 14 as
   the supporting detail; also a natural place for the mascot and the naming joke to hand over to.

6. **A nicer closing.** The current closing is a section slide with two QR codes.

## Plan

Three pull requests, each with before/after PNGs (`slidev export --format png`).

### A. Theme (mechanical, applies to every slide and to future decks)

- Caption becomes a **takeaway band**: fixed at the bottom of content slides, 34px, accent bar on
  the left, text colour instead of grey (findings 1, 2).
- **Code at 28px** by default, 30px where the snippet allows; Shiki theme `github-dark-dimmed` so
  keywords stop shouting red and the deck's blue/amber can mean something (findings 1, 3).
- **Kicker** above the title (`Chapter 1 · Name the failures`) and a **block progress** strip in
  the footer, five blocks; section slides state the block number (finding 6).
- `<Marks>` component: numbered amber or blue badges in the code margin, persisting after clicks
  (finding 4); `.ok` / `.err` already exist for prose.

### B. Content and narrative

- **Cover with the mascot** on the right, about 420px, on the grid texture; the green lambda is a
  third colour, allowed on cover and closing only (observation 1).
- **New slide 2, "Naming things"**: Karlton's two hard things, "fun" too generic, the "domix"
  prefix, the mascot as the punchline. Thirty seconds, then the hook (observation 4).
- **Hook slide**: the signature at 64–72px centred, the question, three amber chips for the three
  answers (finding 5).
- **"Three gaps" replaces the "Why this library" table**: three columns (modern language; real
  documentation: guide, tutorial, sample modules; ecosystem: Spring, Jackson, observability), with
  the table's facts folded in as one line each. "Integrations" stays as the detail of the third gap,
  regrouped into four families with module chips (observation 5, finding 7).
- **Spanish wording**: the pattern is "Railway" (section title, slide 9 "El railway, en una slide",
  slide 12 "De vuelta al railway"); the two tracks stay "vía Ok" / "vía Err" so the metaphor holds
  (observation 2).
- **Slide 3 markers** for the four lies; **demo statuses coloured by track**; "Start at one
  boundary" as three horizontal steps (findings 4, 3, 7).
- **Closing as a bookend**: the signature from the hook, now truthful
  (`Result<User, UserError> findUser(UserId id)`), large, with the one-line ask under it, the two
  QR cards, the mascot small, speaker handle; the repeated sentence goes (observation 6).

### C. The railway diagram

- New `two-tracks.svg`: full canvas width, station-shaped steps, Ok rail above in blue, Err rail
  below in amber, the error record riding the lower rail, skipped steps dimmed, `flatMap` on the
  connectors and `recoverWith` as the curve back up, which the code on slide 8 already shows.
  Three click states: happy path, a failure switching down, the recovery switching back up
  (observation 3, finding 8).

Timing stays at 35 minutes: the naming slide and the three-gaps slide replace the "Why this
library" table and part of its narration.
