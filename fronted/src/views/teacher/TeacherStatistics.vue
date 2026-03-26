<template>
  <el-card>
    <h3>统计信息</h3>
    <pre>{{ JSON.stringify(data, null, 2) }}</pre>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getStatistics } from '@/api/result'

const data = ref<any>(null)
onMounted(async () => {
  data.value = await getStatistics()
})
</script>
