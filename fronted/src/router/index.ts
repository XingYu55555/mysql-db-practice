import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuards } from './guards'

const routes = [
  { path: '/', redirect: '/problems' },
  { path: '/login', component: () => import('@/views/auth/Login.vue') },
  { path: '/register', component: () => import('@/views/auth/Register.vue') },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: 'problems', component: () => import('@/views/common/Problems.vue') },
      { path: 'problems/:problemId', component: () => import('@/views/common/ProblemDetail.vue') },
      { path: 'leaderboard', component: () => import('@/views/common/LeaderboardOverall.vue') },
      { path: 'leaderboard/problem/:problemId', component: () => import('@/views/common/LeaderboardProblem.vue') },
      { path: 'me', component: () => import('@/views/common/Me.vue') },

      { path: 'submissions', component: () => import('@/views/student/Submissions.vue'), meta: { roles: ['STUDENT'] } },
      { path: 'submissions/:id', component: () => import('@/views/student/SubmissionDetail.vue'), meta: { roles: ['STUDENT'] } },
      { path: 'results', component: () => import('@/views/student/MyResults.vue'), meta: { roles: ['STUDENT'] } },

      { path: 'teacher/problems', component: () => import('@/views/teacher/TeacherMyProblems.vue'), meta: { roles: ['TEACHER'] } },
      { path: 'teacher/problems/new', component: () => import('@/views/teacher/TeacherProblemCreate.vue'), meta: { roles: ['TEACHER'] } },
      { path: 'teacher/problems/:id/edit', component: () => import('@/views/teacher/TeacherProblemEdit.vue'), meta: { roles: ['TEACHER'] } },
      { path: 'teacher/problems/:id/tags', component: () => import('@/views/teacher/TeacherProblemTags.vue'), meta: { roles: ['TEACHER'] } },
      { path: 'teacher/problems/import', component: () => import('@/views/teacher/TeacherProblemImport.vue'), meta: { roles: ['TEACHER'] } },
      { path: 'teacher/statistics', component: () => import('@/views/teacher/TeacherStatistics.vue'), meta: { roles: ['TEACHER'] } },
    ],
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

setupRouterGuards(router)
