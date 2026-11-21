<script setup lang="ts">
// Bottom strip: language badge, block progress (from the deck's `blocks` headmatter), slide number.
import { lang, toggleLang } from '../composables/lang'
import { useBlocks } from '../composables/blocks'

const { blocks, index, finished } = useBlocks()
</script>

<template>
  <div class="dmx-footer">
    <button class="lang" :title="lang === 'en' ? 'Switch to Spanish (L)' : 'Cambiar a inglés (L)'" @click="toggleLang">
      {{ lang.toUpperCase() }}
    </button>
    <div v-if="blocks.length" class="progress" aria-hidden="true">
      <span v-for="(b, i) in blocks" :key="i" class="seg" :class="{ done: finished || i < index, now: i === index }" />
    </div>
    <span class="page">{{ $slidev.nav.currentPage }} / {{ $slidev.nav.total }}</span>
  </div>
</template>

<style scoped>
.lang {
  background: none;
  border: 1px solid var(--dmx-border);
  border-radius: 6px;
  color: var(--dmx-text-2);
  cursor: pointer;
  font: inherit;
  padding: 0.1rem 0.5rem;
}
.progress { display: flex; gap: 8px; }
.seg {
  width: 56px;
  height: 5px;
  border-radius: 3px;
  background: var(--dmx-border);
}
.seg.done { background: var(--dmx-ok); opacity: 0.55; }
.seg.now { background: var(--dmx-ok); }
</style>
