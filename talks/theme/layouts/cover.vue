<script setup lang="ts">
// Cover: brand top-left, title with the section gradient, grid texture, the mascot on the right
// (hide it with `mascot: false` in the slide frontmatter).
import { useSlideContext } from '@slidev/client'

const { $frontmatter } = useSlideContext()
const showMascot = ($frontmatter as any)?.mascot !== false
</script>

<template>
  <div class="slidev-layout dmx-grid-bg cover-layout">
    <div class="dmx-brand">
      <span class="mark">λ</span><span>dmx-fun</span>
    </div>
    <div class="cover-body">
      <slot />
    </div>
    <Mascot v-if="showMascot" class="cover-mascot" :size="480" />
    <DmxFooter />
  </div>
</template>

<style scoped>
.cover-layout { display: flex; flex-direction: column; justify-content: center; gap: 2rem; }
.cover-layout .dmx-brand { position: absolute; top: 80px; left: 112px; font-size: 32px; }
.cover-body { max-width: 1080px; }
.cover-body :deep(h1) { font-size: 88px; line-height: 1.08; margin-bottom: 40px; }
.cover-body :deep(p)  { font-size: 38px; line-height: 1.35; color: var(--dmx-text-2); }
.cover-mascot { position: absolute; right: 88px; top: 50%; transform: translateY(-54%); }
</style>
