# RAG 服务 Docker 部署指南

本文档提供了使用 Docker 部署 RAG 服务的详细说明。

## 前提条件

-   安装 Docker：[Docker 安装指南](https://docs.docker.com/get-docker/)
-   安装 Docker Compose：[Docker Compose 安装指南](https://docs.docker.com/compose/install/)

## 快速开始

1. 克隆仓库或下载项目文件到本地

2. 进入项目根目录

```bash
cd 项目目录
```

3. 使用 Docker Compose 构建并启动服务

```bash
docker-compose up -d
```

这将在后台启动 RAG 服务，服务将在端口 17690 上运行。

4. 查看日志

```bash
docker-compose logs -f
```

## 配置说明

### 环境变量

您可以在`docker-compose.yml`文件中修改以下环境变量：

-   `API_URL`: API 服务的 URL 地址
-   `API_KEY`: API 密钥
-   `EMBEDDING_MODEL`: 嵌入模型名称
-   `LLM_MODEL`: 大语言模型名称
-   `RERANKER_MODEL`: 重排序模型名称
-   `TEMPERATURE`: 生成温度参数
-   `HOST`: 服务监听的主机地址（默认为 0.0.0.0）
-   `PORT`: 服务监听的端口（默认为 17690）

### 数据持久化

以下目录通过 Docker 卷挂载实现数据持久化：

-   `./logs:/app/logs`: 日志文件
-   `./docs:/app/docs`: 文档文件
-   `./vector_db:/app/vector_db`: 向量数据库
-   `./models:/app/models`: 模型文件

## 常用命令

### 启动服务

```bash
docker-compose up -d
```

### 停止服务

```bash
docker-compose down
```

### 重建并启动服务

```bash
docker-compose up -d --build
```

### 查看服务状态

```bash
docker-compose ps
```

### 查看服务日志

```bash
docker-compose logs -f
```

## 访问服务

服务启动后，可通过以下 URL 访问：

-   Web 界面：http://localhost:17690
-   API 文档：http://localhost:17690/docs

## 故障排除

1. 如果服务无法启动，请检查日志：

```bash
docker-compose logs -f
```

2. 确保端口 17690 未被其他应用占用。如需更改端口，请修改`docker-compose.yml`文件中的端口映射。

3. 如果遇到权限问题，可能需要确保挂载的目录有正确的权限：

```bash
sudo chown -R 1000:1000 ./logs ./docs ./vector_db ./models
```
