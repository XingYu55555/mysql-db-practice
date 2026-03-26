<template>
  <el-card>
    <h3>编辑题目</h3>
    <el-form :model="form" label-width="120px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="描述"><el-input type="textarea" v-model="form.description" :rows="4" /></el-form-item>
      <el-form-item label="初始化 SQL"><el-input type="textarea" v-model="form.initSql" :rows="4" /></el-form-item>
      <el-form-item label="标准答案(必填)"><el-input type="textarea" v-model="form.standardAnswer" :rows="3" /></el-form-item>
      <el-button type="primary" @click="save">保存</el-button>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProblem, updateProblem } from '@/api/problem'

const route = useRoute()
const router = useRouter()
const form = reactive<any>({ title: '', description: '', initSql: '', standardAnswer: '' })

onMounted(async () => {
  Object.assign(form, await getProblem(Number(route.params.id)))
})

async function save() {
  if (!String(form.standardAnswer || '').trim()) {
    ElMessage.error('standardAnswer 必填')
    return
  }
  await updateProblem(Number(route.params.id), form)
  ElMessage.success('更新成功')
  await router.push('/teacher/problems')
}
</script>
