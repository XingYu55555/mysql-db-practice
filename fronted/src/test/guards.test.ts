import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import { setupRouterGuards } from '@/router/guards'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

vi.mock('element-plus', () => ({ ElMessage: { warning: vi.fn() } }))

describe('router guards', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(ElMessage.warning).mockReset()
    localStorage.clear()
  })

  function makeRouter() {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: { template: '<div />' } },
        { path: '/problems', component: { template: '<div />' } },
        { path: '/submissions', component: { template: '<div />' }, meta: { requiresAuth: true, roles: ['STUDENT'] } },
      ],
    })
    setupRouterGuards(router)
    return router
  }

  it('redirects unauthenticated user to /login', async () => {
    const router = makeRouter()
    await router.push('/submissions')
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('redirects unauthorized role to /problems', async () => {
    const auth = useAuthStore()
    auth.setAuth({ token: 't', userId: 1, role: 'TEACHER' })
    const router = makeRouter()
    await router.push('/submissions')
    expect(router.currentRoute.value.path).toBe('/problems')
    expect(ElMessage.warning).toHaveBeenCalled()
  })
})
