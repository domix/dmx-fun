import type { NavOperations, ShortcutOptions } from '@slidev/types'
import { defineShortcutsSetup } from '@slidev/types'
import { toggleLang } from '../../theme/composables/lang'

// `L` switches the whole deck between English and Spanish, live. Everything else is Slidev's default
// (`D` dark/light, `O` overview, `F` fullscreen, arrows to navigate).
export default defineShortcutsSetup((nav: NavOperations, base: ShortcutOptions[]) => [
  ...base,
  { key: 'l', fn: () => toggleLang(), autoRepeat: false },
])
