# MonkeyCode 单端口部署设计

## 目标

将现有 Vue 前端、Spring Boot 主后端和 FastAPI RAG 服务部署到 MonkeyCode
开发环境。现有阿里云 MySQL 继续运行，应用只向平台公开 Spring Boot 的
`8006` 端口，最终再将 `yaochuangfuture.cn` 反向代理到平台生成的项目地址。

## 约束

- MonkeyCode 是可回收的开发容器，不按传统 VPS 方式使用宝塔、UFW、SSH
  公网端口或多端口安全组。
- 平台是否支持自定义域名未知，因此域名切换不作为应用启动的前置条件。
- 平台网页单文件上传上限为 10MB，部署不得依赖网页上传完整项目或大文件。
- MySQL 保留在现有服务器，部署过程不迁移数据库。
- 仓库中已有未提交修改，部署改造不得覆盖或还原这些修改。
- 仓库当前存在明文数据库密码和第三方云服务密钥。上传前必须改为环境变量，
  已暴露的密钥必须在对应服务控制台轮换。

## 架构

对外只暴露 Spring Boot：

```text
平台公网地址
    |
    v
Spring Boot :8006
    |-- /、前端路由 -> Vue 构建产物
    |-- /api 等接口 -> Spring Boot 控制器
    `-- 127.0.0.1:17690 -> FastAPI RAG

Spring Boot -> 47.113.113.212:3306 MySQL
Spring Boot -> 阿里云 OSS 和第三方 AI API
```

Vue 不以 Vite 开发服务器对外运行。部署时执行生产构建，并将 `vue/dist`
复制到 Spring Boot 的 `src/main/resources/static` 对应构建输入位置。对于
Vue Router history 路由，Spring Boot 将非文件、非 API 的前端路径回退到
`index.html`。

RAG 服务只监听 `127.0.0.1:17690`，由 Spring Boot 内部调用，不生成第二个
公网地址。

## 配置与密钥

`application.yml` 只保留无敏感默认值。以下配置通过环境变量注入：

- MySQL URL、用户名和密码
- JWT 密钥
- 讯飞、NVIDIA、DeepSeek、腾讯云相关凭据
- 阿里云 OSS AccessKey
- 硅基流动或其他 RAG 模型 API 凭据
- 需要覆盖的 RAG 地址和超时

仓库提供不含真实值的环境变量示例文件。部署说明只列变量名，不记录真实
凭据。现有明文凭据视为已泄露，必须轮换后再用于部署。

## 项目传输

源码通过 Git 仓库拉取到 MonkeyCode，不使用网页逐文件上传。当前超过 10MB
的源文件是 `vue/public/2bc8bba6572a5ad71a07d94c6d477e86.mp4`；该视频迁移
到现有 OSS，由前端引用 HTTPS 地址。未跟踪的 `vue/dist.zip` 和本地
`vue/dist` 均视为可重建产物，不上传、不提交。

Git 仓库只包含源码、必要文档和小于平台限制的静态资源。构建产物在
MonkeyCode 内通过启动脚本重新生成。

## 构建与启动

使用一个仓库级启动脚本完成以下工作：

1. 校验 Java 17、Node.js、npm、uv 和必要环境变量。
2. 使用 `npm ci` 构建 Vue。
3. 将 Vue 构建产物交给 Spring Boot 静态资源目录。
4. 使用项目级 `.venv` 和 `uv` 同步 RAG 依赖。
5. 后台启动 RAG 服务并等待 `127.0.0.1:17690` 就绪。
6. 使用 Maven Wrapper；若仓库没有 Wrapper，则使用可用的 Maven 构建
   Spring Boot。
7. 前台启动 Spring Boot，使平台持续检测 `0.0.0.0:8006`。

启动脚本使用进程退出码和就绪检查快速失败，不引入 systemd、Supervisor、
宝塔或容器内防火墙。

## 数据库连接

现有 MySQL 必须满足：

- `47.113.113.212:3306` 可由 MonkeyCode 出口网络访问。
- `wyxm` 数据库账号只拥有目标数据库所需权限。
- MySQL 防火墙或安全组允许实际来源，不长期向所有公网地址开放 root 登录。
- 部署前执行一次只读连接检查；失败时不继续启动主应用。

本次不修改数据库结构，也不自动执行数据迁移脚本。

## 健康检查与错误处理

- RAG：使用其 HTTP 健康接口；如果项目没有现成健康接口，则以 TCP/HTTP
  根路径可连接作为最小就绪条件。
- Spring Boot：使用已有轻量接口；若没有，则只验证端口和首页响应。
- RAG 启动失败时主部署脚本退出，避免 AI 问答以半可用状态上线。
- Spring Boot 启动失败时保留标准输出日志，脚本返回非零状态。
- MySQL 不可达时明确输出连接失败，不尝试在 MonkeyCode 内安装 MySQL。

## 域名切换

应用启动成功后先使用 MonkeyCode 生成的 HTTPS 项目地址进行验收。确认首页、
API、登录、上传和 RAG 均正常后，再配置 `yaochuangfuture.cn`。

由于平台不支持直接绑定自定义域名，DNS A/CNAME 不能在没有平台 Host 和证书
支持的情况下直接替代源站。域名层采用支持 HTTPS 的反向代理服务，将请求
转发到平台项目地址。切换前降低原 DNS TTL，验证完成后再替换当前指向
`47.113.113.212` 的 A 记录。

## 验证范围

- Vue 首页和 history 路由刷新正常。
- 前端同域 API 请求成功，无 CORS 依赖。
- Spring Boot 能连接现有 MySQL，并完成只读查询和登录流程。
- 上传仍写入 OSS，不依赖容器本地持久化。
- 仓库内不存在超过 10MB 的已跟踪文件，平台部署不依赖网页大文件上传。
- Spring Boot 能调用本机 RAG 服务。
- 对外只需要一个平台项目地址和 `8006` 端口。
- 环境重启后可通过同一启动入口恢复全部应用进程。

## 不包含

- 不安装宝塔、UFW、FRP、systemd 服务或 MySQL。
- 不迁移现有数据库和 OSS 文件。
- 不上传 `vue/dist.zip` 或其他可重新生成的构建产物。
- 不承诺 MonkeyCode 的免费环境具备生产级 SLA 或永久存储。
- 不在本阶段切换 `yaochuangfuture.cn` DNS；域名切换在平台地址验收通过后执行。
