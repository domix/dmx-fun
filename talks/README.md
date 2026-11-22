# Talks

Decks for conference and user-group talks, versioned with the library so their code snippets come
from `samples/` and cannot drift. Built with [Slidev](https://sli.dev); one directory per talk plus a
shared theme.

| Directory | Talk |
|---|---|
| `theme/` | `slidev-theme-dmx-fun`: palette, fonts, layouts, bilingual `<T>` component |
| `2026-signatures-you-can-trust/` | From exceptions to signatures you can trust (45 min, EN/ES) |

```bash
cd talks && pnpm install && pnpm dev
```

## Publishing

The site publishes every deck: `pages.yml` runs `pnpm build && pnpm export` here, copies each
`<slug>/dist/` and its two PDFs into `site/public/talks/<slug>/`, and the
[Talks page](https://domix.github.io/dmx-fun/talks) links to them. Locally, `make talks` inside `site/`
does the same copy. To add a talk:

1. Create `talks/<year>-<slug>/` from the existing deck; keep `slidev build --base /dmx-fun/talks/<year>-<slug>/`
   in its `package.json` so the published paths match.
2. Add `site/src/data/talks/<year>-<slug>.md` with the title, abstract, duration, languages, `deck`, `pdf`
   and `source` fields; the Talks page and the landing page read that collection.
3. Add the directory to the table above.
