<template>
  <el-space wrap>
    <el-tag v-for="tag in tags" :key="tag.tagId" :color="tag.color || undefined" effect="light">{{ tag.name }}</el-tag>
  </el-space>
</template>

<script setup lang="ts">
import type { TagItem } from '@/api/types'
defineProps<{ tags: TagItem[] }>()
</script>
