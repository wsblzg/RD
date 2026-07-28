# 竞赛智能客服机器人

基于 RAG（检索增强生成）技术的智能问答系统，支持 PDF 和 TXT 格式的文档检索和问答。

## 功能特点

-   支持 PDF 和 TXT 格式文档的自动加载和解析
-   基于语义向量的相似度检索
-   使用 FAISS 向量数据库高效存储和检索文本向量
-   智能分页处理 PDF 文档
-   实时日志记录
-   友好的 Web 界面
-   支持多文档源的上下文关联回答

## 技术栈

-   FastAPI: Web 框架
-   PyPDF2: PDF 文档解析
-   BGE-M3 / NVIDIA nv-embed-v1: 文本向量嵌入模型
-   Qwen / DeepSeek / NVIDIA NIM 兼容模型: 大语言模型
-   FAISS: 高性能向量检索库
-   Bootstrap: 前端 UI 框架

## 系统要求

-   Python 3.11.x
-   uv
-   足够的磁盘空间用于存储文档和日志
-   稳定的网络连接（用于 API 调用）

## 安装步骤

1. 克隆项目到本地：

```bash
git clone [项目地址]
cd [项目目录]
```

2. 使用 uv 按锁文件创建环境并安装依赖：

```bash
uv sync --frozen --no-dev
```

开发和测试环境使用：

```bash
uv sync --frozen
uv run --frozen pytest
```

4. 配置环境：

-   在项目根目录创建 `docs` 文件夹（如果不存在）
-   将需要检索的 PDF 或 TXT 文档放入 `docs` 文件夹
-   确保有正确的 API 密钥配置

## 使用方法

1. 启动服务：

```bash
HOST=127.0.0.1 PORT=17690 uv run --frozen --no-dev python main.py
```

NVIDIA NIM 演示模式建议使用以下环境变量。切换 embedding 模型后需要删除 `vector_db/faiss_index.bin` 和 `vector_db/metadata.pkl`，让系统重新生成索引。

```powershell
$env:API_URL="https://integrate.api.nvidia.com/v1"
$env:API_KEY="你的NVIDIA_API_KEY"
$env:EMBEDDING_MODEL="nvidia/nv-embed-v1"
$env:VECTOR_DIM="4096"
$env:ENABLE_RERANK="false"
$env:LLM_MODEL="qwen/qwen3-next-80b-a3b-instruct"
$env:LLM_FALLBACK_MODEL="deepseek-ai/deepseek-v4-flash"
$env:LLM_TIMEOUT_SECONDS="8"
$env:FALLBACK_LLM_TIMEOUT_SECONDS="25"
uv run --frozen --no-dev python main.py
```

2. 访问 Web 界面：

-   打开浏览器访问 `http://localhost:17690`
-   在搜索框中输入问题
-   系统会返回基于文档的相关回答

## 文件结构

```
project/
├── main.py            # 主程序文件
├── pyproject.toml     # uv 项目与依赖配置
├── uv.lock            # 锁定的可重复安装依赖
├── README.md         # 说明文档
├── docs/             # 文档目录
│   ├── *.pdf        # PDF文档
│   └── *.txt        # 文本文档
├── logs/            # 日志目录
├── vector_db/       # 向量数据库目录
│   ├── faiss_index.bin  # FAISS索引文件
│   └── metadata.pkl     # 文档元数据
└── templates/       # 前端模板
    └── index.html   # 主页面
```

## 日志系统

-   日志文件位于 `logs` 目录
-   文件名格式：`rag_YYYYMMDD.log`
-   记录内容包括：
    -   文档加载过程
    -   API 调用信息
    -   查询处理过程
    -   错误和异常信息

## 注意事项

1. PDF 文档处理：

    - 系统会自动按页面分割 PDF 文档
    - 每页内容单独编入索引
    - 空白页面会被自动跳过

2. API 使用：

    - 确保 API 密钥配置正确
    - 注意 API 调用频率限制
    - 监控 API 调用成本

3. 向量数据库：

    - 首次启动时会创建向量索引，可能需要较长时间
    - 索引文件保存在 `vector_db` 目录中
    - 添加新文档后系统会自动更新索引

4. 性能考虑：
    - 大型 PDF 文件可能需要较长处理时间
    - 建议将大文件分割成小文件
    - 定期清理日志文件

## 常见问题

1. PDF 解析失败

    - 检查 PDF 文件是否损坏
    - 确认 PDF 文件权限设置
    - 尝试重新保存 PDF 文件

2. API 调用错误

    - 验证 API 密钥是否有效
    - 检查网络连接
    - 查看日志文件获取详细错误信息

3. 系统响应慢
    - 检查文档大小和数量
    - 考虑增加服务器资源
    - 优化文档存储结构

## 维护和更新

-   定期检查并更新依赖包
-   监控日志文件大小
-   及时清理无用的文档
-   备份重要的配置和数据

## 贡献指南

欢迎提交问题和改进建议！

-   提交 Issue 报告问题
-   提交 Pull Request 贡献代码
-   分享使用经验和建议
