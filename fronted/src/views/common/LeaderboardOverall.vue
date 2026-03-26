<template>
  <el-card>
    <h3>总排行榜</h3>
    <el-table :data="entries"><el-table-column prop="rank" label="排名" /><el-table-column prop="studentUsername" label="用户名" /><el-table-column prop="totalScore" label="总分" /></el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getOverallLeaderboard } from '@/api/result'

const entries = ref<any[]>([])
onMounted(async () => {
  const data = await getOverallLeaderboard()
  entries.value = data.entries || []
})
</script>
