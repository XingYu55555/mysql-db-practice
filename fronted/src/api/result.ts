import { apiClient } from './client'
import type { PageResponse, ResultDetail } from './types'

export function getResultBySubmission(submissionId: number) {
  return apiClient.get<ResultDetail>(`/api/result/submission/${submissionId}`).then((r) => r.data)
}

export function getStudentResults(studentId: number, params: { page?: number; size?: number } = {}) {
  return apiClient.get<PageResponse<Pick<ResultDetail, 'resultId' | 'submissionId' | 'createdAt'>>>(`/api/result/student/${studentId}`, { params }).then((r) => r.data)
}

export function getProblemLeaderboard(problemId: number, params: { page?: number; size?: number } = {}) {
  return apiClient.get(`/api/result/problem/${problemId}/leaderboard`, { params }).then((r) => r.data)
}

export function getOverallLeaderboard(params: { page?: number; size?: number } = {}) {
  return apiClient.get('/api/result/leaderboard', { params }).then((r) => r.data)
}

export function getStatistics(params: { problemId?: number } = {}) {
  return apiClient.get('/api/result/statistics', { params }).then((r) => r.data)
}
