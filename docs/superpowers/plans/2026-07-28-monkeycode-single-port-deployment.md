# MonkeyCode Single-Port Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. It will decide whether each batch should run in parallel or serial subagent mode and will pass only task-local context to each subagent. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the Vue frontend, Spring Boot API, and FastAPI RAG service through one MonkeyCode public port while retaining the existing remote MySQL database.

**Architecture:** Vue is built once and copied into the Spring Boot artifact. Spring Boot listens on `0.0.0.0:8006`; RAG listens only on `127.0.0.1:17690`. A repository-level shell script validates configuration, starts RAG, waits for readiness, builds the application, and leaves Spring Boot in the foreground for MonkeyCode port detection.

**Tech Stack:** Vue 3, Vite 6, Spring Boot 2.7, Java 17, Maven, Python 3.10, FastAPI, uv, MySQL, Aliyun OSS

---

## File Structure

- Modify `.gitignore`: exclude generated archives, Python environments, logs, and RAG indexes.
- Create `.env.example`: document deployment variable names without real values.
- Modify `springboot/src/main/resources/application.yml`: replace every credential with an environment variable reference.
- Delete `vue/src/config/sparkConfig.js`: remove unused browser-side API credentials.
- Delete `vue/src/utils/sparkApi.js`: remove the only consumer of the unused browser-side credential module.
- Modify `vue/src/components/guide/GuideKiln3DPanel.vue`: load the large kiln video from configurable OSS storage.
- Modify `vue/.env.example`: add the public video URL and keep browser-safe values only.
- Delete `vue/public/2bc8bba6572a5ad71a07d94c6d477e86.mp4`: remove the tracked file larger than 10MB after OSS upload is verified.
- Create `RAG-Agent/pyproject.toml` and `RAG-Agent/uv.lock`: make uv the only dependency manager.
- Modify `RAG-Agent/main.py`: add a deterministic health endpoint and default to loopback binding.
- Modify `springboot/pom.xml`: copy `vue/dist` into the Spring Boot artifact during resource processing.
- Create `springboot/src/main/java/com/example/controller/SpaController.java`: forward frontend routes to `index.html`.
- Create `springboot/src/test/java/com/example/controller/SpaControllerTest.java`: verify history fallback without swallowing API or static-resource errors.
- Create `scripts/check-deployment.sh`: validate secrets, required variables, tracked file size, and remote MySQL reachability.
- Create `scripts/start-monkeycode.sh`: build Vue, sync RAG, start RAG, build Spring Boot, and run port `8006`.
- Create `docs/deployment/monkeycode.md`: provide Git-based deployment, recovery, validation, and domain-switch instructions.

### Task 1: Remove Committed Secrets and Define Deployment Configuration

**Acceptance:** AC-001, AC-002, AC-009

**Files:**
- Create: `.env.example`
- Modify: `springboot/src/main/resources/application.yml`
- Delete: `vue/src/config/sparkConfig.js`
- Delete: `vue/src/utils/sparkApi.js`
- Test: `scripts/check-deployment.sh`

- [ ] **Step 1: Write the secret-scan check**

Create `scripts/check-deployment.sh` with the initial scan:

```bash
#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

if rg -n \
  'jdbc:mysql://[^$]|access-key-(id|secret):[[:space:]]+[^$]|api-(key|secret):[[:space:]]+[^$]|secret-(id|key):[[:space:]]+[^$]|nvapi-|sk-[A-Za-z0-9]{16,}|LTAI[A-Za-z0-9]+' \
  springboot/src/main/resources RAG-Agent vue/src vue/.env.example .env.example; then
  echo "检测到仓库内明文凭据" >&2
  exit 1
fi
```

- [ ] **Step 2: Run the check and verify it fails**

Run:

```bash
bash scripts/check-deployment.sh
```

Expected: exit code `1` and output `检测到仓库内明文凭据`.

- [ ] **Step 3: Replace Spring Boot secrets with environment references**

Change the sensitive values in `application.yml` to:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    url: ${DB_URL}

jwt:
  secret: ${JWT_SECRET}

xiling:
  app-id: ${XILING_APP_ID}
  app-key: ${XILING_APP_KEY}

