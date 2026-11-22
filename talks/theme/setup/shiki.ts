import { defineShikiSetup } from '@slidev/types'
import oneDarkPro from '@shikijs/themes/one-dark-pro'
import oneLight from '@shikijs/themes/one-light'

// Code palette with a job. The deck's two accents (blue = Ok track, amber = Err track) must keep
// their meaning next to code, so no token is red, blue or amber. Types are the heroes of a talk
// about signatures, so they are the brightest thing; keywords recede into a soft violet (the
// gradient's indigo family); strings stay green; everything else is plain text.
// Base: One Dark Pro / One Light, with the Java scopes overridden below.

const PLAIN = [
  'source.java', 'meta.function-call.java', 'meta.method-call.java', 'meta.definition.variable.java',
  'variable', 'variable.other', 'variable.other.object', 'variable.other.object.property',
  'variable.other.readwrite', 'variable.other.constant', 'variable.other.definition', 'variable.parameter',
  'variable.language',
  'entity.name.function', 'support.function', 'support.function.any-method', 'variable.function',
  'keyword.operator', 'keyword.operator.arithmetic', 'keyword.operator.assignment',
  'keyword.operator.comparison', 'keyword.operator.logical', 'keyword.operator.relational',
  'keyword.operator.ternary', 'keyword.operator.bitwise',
  'punctuation', 'constant.numeric',
]
const KEYWORDS = [
  'keyword', 'keyword.control', 'keyword.operator.new', 'keyword.operator.instanceof.java',
  'storage', 'storage.modifier', 'constant.language',
]
const TYPES = [
  'storage.type.java', 'storage.type.generic.java', 'storage.type.object.array.java',
  'entity.name.type', 'entity.name.type.class', 'support.class', 'support.type',
]
const ANNOTATIONS = ['storage.type.annotation.java', 'punctuation.definition.annotation.java']

function tuned(base: any, name: string, c: { fg: string; type: string; keyword: string; muted: string }) {
  return {
    ...base,
    name,
    fg: c.fg,
    colors: { ...base.colors, 'editor.foreground': c.fg },
    tokenColors: [
      ...base.tokenColors,
      { scope: PLAIN, settings: { foreground: c.fg } },
      { scope: KEYWORDS, settings: { foreground: c.keyword } },
      { scope: TYPES, settings: { foreground: c.type } },
      { scope: ANNOTATIONS, settings: { foreground: c.muted } },
    ],
  }
}

export default defineShikiSetup(() => ({
  themes: {
    dark: tuned(oneDarkPro, 'dmx-dark', { fg: '#c9d1d9', type: '#f0f6fc', keyword: '#a78bfa', muted: '#8b949e' }),
    light: tuned(oneLight, 'dmx-light', { fg: '#383a42', type: '#1f2328', keyword: '#6f42c1', muted: '#6e7781' }),
  },
}))
