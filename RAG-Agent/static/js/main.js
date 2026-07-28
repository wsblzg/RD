// RAG系统主JavaScript文件

// 文档信息刷新
let refreshingDocuments = false;

// 定义问答模板
const questionTemplates = {
    standard: `
    # 要求
    请使用小红书风格回答问题。

    # 特点
    每个段落尽量包含表情符号。
    
    # 注意
    请确保保持文本的原始含义。
    所有对话和指示都需要用中文提供。
    在回答的最后**不要添加#标签**。
    对于知识库没有的问题，请告知用户知识库没有相关信息。`,
};

// 初始化页面
function initializePage() {
    // 搜索表单处理
    document
        .getElementById("searchForm")
        .addEventListener("submit", handleStreamSearch);

    // 上传文件处理
    document
        .getElementById("uploadForm")
        .addEventListener("submit", handleFileUpload);

    // 刷新文档列表按钮
    document
        .getElementById("refreshDocuments")
        .addEventListener("click", loadDocuments);

    // 加载文档列表
    loadDocuments();
}

// 处理搜索 (传统方式)
async function handleSearch(e) {
    e.preventDefault();
    const query = document.getElementById("query").value;
    if (!query.trim()) {
        alert("请输入查询内容");
        return;
    }

    const templateType = document.getElementById("templateSelect").value;
    const example = templateType ? questionTemplates[templateType] : "";

    const loading = document.querySelector(".loading");
    const results = document.getElementById("results");
    const answer = document.getElementById("answer");
    const sources = document.getElementById("sources");

    loading.style.display = "block";
    answer.innerHTML = "";
    sources.innerHTML = "";

    try {
        const response = await fetch("/query", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
            },
            body: `query=${encodeURIComponent(
                query
            )}&example=${encodeURIComponent(example)}`,
        });

        const data = await response.json();

        if (data.error) {
            results.innerHTML = `<div class="alert alert-danger">${data.error}</div>`;
            return;
        }

        // 显示AI回答
        const answerCard = document.createElement("div");
        answerCard.className = "card answer-card fade-in";
        answerCard.innerHTML = `
            <div class="card-body">
                <h5 class="card-title">AI 回答</h5>
                <p class="card-text">${data.answer.replace(/\n/g, "<br>")}</p>
            </div>
        `;
        answer.appendChild(answerCard);

        // 显示来源文档
        if (data.sources && data.sources.length > 0) {
            const sourcesTitle = document.createElement("h5");
            sourcesTitle.className = "sources-title";
            sourcesTitle.textContent = "参考文档";
            sources.appendChild(sourcesTitle);

            data.sources.forEach((doc, index) => {
                setTimeout(() => {
                    const card = document.createElement("div");
                    card.className = "card result-card fade-in";
                    card.innerHTML = `
                        <div class="card-body">
                            <h6 class="card-subtitle mb-2 text-muted">${doc.path}</h6>
                            <p class="card-text">${doc.content}</p>
                        </div>
                    `;
                    sources.appendChild(card);
                }, index * 100); // 添加延迟，创建展开效果
            });
        }
    } catch (error) {
        console.error("搜索出错:", error);
        results.innerHTML =
            '<div class="alert alert-danger">搜索出错，请稍后重试</div>';
    } finally {
        loading.style.display = "none";
    }
}

