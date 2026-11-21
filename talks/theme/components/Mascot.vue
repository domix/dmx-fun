<script setup lang="ts">
// The library's mascot (the same artwork as site/public/logo.png). Brand element: cover and closing only.
import mascot from '../assets/mascot.png'

withDefaults(defineProps<{ size?: number }>(), { size: 320 })
</script>

<template>
  <img class="mascot" :src="mascot" :style="{ height: `${size}px` }" alt="" />
</template>

<style scoped>
.mascot { display: block; width: auto; }
</style>
