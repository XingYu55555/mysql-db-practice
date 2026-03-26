<template>
  <el-input type="textarea" :rows="12" v-model="text" placeholder="粘贴 JSON" />
  <el-alert v-if="error" :title="error" type="error" :closable="false" style="margin-top: 8px" />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const model = defineModel<string>({ required: true })
const emit = defineEmits<{ (e: 'parsed', value: unknown[] | null): void }>()

const text = ref(model.value)
const error = ref('')

watch(text, (value) => {
  model.value = value
  try {
    const parsed = JSON.parse(value)
    error.value = ''
    emit('parsed', Array.isArray(parsed) ? parsed : parsed.problems || null)
  } catch {
    error.value = 'JSON 格式错误'
    emit('parsed', null)
  }
})
</script>
