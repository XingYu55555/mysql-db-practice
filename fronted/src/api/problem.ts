import { apiClient } from './client'
import type { PageResponse, ProblemItem, TagItem } from './types'

export function listProblems(params: { page?: number; size?: number; difficulty?: string; sqlType?: string }) {
  return apiClient.get<PageResponse<ProblemItem>>('/api/problem', { params }).then((r) => r.data)
}

export function getProblem(problemId: number) {
  return apiClient.get<ProblemItem>(`/api/problem/${problemId}`).then((r) => r.data)
}

export function listTeacherProblems(params: { page?: number; size?: number; status?: string }) {
  return apiClient.get<PageResponse<ProblemItem>>('/api/problem/teacher/my', { params }).then((r) => r.data)
}

export function createProblem(payload: Partial<ProblemItem>) {
  return apiClient.post<ProblemItem>('/api/problem', payload).then((r) => r.data)
}

export function updateProblem(problemId: number, payload: Partial<ProblemItem>) {
  return apiClient.put<ProblemItem>(`/api/problem/${problemId}`, payload).then((r) => r.data)
}

export function updateProblemStatus(problemId: number, status: string) {
  return apiClient.put<ProblemItem>(`/api/problem/${problemId}/status`, { status }).then((r) => r.data)
}

export function batchImportProblems(problems: Partial<ProblemItem>[]) {
  return apiClient.post('/api/problem/batch', { problems }).then((r) => r.data)
}

export function listTags() {
  return apiClient.get<TagItem[]>('/api/tag').then((r) => r.data)
}

export function createTag(payload: { name: string; color?: string }) {
  return apiClient.post<TagItem>('/api/tag', payload).then((r) => r.data)
}

export function updateProblemTags(problemId: number, tagIds: number[]) {
  return apiClient.put(`/api/problem/${problemId}/tags`, { tagIds }).then((r) => r.data)
}
