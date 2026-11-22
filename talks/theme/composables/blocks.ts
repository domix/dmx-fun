// Talk blocks: declared once in the deck headmatter and read by the footer (progress strip) and the
// layouts (kicker above the title). Page ranges keep slides free of per-slide bookkeeping.
//
//   blocks:
//     - { en: Name the failures, es: Nombrar los fallos, from: 2, to: 6 }
//
// A slide may also set `kicker` in its frontmatter, as a string or as `{ en, es }`, to label itself.
import { computed } from 'vue'
import { useSlideContext } from '@slidev/client'
import { lang } from './lang'

export interface Block { en: string; es: string; from: number; to: number }

export function useBlocks() {
  const { $slidev, $page, $frontmatter } = useSlideContext()
  const blocks = computed<Block[]>(() => ((($slidev.configs as any).blocks) ?? []) as Block[])
  const index = computed(() => blocks.value.findIndex(b => $page.value >= b.from && $page.value <= b.to))
  const current = computed(() => (index.value >= 0 ? blocks.value[index.value] : null))
  // Pages after the last block (questions, backup) show every block as done.
  const finished = computed(() => blocks.value.length > 0 && $page.value > blocks.value[blocks.value.length - 1].to)
  const kicker = computed<string | null>(() => {
    const k = ($frontmatter as any)?.kicker
    if (!k) return null
    return typeof k === 'string' ? k : (k[lang.value] ?? k.en ?? null)
  })
  return { blocks, index, current, finished, kicker }
}
