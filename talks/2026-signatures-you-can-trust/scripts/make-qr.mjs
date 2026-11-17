#!/usr/bin/env node
// Writes the QR codes shown on the closing slide into qr/, one SVG per link. Run after changing a URL.
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import QRCode from 'qrcode'

const links = {
  tutorial: 'https://domix.github.io/dmx-fun/guide/tutorial',
  repo: 'https://github.com/domix/dmx-fun',
}

const out = resolve(dirname(fileURLToPath(import.meta.url)), '..', 'qr')
mkdirSync(out, { recursive: true })
for (const [name, url] of Object.entries(links)) {
  const svg = await QRCode.toString(url, { type: 'svg', errorCorrectionLevel: 'M', margin: 0 })
  writeFileSync(resolve(out, `${name}.svg`), svg)
  console.log(`qr/${name}.svg <- ${url}`)
}
