<script setup lang="ts">
// A QR code from qr/<name>.svg, inlined on a white card so it scans in dark and light mode alike.
// Regenerate the SVGs with `pnpm qr` when a URL changes (scripts/make-qr.mjs).
import { computed } from 'vue'

const codes = import.meta.glob('../qr/*.svg', { query: '?raw', import: 'default', eager: true }) as Record<string, string>
const props = defineProps<{ name: string; url: string }>()
const svg = computed(() => codes[`../qr/${props.name}.svg`] ?? `<p>missing QR: ${props.name}</p>`)
</script>

<template>
  <figure class="qr">
    <div class="card" v-html="svg" />
    <figcaption>
      <slot />
      <span class="url">{{ url }}</span>
    </figcaption>
  </figure>
</template>

<style scoped>
.qr { display: flex; flex-direction: column; align-items: center; gap: 24px; margin: 0; }
.card { background: #fff; border-radius: 16px; padding: 20px; width: 360px; height: 360px; }
.card :deep(svg) { display: block; width: 100%; height: 100%; }
figcaption { text-align: center; font-size: 28px; line-height: 1.4; }
.url { display: block; color: var(--dmx-text-2); font-family: 'JetBrains Mono', monospace; font-size: 22px; margin-top: 8px; }
</style>
