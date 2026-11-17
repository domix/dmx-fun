# slidev-theme-dmx-fun

Local Slidev theme for dmx-fun talks. Used from a deck with `theme: ../theme`.

- `styles/tokens.css` — the palette, derived from the site. Two semantic accents only: `--dmx-ok` (success track) and `--dmx-err` (failure track). Dark is the default; `D` toggles light.
- `styles/base.css` — layout rules: one idea per slide, code at 24 px minimum, grid texture for cover and section slides, brand mark, footer. Sizes are in px for a 1920×1080 canvas, so decks set `canvasWidth: 1920` in their headmatter and 24 px means 24 px on a 1080p projector.
- `layouts/` — `cover`, `section`, `default`.
- `components/` — `T` (inline bilingual text, `en`/`es` props), `Track` (colors a word with its track), `Diagram` (inlines an SVG from `diagrams/` so it picks up the tokens), `DmxFooter` (language badge + slide number).
- `composables/lang.ts` — the deck language: `?lang=es` on open, `L` to toggle, remembered across reloads, `VITE_DECK_LANG` for exports.
- `diagrams/` — SVGs drawn with the two accents, identifiers only (no prose, so nothing escapes the i18n rule); the same files can illustrate the site later.

Fonts (Inter Variable, JetBrains Mono) come from fontsource packages, so nothing is fetched at show time.
