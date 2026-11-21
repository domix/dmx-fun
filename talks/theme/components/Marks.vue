<script setup lang="ts">
// Numbered badges in the margin of a code block, one per line in `at` (1-based), persisting across
// clicks. Amber by default (the failure track); `ok` makes them blue. Wrap the snippet:
//
//   <Marks :at="[3, 7, 10]">
//
//   <<< @/snippets/... java
//
//   </Marks>
//
// Line geometry follows base.css: 28px code at 1.4, or 24px on a `dense` slide, plus the 20px pre padding.
const props = withDefaults(defineProps<{ at: number[]; ok?: boolean; dense?: boolean }>(), { ok: false, dense: false })
const lineHeight = () => (props.dense ? 24 : 28) * 1.4
const top = (line: number) => 20 + (line - 1) * lineHeight() + lineHeight() / 2
</script>

<template>
  <div class="marks">
    <slot />
    <span v-for="(line, i) in at" :key="line" class="mark" :class="{ ok }" :style="{ top: `${top(line)}px` }">{{ i + 1 }}</span>
  </div>
</template>

<style scoped>
.marks { position: relative; }
.mark {
  position: absolute;
  left: -60px;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font: 650 22px/1 'Inter Variable', Inter, system-ui, sans-serif;
  color: var(--dmx-bg);
  background: var(--dmx-err);
}
.mark.ok { background: var(--dmx-ok); }
</style>
