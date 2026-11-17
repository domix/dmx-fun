<script setup lang="ts">
// Inlines an SVG from theme/diagrams so it inherits the design tokens (an <img> would not):
// <Diagram name="two-tracks" />
import { computed } from 'vue'

const diagrams = import.meta.glob('../diagrams/*.svg', { query: '?raw', import: 'default', eager: true }) as Record<string, string>
const props = defineProps<{ name: string }>()
const svg = computed(() => diagrams[`../diagrams/${props.name}.svg`] ?? `<p>missing diagram: ${props.name}</p>`)
</script>

<template>
  <div class="dmx-diagram" v-html="svg" />
</template>
