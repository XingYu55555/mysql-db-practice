import { render, screen, fireEvent } from '@testing-library/vue'
import { describe, expect, it, vi, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import MockAdapter from 'axios-mock-adapter'
import ProblemDetail from '@/views/common/ProblemDetail.vue'
import { apiClient } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<any>('element-plus')
  return { ...actual, ElMessage: { success: vi.fn(), error: vi.fn() } }
})

describe('ProblemDetail', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.useFakeTimers()
  })

  it('submit -> polling success -> render summary; failed supports collapsed error', async () => {
    const auth = useAuthStore()
    auth.setAuth({ token: 'x', userId: 1, role: 'STUDENT' })

    const mock = new MockAdapter(apiClient)
    mock.onGet('/api/problem/1').reply(200, { problemId: 1, title: '题目1', description: '描述', tags: [] })
    mock.onPost('/api/submission').reply(202, { submissionId: 100 })
    mock.onGet('/api/submission/100/status').replyOnce(200, { submissionId: 100, status: 'JUDGING' }).onGet('/api/submission/100/status').replyOnce(200, { submissionId: 100, status: 'SUCCESS' })
    mock.onGet('/api/result/submission/100').reply(200, { submissionId: 100, status: 'CORRECT', score: 100, executionTimeMs: 12, errorMessage: null })

    const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/problems/:problemId', component: ProblemDetail }] })
    await router.push('/problems/1')
    await router.isReady()

    render(ProblemDetail, { global: { plugins: [createPinia(), router] } })

    const input = await screen.findByPlaceholderText('请输入 SQL')
    await fireEvent.update(input, 'select 1')
    await fireEvent.click(screen.getByRole('button', { name: '提交' }))

    await vi.runAllTimersAsync()

    expect(await screen.findByText('CORRECT')).toBeInTheDocument()
    expect(screen.getByText('100')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()

    mock.resetHandlers()
    mock.onGet('/api/problem/1').reply(200, { problemId: 1, title: '题目1', description: '描述', tags: [] })
    mock.onPost('/api/submission').reply(202, { submissionId: 101 })
    mock.onGet('/api/submission/101/status').reply(200, { submissionId: 101, status: 'FAILED' })
    mock.onGet('/api/submission/101').reply(200, { submissionId: 101, status: 'FAILED', errorMessage: 'SQL syntax error', judgeStatus: 'ERROR', score: 0, executionTimeMs: 2 })

    await fireEvent.click(screen.getByRole('button', { name: '提交' }))
    await vi.runAllTimersAsync()

    expect(await screen.findByRole('button', { name: '展开查看错误信息' })).toBeInTheDocument()
    expect(screen.queryByText('SQL syntax error')).not.toBeInTheDocument()
    await fireEvent.click(screen.getByRole('button', { name: '展开查看错误信息' }))
    expect(await screen.findByText('SQL syntax error')).toBeInTheDocument()
  })
})
