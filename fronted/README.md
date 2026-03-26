# fronted

Vue3 前端项目（Vite + TypeScript），通过网关访问后端 API。

## 环境变量

创建（已提供）`.env.development`：

```env
VITE_API_BASE_URL=http://localhost:8080
```

> 后端网关需运行在 `8080`。测试使用 mock API，不依赖真实后端。

## 安装与运行

```bash
pnpm install
pnpm dev
```

## 测试

```bash
pnpm test
pnpm e2e
```

## 构建

```bash
pnpm run build
```
