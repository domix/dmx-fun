<script setup lang="ts">
// Cover: brand top-left, title with the section gradient, grid texture.
</script>

<template>
  <div class="slidev-layout dmx-grid-bg cover-layout">
    <div class="dmx-brand">
      <span class="mark">λ</span><span>dmx-fun</span>
    </div>
    <div class="cover-body">
      <slot />
    </div>
    <DmxFooter />
  </div>
</template>

<style scoped>
.cover-layout { display: flex; flex-direction: column; justify-content: center; gap: 2rem; }
.cover-layout .dmx-brand { position: absolute; top: 80px; left: 112px; font-size: 32px; }
.cover-body :deep(h1) { font-size: 100px; line-height: 1.08; margin-bottom: 40px; }
.cover-body :deep(p)  { font-size: 40px; line-height: 1.35; color: var(--dmx-text-2); max-width: 1500px; }
</style>
