export type UserRole = 'STUDENT' | 'TEACHER'

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface UserInfo {
  userId: number
  username: string
  role: UserRole
  email?: string
  createdAt?: string
}

export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userId: number
  username: string
  role: UserRole
}

export interface TagItem {
  tagId: number
  name: string
  color?: string
  createdAt?: string
}

export interface ProblemItem {
  problemId: number
  title: string
  description?: string
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD'
  sqlType?: 'DQL' | 'DML' | 'DDL' | 'DCL'
  status?: 'DRAFT' | 'READY' | 'PUBLISHED' | 'ARCHIVED'
  initSql?: string
  standardAnswer?: string
  expectedType?: string
  tags?: TagItem[]
}

export interface SubmissionStatus {
  submissionId: number
  status: 'PENDING' | 'JUDGING' | 'SUCCESS' | 'FAILED'
  score?: number | null
  judgeStatus?: string | null
}

export interface SubmissionDetail {
  submissionId: number
  problemId: number
  problemTitle: string
  sqlContent: string
  status: string
  score?: number | null
  executionTimeMs?: number | null
  judgeStatus?: string | null
  errorMessage?: string | null
  submittedAt?: string
}

export interface ResultDetail {
  resultId: number
  submissionId: number
  problemId: number
  problemTitle: string
  studentId: number
  studentUsername: string
  score: number
  status: string
  executionTimeMs: number
  errorMessage?: string | null
  createdAt?: string
}
