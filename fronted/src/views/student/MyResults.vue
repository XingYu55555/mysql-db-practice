<template>
  <el-card>
    <h3>我的成绩</h3>
    <el-table :data="items"><el-table-column prop="resultId" label="结果ID" /><el-table-column prop="submissionId" label="提交ID" /><el-table-column prop="createdAt" label="时间" /></el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getStudentResults } from '@/api/result'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const items = ref<any[]>([])
onMounted(async () => {
  if (!auth.userId) return
  const data = await getStudentResults(auth.userId)
  items.value = data.content
})
</script>
