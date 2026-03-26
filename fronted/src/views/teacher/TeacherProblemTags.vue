<template>
  <el-card>
    <h3>题目标签</h3>
    <el-space>
      <el-input v-model="newTag.name" placeholder="标签名" style="width:200px" />
      <el-input v-model="newTag.color" placeholder="#3B82F6" style="width:140px" />
      <el-button @click="addTag">创建标签</el-button>
    </el-space>
    <el-select v-model="selected" multiple style="width:100%;margin-top:12px" placeholder="选择标签">
      <el-option v-for="tag in tags" :key="tag.tagId" :label="tag.name" :value="tag.tagId" />
    </el-select>
    <el-button type="primary" style="margin-top:12px" @click="save">保存题目标签</el-button>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createTag, getProblem, listTags, updateProblemTags } from '@/api/problem'
import type { TagItem } from '@/api/types'

const route = useRoute()
const tags = ref<any[]>([])
const selected = ref<number[]>([])
const newTag = reactive({ name: '', color: '#3B82F6' })

async function load() {
  tags.value = await listTags()
  const problem = await getProblem(Number(route.params.id))
  selected.value = (problem.tags || []).map((t: TagItem) => t.tagId)
}

async function addTag() {
  if (!newTag.name.trim()) return
  await createTag(newTag)
  newTag.name = ''
  ElMessage.success('标签创建成功')
  await load()
}

async function save() {
  await updateProblemTags(Number(route.params.id), selected.value)
  ElMessage.success('标签已更新')
}

onMounted(load)
</script>
