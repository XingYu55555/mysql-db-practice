<template>
  <el-card>
    <el-space>
      <h3>我的题目</h3>
      <el-button type="primary" @click="$router.push('/teacher/problems/new')">新建题目</el-button>
    </el-space>
    <el-table :data="items" style="margin-top:12px">
      <el-table-column prop="problemId" label="ID" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="status" label="状态" />
      <el-table-column label="状态流转">
        <template #default="scope">
          <el-button-group>
            <el-button size="small" @click="setStatus(scope.row.problemId, 'READY')">READY</el-button>
            <el-button size="small" @click="setStatus(scope.row.problemId, 'PUBLISHED')">PUBLISHED</el-button>
            <el-button size="small" @click="setStatus(scope.row.problemId, 'ARCHIVED')">ARCHIVED</el-button>
          </el-button-group>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="$router.push(`/teacher/problems/${scope.row.problemId}/edit`)">编辑</el-button>
          <el-button link @click="$router.push(`/teacher/problems/${scope.row.problemId}/tags`)">标签</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listTeacherProblems, updateProblemStatus } from '@/api/problem'

const items = ref<any[]>([])

async function load() {
  const data = await listTeacherProblems({ page: 1, size: 20 })
  items.value = data.content
}

async function setStatus(problemId: number, status: string) {
  await updateProblemStatus(problemId, status)
  ElMessage.success('状态更新成功')
  await load()
}

onMounted(load)
</script>
