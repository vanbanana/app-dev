# Atelier — 无限 AI 画布

暗色无限画布上的 AI 图像生成工作台，参考 [lovart.ai](https://www.lovart.ai/) 的功能页面，复用原 `Sketch3View` Android 应用的三视图生成提示词与生成流程。

## 技术栈

- **Next.js 15 (App Router) + TypeScript** —— 单一技术栈，自带 API 路由作为生成代理（密钥不落前端）
- **tldraw 3** —— 无限画布；自带视口裁剪，几百张图片同屏仍流畅
- **Tailwind CSS v4 + lucide-react** —— 干净留白的暗色界面
- 画布上的 **Image Generator 节点**：参考图上传、提示词、写实/Q版风格、画幅、一键生成三视图

## 本地运行

```bash
cd web
cp .env.example .env.local   # 填入 BLTCY_API_KEY
npm install
npm run dev                  # http://localhost:3000
```

### 环境变量（仅服务端使用）

| 变量 | 说明 | 默认值 |
| --- | --- | --- |
| `BLTCY_API_KEY` | OpenAI 兼容代理的密钥 | 必填 |
| `BLTCY_BASE_URL` | 代理 Base URL | `https://api.bltcy.ai` |
| `IMAGE_MODEL` | 上游模型名 | `gpt-image-1` |

## 脚本

- `npm run dev` —— 开发服务器
- `npm run build` —— 生产构建
- `npm run lint` —— ESLint
- `npm run typecheck` —— TypeScript 类型检查

## 生成逻辑

`src/lib/prompts.ts` 内含从原 Android 应用 `DefaultImageGenerationApi.kt` 原样移植的 `写实`/`Q版` 三视图提示词。`/api/generate` 在服务端：有参考图时走 `/v1/images/edits`（multipart），无参考图时走 `/v1/images/generations`，返回 base64 图片给画布渲染。

> 注：tldraw SDK 免费版会显示 "Made with tldraw" 水印，去除需购买其商业授权。
