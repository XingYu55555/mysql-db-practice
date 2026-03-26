<template>
  <el-container>
    <el-header class="header">
      <el-menu mode="horizontal" :router="true" :default-active="$route.path">
        <el-menu-item index="/problems">题目</el-menu-item>
        <el-menu-item index="/leaderboard">排行榜</el-menu-item>
        <el-menu-item index="/submissions" v-if="role === 'STUDENT'">提交</el-menu-item>
        <el-menu-item index="/results" v-if="role === 'STUDENT'">成绩</el-menu-item>
        <el-menu-item index="/teacher/problems" v-if="role === 'TEACHER'">我的题目</el-menu-item>
        <el-menu-item index="/teacher/problems/import" v-if="role === 'TEACHER'">批量导入</el-menu-item>
        <el-menu-item index="/teacher/statistics" v-if="role === 'TEACHER'">统计</el-menu-item>
        <el-menu-item index="/me">我</el-menu-item>
      </el-menu>
    </el-header>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'

const { role } = storeToRefs(useAuthStore())
</script>

<style scoped>
.header { border-bottom: 1px solid #eee; }
</style>
