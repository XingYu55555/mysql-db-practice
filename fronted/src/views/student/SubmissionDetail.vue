<template>
  <el-card>
    <h3>提交详情</h3>
    <ResultSummary :result="detail" />
    <el-input type="textarea" :model-value="detail?.sqlContent || ''" readonly :rows="8" />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getSubmission } from '@/api/submission'
import ResultSummary from '@/components/ResultSummary.vue'

const route = useRoute()
const detail = ref<any>()
onMounted(async () => {
  detail.value = await getSubmission(Number(route.params.id))
})
</script>
