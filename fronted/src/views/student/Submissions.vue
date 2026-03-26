<template>
  <el-card>
    <h3>提交记录</h3>
    <el-table :data="items"><el-table-column prop="submissionId" label="ID" /><el-table-column prop="problemTitle" label="题目" /><el-table-column prop="status" label="状态" /><el-table-column label="操作"><template #default="scope"><el-button link @click="$router.push(`/submissions/${scope.row.submissionId}`)">详情</el-button></template></el-table-column></el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listSubmissions } from '@/api/submission'

const items = ref<any[]>([])
onMounted(async () => {
  const data = await listSubmissions({ page: 1, size: 10 })
  items.value = data.content
})
</script>
