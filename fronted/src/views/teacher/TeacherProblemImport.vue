<template>
  <el-card>
    <h3>批量导入题目</h3>
    <JsonImportEditor v-model="jsonText" @parsed="onParsed" />
    <el-button type="primary" style="margin-top:12px" :disabled="!parsed" @click="submit">导入</el-button>
    <el-descriptions v-if="result" border :column="1" style="margin-top:12px">
      <el-descriptions-item label="successCount">{{ result.successCount }}</el-descriptions-item>
      <el-descriptions-item label="failCount">{{ result.failCount }}</el-descriptions-item>
      <el-descriptions-item label="errors">{{ JSON.stringify(result.errors || []) }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import JsonImportEditor from '@/components/JsonImportEditor.vue'
import { batchImportProblems } from '@/api/problem'

const jsonText = ref('[]')
const parsed = ref<any[] | null>([])
const result = ref<any>()

function onParsed(v: any[] | null) {
  parsed.value = v
}

async function submit() {
  result.value = await batchImportProblems(parsed.value || [])
}
</script>
