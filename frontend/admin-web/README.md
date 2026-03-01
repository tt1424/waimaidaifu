# Admin Web

## 运行模式

- 默认使用前端 Mock 数据（不依赖后端）。
- 若要切换后端联调，新建 `.env.local` 并设置：

```env
VITE_USE_MOCK=false
```

## Install

```bash
npm install
```

## Run

```bash
npm run dev
```

Vite proxy forwards `/api` to `http://localhost:8080`.
