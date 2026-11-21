# slidev-theme-dmx-fun

Local Slidev theme for dmx-fun talks. Used from a deck with `theme: ../theme`.

- `styles/tokens.css` — the palette, derived from the site. Two semantic accents only: `--dmx-ok` (success track) and `--dmx-err` (failure track). Dark is the default; `D` toggles light.
- `styles/base.css` — layout rules: one idea per slide, code at 28 px (24 px on a slide with `class: dense`), the takeaway band (`.caption`, pinned above the footer with the success-track bar; `.caption.err` for the failure track), grid texture for cover and section slides, brand mark, footer. Sizes are in px for a 1920×1080 canvas, so decks set `canvasWidth: 1920` in their headmatter and 28 px means 28 px on a 1080p projector.
- `setup/shiki.ts` — the code palette: One Dark Pro / One Light with the Java scopes retuned so no token is red, blue or amber (types bright, keywords soft violet, strings green, the rest plain); the deck's two accents keep their meaning next to code.
- `layouts/` — `cover`, `section`, `default`. `default` and `section` show a kicker above the title: the slide's `kicker` frontmatter (`string` or `{ en, es }`), or the current block's name.
- `assets/mascot.png` — the library's mascot, the same artwork as `site/public/logo.png`; shown on the cover (`mascot: false` hides it) and through `<Mascot :size>` elsewhere, cover and closing only.
- `components/` — `T` (inline bilingual text, `en`/`es` props), `Mascot`, `Track` (colors a word with its track), `Diagram` (inlines an SVG from `diagrams/` so it picks up the tokens), `Kicker`, `Marks` (numbered badges in the code margin: `<Marks :at="[4, 11]" dense>` around a snippet; `ok` for blue), `DmxFooter` (language badge, block progress, slide number).
- `composables/lang.ts` — the deck language: `?lang=es` on open, `L` to toggle, remembered across reloads, `VITE_DECK_LANG` for exports.
- `composables/blocks.ts` — the talk's blocks, declared once in the deck headmatter as page ranges (`blocks: [{ en, es, from, to }]`); the footer draws one segment per block and the kicker names the current one.
- `diagrams/` — SVGs drawn with the two accents, identifiers only (no prose, so nothing escapes the i18n rule); the same files can illustrate the site later.

Fonts (Inter Variable, JetBrains Mono) come from fontsource packages, so nothing is fetched at show time.
