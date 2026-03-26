<template>
  <el-card>
    <el-form inline>
      <el-form-item label="难度"><el-select v-model="query.difficulty" clearable><el-option value="EASY" label="EASY" /><el-option value="MEDIUM" label="MEDIUM" /><el-option value="HARD" label="HARD" /></el-select></el-form-item>
      <el-form-item label="类型"><el-select v-model="query.sqlType" clearable><el-option value="DQL" label="DQL" /><el-option value="DML" label="DML" /><el-option value="DDL" label="DDL" /><el-option value="DCL" label="DCL" /></el-select></el-form-item>
      <el-button type="primary" @click="load">筛选</el-button>
    </el-form>

    <el-table :data="items" style="width: 100%">
      <el-table-column prop="problemId" label="ID" width="90" />
      <el-table-column prop="title" label="标题" />
      <el-table-column label="标签">
        <template #default="scope"><ProblemTags :tags="scope.row.tags || []" /></template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope"><el-button link type="primary" @click="$router.push(`/problems/${scope.row.problemId}`)">详情</el-button></template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { listProblems } from '@/api/problem'
import type { ProblemItem } from '@/api/types'
import ProblemTags from '@/components/ProblemTags.vue'

const query = reactive({ page: 1, size: 10, difficulty: '', sqlType: '' })
const items = ref<ProblemItem[]>([])

async function load() {
  const data = await listProblems(query)
  items.value = data.content
}

onMounted(load)
</script>
