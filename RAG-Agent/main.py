from fastapi import FastAPI, Request, Form
from fastapi.templating import Jinja2Templates
from fastapi.staticfiles import StaticFiles
from fastapi.responses import JSONResponse, StreamingResponse
import uvicorn
from typing import List, Dict, Union, Optional
import numpy as np
from pathlib import Path
import requests
import time
import logging
from datetime import datetime
import re
import faiss
import pickle
import os
from fastapi import UploadFile, File, Form, BackgroundTasks
import shutil
import aiohttp
import json
from concurrent.futures import FIRST_COMPLETED, ThreadPoolExecutor, wait

BASE_DIR = Path(__file__).resolve().parent

# 配置日志
log_dir = BASE_DIR / "logs"
log_dir.mkdir(exist_ok=True)
log_file = log_dir / f"rag_{datetime.now().strftime('%Y%m%d')}.log"

# 配置日志格式
log_format = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')

# 创建logger实例
logger = logging.getLogger('RAG_Service')
logger.setLevel(logging.INFO)

# 创建文件处理器
file_handler = logging.FileHandler(log_file, encoding='utf-8', mode='a')
file_handler.setFormatter(log_format)
file_handler.setLevel(logging.INFO)

# 创建控制台处理器
console_handler = logging.StreamHandler()
console_handler.setFormatter(log_format)
console_handler.setLevel(logging.INFO)

# 添加处理器到logger
logger.addHandler(file_handler)
logger.addHandler(console_handler)

# 确保所有级别的日志都会被处理
logger.propagate = False

# 配置模型目录
model_dir = BASE_DIR / "models"
model_dir.mkdir(exist_ok=True)

# 添加初始日志
logger.info("="*50)
logger.info("RAG服务启动")
logger.info(f"日志文件路径: {log_file}")
logger.info(f"模型目录路径: {model_dir}")
logger.info("="*50)

# 从环境变量读取配置。API_KEY 不写默认值，避免密钥进入代码仓库。
API_URL = os.environ.get("API_URL") or os.environ.get("NVIDIA_API_URL", "https://integrate.api.nvidia.com/v1")
API_KEY = os.environ.get("API_KEY") or os.environ.get("NVIDIA_API_KEY", "")
LLM_API_URL = (os.environ.get("LLM_API_URL") or os.environ.get("DEEPSEEK_API_URL") or API_URL).rstrip("/")
LLM_API_KEY = os.environ.get("LLM_API_KEY") or os.environ.get("DEEPSEEK_API_KEY") or API_KEY
if not API_KEY:
    logger.warning("未配置 API_KEY，RAG 服务可启动，但嵌入和重排会失败。")
if not LLM_API_KEY:
    logger.warning("未配置 LLM_API_KEY/API_KEY，大模型调用会失败。")

EMBEDDING_MODEL = os.environ.get("EMBEDDING_MODEL", "nvidia/nv-embed-v1")
LLM_MODEL = os.environ.get("LLM_MODEL", "deepseek-chat")
LLM_FALLBACK_MODEL = os.environ.get("LLM_FALLBACK_MODEL", "")
RERANKER_MODEL = os.environ.get("RERANKER_MODEL", "nvidia/nv-rerankqa-mistral-4b-v3")
LLM_THINKING_TYPE = os.environ.get("LLM_THINKING_TYPE", "").strip()

def parse_timeout_env(name: str, default_value: str) -> Optional[int]:
    value = os.environ.get(name, default_value).strip()
    try:
        timeout_seconds = int(value)
    except ValueError:
        logger.warning(f"{name} 配置无效: {value}，使用默认值 {default_value}")
        timeout_seconds = int(default_value)
    return None if timeout_seconds <= 0 else timeout_seconds


def format_timeout(timeout_seconds: Optional[int]) -> str:
    return "不限" if timeout_seconds is None else f"{timeout_seconds}s"


TEMPERATURE = float(os.environ.get("TEMPERATURE", "0.35"))
LLM_TIMEOUT_SECONDS = parse_timeout_env("LLM_TIMEOUT_SECONDS", "8")
FALLBACK_LLM_TIMEOUT_SECONDS = parse_timeout_env("FALLBACK_LLM_TIMEOUT_SECONDS", "25")
EMBEDDING_TIMEOUT_SECONDS = int(os.environ.get("EMBEDDING_TIMEOUT_SECONDS", "30"))
RERANK_TIMEOUT_SECONDS = int(os.environ.get("RERANK_TIMEOUT_SECONDS", "20"))
ENABLE_RERANK = os.environ.get("ENABLE_RERANK", "false").lower() == "true"
ENABLE_LLM_RACE = os.environ.get("ENABLE_LLM_RACE", "true").lower() == "true"
RAG_FINAL_TOP_K = int(os.environ.get("RAG_FINAL_TOP_K", "3"))
RAG_MAX_DOC_CHARS = int(os.environ.get("RAG_MAX_DOC_CHARS", "900"))
LLM_MAX_TOKENS = int(os.environ.get("LLM_MAX_TOKENS", "512"))

# 文本处理配置
MAX_SECTION_LENGTH = 2048  # 每个文本段的最大长度（调整为更小的值，便于精确切分）
OVERLAP_LENGTH = 512       # 重叠长度（增加重叠，保证上下文连贯性）
MIN_SECTION_LENGTH = 128   # 最小段落长度（保持不变）
MAX_WINDOW_SIZE = 5        # 最大滑动窗口的句子数

# 向量数据库相关常量
VECTOR_DIM = int(os.environ.get("VECTOR_DIM", "4096"))  # nvidia/nv-embed-v1 默认维度
VECTOR_DB_DIR = BASE_DIR / "vector_db"  # 向量数据库目录
INDEX_PATH = VECTOR_DB_DIR / "faiss_index.bin"  # 索引文件路径
METADATA_PATH = VECTOR_DB_DIR / "metadata.pkl"  # 元数据文件路径

# 确保向量数据库目录存在
VECTOR_DB_DIR.mkdir(exist_ok=True)

# 全局变量
index = None  # FAISS索引
metadata = []  # 文档元数据
document_info = {}

app = FastAPI()
templates = Jinja2Templates(directory=str(BASE_DIR / "templates"))
app.mount("/static", StaticFiles(directory=str(BASE_DIR / "static")), name="static")

