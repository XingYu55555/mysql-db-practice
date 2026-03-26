<template>
  <el-card>
    <h3>创建题目</h3>
    <el-form :model="form" label-width="120px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="描述"><el-input type="textarea" v-model="form.description" :rows="4" /></el-form-item>
      <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option value="EASY" /><el-option value="MEDIUM" /><el-option value="HARD" /></el-select></el-form-item>
      <el-form-item label="SQL 类型"><el-select v-model="form.sqlType"><el-option value="DQL" /><el-option value="DML" /><el-option value="DDL" /><el-option value="DCL" /></el-select></el-form-item>
      <el-form-item label="初始化 SQL"><el-input type="textarea" v-model="form.initSql" :rows="4" /></el-form-item>
      <el-form-item label="标准答案(必填)"><el-input type="textarea" v-model="form.standardAnswer" :rows="3" /></el-form-item>
      <el-button type="primary" @click="save">创建</el-button>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createProblem } from '@/api/problem'

const router = useRouter()
const form = reactive({ title: '', description: '', difficulty: 'MEDIUM', sqlType: 'DQL', initSql: '', standardAnswer: '' })

async function save() {
  if (!form.standardAnswer.trim()) {
    ElMessage.error('standardAnswer 必填')
    return
  }
  await createProblem(form)
  ElMessage.success('创建成功')
  await router.push('/teacher/problems')
}
</script>