aliyun:
  oss:
    endpoint: ${OSS_ENDPOINT:https://oss-cn-shenzhen.aliyuncs.com}
    access-key-id: ${OSS_ACCESS_KEY_ID}
    access-key-secret: ${OSS_ACCESS_KEY_SECRET}
    bucket: ${OSS_BUCKET:wsnlzg}
    url-prefix: ${OSS_URL_PREFIX:https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/}

ai:
  xunfei:
    app-id: ${XUNFEI_APP_ID}
    api-key: ${XUNFEI_API_KEY}
    api-secret: ${XUNFEI_API_SECRET}
    api-url: ${XUNFEI_API_URL:https://spark-api.xf-yun.com/v3.5/chat}
    domain: ${XUNFEI_DOMAIN:generalv3.5}
  nvidia:
    api-url: ${NVIDIA_API_URL:https://integrate.api.nvidia.com/v1}
    api-key: ${NVIDIA_API_KEY}
  deepseek:
    api-url: ${DEEPSEEK_API_URL:https://api.deepseek.com}
    api-key: ${DEEPSEEK_API_KEY}
  tencent:
    ai3d:
      secret-id: ${TENCENT_AI3D_SECRET_ID}
      secret-key: ${TENCENT_AI3D_SECRET_KEY}
      region: ${TENCENT_AI3D_REGION:ap-guangzhou}
```

Keep existing non-secret model names, timeouts, queue settings, paths, and payment settings unchanged.

- [ ] **Step 4: Remove the unused browser-side Spark client**

Verify no production file imports it:

```bash
rg -n "sparkApi|getSparkConfig" vue/src --glob '!config/sparkConfig.js' --glob '!utils/sparkApi.js'
```

Expected: no output. Delete `vue/src/config/sparkConfig.js` and `vue/src/utils/sparkApi.js`.

- [ ] **Step 5: Add the root environment example**

Create `.env.example`:

```dotenv
DB_URL=jdbc:mysql://47.113.113.212:3306/wyxm_ycwl?useSSL=false&characterEncoding=utf8&useUnicode=true
DB_USERNAME=wyxm
DB_PASSWORD=
JWT_SECRET=
XILING_APP_ID=
XILING_APP_KEY=
OSS_ENDPOINT=https://oss-cn-shenzhen.aliyuncs.com
OSS_ACCESS_KEY_ID=
OSS_ACCESS_KEY_SECRET=
OSS_BUCKET=wsnlzg
OSS_URL_PREFIX=https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/
XUNFEI_APP_ID=
XUNFEI_API_KEY=
XUNFEI_API_SECRET=
NVIDIA_API_KEY=
DEEPSEEK_API_KEY=
TENCENT_AI3D_SECRET_ID=
TENCENT_AI3D_SECRET_KEY=
API_URL=https://api.siliconflow.cn/v1
API_KEY=
LLM_API_URL=https://api.siliconflow.cn/v1
LLM_API_KEY=
EMBEDDING_MODEL=BAAI/bge-m3
VECTOR_DIM=1024
LLM_MODEL=Qwen/Qwen2.5-7B-Instruct
RERANKER_MODEL=BAAI/bge-reranker-v2-m3
```

- [ ] **Step 6: Run the scan and project tests**

Run:

```bash
bash scripts/check-deployment.sh
cd vue && npm test --if-present && npm run build
cd ../springboot && mvn test
```

Expected: secret scan passes, Vue build returns `0`, Maven tests pass.

- [ ] **Step 7: Rotate every exposed credential**

In the MySQL, Aliyun, Xunfei, NVIDIA, DeepSeek, Tencent, Xiling, and RAG provider consoles, revoke the values previously committed to Git and create replacements. Store replacements only in MonkeyCode environment variables.

Expected: old credentials no longer authenticate.

- [ ] **Step 8: Commit**

```bash
git add .env.example scripts/check-deployment.sh springboot/src/main/resources/application.yml
git rm vue/src/config/sparkConfig.js vue/src/utils/sparkApi.js
git commit -m "security: move deployment secrets to environment"
```

### Task 2: Move the Large Video to OSS and Enforce the 10MB Limit

**Acceptance:** AC-003, AC-004, AC-005

**Files:**
- Modify: `.gitignore`
- Modify: `vue/.env.example`
- Modify: `vue/src/components/guide/GuideKiln3DPanel.vue`
- Delete: `vue/public/2bc8bba6572a5ad71a07d94c6d477e86.mp4`
- Modify: `scripts/check-deployment.sh`

- [ ] **Step 1: Upload and verify the video outside MonkeyCode**

Upload the existing file to:

```text
project-media/videos/kiln-firing.mp4
```

Verify:

```bash
curl -fsSI https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/project-media/videos/kiln-firing.mp4
```

Expected: HTTP `200` and a video content type. Do not delete the local file until this passes.

- [ ] **Step 2: Add a failing source assertion**

Append to `scripts/check-deployment.sh`:

```bash
video_source="vue/src/components/guide/GuideKiln3DPanel.vue"
grep -q "VITE_KILN_VIDEO_URL" "$video_source" || {
  echo "窑炉视频尚未改为 OSS 环境地址" >&2
  exit 1
}

while IFS= read -r file; do
  size="$(wc -c < "$file")"
  if (( size > 10 * 1024 * 1024 )); then
    echo "Git 跟踪文件超过 10MB: $file" >&2
    exit 1
  fi
done < <(git ls-files)
```

Run `bash scripts/check-deployment.sh`.

Expected: failure because the component still uses the local MP4.

- [ ] **Step 3: Make the video URL configurable**

Replace the component constant with:

```js
const kilnVideoUrl =
  import.meta.env.VITE_KILN_VIDEO_URL ||
  'https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/project-media/videos/kiln-firing.mp4'
```

Append to `vue/.env.example`:

```dotenv
VITE_KILN_VIDEO_URL=https://wsnlzg.oss-cn-shenzhen.aliyuncs.com/project-media/videos/kiln-firing.mp4
```

- [ ] **Step 4: Exclude generated files and remove the large tracked file**

Append to `.gitignore`:

```gitignore
vue/dist.zip
RAG-Agent/.venv/
RAG-Agent/logs/
RAG-Agent/vector_db/
```

Then run:

```bash
git rm vue/public/2bc8bba6572a5ad71a07d94c6d477e86.mp4
```

Keep the user's existing untracked `vue/dist.zip` untouched; `.gitignore` prevents it
from entering deployment commits.

- [ ] **Step 5: Verify and commit**

Run:

```bash
bash scripts/check-deployment.sh
cd vue && npm run build
```

Expected: no tracked file exceeds 10MB and the Vue build succeeds.

Commit:

```bash
git add .gitignore vue/.env.example vue/src/components/guide/GuideKiln3DPanel.vue scripts/check-deployment.sh
git commit -m "chore: serve large kiln video from OSS"
```

### Task 3: Convert RAG-Agent to uv and Add Health Readiness

**Acceptance:** AC-011, AC-012, AC-015

**Files:**
- Create: `RAG-Agent/pyproject.toml`
- Create: `RAG-Agent/uv.lock`
- Modify: `RAG-Agent/main.py`
- Delete: `RAG-Agent/requirements.txt`
- Test: `RAG-Agent/test_health.py`

- [ ] **Step 1: Add the failing health test**

Create `RAG-Agent/test_health.py`:

```python
from fastapi.testclient import TestClient

from main import app


def test_health():
    response = TestClient(app).get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
```

Run:

```bash
cd RAG-Agent
uv run --with pytest --with httpx pytest test_health.py -q
```

Expected: `404` assertion failure.

- [ ] **Step 2: Add the minimal health endpoint and loopback default**

Add after `app = FastAPI()`:

```python
@app.get("/health")
def health():
    return {"status": "ok"}
```

Change the startup default:

```python
host = os.environ.get("HOST", "127.0.0.1")
```

- [ ] **Step 3: Create the uv project using uv commands**

Run:

```bash
cd RAG-Agent
uv init --bare --python 3.10
uv add "fastapi==0.104.1" "uvicorn==0.24.0" "python-multipart==0.0.6" \
  "jinja2==3.1.2" "numpy>=1.24.4" "requests==2.31.0" \
  "langchain==0.1.12" "faiss-cpu==1.7.4" "aiohttp==3.8.6"
uv add --dev pytest httpx
```

Delete `requirements.txt` after `pyproject.toml` and `uv.lock` are generated.

- [ ] **Step 4: Run the test and start smoke check**

Run:

```bash
uv sync --frozen
uv run pytest test_health.py -q
HOST=127.0.0.1 PORT=17690 uv run python main.py &
rag_pid=$!
trap 'kill "$rag_pid" 2>/dev/null || true' EXIT
for _ in $(seq 1 30); do
  curl -fsS http://127.0.0.1:17690/health && break
  sleep 1
done
curl -fsS http://127.0.0.1:17690/health
```

Expected: test passes and HTTP response is `{"status":"ok"}`.

- [ ] **Step 5: Commit**

```bash
git add RAG-Agent/main.py RAG-Agent/pyproject.toml RAG-Agent/uv.lock RAG-Agent/test_health.py
git rm RAG-Agent/requirements.txt
git commit -m "build: manage RAG service with uv"
```

### Task 4: Package Vue Inside Spring Boot

**Acceptance:** AC-006, AC-007, AC-009, AC-010

**Files:**
- Modify: `springboot/pom.xml`
- Test: `springboot/src/test/java/com/example/StaticFrontendPackagingTest.java`

- [ ] **Step 1: Add a failing packaging test**

Create:

```java
package com.example;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticFrontendPackagingTest {
    @Test
    void vueIndexIsCopiedIntoSpringBootClasses() {
        assertTrue(Files.isRegularFile(Path.of("target/classes/static/index.html")));
    }
}
```

Run after a clean Maven build without the copy configuration:

```bash
cd vue && npm ci && npm run build
cd ../springboot && mvn clean test
```

Expected: `StaticFrontendPackagingTest` fails because `target/classes/static/index.html` is absent.

- [ ] **Step 2: Copy Vue output during Maven resource processing**

Add one `maven-resources-plugin` execution to `springboot/pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <version>3.3.1</version>
    <executions>
        <execution>
            <id>copy-vue-dist</id>
            <phase>process-resources</phase>
            <goals>
                <goal>copy-resources</goal>
            </goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}/static</outputDirectory>
                <resources>
                    <resource>
                        <directory>${project.basedir}/../vue/dist</directory>
                        <filtering>false</filtering>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

- [ ] **Step 3: Verify packaging**

Run:

```bash
cd vue && npm ci && npm run build
cd ../springboot && mvn clean test package
jar tf target/springboot-0.0.1-SNAPSHOT.jar | grep 'BOOT-INF/classes/static/index.html'
```

Expected: all tests pass and the JAR listing contains the Vue index.

- [ ] **Step 4: Commit**

```bash
git add springboot/pom.xml springboot/src/test/java/com/example/StaticFrontendPackagingTest.java
git commit -m "build: package Vue frontend in Spring Boot"
```

### Task 5: Add Safe Vue History Fallback

**Acceptance:** AC-007, AC-008

**Files:**
- Create: `springboot/src/main/java/com/example/controller/SpaController.java`
- Create: `springboot/src/test/java/com/example/controller/SpaControllerTest.java`
- Modify: `springboot/src/main/java/com/example/config/WebConfig.java`

- [ ] **Step 1: Add focused MockMvc tests**

Create `SpaControllerTest.java`:

```java
package com.example.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SpaController()).build();
    }

    @Test
    void forwardsVueHistoryRoute() throws Exception {
        mockMvc.perform(get("/ceramics/home"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void doesNotSwallowMissingStaticResource() throws Exception {
        mockMvc.perform(get("/assets/missing.js"))
                .andExpect(status().isNotFound());
    }

    @Test
    void doesNotSwallowMissingApiRoute() throws Exception {
        mockMvc.perform(get("/api/not-a-real-route"))
                .andExpect(status().isNotFound());
    }
}
```

Run:

```bash
cd springboot
mvn -Dtest=SpaControllerTest test
```

Expected: the frontend route test fails before the controller exists.

- [ ] **Step 2: Add the minimal fallback controller**

Create:

```java
package com.example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({
        "/",
        "/home",
        "/login",
        "/ceramics",
        "/ceramics/**"
    })
    public String frontend() {
        return "forward:/index.html";
    }
}
```

Do not add a catch-all mapping; API and missing asset paths must retain 404 behavior.

- [ ] **Step 3: Exclude public frontend paths from JWT interception**

Add the exact frontend entry paths to `excludePathPatterns`:

```java
"/",
"/index.html",
"/assets/**",
"/favicon.ico",
"/ceramics/**"
```

- [ ] **Step 4: Run tests and commit**

```bash
cd springboot
mvn -Dtest=SpaControllerTest test
mvn test
git add src/main/java/com/example/controller/SpaController.java \
  src/main/java/com/example/config/WebConfig.java \
  src/test/java/com/example/controller/SpaControllerTest.java
git commit -m "feat: serve Vue history routes from Spring Boot"
```

### Task 6: Add the Single Deployment Entry Point

**Acceptance:** AC-010, AC-013, AC-014, AC-015, AC-016, AC-017, AC-018, AC-021

**Files:**
- Modify: `scripts/check-deployment.sh`
- Create: `scripts/start-monkeycode.sh`
- Create: `docs/deployment/monkeycode.md`

- [ ] **Step 1: Extend configuration and MySQL validation**

Add required-variable validation:

```bash
required_vars=(
  DB_URL DB_USERNAME DB_PASSWORD JWT_SECRET
  OSS_ACCESS_KEY_ID OSS_ACCESS_KEY_SECRET
  XILING_APP_ID XILING_APP_KEY
  API_KEY
)
for name in "${required_vars[@]}"; do
  [[ -n "${!name:-}" ]] || {
    echo "缺少环境变量: $name" >&2
    exit 1
  }
done
```

Extract the MySQL host and port from `DB_URL`, then test with `/dev/tcp`:

```bash
db_host="$(sed -E 's#^jdbc:mysql://([^:/?]+).*#\1#' <<<"$DB_URL")"
db_port="$(sed -E 's#^jdbc:mysql://[^:/?]+:([0-9]+).*#\1#' <<<"$DB_URL")"
timeout 5 bash -c "</dev/tcp/$db_host/$db_port" || {
  echo "MySQL 不可达: $db_host:$db_port" >&2
  exit 1
}
```

- [ ] **Step 2: Verify the database failure path**

Run:

```bash
DB_URL='jdbc:mysql://127.0.0.1:1/test' \
DB_USERNAME=x DB_PASSWORD=x JWT_SECRET=x \
OSS_ACCESS_KEY_ID=x OSS_ACCESS_KEY_SECRET=x \
XILING_APP_ID=x XILING_APP_KEY=x API_KEY=x \
bash scripts/check-deployment.sh
```

Expected: non-zero exit and `MySQL 不可达: 127.0.0.1:1`.

- [ ] **Step 3: Create the startup script**

Create `scripts/start-monkeycode.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

bash scripts/check-deployment.sh

(cd vue && npm ci && npm run build)
(cd RAG-Agent && uv sync --frozen)

cleanup() {
  if [[ -n "${rag_pid:-}" ]]; then
    kill "$rag_pid" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

(
  cd RAG-Agent
  HOST=127.0.0.1 PORT=17690 uv run python main.py
) &
rag_pid=$!

for _ in $(seq 1 60); do
  if curl -fsS http://127.0.0.1:17690/health >/dev/null; then
    break
  fi
  kill -0 "$rag_pid" 2>/dev/null || {
    echo "RAG 服务启动失败" >&2
    exit 1
  }
  sleep 1
done
curl -fsS http://127.0.0.1:17690/health >/dev/null || {
  echo "RAG 服务健康检查超时" >&2
  exit 1
}

(cd springboot && mvn clean package -DskipTests)
exec java -jar springboot/target/springboot-0.0.1-SNAPSHOT.jar
```

Make both scripts executable:

```bash
chmod +x scripts/check-deployment.sh scripts/start-monkeycode.sh
```

- [ ] **Step 4: Write deployment and recovery instructions**

Document these exact operations in `docs/deployment/monkeycode.md`:

```bash
: "${REPO_URL:?请先设置私有 Git 仓库地址}"
git clone "$REPO_URL" wyxm
cd wyxm
cp .env.example .env
set -a
source .env
set +a
bash scripts/start-monkeycode.sh
```

The document must also state:

- Use a private Git repository; do not use the 10MB browser uploader.
- Do not commit `.env`.
- MonkeyCode may recycle the environment; source must remain in Git.
- RAG indexes are regenerated from `RAG-Agent/docs`.
- Do not install systemd, UFW, MySQL, SSH, BaoTa, or FRP.
- Retrieve the platform HTTPS URL only after port `8006` is detected.
- Validate `/`, `/ceramics/home`, one read-only API, login, OSS upload, and RAG.

- [ ] **Step 5: Run local verification**

On a Linux-compatible environment with valid rotated credentials:

```bash
bash -n scripts/check-deployment.sh
bash -n scripts/start-monkeycode.sh
bash scripts/check-deployment.sh
bash scripts/start-monkeycode.sh
```

In another shell:

```bash
curl -fsS http://127.0.0.1:8006/ >/dev/null
curl -fsS http://127.0.0.1:8006/ceramics/home >/dev/null
curl -fsS http://127.0.0.1:17690/health
ss -lnt | grep ':8006'
ss -lnt | grep '127.0.0.1:17690'
```

Expected: frontend routes return `200`, RAG returns `{"status":"ok"}`, Spring Boot listens publicly on `8006`, and RAG listens only on loopback.

- [ ] **Step 6: Commit**

```bash
git add scripts/check-deployment.sh scripts/start-monkeycode.sh docs/deployment/monkeycode.md
git commit -m "deploy: add MonkeyCode single-port startup"
```

### Task 7: Verify MonkeyCode and Switch the Custom Domain

**Acceptance:** AC-018, AC-019, AC-020

**Files:**
- Modify: `docs/deployment/monkeycode.md`

- [ ] **Step 1: Push the deployment branch to a private remote**

Create a private repository in the selected Git provider, copy its clone URL, and set it
without writing credentials into Git:

```bash
: "${REPO_URL:?请设置新建私有仓库的克隆地址}"
git remote add origin "$REPO_URL"
git status --short
git push -u origin feat/digital-human-rag
```

Expected: only intended user changes and deployment commits are present; the private remote receives the branch.

- [ ] **Step 2: Clone and start in MonkeyCode**

In MonkeyCode:

```bash
: "${REPO_URL:?请设置同一个私有 Git 仓库地址}"
git clone --branch feat/digital-human-rag "$REPO_URL" wyxm
cd wyxm
cp .env.example .env
```

Fill `.env` with rotated values using the platform editor, then:

```bash
set -a
source .env
set +a
bash scripts/start-monkeycode.sh
```

Expected: platform detects `8006` and provides one HTTPS project URL.

- [ ] **Step 3: Complete platform acceptance before DNS changes**

Using the platform URL, verify:

```text
/
/ceramics/home
/ceramics/user-login
/ceramics/intelligence/qa
```

Also verify one database-backed read, one login, one OSS upload below the application upload limit, and one RAG query. Keep `yaochuangfuture.cn` pointing to `47.113.113.212` until all checks pass.

- [ ] **Step 4: Configure an HTTPS reverse proxy**

Use a reverse-proxy provider that supports a custom hostname and HTTPS origin. Configure:

```text
Public hostname: yaochuangfuture.cn
Origin: the MonkeyCode HTTPS project URL
Forward path and query: unchanged
WebSocket: enabled
TLS verification: enabled
```

Do not point a bare CNAME at MonkeyCode unless the platform explicitly accepts the custom Host header and issues a certificate for `yaochuangfuture.cn`.

- [ ] **Step 5: Switch DNS and verify**

After the proxy provider gives its required DNS record, replace the current A record to `47.113.113.212` with that record. Verify:

```bash
curl -fsSI https://yaochuangfuture.cn/
curl -fsS https://yaochuangfuture.cn/ceramics/home >/dev/null
```

In a browser, verify login, API requests, RAG, video, and upload have no CORS, mixed-content, certificate, or WebSocket errors.

- [ ] **Step 6: Record the final operational values**

Add only non-secret values to `docs/deployment/monkeycode.md`:

```text
MonkeyCode project URL
Public application port: 8006
RAG internal port: 17690
Reverse-proxy provider
DNS record type and hostname
Recovery command: bash scripts/start-monkeycode.sh
```

Do not record passwords, API keys, access keys, or JWT secrets.

- [ ] **Step 7: Commit**

```bash
git add docs/deployment/monkeycode.md
git commit -m "docs: record MonkeyCode production endpoint"
```

## Final Verification

Run:

```bash
bash scripts/check-deployment.sh
cd vue && npm ci && npm run build
cd ../RAG-Agent && uv sync --frozen && uv run pytest -q
cd ../springboot && mvn clean test package
cd ..
git ls-files | while read -r file; do
  test "$(wc -c < "$file")" -le 10485760 || exit 1
done
```

Expected:

- No committed plaintext credentials.
- No tracked file exceeds 10MB.
- Vue, RAG tests, and Maven tests pass.
- The Spring Boot JAR contains `BOOT-INF/classes/static/index.html`.
- MonkeyCode exposes only `8006`; RAG remains on `127.0.0.1:17690`.
- `https://yaochuangfuture.cn` serves the MonkeyCode deployment after all platform checks pass.
