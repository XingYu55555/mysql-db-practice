import { defineStore } from 'pinia'
import type { UserRole } from '@/api/types'

interface AuthState {
  token: string
  userId: number | null
  role: UserRole | null
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null,
    role: (localStorage.getItem('role') as UserRole | null) || null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token),
  },
  actions: {
    setAuth(payload: { token: string; userId: number; role: UserRole }) {
      this.token = payload.token
      this.userId = payload.userId
      this.role = payload.role
      localStorage.setItem('token', payload.token)
      localStorage.setItem('userId', String(payload.userId))
      localStorage.setItem('role', payload.role)
    },
    clearAuth() {
      this.token = ''
      this.userId = null
      this.role = null
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('role')
    },
  },
})
