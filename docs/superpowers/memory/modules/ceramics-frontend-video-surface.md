---
type: module_card
title: ceramics-frontend-video-surface
summary: 记录窑创未来前端中适合项目介绍演示视频取景的页面、素材和约束。
tags:
  - vue
  - ceramics
  - remotion
owned_paths:
  - vue/src/views/ceramics/**
  - vue/src/router/index.js
  - vue/public/**
  - 窑创未来.md
entrypoints:
  - vue/src/router/index.js
  - vue/src/views/ceramics/HomeView.vue
  - 窑创未来.md
last_verified_commit: ec81ddc
status: draft
---

# Ceramics Frontend Video Surface

## Responsibilities

- 提供“窑创未来”项目的可视化展示面，覆盖首页叙事、柴烧导览、数字藏品、智鉴中枢、实践转化、关于项目、社区和商城。
- 为项目介绍视频提供真实网页画面和本地静态素材，例如 `logo.webp`、窑炉、陶器、工艺流程图片。

## Entry Points

- 前端开发服务：`vue/package.json` 的 `dev` 脚本，命令为 `vite`。
- 路由基底：`/ceramics`，根路径和旧路径会重定向到 `/ceramics/...`。
- 适合视频取景的主要页面：
  - `/ceramics/home`
  - `/ceramics/guide/kiln`
  - `/ceramics/guide/process`
  - `/ceramics/collections/catalog`
  - `/ceramics/intelligence/appraisal`
  - `/ceramics/intelligence/qa`
  - `/ceramics/about/project`
  - `/ceramics/about/practice`
  - `/ceramics/about/visit`

## Invariants

- 项目编码按 UTF-8 处理，新增文本文件应保持 UTF-8 无 BOM。
- 本仓库工作区已有大量未提交变更，视频制作应优先放在独立目录，避免改动业务源码。
- 项目介绍事实优先来自 `窑创未来.md` 和真实页面截图，不应编造超出项目已实现范围的能力。

## Extension Points

- 可以通过 Playwright 或浏览器 MCP 截取本地 Vite 页面，作为 Remotion 的真实网页素材。
- Remotion 项目适合独立放置在 `motion-reel/` 或类似目录，静态截图放入其 `public/` 下并通过 `staticFile()` 引用。

## Common Pitfalls

- 不要把大型源码文件、图片或截图内容整体塞入模型上下文；只读取小片段、文件列表和必要元数据，避免超过请求体限制。
- 不要依赖线上域名一定可访问；本地源码可作为稳定的真实页面来源。
- 如果启动本地服务，端口可能被占用，应改用空闲端口。
