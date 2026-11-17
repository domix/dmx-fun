<script setup lang="ts">
// Inline bilingual text. Both props are required: the i18n check fails the build when one is missing.
import { computed } from 'vue'
import { lang } from '../composables/lang'

const props = defineProps<{ en: string; es: string }>()
const text = computed(() => (lang.value === 'es' ? props.es : props.en))
</script>

<template>
  <span :lang="lang">{{ text }}</span>
</template>
