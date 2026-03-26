import { apiClient } from './client'
import type { PageResponse, SubmissionDetail, SubmissionStatus } from './types'

export function createSubmission(payload: { problemId: number; sqlContent: string }) {
  return apiClient.post<{ submissionId: number }>('/api/submission', payload).then((r) => r.data)
}

export function listSubmissions(params: Record<string, unknown>) {
  return apiClient.get<PageResponse<SubmissionDetail>>('/api/submission', { params }).then((r) => r.data)
}

export function getSubmission(submissionId: number) {
  return apiClient.get<SubmissionDetail>(`/api/submission/${submissionId}`).then((r) => r.data)
}

export function getSubmissionStatus(submissionId: number) {
  return apiClient.get<SubmissionStatus>(`/api/submission/${submissionId}/status`).then((r) => r.data)
}
