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
.cover-layout .dmx-brand { position: absolute; top: 2.5rem; left: 3.5rem; font-size: 1.1rem; }
.cover-body :deep(h1) { font-size: 3.4rem; line-height: 1.1; }
.cover-body :deep(p)  { font-size: 1.4rem; color: var(--dmx-text-2); }
</style>
