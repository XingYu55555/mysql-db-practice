import MockAdapter from 'axios-mock-adapter'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { apiClient, setUnauthorizedHandler } from '@/api/client'

describe('api client', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('injects Authorization header', async () => {
    localStorage.setItem('token', 'abc')
    const mock = new MockAdapter(apiClient)
    mock.onGet('/api/problem').reply((config) => {
      expect(config.headers?.Authorization).toBe('Bearer abc')
      return [200, {}]
    })

    await apiClient.get('/api/problem')
  })

  it('handles 401 by clearing auth and redirecting', async () => {
    localStorage.setItem('token', 'abc')
    localStorage.setItem('userId', '1')
    localStorage.setItem('role', 'STUDENT')
    const push = vi.fn()
    setUnauthorizedHandler(() => push('/login'))

    const mock = new MockAdapter(apiClient)
    mock.onGet('/api/user/me').reply(401)

    await expect(apiClient.get('/api/user/me')).rejects.toBeTruthy()
    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('userId')).toBeNull()
    expect(localStorage.getItem('role')).toBeNull()
    expect(push).toHaveBeenCalledWith('/login')
  })
})
