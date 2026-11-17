#!/usr/bin/env node
// Copies the source files listed in snippets.json from the repository into ./snippets/, preserving
// their paths. The copy is git-ignored and regenerated on every dev/build/export, so the deck can
// only show code that exists, compiles and is tested in the repository.
import { copyFileSync, existsSync, mkdirSync, readFileSync, rmSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const deckRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const repoRoot = resolve(deckRoot, '..', '..')
const manifest = JSON.parse(readFileSync(resolve(deckRoot, 'snippets.json'), 'utf8'))
const target = resolve(deckRoot, 'snippets')

rmSync(target, { recursive: true, force: true })
const missing = []
for (const rel of manifest.files) {
  const src = resolve(repoRoot, rel)
  if (!existsSync(src)) { missing.push(rel); continue }
  const dst = resolve(target, rel)
  mkdirSync(dirname(dst), { recursive: true })
  copyFileSync(src, dst)
}
if (missing.length) {
  console.error(`sync-snippets: ${missing.length} source file(s) not found:\n  ` + missing.join('\n  '))
  process.exit(1)
}
console.log(`sync-snippets: ${manifest.files.length} file(s) copied into snippets/`)
