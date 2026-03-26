<template>
  <el-card>
    <h3>我的信息</h3>
    <el-descriptions border :column="1">
      <el-descriptions-item label="用户ID">{{ me?.userId }}</el-descriptions-item>
      <el-descriptions-item label="用户名">{{ me?.username }}</el-descriptions-item>
      <el-descriptions-item label="角色">{{ me?.role }}</el-descriptions-item>
    </el-descriptions>
    <el-button type="danger" style="margin-top:12px" @click="logout">退出登录</el-button>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser } from '@/api/user'
import type { UserInfo } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const me = ref<UserInfo>()

onMounted(async () => {
  me.value = await getCurrentUser()
})

async function logout() {
  auth.clearAuth()
  await router.push('/login')
}
</script>
