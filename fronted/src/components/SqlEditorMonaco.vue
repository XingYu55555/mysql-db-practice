<template>
  <div>
    <div v-if="usePlainText">
      <el-input type="textarea" :rows="10" :model-value="modelValue" @input="onInput" placeholder="请输入 SQL" />
    </div>
    <div v-else ref="containerRef" class="editor"></div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const containerRef = ref<HTMLElement>()
const editorRef = ref<any>(null)
const usePlainText = import.meta.env.MODE === 'test' || import.meta.env.VITE_USE_PLAIN_EDITOR === '1'

onMounted(async () => {
  if (usePlainText || !containerRef.value) return
  const monaco = await import('monaco-editor')
  editorRef.value = monaco.editor.create(containerRef.value, {
    value: props.modelValue,
    language: 'sql',
    minimap: { enabled: false },
    automaticLayout: true,
  })
  editorRef.value.onDidChangeModelContent(() => {
    emit('update:modelValue', editorRef.value.getValue())
  })
})

onBeforeUnmount(() => {
  editorRef.value?.dispose?.()
})

watch(
  () => props.modelValue,
  (value) => {
    if (editorRef.value && value !== editorRef.value.getValue()) {
      editorRef.value.setValue(value)
    }
  },
)

function onInput(value: string) {
  emit('update:modelValue', value)
}
</script>

<style scoped>
.editor { height: 320px; border: 1px solid #ddd; }
</style>
