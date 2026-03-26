<template>
  <el-card class="auth-card">
    <h2>注册</h2>
    <el-form :model="form" @submit.prevent="onSubmit">
      <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.role"><el-option value="STUDENT" label="学生" /><el-option value="TEACHER" label="教师" /></el-select>
      </el-form-item>
      <el-button type="primary" @click="onSubmit">注册</el-button>
      <el-button link @click="$router.push('/login')">去登录</el-button>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerUser } from '@/api/user'

const router = useRouter()
const form = reactive({ username: '', password: '', email: '', role: 'STUDENT' as 'STUDENT' | 'TEACHER' })

async function onSubmit() {
  await registerUser(form)
  ElMessage.success('注册成功，请登录')
  await router.push('/login')
}
</script>

<style scoped>
.auth-card { max-width: 420px; margin: 40px auto; }
</style>
