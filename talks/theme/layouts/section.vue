<template>
  <div class="slidev-layout dmx-grid-bg section-layout">
    <div class="section-body">
      <slot />
    </div>
    <DmxFooter />
  </div>
</template>

<style scoped>
.section-layout { display: flex; align-items: center; }
.section-body :deep(h1) { font-size: 3rem; }
</style>
