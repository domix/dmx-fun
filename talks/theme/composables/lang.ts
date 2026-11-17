// Deck language, chosen when presenting: `?lang=es` on open, `L` to toggle live, remembered across reloads.
// VITE_DECK_LANG sets the default at build/export time (one PDF per language).
import { ref } from 'vue'

export type Lang = 'en' | 'es'

const STORAGE_KEY = 'dmx-fun-talk-lang'

function pick(...candidates: Array<string | null | undefined>): Lang {
  for (const c of candidates) {
    if (c === 'en' || c === 'es') return c
  }
  return 'en'
}

const fromUrl = typeof location !== 'undefined' ? new URLSearchParams(location.search).get('lang') : null
const fromStorage = typeof localStorage !== 'undefined' ? safeGet(STORAGE_KEY) : null
const fromEnv = (import.meta as any).env?.VITE_DECK_LANG as string | undefined

export const lang = ref<Lang>(pick(fromUrl, fromEnv, fromStorage))

export function setLang(next: Lang) {
  lang.value = next
  try { localStorage.setItem(STORAGE_KEY, next) } catch { /* private mode, previews */ }
}

export function toggleLang() {
  setLang(lang.value === 'en' ? 'es' : 'en')
}

function safeGet(key: string): string | null {
  try { return localStorage.getItem(key) } catch { return null }
}
