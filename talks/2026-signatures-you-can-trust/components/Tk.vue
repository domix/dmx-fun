<script setup lang="ts">
// Bilingual text by key, for longer passages kept in i18n/en.json and i18n/es.json.
// The i18n check fails the build when a key is missing in either file.
import { computed } from 'vue'
import { lang } from '../../theme/composables/lang'
import en from '../i18n/en.json'
import es from '../i18n/es.json'

const props = defineProps<{ k: string }>()
const dict: Record<string, Record<string, string>> = { en, es }
const text = computed(() => dict[lang.value][props.k] ?? `⟪missing: ${props.k}⟫`)
</script>

<template>
  <span :lang="lang">{{ text }}</span>
</template>