def clean_text(text: str) -> str:
    """清理文本，去除多余的空白字符和特殊格式"""
    # 替换多个空格为单个空格
    text = re.sub(r'\s+', ' ', text)
    # 去除特殊字符，但保留基本标点
    text = re.sub(r'[^\w\s\u4e00-\u9fff,.!?;:，。！？；：]', '', text)
    return text.strip()

def process_file_as_single_chunk(file_path: Path, content: str) -> dict:
    """将整个文件作为单个段落处理，不进行分段
    
    Args:
        file_path: 文件路径
        content: 文件内容
        
    Returns:
        dict: 包含处理后的内容和元数据
    """
    # 清理文本
    cleaned_content = clean_text(content)
    if not cleaned_content:
        return None
        
    # 创建文档元数据
    doc_metadata = {
        "content": cleaned_content,
        "context": f"文档来源：{file_path}",
        "path": str(file_path),
        "position_info": "完整文档"
    }
    
    return doc_metadata

def process_json_file(file_path: Path, json_data: dict) -> List[dict]:
    """处理JSON文件，提取问答对并生成文档片段
    
    Args:
        file_path: 文件路径
        json_data: 解析后的JSON数据
        
    Returns:
        List[dict]: 包含处理后的文档片段列表
    """
    results = []
    
    try:
        # 检查是否包含问答列表
        if "list" in json_data and isinstance(json_data["list"], list):
            items = json_data["list"]
            logger.info(f"处理JSON文件: {file_path}，包含 {len(items)} 个问答项")
            
            # 将整个JSON的问答列表作为一个整体处理
            all_qa_content = []
            
            for item in items:
                # 确保必要的字段存在
                if "question" in item and "answer" in item and item.get("isUsing", True):
                    question = item["question"]
                    answer = item["answer"]
                    item_type = item.get("type", "未分类")
                    item_id = item.get("id", "unknown")
                    
                    # 清理文本
                    clean_question = clean_text(question)
                    clean_answer = clean_text(answer)
                    
                    # 添加到整体内容中
                    qa_pair = f"问题：{clean_question}\n答案：{clean_answer}\n\n"
                    all_qa_content.append(qa_pair)
            
            # 如果有内容，创建整体文档
            if all_qa_content:
                combined_content = "".join(all_qa_content).strip()
                
                # 创建文档元数据
                doc_metadata = {
                    "content": combined_content,
                    "context": f"文档类型：JSON问答合集, 来源：{file_path}, 包含问答对数量：{len(all_qa_content)}",
                    "path": f"{file_path}#combined",
                    "position_info": f"完整问答集，共{len(all_qa_content)}个问答对"
                }
                results.append(doc_metadata)
                logger.info(f"JSON文件 {file_path} 的全部问答项已合并为单个文档")
        else:
            # 如果不是问答格式，将整个JSON作为一个文档
            json_content = json.dumps(json_data, ensure_ascii=False, indent=2)
            doc_metadata = process_file_as_single_chunk(file_path, json_content)
            if doc_metadata:
                results.append(doc_metadata)
    
    except Exception as e:
        logger.error(f"处理JSON文件 {file_path} 失败: {str(e)}")
    
    return results

