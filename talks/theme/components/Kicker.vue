<script setup lang="ts">
// Small line above the title: the slide's own `kicker` frontmatter, or the current block's name.
import { computed } from 'vue'
import { useBlocks } from '../composables/blocks'

const { kicker, current, index, blocks } = useBlocks()
const text = computed(() => kicker.value ?? current.value?.[langKey()] ?? null)
const position = computed(() => (index.value >= 0 && blocks.value.length ? `${index.value + 1} / ${blocks.value.length}` : null))

import { lang } from '../composables/lang'
function langKey(): 'en' | 'es' { return lang.value }
</script>

<template>
  <div v-if="text" class="kicker">
    <span v-if="position" class="pos">{{ position }}</span>
    <span>{{ text }}</span>
  </div>
</template>

<style scoped>
.kicker {
  display: flex;
  align-items: baseline;
  gap: 16px;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--dmx-text-2);
  margin-bottom: 14px;
}
.kicker .pos {
  color: var(--dmx-ok);
  font-variant-numeric: tabular-nums;
}
</style>
