<script setup lang="ts">
// Colors a word with the track it belongs to: <Track ok>Ok</Track>, <Track err>Err</Track>.
defineProps<{ ok?: boolean; err?: boolean }>()
</script>

<template>
  <span :class="{ ok, err }"><slot /></span>
</template>
