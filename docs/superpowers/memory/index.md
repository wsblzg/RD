---
type: index
title: repository-memory-index
summary: 窑创未来项目的首份仓库记忆索引，当前覆盖前端项目展示与视频制作所需入口。
tags:
  - repository-memory
  - ceramics-frontend
owned_paths:
  - vue/**
  - 窑创未来.md
related_docs:
  - docs/superpowers/memory/modules/ceramics-frontend-video-surface.md
last_verified_commit: ec81ddc
status: draft
---

# Repository Memory Index

## Covered Domains

- `vue/`：窑创未来前端，基于 Vite + Vue 3 + Element Plus，主要页面位于 `vue/src/views/ceramics/`。
- `窑创未来.md`：项目功能、页面结构和创新点的文字介绍，可作为项目介绍视频的旁白与字幕事实来源。

## Primary Docs

- [ceramics-frontend-video-surface](modules/ceramics-frontend-video-surface.md)：前端展示面、路由入口、素材边界和视频制作注意事项。

## Major Gaps

- 后端 `springboot/` 当前存在大量未提交变更，本轮未建立后端接口记忆。
- 管理端、商城、社区等新增功能尚未完成稳定性审计。
- 线上域名 `https://yaochuangfuture.cn/` 需要通过浏览器或截图工具确认运行状态，不能只依赖本地源码推断。
