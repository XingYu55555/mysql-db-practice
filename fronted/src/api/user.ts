import { apiClient } from './client'
import type { LoginResponse, UserInfo, UserRole } from './types'

export interface RegisterPayload {
  username: string
  password: string
  role: UserRole
  email?: string
}

export function registerUser(payload: RegisterPayload) {
  return apiClient.post<UserInfo>('/api/user/register', payload).then((r) => r.data)
}

export function loginUser(payload: { username: string; password: string }) {
  return apiClient.post<LoginResponse>('/api/user/login', payload).then((r) => r.data)
}

export function getCurrentUser() {
  return apiClient.get<UserInfo>('/api/user/me').then((r) => r.data)
}