// 处理流式搜索
async function handleStreamSearch(e) {
    e.preventDefault();
    const query = document.getElementById("query").value;
    if (!query.trim()) {
        alert("请输入查询内容");
        return;
    }

    const templateType = document.getElementById("templateSelect").value;
    const example = templateType ? questionTemplates[templateType] : "";

    const loading = document.querySelector(".loading");
    const results = document.getElementById("results");
    const answer = document.getElementById("answer");
    const sources = document.getElementById("sources");

    loading.style.display = "block";
    answer.innerHTML = "";
    sources.innerHTML = "";

    // 创建回答卡片和流式输出元素
    const answerCard = document.createElement("div");
    answerCard.className = "card answer-card fade-in";
    answerCard.innerHTML = `
        <div class="card-body">
            <h5 class="card-title">AI 回答</h5>
            <p id="streaming-answer" class="card-text"></p>
        </div>
    `;
    answer.appendChild(answerCard);

    const streamingAnswer = document.getElementById("streaming-answer");
    let responseText = "";

    try {
        const response = await fetch("/query_stream", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded",
            },
            body: `query=${encodeURIComponent(
                query
            )}&example=${encodeURIComponent(example)}`,
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.error || "请求失败");
        }

        // 处理流式响应
        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        // 用于处理Sources数据
        let sourcesData = null;

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });

            // 检查是否包含source数据（特殊格式）
            if (chunk.includes("SOURCES_DATA:")) {
                const parts = chunk.split("\n\n");
                for (const part of parts) {
                    if (part.startsWith("SOURCES_DATA:")) {
                        try {
                            const sourcesJson = part
                                .replace("SOURCES_DATA:", "")
                                .trim();
                            sourcesData = JSON.parse(sourcesJson);
                        } catch (e) {
                            console.error("解析sources数据失败:", e);
                        }
                    } else {
                        // 正常文本部分
                        responseText += part;
                        streamingAnswer.innerHTML =
                            formatStreamingText(responseText);
                    }
                }
            } else {
                // 正常文本部分
                responseText += chunk;
                streamingAnswer.innerHTML = formatStreamingText(responseText);
            }
        }

        // 显示来源文档
        if (
            sourcesData &&
            sourcesData.sources &&
            sourcesData.sources.length > 0
        ) {
            const sourcesTitle = document.createElement("h5");
            sourcesTitle.className = "sources-title";
            sourcesTitle.textContent = "参考文档";
            sources.appendChild(sourcesTitle);

            sourcesData.sources.forEach((doc, index) => {
                setTimeout(() => {
                    const card = document.createElement("div");
                    card.className = "card result-card fade-in";
                    card.innerHTML = `
                        <div class="card-body">
                            <h6 class="card-subtitle mb-2 text-muted">${doc.path}</h6>
                            <p class="card-text">${doc.content}</p>
                        </div>
                    `;
                    sources.appendChild(card);
                }, index * 100); // 添加延迟，创建展开效果
            });
        }
    } catch (error) {
        console.error("搜索出错:", error);
        streamingAnswer.innerHTML = `<div class="alert alert-danger">搜索出错: ${error.message}</div>`;
    } finally {
        loading.style.display = "none";
    }
}

// 格式化流式文本，将换行符转换为HTML
function formatStreamingText(text) {
    return text.replace(/\n/g, "<br>");
}

// 获取知识库文档信息
async function loadDocuments() {
    if (refreshingDocuments) return;
    refreshingDocuments = true;

    const documentList = document.getElementById("documentList");
    const dbStats = document.getElementById("dbStats");

    documentList.innerHTML = `
        <div class="text-center">
            <div class="spinner-border spinner-border-sm text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
            </div>
            <span class="ms-2">加载文档列表...</span>
        </div>
    `;

    dbStats.innerHTML = `
        <div class="text-center">
            <div class="spinner-border spinner-border-sm text-primary" role="status">
                <span class="visually-hidden">加载中...</span>
            </div>
            <span class="ms-2">加载知识库信息...</span>
        </div>
    `;

    try {
        const response = await fetch("/documents");
        const data = await response.json();

        // 更新知识库统计信息
        dbStats.innerHTML = `
            <div class="row g-3">
                <div class="col-6">
                    <div class="p-3 border bg-light rounded text-center">
                        <h3>${data.total_documents}</h3>
                        <p class="mb-0">文档总数</p>
                    </div>
                </div>
                <div class="col-6">
                    <div class="p-3 border bg-light rounded text-center">
                        <h3>${data.total_sections}</h3>
                        <p class="mb-0">文本段落总数</p>
                    </div>
                </div>
            </div>
        `;

        // 更新文档列表
        if (data.documents.length === 0) {
            documentList.innerHTML =
                '<div class="alert alert-info">暂无文档</div>';
            return;
        }

        // 创建表格显示文档列表
        let tableHtml = `
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>文件名</th>
                        <th>段落数</th>
                        <th>最后更新时间</th>
                        <th width="100px">操作</th>
                    </tr>
                </thead>
                <tbody>
        `;

        data.documents.forEach((doc) => {
            tableHtml += `
                <tr>
                    <td title="${doc.full_path}">${doc.filename}</td>
                    <td>${doc.sections}</td>
                    <td>${doc.last_updated}</td>
                    <td>
                        <button class="btn btn-sm btn-danger delete-doc-btn" 
                            data-filename="${doc.filename}" 
                            data-fullpath="${doc.full_path}">
                            <i class="bi bi-trash"></i> 删除
                        </button>
                    </td>
                </tr>
            `;
        });

        tableHtml += `
                </tbody>
            </table>
        `;

        documentList.innerHTML = tableHtml;

        // 为所有删除按钮添加事件监听器
        document.querySelectorAll(".delete-doc-btn").forEach((btn) => {
            btn.addEventListener("click", handleDocumentDelete);
        });
    } catch (error) {
        console.error("加载文档列表失败:", error);
        documentList.innerHTML =
            '<div class="alert alert-danger">加载文档列表失败</div>';
        dbStats.innerHTML =
            '<div class="alert alert-danger">加载知识库信息失败</div>';
    } finally {
        refreshingDocuments = false;
    }
}

