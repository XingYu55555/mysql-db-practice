<template>
  <el-card class="auth-card">
    <h2>登录</h2>
    <el-form :model="form" @submit.prevent="onSubmit">
      <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
      <el-form-item label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
      <el-button type="primary" @click="onSubmit">登录</el-button>
      <el-button link @click="$router.push('/register')">去注册</el-button>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginUser } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const form = reactive({ username: '', password: '' })

async function onSubmit() {
  const res = await loginUser(form)
  auth.setAuth({ token: res.token, userId: res.userId, role: res.role })
  ElMessage.success('登录成功')
  await router.push('/problems')
}
</script>

<style scoped>
.auth-card { max-width: 420px; margin: 40px auto; }
</style>
