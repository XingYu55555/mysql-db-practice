<template>
  <el-card>
    <h3>题目排行榜</h3>
    <el-table :data="entries"><el-table-column prop="rank" label="排名" /><el-table-column prop="studentUsername" label="用户名" /><el-table-column prop="bestScore" label="最高分" /></el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getProblemLeaderboard } from '@/api/result'

const route = useRoute()
const entries = ref<any[]>([])
onMounted(async () => {
  const data = await getProblemLeaderboard(Number(route.params.problemId))
  entries.value = data.entries || []
})
</script>
