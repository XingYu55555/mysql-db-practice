<template>
  <el-card>
    <el-descriptions :column="1" border>
      <el-descriptions-item label="结论">{{ judgeText }}</el-descriptions-item>
      <el-descriptions-item label="分数">{{ scoreText }}</el-descriptions-item>
      <el-descriptions-item label="耗时(ms)">{{ timeText }}</el-descriptions-item>
    </el-descriptions>
    <div v-if="canExpandError" style="margin-top: 12px">
      <el-button size="small" @click="expanded = !expanded">{{ expanded ? '收起' : '展开查看' }}错误信息</el-button>
      <el-alert v-if="expanded" :title="errorMessage || '未知错误'" type="error" :closable="false" show-icon style="margin-top: 8px" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'

const props = defineProps<{
  result?: { status?: string; judgeStatus?: string; score?: number | null; executionTimeMs?: number | null; errorMessage?: string | null }
}>()

const expanded = ref(false)
const failureStatuses = ['FAILED', 'ERROR', 'INCORRECT', 'TIME_LIMIT', 'AI_REJECTED']

const judgeText = computed(() => props.result?.status || props.result?.judgeStatus || '-')
const scoreText = computed(() => (props.result?.score ?? '-') as string | number)
const timeText = computed(() => (props.result?.executionTimeMs ?? '-') as string | number)
const errorMessage = computed(() => props.result?.errorMessage || '')
const canExpandError = computed(() => {
  const s = props.result?.status || props.result?.judgeStatus || ''
  return Boolean(errorMessage.value) && failureStatuses.includes(s)
})
</script>
