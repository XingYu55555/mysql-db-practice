import { test, expect } from '@playwright/test'

test('register -> login -> list -> detail -> submit -> result', async ({ page }) => {
  const api = 'http://localhost:8080'
  let poll = 0

  await page.route(`${api}/api/user/register`, async (route) => {
    await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify({ userId: 1, username: 'stu', role: 'STUDENT' }) })
  })

  await page.route(`${api}/api/user/login`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ token: 'jwt-token', tokenType: 'Bearer', expiresIn: 86400, userId: 1, username: 'stu', role: 'STUDENT' }) })
  })

  await page.route(`${api}/api/problem?page=1&size=10&difficulty=&sqlType=`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ content: [{ problemId: 1, title: '题目1', description: 'desc', tags: [{ tagId: 1, name: 'SELECT' }] }], page: 1, size: 10, totalElements: 1, totalPages: 1 }) })
  })

  await page.route(`${api}/api/problem/1`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ problemId: 1, title: '题目1', description: 'desc', tags: [{ tagId: 1, name: 'SELECT' }] }) })
  })

  await page.route(`${api}/api/submission`, async (route) => {
    await route.fulfill({ status: 202, contentType: 'application/json', body: JSON.stringify({ submissionId: 100 }) })
  })

  await page.route(`${api}/api/submission/100/status`, async (route) => {
    poll += 1
    if (poll < 2) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ submissionId: 100, status: 'JUDGING' }) })
      return
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ submissionId: 100, status: 'SUCCESS' }) })
  })

  await page.route(`${api}/api/result/submission/100`, async (route) => {
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ resultId: 1, submissionId: 100, problemId: 1, problemTitle: '题目1', studentId: 1, studentUsername: 'stu', score: 95, status: 'CORRECT', executionTimeMs: 23, errorMessage: null }) })
  })

  await page.goto('/register')
  await page.getByLabel('用户名').fill('stu')
  await page.getByLabel('密码').fill('password123')
  await page.getByLabel('邮箱').fill('stu@example.com')
  await page.getByRole('button', { name: '注册' }).click()

  await expect(page).toHaveURL(/\/login$/)

  await page.getByLabel('用户名').fill('stu')
  await page.getByLabel('密码').fill('password123')
  await page.getByRole('button', { name: '登录' }).click()

  await expect(page).toHaveURL(/\/problems$/)
  await expect(page.getByText('题目1')).toBeVisible()
  await page.getByRole('button', { name: '详情' }).click()

  await expect(page).toHaveURL(/\/problems\/1$/)
  await page.getByPlaceholder('请输入 SQL').fill('SELECT * FROM t')
  await page.getByRole('button', { name: '提交' }).click()

  await expect(page.getByText('CORRECT')).toBeVisible()
  await expect(page.getByText('95')).toBeVisible()
  await expect(page.getByText('23')).toBeVisible()
})
