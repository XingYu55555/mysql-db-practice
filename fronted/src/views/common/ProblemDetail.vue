<template>
  <el-card v-if="problem">
    <h2>{{ problem.title }}</h2>
    <p>{{ problem.description }}</p>
    <ProblemTags :tags="problem.tags || []" />

    <div style="margin-top: 16px">
      <SqlEditorMonaco v-model="sqlContent" />
      <el-space style="margin-top: 12px">
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" @click="submit" :disabled="auth.role !== 'STUDENT' || loading">提交</el-button>
      </el-space>
    </div>

    <div v-if="result" style="margin-top: 16px">
      <ResultSummary :result="result" />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProblem } from '@/api/problem'
import { createSubmission, getSubmission, getSubmissionStatus } from '@/api/submission'
import { getResultBySubmission } from '@/api/result'
import type { ProblemItem } from '@/api/types'
import ProblemTags from '@/components/ProblemTags.vue'
import SqlEditorMonaco from '@/components/SqlEditorMonaco.vue'
import ResultSummary from '@/components/ResultSummary.vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const problem = ref<ProblemItem>()
const sqlContent = ref('')
const loading = ref(false)
const result = ref<{ status?: string; judgeStatus?: string; score?: number | null; executionTimeMs?: number | null; errorMessage?: string | null }>()

const problemId = computed(() => Number(route.params.problemId))

function draftKey() {
  return `sql-draft-${problemId.value}`
}

function saveDraft() {
  localStorage.setItem(draftKey(), sqlContent.value)
  ElMessage.success('草稿已保存')
}

async function load() {
  problem.value = await getProblem(problemId.value)
  sqlContent.value = localStorage.getItem(draftKey()) || ''
}

function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function submit() {
  if (auth.role !== 'STUDENT') return
  loading.value = true
  result.value = undefined
  try {
    const created = await createSubmission({ problemId: problemId.value, sqlContent: sqlContent.value })
    for (let i = 0; i < 60; i += 1) {
      const statusRes = await getSubmissionStatus(created.submissionId)
      if (statusRes.status === 'SUCCESS') {
        const rs = await getResultBySubmission(created.submissionId)
        result.value = {
          status: rs.status,
          score: rs.score,
          executionTimeMs: rs.executionTimeMs,
          errorMessage: rs.errorMessage,
        }
        ElMessage.success('提交成功')
        return
      }
      if (statusRes.status === 'FAILED') {
        const detail = await getSubmission(created.submissionId)
        result.value = {
          status: 'FAILED',
          judgeStatus: detail.judgeStatus || undefined,
          score: detail.score,
          executionTimeMs: detail.executionTimeMs,
          errorMessage: detail.errorMessage,
        }
        ElMessage.error('判题失败')
        return
      }
      await sleep(1000)
    }
    result.value = { status: 'FAILED', errorMessage: '轮询超时' }
    ElMessage.error('轮询超时')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
