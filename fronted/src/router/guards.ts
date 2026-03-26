import type { Router } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { UserRole } from '@/api/types'

export function setupRouterGuards(router: Router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return '/login'
    }

    const roles = to.meta.roles as UserRole[] | undefined
    if (roles && authStore.role && !roles.includes(authStore.role)) {
      ElMessage.warning('当前角色无权访问该页面')
      return '/problems'
    }

    if (to.path === '/login' && authStore.isAuthenticated) {
      return '/problems'
    }

    return true
  })
}
