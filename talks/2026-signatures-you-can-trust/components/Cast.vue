<script setup lang="ts">
// Plays the recorded demo (demo.cast) inside the deck: the fallback when the live demo cannot run.
// Space plays and pauses, the arrow keys seek; the recording is asciicast v2 made by scripts/record-demo.sh.
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as AsciinemaPlayer from 'asciinema-player'
import 'asciinema-player/dist/bundle/asciinema-player.css'
import castUrl from '../demo.cast?url'

const host = ref<HTMLDivElement>()
let player: { dispose(): void } | undefined

onMounted(() => {
  if (!host.value) return
  player = AsciinemaPlayer.create(castUrl, host.value, {
    cols: 100,
    rows: 22,
    fit: 'both',
    poster: 'npt:0:04',
    theme: 'dmx',
    terminalFontFamily: "'JetBrains Mono', ui-monospace, monospace",
    terminalLineHeight: 1.4,
    idleTimeLimit: 3,
    controls: true,
  })
})
onBeforeUnmount(() => player?.dispose())
</script>

<template>
  <div ref="host" class="cast" />
</template>

<style>
.cast { height: 740px; }
/* Player theme from the design tokens: surface background, site text color, the two accents in the ANSI slots. */
.asciinema-player-theme-dmx {
  --term-color-foreground: var(--dmx-text);
  --term-color-background: var(--dmx-surface);
  --term-color-0: #30363d; --term-color-8: #484f58;
  --term-color-1: #ff7b72; --term-color-9: #ffa198;
  --term-color-2: #3fb950; --term-color-10: #56d364;
  --term-color-3: var(--dmx-err); --term-color-11: #e3b341;
  --term-color-4: var(--dmx-ok); --term-color-12: #79c0ff;
  --term-color-5: #bc8cff; --term-color-13: #d2a8ff;
  --term-color-6: #39c5cf; --term-color-14: #56d4dd;
  --term-color-7: #c9d1d9; --term-color-15: #f0f6fc;
}
.cast .ap-wrapper { height: 100%; border: 1px solid var(--dmx-border); border-radius: 14px; overflow: hidden; }
.cast .ap-player { height: 100%; }
</style>
