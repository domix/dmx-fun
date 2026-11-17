<script setup lang="ts">
import { lang, toggleLang } from '../composables/lang'
</script>

<template>
  <div class="dmx-footer">
    <button class="lang" :title="lang === 'en' ? 'Switch to Spanish (L)' : 'Cambiar a inglés (L)'" @click="toggleLang">
      {{ lang.toUpperCase() }}
    </button>
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
</style>
