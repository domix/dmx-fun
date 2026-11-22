#!/usr/bin/env node
// Fails the build when a sentence exists in only one language.
//  1. every <T .../> in slides.md has both en="…" and es="…", non-empty
//  2. i18n/en.json and i18n/es.json have exactly the same keys, all non-empty
//  3. every <Tk k="…"/> in slides.md refers to an existing key
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const slides = readFileSync(resolve(root, 'slides.md'), 'utf8')
const en = JSON.parse(readFileSync(resolve(root, 'i18n/en.json'), 'utf8'))
const es = JSON.parse(readFileSync(resolve(root, 'i18n/es.json'), 'utf8'))
const problems = []

// 1. inline <T> tags
for (const m of slides.matchAll(/<T\b([^>]*?)\/?>/g)) {
  const attrs = m[1]
  const has = (name) => new RegExp(`\\b${name}="[^"]+"`).test(attrs)
  if (!has('en') || !has('es')) {
    const line = slides.slice(0, m.index).split('\n').length
    problems.push(`slides.md:${line}: <T> needs both en and es: <T${attrs}>`)
  }
}

// 2. dictionaries
const enKeys = new Set(Object.keys(en)), esKeys = new Set(Object.keys(es))
for (const k of enKeys) if (!esKeys.has(k)) problems.push(`i18n/es.json: missing key "${k}"`)
for (const k of esKeys) if (!enKeys.has(k)) problems.push(`i18n/en.json: missing key "${k}"`)
for (const [file, dict] of [['en', en], ['es', es]])
  for (const [k, v] of Object.entries(dict))
    if (typeof v !== 'string' || !v.trim()) problems.push(`i18n/${file}.json: empty value for "${k}"`)

// 3. <Tk k="…"/> references
for (const m of slides.matchAll(/<Tk\b[^>]*\bk="([^"]+)"/g)) {
  if (!enKeys.has(m[1]) || !esKeys.has(m[1])) {
    const line = slides.slice(0, m.index).split('\n').length
    problems.push(`slides.md:${line}: <Tk k="${m[1]}"> has no entry in both dictionaries`)
  }
}

if (problems.length) {
  console.error(`i18n check failed (${problems.length}):\n  ` + problems.join('\n  '))
  process.exit(1)
}
const tCount = [...slides.matchAll(/<T\b/g)].length, tkCount = [...slides.matchAll(/<Tk\b/g)].length
console.log(`i18n check ok: ${tCount} inline <T>, ${tkCount} <Tk>, ${enKeys.size} dictionary keys in en and es.`)
