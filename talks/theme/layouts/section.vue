<template>
  <div class="slidev-layout dmx-grid-bg section-layout">
    <div class="section-body">
      <Kicker />
      <slot />
    </div>
    <DmxFooter />
  </div>
</template>

<style scoped>
.section-layout { display: flex; align-items: center; }
.section-body :deep(h1) { font-size: 84px; line-height: 1.1; margin-bottom: 32px; }
.section-body :deep(p)  { font-size: 38px; line-height: 1.35; color: var(--dmx-text-2); max-width: 1500px; }
.section-body :deep(.kicker) { font-size: 28px; margin-bottom: 24px; }
</style>