// 处理文件上传
async function handleFileUpload(e) {
    e.preventDefault();

    const fileInput = document.getElementById("fileUpload");
    const file = fileInput.files[0];

    if (!file) {
        showUploadStatus("请选择要上传的文件", "danger");
        return;
    }

    const updateMode = document.querySelector(
        'input[name="updateMode"]:checked'
    ).value;

    // 创建FormData对象
    const formData = new FormData();
    formData.append("file", file);
    formData.append("update_mode", updateMode);

    showUploadStatus("文件上传中...", "info");

    try {
        const response = await fetch("/upload", {
            method: "POST",
            body: formData,
        });

        const data = await response.json();

        if (response.ok) {
            showUploadStatus(data.message, "success");
            // 清空文件输入
            fileInput.value = "";
            // 延迟3秒后刷新文档列表
            setTimeout(loadDocuments, 3000);
        } else {
            showUploadStatus(data.message || "上传失败", "danger");
        }
    } catch (error) {
        console.error("上传文件失败:", error);
        showUploadStatus("上传文件失败，请稍后重试", "danger");
    }
}

// 处理文档删除
async function handleDocumentDelete(e) {
    const button = e.currentTarget;
    const filename = button.getAttribute("data-filename");
    const fullPath = button.getAttribute("data-fullpath");

    // 使用Bootstrap模态对话框代替浏览器的confirm
    const deleteModal = document.getElementById("deleteConfirmModal");
    const bsModal = new bootstrap.Modal(deleteModal);

    // 设置文件名
    document.getElementById("deleteFileName").textContent = filename;

    // 设置确认按钮的点击事件
    const confirmBtn = document.getElementById("confirmDeleteBtn");

    // 移除旧的事件监听器（如果有）
    const newConfirmBtn = confirmBtn.cloneNode(true);
    confirmBtn.parentNode.replaceChild(newConfirmBtn, confirmBtn);

    // 添加新的事件监听器
    newConfirmBtn.addEventListener("click", async function () {
        // 更改按钮状态
        newConfirmBtn.disabled = true;
        newConfirmBtn.innerHTML =
            '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> 删除中...';

        try {
            // 从fullPath中提取相对于docs目录的路径
            const docPath = fullPath.includes("/docs/")
                ? fullPath.split("/docs/")[1]
                : fullPath.includes("\\docs\\")
                ? fullPath.split("\\docs\\")[1]
                : filename;

            const response = await fetch(
                `/documents/${encodeURIComponent(docPath)}`,
                {
                    method: "DELETE",
                }
            );

            const data = await response.json();

            // 关闭模态对话框
            bsModal.hide();

            if (response.ok) {
                showStatusMessage(`文档 "${filename}" 已成功删除`, "success");
                // 刷新文档列表
                setTimeout(loadDocuments, 1000);
            } else {
                showStatusMessage(`删除失败: ${data.message}`, "danger");
            }
        } catch (error) {
            console.error("删除文档失败:", error);
            showStatusMessage("删除文档失败，请稍后重试", "danger");
            bsModal.hide();
        }
    });

    // 显示模态对话框
    bsModal.show();
}

// 显示上传状态
function showUploadStatus(message, type) {
    const statusDiv = document.getElementById("uploadStatus");
    statusDiv.innerText = message;
    statusDiv.className = `upload-status alert alert-${type}`;
    statusDiv.style.display = "block";
}

// 显示一般状态消息
function showStatusMessage(message, type) {
    const statusContainer = document.getElementById("statusContainer");

    // 如果状态容器不存在，创建一个
    if (!statusContainer) {
        const container = document.createElement("div");
        container.id = "statusContainer";
        container.className = "position-fixed bottom-0 end-0 p-3";
        container.style.zIndex = "5";
        document.body.appendChild(container);
    }

    // 创建消息元素
    const statusDiv = document.createElement("div");
    statusDiv.className = `toast align-items-center text-white bg-${type} border-0`;
    statusDiv.role = "alert";
    statusDiv.ariaLive = "assertive";
    statusDiv.ariaAtomic = "true";

    statusDiv.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="关闭"></button>
        </div>
    `;

    document.getElementById("statusContainer").appendChild(statusDiv);

    // 使用Bootstrap的Toast组件显示消息
    const toast = new bootstrap.Toast(statusDiv, { delay: 3000 });
    toast.show();

    // 消息消失后移除元素
    statusDiv.addEventListener("hidden.bs.toast", function () {
        statusDiv.remove();
    });
}

// 页面加载完成后初始化
document.addEventListener("DOMContentLoaded", function () {
    initializePage();

    // 创建状态消息容器
    const container = document.createElement("div");
    container.id = "statusContainer";
    container.className = "position-fixed bottom-0 end-0 p-3";
    container.style.zIndex = "5";
    document.body.appendChild(container);
});