def split_text_with_overlap(text: str, min_length: int = 256) -> List[Dict[str, Union[str, int]]]:
    """
    使用滑动窗口机制按句切分文本，提高语义连贯性
    
    Args:
        text: 要分段的文本
        min_length: 最小段落长度，默认256字符
        
    Returns:
        List[Dict[str, Union[str, int]]]: 包含文本段落及其位置信息的列表
        格式: [{"content": "文本内容", "start_char": 起始位置, "end_char": 结束位置}]
    """
    # 清理文本
    text = clean_text(text)
    if not text:
        return []
    
    # 如果文本长度小于最小分段长度，直接返回整个文本
    if len(text) <= min_length:
        return [{"content": text, "start_char": 0, "end_char": len(text)}]
    
    # 按句子分割文本
    # 中文句子结束符: 。！？；
    # 英文句子结束符: .!?;
    sentence_ends = re.finditer(r'[。！？；.!?;]+', text)
    sentence_boundaries = [0]  # 第一个句子从0开始
    
    # 收集所有句子边界
    for match in sentence_ends:
        sentence_boundaries.append(match.end())
    
    # 确保最后一个字符也被包含
    if sentence_boundaries[-1] != len(text):
        sentence_boundaries.append(len(text))
    
    segments = []
    start_idx = 0
    current_length = 0
    
    # 基于句子边界构建分段
    for i in range(1, len(sentence_boundaries)):
        current_length += sentence_boundaries[i] - sentence_boundaries[i-1]
        
        # 判断是否达到最小长度
        if current_length >= min_length:
            # 提取分段内容
            segment_text = text[sentence_boundaries[start_idx]:sentence_boundaries[i]]
            segments.append({
                "content": segment_text.strip(),
                "start_char": sentence_boundaries[start_idx],
                "end_char": sentence_boundaries[i]
            })
            
            # 更新下一个分段的开始位置
            # 将起始位置设为当前边界的1/3位置，确保有足够的重叠
            overlap_idx = max(start_idx, i - max(1, (i - start_idx) // 3))
            start_idx = overlap_idx
            current_length = sentence_boundaries[i] - sentence_boundaries[start_idx]
    
    # 处理最后一段（如果还有剩余内容）
    if start_idx < len(sentence_boundaries) - 1:
        remaining_text = text[sentence_boundaries[start_idx]:]
        if len(remaining_text.strip()) > 0:
            segments.append({
                "content": remaining_text.strip(),
                "start_char": sentence_boundaries[start_idx],
                "end_char": len(text)
            })
    
    # 如果没有生成任何分段，则返回整个文本
    if not segments:
        return [{"content": text, "start_char": 0, "end_char": len(text)}]
    
    return segments

def get_embeddings(text: str) -> np.ndarray:
    """将文本转换为嵌入向量，使用API而不是langchain"""
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }
    
    data = {
        "model": EMBEDDING_MODEL,
        "input": text
    }
    
    max_retries = 3
    for attempt in range(max_retries):
        try:
            response = requests.post(
                f"{API_URL}/embeddings",
                headers=headers,
                json=data,
                timeout=EMBEDDING_TIMEOUT_SECONDS
            )
            response.raise_for_status()
            embedding = np.array(response.json()["data"][0]["embedding"])
            return embedding
        except Exception as e:
            logger.error(f"获取嵌入向量失败 (尝试 {attempt + 1}/{max_retries}): {str(e)}")
            if attempt == max_retries - 1:
                raise e
            time.sleep(1)

def get_semantic_similarity(query: str, text: str) -> float:
    """计算查询和文本之间的语义相似度"""
    # 获取查询和文本的嵌入向量
    query_embedding = get_embeddings(query)
    text_embedding = get_embeddings(text)
    
    # 计算余弦相似度
    similarity = np.dot(query_embedding, text_embedding) / (
        np.linalg.norm(query_embedding) * np.linalg.norm(text_embedding)
    )
    
    return float(similarity)

def build_llm_prompt(query: str, context: str, rule: str) -> str:
    return f"""请基于以下参考文档回答用户的问题。如果无法从参考文档中找到答案，请明确说明无法回答。
请直接进入答案正文，不要使用“根据参考文档”“根据参考资料”“基于检索资料”等机械化开头。

参考文档：
{context}

用户问题：{query}

{f"回答风格：{rule}" if rule else ""}

"""


def clean_llm_answer(answer: str) -> str:
    normalized = (answer or "").strip()
    return re.sub(
        r"^(?:(?:根据|基于)(?:以上|上述)?(?:参考文档|参考资料|检索资料|检索到的资料)(?:内容)?[，,:：]?\s*)+",
        "",
        normalized
    ).strip()


def truncate_doc_text(text: str, max_chars: int = RAG_MAX_DOC_CHARS) -> str:
    normalized = clean_text(text or "")
    if len(normalized) <= max_chars:
        return normalized
    return normalized[:max_chars].rstrip() + "..."


def build_context_from_docs(docs: List[Dict]) -> str:
    return "\n\n".join([
        (
            f"文档 ({doc['path']}):\n"
            f"上下文信息:\n{truncate_doc_text(doc.get('context', ''), 220)}\n"
            f"当前内容:\n{truncate_doc_text(doc.get('content', ''), RAG_MAX_DOC_CHARS)}"
        )
        for doc in docs
    ])


def call_llm_model(model: str, prompt: str, timeout_seconds: Optional[int], headers: Dict[str, str]) -> str:
    data = {
        "model": model,
        "messages": [
            {"role": "user", "content": prompt}
        ],
        "temperature": TEMPERATURE,
        "max_tokens": LLM_MAX_TOKENS
    }
    if LLM_THINKING_TYPE:
        data["thinking"] = {"type": LLM_THINKING_TYPE}

    logger.info(f"正在调用LLM API，模型: {model}, timeout={format_timeout(timeout_seconds)}")
    response = requests.post(
        f"{LLM_API_URL}/chat/completions",
        headers=headers,
        json=data,
        timeout=timeout_seconds
    )
    response.raise_for_status()
    response_content = response.json()["choices"][0]["message"]["content"]
    if not response_content or not response_content.strip():
        raise RuntimeError("LLM返回内容为空")
    logger.info(f"成功获取LLM回答，模型: {model}")
    logger.info("LLM回答:")
    logger.info("-"*50)
    logger.info(response_content)
    logger.info("-"*50)
    return response_content


def get_llm_response_sequential(prompt: str, models_to_try: List[tuple], headers: Dict[str, str]) -> str:
    last_error = None
    for model, timeout_seconds in models_to_try:
        try:
            return call_llm_model(model, prompt, timeout_seconds, headers)
        except Exception as e:
            last_error = e
            logger.error(f"调用LLM API失败，模型: {model}, 错误: {str(e)}")

    raise last_error


def get_llm_response_race(prompt: str, models_to_try: List[tuple], headers: Dict[str, str]) -> str:
    if len(models_to_try) <= 1:
        return get_llm_response_sequential(prompt, models_to_try, headers)

    logger.info(f"启用LLM并发竞速，模型列表: {', '.join([item[0] for item in models_to_try])}")
    errors = []
    executor = ThreadPoolExecutor(max_workers=len(models_to_try))
    future_to_model = {
        executor.submit(call_llm_model, model, prompt, timeout_seconds, headers): (model, timeout_seconds)
        for model, timeout_seconds in models_to_try
    }
    pending = set(future_to_model.keys())

    while pending:
        done, pending = wait(pending, return_when=FIRST_COMPLETED)
        for future in done:
            model, _ = future_to_model[future]
            try:
                result = future.result()
                for pending_future in pending:
                    pending_model, _ = future_to_model[pending_future]
                    if pending_future.cancel():
                        logger.info(f"已取消较慢LLM请求，模型: {pending_model}")
                    else:
                        logger.info(f"较慢LLM请求已发出，将不等待其完成，模型: {pending_model}")
                executor.shutdown(wait=False, cancel_futures=True)
                return result
            except Exception as e:
                errors.append(e)
                logger.error(f"调用LLM API失败，模型: {model}, 错误: {str(e)}")

    executor.shutdown(wait=False, cancel_futures=True)

    if errors:
        raise errors[-1]
    raise RuntimeError("LLM并发竞速失败：无可用模型")


def get_llm_response(query: str, context: str, rule: str) -> str:
    headers = {
        "Authorization": f"Bearer {LLM_API_KEY}",
        "Content-Type": "application/json"
    }
    
    prompt = build_llm_prompt(query, context, rule)
    
    # 记录完整的prompt
    logger.info("发送到LLM的完整Prompt:")
    logger.info("="*50)
    logger.info(prompt)
    logger.info("="*50)

    models_to_try = [(LLM_MODEL, LLM_TIMEOUT_SECONDS)]
    if LLM_FALLBACK_MODEL and LLM_FALLBACK_MODEL != LLM_MODEL:
        models_to_try.append((LLM_FALLBACK_MODEL, FALLBACK_LLM_TIMEOUT_SECONDS))

    if ENABLE_LLM_RACE:
        return get_llm_response_race(prompt, models_to_try, headers)
    return get_llm_response_sequential(prompt, models_to_try, headers)

async def get_llm_response_stream(query: str, context: str, rule: str):
    """
    使用流式API获取LLM响应，实时返回生成内容
    """
    headers = {
        "Authorization": f"Bearer {LLM_API_KEY}",
        "Content-Type": "application/json"
    }
    
    prompt = f"""
    参考文档：
    {context}

    用户问题：{query}
    """
    
    # 记录完整的prompt
    logger.info("发送到LLM的流式API Prompt:")
    logger.info("="*50)
    logger.info(prompt)
    logger.info("="*50)
    
    models_to_try = [(LLM_MODEL, LLM_TIMEOUT_SECONDS)]
    if LLM_FALLBACK_MODEL and LLM_FALLBACK_MODEL != LLM_MODEL:
        models_to_try.append((LLM_FALLBACK_MODEL, FALLBACK_LLM_TIMEOUT_SECONDS))

    for model, timeout_seconds in models_to_try:
        data = {
            "model": model,
            "messages": [
                {"role": "system", "content": "你是柴烧非遗数字传承平台的知识问答助手。请基于参考文档回答，内容要准确、克制、中文表达，不要替代专家鉴定。请直接进入答案正文，不要使用“根据参考文档”“根据参考资料”“基于检索资料”等机械化开头。"},
                {"role": "user", "content": prompt}
            ],
            "temperature": TEMPERATURE,
            "max_tokens": 2048,
            "stream": True
        }
        if LLM_THINKING_TYPE:
            data["thinking"] = {"type": LLM_THINKING_TYPE}

        try:
            logger.info(f"正在调用流式LLM API，模型: {model}, timeout={format_timeout(timeout_seconds)}")
            
            timeout = aiohttp.ClientTimeout(total=timeout_seconds)
            async with aiohttp.ClientSession(timeout=timeout) as session:
                async with session.post(
                    f"{LLM_API_URL}/chat/completions",
                    headers=headers,
                    json=data
                ) as response:
                    response.raise_for_status()
                    
                    # 读取流式响应
                    full_response = ""
                    async for line in response.content:
                        if line:
                            line_text = line.decode('utf-8').strip()
                            # 跳过空行
                            if not line_text:
                                continue
                                
                            # 处理SSE格式
                            if line_text.startswith('data:'):
                                data_json = line_text[5:].strip()
                                if data_json == "[DONE]":
                                    break
                                    
                                try:
                                    chunk = json.loads(data_json)
                                    if (
                                        chunk 
                                        and "choices" in chunk 
                                        and len(chunk["choices"]) > 0
                                        and "delta" in chunk["choices"][0]
                                        and "content" in chunk["choices"][0]["delta"]
                                    ):
                                        content = chunk["choices"][0]["delta"]["content"]
                                        full_response += content
                                        yield content
                                except json.JSONDecodeError:
                                    logger.error(f"JSON解析错误: {data_json}")
                                    continue
            
            logger.info("流式响应完成")
            logger.info("完整响应内容:")
            logger.info("-"*50)
            logger.info(full_response)
            logger.info("-"*50)
            return
            
        except Exception as e:
            logger.error(f"流式API调用失败，模型: {model}, 错误: {str(e)}")

    yield "抱歉，响应生成出现问题，请稍后再试。"

def load_documents():
    global index, metadata, document_info
    docs_dir = BASE_DIR / "docs"
    documents = []
    logger.info(f"开始加载文档从目录: {docs_dir}")
    
    # 确保向量数据库目录存在
    VECTOR_DB_DIR.mkdir(exist_ok=True)
    
    # 初始化文档信息字典
    document_info = {}
    
    # 检查是否存在向量索引
    if os.path.exists(INDEX_PATH) and os.path.exists(METADATA_PATH):
        logger.info("找到现有向量索引，正在加载...")
        try:
            # 加载FAISS索引
            index = faiss.read_index(str(INDEX_PATH))
            # 加载元数据
            with open(METADATA_PATH, 'rb') as f:
                metadata = pickle.load(f)
            
            # 统计文档信息
            for doc in metadata:
                path = doc["path"].split("#")[0] if "#" in doc["path"] else doc["path"]
                if path not in document_info:
                    document_info[path] = {"sections": 0, "last_updated": os.path.getmtime(path) if os.path.exists(path) else "未知"}
                document_info[path]["sections"] += 1
                
            logger.info(f"成功从向量数据库加载 {len(metadata)} 个文档")
            return metadata
        except Exception as e:
            logger.error(f"加载向量数据库失败: {str(e)}")
            logger.info("将重新建立索引...")
            # 清空元数据，以便重新构建
            metadata = []
    else:
        logger.info("未发现向量索引，将创建新索引...")
        
    # 创建新的FAISS索引
    index = faiss.IndexFlatIP(VECTOR_DIM)  # 使用内积（余弦相似度）
    
    # 处理文档文件
    for file_path in docs_dir.glob("**/*.*"):
        try:
            # 根据文件扩展名处理不同类型的文件
            file_extension = file_path.suffix.lower()
            
            if file_extension == ".txt":
                # 处理文本文件
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()
                    logger.info(f"正在处理文本文件: {file_path}")
                    
                    # 将整个文件作为一个段落处理
                    doc_metadata = process_file_as_single_chunk(file_path, content)
                    
                    if doc_metadata:
                        # 获取嵌入向量
                        full_content = f"{doc_metadata['context']}\n\n当前内容：{doc_metadata['content']}"
                        embedding = get_embeddings(full_content)
                        
                        # 存储文档元数据
                        metadata.append(doc_metadata)
                        
                        # 存储嵌入向量并添加到索引
                        embedding_array = np.array([embedding]).astype('float32')
                        index.add(embedding_array)
                        
                        # 添加文档信息
                        document_info[str(file_path)] = {
                            "sections": 1,
                            "last_updated": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                        }
                        
                        # 保存索引和元数据
                        try:
                            logger.info(f"保存向量索引和元数据，当前文档: {file_path}")
                            faiss.write_index(index, str(INDEX_PATH))
                            with open(METADATA_PATH, 'wb') as f:
                                pickle.dump(metadata, f)
                            logger.info(f"成功保存向量索引，当前包含 {len(metadata)} 个文档")
                        except Exception as e:
                            logger.error(f"保存向量索引失败: {str(e)}")
                    else:
                        logger.warning(f"文件 {file_path} 处理后内容为空，已跳过")
                
            elif file_extension == ".json":
                # 处理JSON文件
                with open(file_path, "r", encoding="utf-8") as f:
                    try:
                        json_data = json.loads(f.read())
                        logger.info(f"正在处理JSON文件: {file_path}")
                        
                        # 处理JSON文件
                        doc_metadata_list = process_json_file(file_path, json_data)
                        
                        if doc_metadata_list:
                            # 添加嵌入向量
                            embeddings = []
                            for doc_metadata in doc_metadata_list:
                                # 获取嵌入向量
                                full_content = f"{doc_metadata['context']}\n\n当前内容：{doc_metadata['content']}"
                                embedding = get_embeddings(full_content)
                                embeddings.append(embedding)
                                
                                # 存储文档元数据
                                metadata.append(doc_metadata)
                            
                            # 将嵌入向量添加到索引
                            if embeddings:
                                embeddings_array = np.array(embeddings).astype('float32')
                                index.add(embeddings_array)
                        
                            # 更新文档信息
                            document_info[str(file_path)] = {
                                "sections": len(doc_metadata_list),
                                "last_updated": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                            }
                        
                            # 保存索引和元数据
                            faiss.write_index(index, str(INDEX_PATH))
                            with open(METADATA_PATH, 'wb') as f:
                                pickle.dump(metadata, f)
                        
                            logger.info(f"成功处理JSON文件 {file_path}，添加了 {len(doc_metadata_list)} 个文档")
                        else:
                            logger.warning(f"JSON文件 {file_path} 处理后没有有效内容，已跳过")
                    
                    except json.JSONDecodeError as e:
                        logger.error(f"解析JSON文件失败 {file_path}: {str(e)}")
                
        except Exception as e:
            logger.error(f"处理文件失败 {file_path}: {str(e)}")
    
    logger.info(f"文档处理完成，共添加 {len(metadata)} 个文档到向量索引")
    return metadata

# 初始化文档。健康检查和单元测试可关闭启动建索引，生产默认开启。
if os.environ.get("RAG_BUILD_INDEX_ON_STARTUP", "true").lower() == "true":
    try:
        documents = load_documents()
        logger.info(f"成功加载 {len(documents)} 个文档段落")
    except Exception as e:
        logger.error(f"加载文档时出错: {str(e)}")
        documents = []
        metadata = []

        VECTOR_DB_DIR.mkdir(exist_ok=True)
        index = faiss.IndexFlatIP(VECTOR_DIM)

        try:
            logger.info("创建并保存空向量索引")
            faiss.write_index(index, str(INDEX_PATH))
            with open(METADATA_PATH, 'wb') as f:
                pickle.dump(metadata, f)
            logger.info("成功创建空向量索引")
        except Exception as save_error:
            logger.error(f"保存空向量索引失败: {str(save_error)}")
else:
    logger.info("已跳过启动阶段的知识库索引初始化")
    documents = []
    metadata = []
    index = faiss.IndexFlatIP(VECTOR_DIM)

def get_rerank_scores(query: str, documents: List[Dict]) -> List[float]:
    """
    使用重排序模型对文档进行重新排序
    返回每个文档的相关性得分
    """
    headers = {
        "Authorization": f"Bearer {API_KEY}",
        "Content-Type": "application/json"
    }
    
    # 构建文档列表
    doc_texts = []
    for doc in documents:
        # 构建文本，包含上下文信息
        text = f"上下文信息:\n{doc['context']}\n当前内容:\n{doc['content']}"
        doc_texts.append(text)
    
    data = {
        "model": RERANKER_MODEL,
        "query": query,
        "documents": doc_texts,
        "top_n": len(documents),  # 返回所有文档的分数
        "return_documents": False,  # 只返回分数
        "max_chunks_per_doc": 1024,
        "overlap_tokens": 80
    }
    
    max_retries = 3
    for attempt in range(max_retries):
        try:
            logger.info(f"正在使用重排序模型评分，文档数量: {len(documents)}")
            response = requests.post(
                f"{API_URL}/rerank",
                headers=headers,
                json=data,
                timeout=RERANK_TIMEOUT_SECONDS
            )
            response.raise_for_status()
            
            # 解析API返回结果
            response_data = response.json()
            # 提取相关性得分并按原始顺序返回
            scores = []
            for result in response_data["results"]:
                scores.append(result["relevance_score"])
            
            # 记录token使用情况
            if "tokens" in response_data:
                logger.info(f"重排序token使用: 输入={response_data['tokens']['input_tokens']}, 输出={response_data['tokens']['output_tokens']}")
            
            logger.info(f"重排序评分完成，分数范围: {min(scores):.4f} - {max(scores):.4f}")
            return scores
        except Exception as e:
            logger.error(f"重排序评分失败 (尝试 {attempt + 1}/{max_retries}): {str(e)}")
            if attempt == max_retries - 1:
                raise e
            time.sleep(1)

def find_relevant_documents(query: str, initial_top_k: int = 25, final_top_k: int = 3):
    """
    两阶段检索：先使用向量检索获取候选文档，然后使用重排序模型进行精排
    """
    global index, metadata
    logger.info(f"搜索相关文档，查询: {query}")
    
    # 检查索引和元数据是否为空
    if index is None or index.ntotal == 0 or not metadata:
        logger.warning("向量索引为空或没有文档，无法执行检索")
        return []
    
    # 第一阶段：向量检索
    query_embedding = get_embeddings(query)
    query_embedding = np.array([query_embedding]).astype('float32')
    
    # 限制初始检索数量，不超过索引中的实际文档数
    actual_initial_top_k = min(initial_top_k, index.ntotal)
    
    # 使用FAISS进行搜索
    D, I = index.search(query_embedding, actual_initial_top_k)
    
    # 获取搜索结果的索引，转换为文档
    initial_docs = [metadata[idx] for idx in I[0] if idx < len(metadata)]
    
    logger.info(f"向量检索找到 {len(initial_docs)} 个候选文档")
    
    if not initial_docs:
        logger.warning("没有找到相关文档")
        return []
    
    # 计算来自JSON的文档数量
    json_doc_count = 0
    for doc in initial_docs:
        # 判断文档是否来自JSON (通过路径中包含"#"或上下文中包含"JSON问答")
        if "#" in doc["path"] or "JSON问答" in doc.get("context", ""):
            json_doc_count += 1
    
    logger.info(f"候选文档中有 {json_doc_count} 个来自JSON文件")
    
    # 动态计算重排序数量：JSON文档数 + 5
    dynamic_final_top_k = json_doc_count + 5
    # 确保最终返回的文档数量不少于原始设定的final_top_k
    dynamic_final_top_k = max(dynamic_final_top_k, final_top_k)
    
    logger.info(f"动态计算的重排序数量: JSON文档数({json_doc_count}) + 5 = {dynamic_final_top_k}")
    
    if not ENABLE_RERANK:
        actual_final_top_k = min(dynamic_final_top_k, len(initial_docs))
        logger.info(f"已关闭重排序，返回向量检索前 {actual_final_top_k} 个结果")
        return initial_docs[:actual_final_top_k]

    # 第二阶段：重排序
    try:
        rerank_scores = get_rerank_scores(query, initial_docs)
        
        # 将重排序分数与文档组合并排序
        scored_docs = list(zip(rerank_scores, initial_docs))
        scored_docs.sort(reverse=True)  # 按重排序分数降序排序
        
        # 限制最终返回数量，不超过候选文档数
        actual_final_top_k = min(dynamic_final_top_k, len(scored_docs))
        final_docs = [doc for _, doc in scored_docs[:actual_final_top_k]]
        
        logger.info(f"重排序后选择 {len(final_docs)} 个最相关文档:")
        for score, doc in zip(rerank_scores[:actual_final_top_k], final_docs):
            logger.info(f"- {doc['path']} (相关性得分: {score:.4f}, 相关段落: {doc['content'][:100]}...)")
        
        return final_docs
    except Exception as e:
        logger.error(f"重排序失败，回退到向量检索结果: {str(e)}")
        # 限制回退结果数量
        actual_final_top_k = min(dynamic_final_top_k, len(initial_docs))
        return initial_docs[:actual_final_top_k]

# 处理单个文件的函数（用于知识库更新）
def process_file(file_path: Path, update_mode: str = "add") -> Dict:
    """
    处理单个文件并更新向量数据库
    update_mode: "add" (添加新文件) 或 "replace" (替换同名文件)
    返回处理结果信息
    """
    global index, metadata, document_info
    
    result = {"status": "success", "message": "", "sections_added": 0}
    
    try:
        file_str_path = str(file_path)
        file_extension = file_path.suffix.lower()
        
        # 如果是替换模式，先删除已有文件的向量
        if update_mode == "replace":
            result["message"] += f"替换文件: {file_path}.\n"
            # 创建新的索引和元数据列表
            new_index = faiss.IndexFlatIP(VECTOR_DIM)
            new_metadata = []
            
            # 标记已存在的同名文件
            file_exists = False
            
            # 遍历现有元数据
            vectors_to_keep = []
            for idx, doc in enumerate(metadata):
                doc_path = doc["path"].split("#")[0] if "#" in doc["path"] else doc["path"]
                if doc_path != file_str_path:
                    # 保留其他文件的元数据
                    new_metadata.append(doc)
                    vectors_to_keep.append(idx)
                else:
                    file_exists = True
            
            if file_exists:
                # 从原索引中提取要保留的向量
                if vectors_to_keep:
                    if len(vectors_to_keep) < index.ntotal:
                        # 创建选择器
                        selector = faiss.IDSelectorBatch(len(vectors_to_keep), np.array(vectors_to_keep, dtype=np.int64))
                        # 提取指定索引的向量
                        vectors = faiss.extract_index_vectors(index)[1]
                        selected_vectors = vectors.subset(selector)
                        # 将保留的向量添加到新索引
                        new_index.add(selected_vectors)
                    else:
                        # 如果没有要删除的向量，直接复制原索引
                        new_index = faiss.clone_index(index)
                
                # 更新全局变量
                index = new_index
                metadata = new_metadata
                
                # 更新文档信息字典
                if file_str_path in document_info:
                    del document_info[file_str_path]
                
                result["message"] += f"已删除旧文件 {file_path} 的内容.\n"
            else:
                result["message"] += f"未找到文件 {file_path} 的旧版本.\n"
        
        # 根据文件类型处理
        if file_extension == ".txt":
            # 处理文本文件
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
                
                # 将整个文件作为一个段落处理
                doc_metadata = process_file_as_single_chunk(file_path, content)
                
                if doc_metadata:
                    # 获取嵌入向量
                    full_content = f"{doc_metadata['context']}\n\n当前内容：{doc_metadata['content']}"
                    embedding = get_embeddings(full_content)
                    
                    # 存储文档元数据
                    metadata.append(doc_metadata)
                    
                    # 存储嵌入向量并添加到索引
                    embedding_array = np.array([embedding]).astype('float32')
                    index.add(embedding_array)
                    
                    # 更新文档信息
                    document_info[file_str_path] = {
                        "sections": 1,
                        "last_updated": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    }
                    
                    # 保存索引和元数据
                    faiss.write_index(index, str(INDEX_PATH))
                    with open(METADATA_PATH, 'wb') as f:
                        pickle.dump(metadata, f)
                    
                    result["sections_added"] = 1
                    result["message"] += f"成功处理 {file_path}，作为单个文档添加.\n"
                    logger.info(f"成功处理 {file_path}，作为单个文档添加")
                else:
                    result["status"] = "warning"
                    result["message"] += f"文件 {file_path} 未提取到有效内容.\n"
        
        elif file_extension == ".json":
            # 处理JSON文件
            with open(file_path, "r", encoding="utf-8") as f:
                try:
                    json_data = json.loads(f.read())
                    
                    # 处理JSON文件
                    doc_metadata_list = process_json_file(file_path, json_data)
                    
                    if doc_metadata_list:
                        # 添加嵌入向量
                        embeddings = []
                        for doc_metadata in doc_metadata_list:
                            # 获取嵌入向量
                            full_content = f"{doc_metadata['context']}\n\n当前内容：{doc_metadata['content']}"
                            embedding = get_embeddings(full_content)
                            embeddings.append(embedding)
                            
                            # 存储文档元数据
                            metadata.append(doc_metadata)
                        
                        # 将嵌入向量添加到索引
                        if embeddings:
                            embeddings_array = np.array(embeddings).astype('float32')
                            index.add(embeddings_array)
                        
                        # 更新文档信息
                        document_info[file_str_path] = {
                            "sections": len(doc_metadata_list),
                            "last_updated": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                        }
                        
                        # 保存索引和元数据
                        faiss.write_index(index, str(INDEX_PATH))
                        with open(METADATA_PATH, 'wb') as f:
                            pickle.dump(metadata, f)
                        
                        result["sections_added"] = len(doc_metadata_list)
                        result["message"] += f"成功处理JSON文件 {file_path}，添加了 {len(doc_metadata_list)} 个文档.\n"
                        logger.info(f"成功处理JSON文件 {file_path}，添加了 {len(doc_metadata_list)} 个文档")
                    else:
                        result["status"] = "warning"
                        result["message"] += f"JSON文件 {file_path} 未提取到有效内容.\n"
                
                except json.JSONDecodeError as e:
                    result["status"] = "error"
                    result["message"] = f"解析JSON文件 {file_path} 失败: {str(e)}"
                    logger.error(f"解析JSON文件失败 {file_path}: {str(e)}")
        
        else:
            result["status"] = "warning"
            result["message"] = f"不支持的文件类型: {file_extension}"
            logger.warning(f"不支持的文件类型: {file_extension}")
    
    except Exception as e:
        result["status"] = "error"
        result["message"] = f"处理文件 {file_path} 失败: {str(e)}"
        logger.error(f"处理文件失败 {file_path}: {str(e)}")
    
    return result

# 异步处理上传文件
async def process_uploaded_file(file_path: Path, update_mode: str):
    """异步处理上传的文件"""
    result = process_file(file_path, update_mode)
    logger.info(f"文件处理结果: {result}")

@app.get("/health")
async def health():
    return {"status": "ok"}


@app.get("/")
async def home(request: Request):
    return templates.TemplateResponse("index.html", {"request": request})

@app.get("/documents")
async def get_documents():
    """获取当前知识库中的文档信息"""
    docs_list = []
    for path, info in document_info.items():
        try:
            # 提取文件名，不含路径
            filename = os.path.basename(path)
            docs_list.append({
                "filename": filename,
                "full_path": path,
                "sections": info["sections"],
                "last_updated": info["last_updated"]
            })
        except Exception as e:
            logger.error(f"获取文档信息时出错 {path}: {str(e)}")
    
    return JSONResponse({
        "total_documents": len(docs_list),
        "total_sections": sum(doc["sections"] for doc in docs_list),
        "documents": docs_list
    })

@app.post("/upload")
async def upload_file(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    update_mode: str = Form("add")  # "add" 或 "replace"
):
    """
    上传文件到知识库
    update_mode: "add" (添加新文件) 或 "replace" (替换同名文件)
    """
    if not file:
        return JSONResponse({
            "status": "error",
            "message": "未提供文件"
        }, status_code=400)
    
    # 检查文件类型
    if not file.filename.lower().endswith('.txt'):
        return JSONResponse({
            "status": "error",
            "message": "只支持TXT文件"
        }, status_code=400)
    
    # 保存文件
    try:
        # 确保文档目录存在
        docs_dir = BASE_DIR / "docs"
        docs_dir.mkdir(exist_ok=True)
        
        # 文件保存路径
        file_path = docs_dir / file.filename
        
        # 保存上传的文件
        with open(file_path, "wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
        
        # 异步处理文件
        background_tasks.add_task(process_uploaded_file, file_path, update_mode)
        
        return JSONResponse({
            "status": "success",
            "message": f"文件 {file.filename} 已上传，正在处理中。请稍后查看知识库状态。",
            "file_path": str(file_path)
        })
    except Exception as e:
        logger.error(f"文件上传失败: {str(e)}")
        return JSONResponse({
            "status": "error",
            "message": f"文件上传失败: {str(e)}"
        }, status_code=500)

@app.post("/query")
async def query(
    query: str = Form(...),
    example: str = Form(None, description="问答示例，用于指导LLM回答格式")
):
    logger.info(f"收到新的查询请求: {query}")
    if example:
        logger.info(f"问答示例: {example}")
    
    try:
        # 获取相关文档（使用两阶段检索）
        relevant_docs = find_relevant_documents(query, initial_top_k=10, final_top_k=RAG_FINAL_TOP_K)
        
        # 构建上下文
        context = build_context_from_docs(relevant_docs)
        
        # 获取LLM回答，传入问答示例
        llm_response = clean_llm_answer(get_llm_response(query, context, example or ""))
        
        logger.info("查询处理完成")
        return JSONResponse({
            "answer": llm_response,
            "sources": [
                {
                    "content": f"上下文信息:\n{doc['context']}\n当前内容:\n{doc['content']}",
                    "path": doc["path"]
                }
                for doc in relevant_docs
            ]
        })
    except Exception as e:
        error_msg = f"处理查询时出错: {str(e)}"
        logger.error(error_msg)
        return JSONResponse({
            "error": error_msg
        }, status_code=500)

@app.post("/query_stream")
async def query_stream(
    query: str = Form(...),
    example: str = Form(None, description="问答示例，用于指导LLM回答格式")
):
    """
    流式处理查询请求，实时返回生成的回答内容
    """
    logger.info(f"收到新的流式查询请求: {query}")
    if example:
        logger.info(f"问答示例: {example}")
    
    try:
        # 获取相关文档（使用两阶段检索）
        relevant_docs = find_relevant_documents(query, initial_top_k=10, final_top_k=RAG_FINAL_TOP_K)
        
        # 构建上下文
        context = build_context_from_docs(relevant_docs)
        
        # 创建返回的sources信息
        sources = [
            {
                "content": f"上下文信息:\n{doc['context']}\n当前内容:\n{doc['content']}",
                "path": doc["path"]
            }
            for doc in relevant_docs
        ]
        
        # 准备sources的JSON字符串，稍后与响应一起发送
        sources_json = json.dumps({"sources": sources}, ensure_ascii=False)
        
        # 返回流式响应
        async def stream_response():
            # 先返回sources信息，以特殊格式标记以便前端识别
            yield f"SOURCES_DATA: {sources_json}\n\n"
            
            # 获取LLM流式回答
            async for chunk in get_llm_response_stream(query, context, example or ""):
                yield chunk
        
        logger.info("流式查询处理开始")
        return StreamingResponse(
            stream_response(),
            media_type="text/plain",
            headers={"X-Accel-Buffering": "no"}  # 禁用Nginx缓冲
        )
        
    except Exception as e:
        error_msg = f"处理流式查询时出错: {str(e)}"
        logger.error(error_msg)
        return JSONResponse({
            "error": error_msg
        }, status_code=500)

# 删除知识库中的文档
def delete_document(file_path: str) -> Dict:
    """
    从知识库中删除指定文档
    返回处理结果信息
    """
    global index, metadata, document_info
    
    result = {"status": "success", "message": "", "sections_removed": 0}
    
    try:
        # 检查文件是否存在于知识库中
        if file_path not in document_info:
            result["status"] = "error"
            result["message"] = f"文件 {file_path} 不存在于知识库中"
            return result
        
        # 创建新的索引和元数据列表
        new_index = faiss.IndexFlatIP(VECTOR_DIM)
        new_metadata = []
        
        # 遍历现有元数据，找出需要保留的文档
        vectors_to_keep = []
        sections_removed = 0
        
        for idx, doc in enumerate(metadata):
            doc_path = doc["path"].split("#")[0] if "#" in doc["path"] else doc["path"]
            if doc_path != file_path:
                # 保留其他文件的元数据
                new_metadata.append(doc)
                vectors_to_keep.append(idx)
            else:
                sections_removed += 1
        
        # 从原索引中提取要保留的向量
        if vectors_to_keep:
            if len(vectors_to_keep) < index.ntotal:
                # 创建选择器
                selector = faiss.IDSelectorBatch(len(vectors_to_keep), np.array(vectors_to_keep, dtype=np.int64))
                # 提取指定索引的向量
                vectors = faiss.extract_index_vectors(index)[1]
                selected_vectors = vectors.subset(selector)
                # 将保留的向量添加到新索引
                new_index.add(selected_vectors)
            else:
                # 如果没有要删除的向量，直接复制原索引
                new_index = faiss.clone_index(index)
        
        # 更新全局变量
        index = new_index
        metadata = new_metadata
        
        # 从文档信息字典中删除文件条目
        if file_path in document_info:
            del document_info[file_path]
        
        # 保存更新后的索引和元数据
        faiss.write_index(index, str(INDEX_PATH))
        with open(METADATA_PATH, 'wb') as f:
            pickle.dump(metadata, f)
        
        # 尝试删除实际文件（如果存在）
        try:
            if os.path.exists(file_path):
                os.remove(file_path)
                result["message"] = f"已从知识库中删除文件 {file_path}，并删除了物理文件。"
            else:
                result["message"] = f"已从知识库中删除文件 {file_path}，但未找到物理文件。"
        except Exception as file_e:
            logger.warning(f"删除物理文件 {file_path} 失败: {str(file_e)}")
            result["message"] = f"已从知识库中删除文件 {file_path}，但删除物理文件失败。"
        
        result["sections_removed"] = sections_removed
        logger.info(f"成功从知识库中删除文件 {file_path}")
        
    except Exception as e:
        result["status"] = "error"
        result["message"] = f"删除文件 {file_path} 失败: {str(e)}"
        logger.error(f"删除文件失败 {file_path}: {str(e)}")
    
    return result

@app.delete("/documents/{file_path:path}")
async def delete_file(file_path: str, background_tasks: BackgroundTasks):
    """
    从知识库中删除指定文档
    """
    try:
        # 构建完整的文件路径
        full_path = str(BASE_DIR / "docs" / file_path)
        
        # 异步执行删除操作
        result = delete_document(full_path)
        
        if result["status"] == "success":
            return JSONResponse({
                "status": "success",
                "message": result["message"],
                "sections_removed": result["sections_removed"]
            })
        else:
            return JSONResponse({
                "status": "error",
                "message": result["message"]
            }, status_code=400)
    except Exception as e:
        error_msg = f"删除文档时出错: {str(e)}"
        logger.error(error_msg)
        return JSONResponse({
            "status": "error",
            "message": error_msg
        }, status_code=500)

def search_documents(query: str, top_k: int = 5, rerank: bool = True) -> List[Dict]:
    """搜索文档并返回最相关的片段"""
    if not index or index.ntotal == 0:
        logger.warning("索引为空，无法搜索")
        return []
    
    # 获取查询的嵌入向量
    query_embedding = get_embeddings(query)
    
    # 使用FAISS搜索最相关的文档
    D, I = index.search(np.array([query_embedding]).astype('float32'), min(top_k * 2, index.ntotal))
    
    # 获取搜索结果
    results = []
    for i, idx in enumerate(I[0]):
        if idx != -1:  # FAISS返回-1表示无效结果
            score = float(D[0][i])
            doc = metadata[idx]
            result = {
                "content": doc["content"],
                "context": doc.get("context", ""),
                "position_info": doc.get("position_info", ""),
                "path": doc["path"],
                "score": score
            }
            results.append(result)
    
    # 如果启用重排序，使用更复杂的语义相似度计算重新排序
    if rerank and ENABLE_RERANK and results:
        # 计算更精确的语义相似度得分
        for result in results:
            # 使用完整内容（包括上下文）重新计算相似度
            full_content = f"{result['context']}\n\n当前内容：{result['content']}"
            
            # 使用句子相似度重新计算得分
            rerank_score = get_semantic_similarity(query, full_content)
            
            # 将重排序得分添加到结果中
            result["rerank_score"] = float(rerank_score)
        
        # 按重排序得分降序排序
        results = sorted(results, key=lambda x: x["rerank_score"], reverse=True)
        # 只保留前top_k个结果
        results = results[:top_k]
    else:
        # 按原始相似度得分降序排序并保留前top_k个结果
        results = sorted(results, key=lambda x: x["score"], reverse=True)[:top_k]
    
    logger.info(f"查询 '{query}' 找到 {len(results)} 个相关文档段落")
    return results

def format_results(results: List[Dict]) -> str:
    """将搜索结果格式化为可读文本"""
    if not results:
        return "未找到相关文档。"
    
    context = ""
    used_contents = set()  # 跟踪已使用的内容以避免重复
    
    for i, result in enumerate(results):
        content = result["content"].strip()
        
        # 检查是否已包含相同内容
        content_hash = hash(content)
        if content_hash in used_contents:
            continue
        
        used_contents.add(content_hash)
        
        # 添加文档来源信息
        path = result["path"]
        file_name = os.path.basename(path)
        
        # 添加位置信息
        position = result.get("position_info", "")
        
        # 添加相似度得分信息
        score_info = f"相似度：{result.get('rerank_score', result.get('score', 0)):.4f}"
        
        # 格式化片段内容
        context += f"\n【文档{i+1}】{file_name} {position} {score_info}\n{content}\n"
    
    # 添加搜索结果说明
    explanation = f"找到 {len(results)} 个相关片段。以下是最相关的内容："
    
    return explanation + "\n" + context

if __name__ == "__main__":
    host = os.environ.get("HOST", "127.0.0.1")
    port = int(os.environ.get("PORT", "17690"))
    logger.info(f"启动RAG服务... 主机: {host}, 端口: {port}")
    uvicorn.run(app, host=host, port=port)
