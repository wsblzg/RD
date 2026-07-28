<template>
  <div class="login-page">
    <div class="bg-layer" aria-hidden="true"></div>
    <div class="vignette" aria-hidden="true"></div>

    <main class="login-main">
      <section class="brand-side">
        <div class="brand-chip">
          <img class="chip-mark" src="/logo.webp" alt="窑创未来logo" />
          <span class="chip-text">窑创未来</span>
        </div>
        <p class="brand-kicker">YAOCHUANG FUTURE</p>
        <h1>守护窑火温度<br />连接非遗未来</h1>
        <p class="brand-desc">
          进入数字导览系统，体验柴烧导览、AI 智鉴问答与数字藏品服务。
        </p>
        <div class="brand-actions">
          <router-link class="ghost-btn" to="/ceramics/home">首页</router-link>
          <router-link class="ghost-btn muted" to="/ceramics/intelligence/qa">体验智鉴问答</router-link>
        </div>
      </section>

      <section class="form-side">
        <article class="login-card">
          <header class="card-head">
            <h2>{{ authTab === 'login' ? '欢迎登录' : '创建账号' }}</h2>
            <p>{{ authTab === 'login' ? '窑创未来 · 用户中心' : '窑创未来 · 注册中心' }}</p>
          </header>

          <div class="auth-tabs" role="tablist" aria-label="登录注册切换">
            <button
              type="button"
              class="auth-tab"
              :class="{ active: authTab === 'login' }"
              @click="authTab = 'login'"
            >
              登录
            </button>
            <button
              type="button"
              class="auth-tab"
              :class="{ active: authTab === 'register' }"
              @click="authTab = 'register'"
            >
              注册
            </button>
          </div>

          <template v-if="authTab === 'login'">
            <div class="mode-tabs" role="tablist" aria-label="登录模式">
              <button
                type="button"
                class="mode-tab"
                :class="{ active: loginMode === 'user' }"
                @click="loginMode = 'user'"
              >
                用户登录
              </button>
              <button
                type="button"
                class="mode-tab"
                :class="{ active: loginMode === 'admin' }"
                @click="loginMode = 'admin'"
              >
                管理员登录
              </button>
            </div>

            <form class="login-form" @submit.prevent="submitLogin">
              <label>
                <span>账号</span>
                <input
                  v-model.trim="form.username"
                  type="text"
                  autocomplete="username"
                  placeholder="请输入用户名"
                />
              </label>

              <label>
                <span>密码</span>
                <input
                  v-model="form.password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="请输入密码"
                />
              </label>

              <div class="form-meta">
                <label class="remember">
                  <input v-model="rememberMe" type="checkbox" />
                  <span>记住账号</span>
                </label>
                <button type="button" class="switch-link" @click="authTab = 'register'">没有账号？去注册</button>
              </div>

              <button class="submit-btn" type="submit" :disabled="submitting">
                {{ submitting ? '登录中...' : '点击登录' }}
              </button>

              <p v-if="feedback.text" :class="['feedback', feedback.ok ? 'ok' : 'error']">
                {{ feedback.text }}
              </p>
            </form>
          </template>

          <template v-else>
            <form class="login-form" @submit.prevent="submitRegister">
              <label>
                <span>昵称</span>
                <input
                  v-model.trim="registerForm.displayName"
                  type="text"
                  autocomplete="nickname"
                  placeholder="请输入昵称"
                />
              </label>

              <label>
                <span>用户名</span>
                <input
                  v-model.trim="registerForm.username"
                  type="text"
                  autocomplete="username"
                  placeholder="请输入用户名"
                />
              </label>

              <label>
                <span>密码</span>
                <input
                  v-model="registerForm.password"
                  type="password"
                  autocomplete="new-password"
                  placeholder="请输入密码"
                />
              </label>

              <label>
                <span>确认密码</span>
                <input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  autocomplete="new-password"
                  placeholder="请再次输入密码"
                />
              </label>

              <div class="form-meta">
                <span class="register-tip">注册成功后请重新登录</span>
                <button type="button" class="switch-link" @click="authTab = 'login'">已有账号？去登录</button>
              </div>

              <button class="submit-btn" type="submit" :disabled="submitting">
                {{ submitting ? '注册中...' : '点击注册' }}
              </button>

              <p v-if="feedback.text" :class="['feedback', feedback.ok ? 'ok' : 'error']">
                {{ feedback.text }}
              </p>
            </form>
          </template>

          <footer class="card-foot">
            <p v-if="authTab === 'login' && loginMode === 'admin'">管理员请使用已分配账号登录后台管理能力。</p>
            <p v-else-if="authTab === 'login'">登录后可进入个人中心，查看数字藏品、文章并发布动态。</p>
            <p v-else>注册后可使用同一账号登录，并通过兑换码获取数字藏品。</p>
          </footer>
        </article>
      </section>
    </main>

    <div v-if="captchaDialogVisible" class="captcha-dialog-backdrop" @click.self="closeCaptchaDialog">
      <section class="captcha-dialog" role="dialog" aria-modal="true" aria-label="登录人机验证">
        <button class="captcha-close" type="button" aria-label="关闭验证弹窗" @click="closeCaptchaDialog">×</button>
        <PlayCaptchaGate
          v-if="captchaChallenge"
          :reset-key="captchaKey"
          :target="captchaChallenge.target"
          @verified="handleCaptchaVerified"
        />
        <p v-else class="captcha-state pending">正在准备人机验证...</p>
        <p class="captcha-state pending">完成抓娃娃验证后将自动继续登录。</p>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PlayCaptchaGate from '@/components/PlayCaptchaGate.vue'
import { useCollectiblesCenter } from '@/composables/useCollectiblesCenter'
import { collectiblesAuthAPI } from '@/utils/collectiblesApi'
import { canSubmitLogin, hasLoginCredentials } from '@/utils/loginCaptcha'

const router = useRouter()
const route = useRoute()
const { login, logout, register } = useCollectiblesCenter()

const LOGIN_HINT_KEY = 'yc_login_hint'

const authTab = ref('login')
const loginMode = ref('user')
const rememberMe = ref(true)
const submitting = ref(false)
const captchaVerified = ref(false)
const captchaDialogVisible = ref(false)
const captchaKey = ref(0)
const captchaChallenge = ref(null)
const captchaToken = ref('')
const form = reactive({
  username: '',
  password: ''
})
const registerForm = reactive({
  displayName: '',
  username: '',
  password: '',
  confirmPassword: ''
})
const feedback = reactive({
  ok: true,
  text: ''
})

const setFeedback = (ok, text) => {
  feedback.ok = ok
  feedback.text = text
}

const resetCaptcha = () => {
  captchaVerified.value = false
  captchaDialogVisible.value = false
  captchaChallenge.value = null
  captchaToken.value = ''
  captchaKey.value += 1
}

const closeCaptchaDialog = () => {
  captchaDialogVisible.value = false
}

const openCaptchaDialog = async () => {
  submitting.value = true
  setFeedback(true, '')
  try {
    captchaChallenge.value = await collectiblesAuthAPI.createCaptchaChallenge()
    captchaDialogVisible.value = true
    captchaKey.value += 1
  } catch (error) {
    setFeedback(false, error?.message || '人机验证初始化失败，请稍后再试。')
  } finally {
    submitting.value = false
  }
}

const handleCaptchaVerified = async ({ target, elapsedMs }) => {
  if (!captchaChallenge.value?.challengeId) {
    resetCaptcha()
    setFeedback(false, '人机验证已失效，请重试。')
    return
  }
  submitting.value = true
  try {
    const result = await collectiblesAuthAPI.verifyCaptcha({
      challengeId: captchaChallenge.value.challengeId,
      target,
      elapsedMs
    })
    captchaToken.value = result?.captchaToken || ''
    captchaVerified.value = Boolean(captchaToken.value)
    captchaDialogVisible.value = false
    await performLogin()
  } catch (error) {
    submitting.value = false
    resetCaptcha()
    setFeedback(false, error?.message || '人机验证失败，请重试。')
  }
}

watch(loginMode, (mode) => {
  if (mode === 'admin' && !form.username) {
    form.username = 'admin'
  }
})

watch(authTab, (tab) => {
  setFeedback(true, '')
  resetCaptcha()
  if (tab === 'register') {
    loginMode.value = 'user'
  }
})

onMounted(() => {
  const rawHint = localStorage.getItem(LOGIN_HINT_KEY)
  if (!rawHint) return
  try {
    const hint = JSON.parse(rawHint)
    if (hint?.username) form.username = hint.username
    if (hint?.mode === 'admin' || hint?.mode === 'user') loginMode.value = hint.mode
  } catch (error) {
    localStorage.removeItem(LOGIN_HINT_KEY)
  }
})

const submitRegister = async () => {
  if (!registerForm.displayName || !registerForm.username || !registerForm.password) {
    setFeedback(false, '请完整填写昵称、用户名和密码。')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    setFeedback(false, '两次输入的密码不一致。')
    return
  }
  submitting.value = true
  setFeedback(true, '')

  const result = await register({
    displayName: registerForm.displayName,
    username: registerForm.username,
    password: registerForm.password
  })

  submitting.value = false
  setFeedback(result.ok, result.message || (result.ok ? '注册成功，请登录。' : '注册失败，请稍后再试。'))
  if (!result.ok) return

  form.username = registerForm.username
  form.password = ''
  authTab.value = 'login'
  registerForm.displayName = ''
  registerForm.username = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
}

const submitLogin = async () => {
  const credentials = hasLoginCredentials({
    username: form.username,
    password: form.password
  })
  if (!credentials.ok) {
    setFeedback(false, credentials.message)
    return
  }

  if (!captchaVerified.value) {
    await openCaptchaDialog()
    return
  }

  await performLogin()
}

const performLogin = async () => {
  const validation = canSubmitLogin({
    username: form.username,
    password: form.password,
    captchaVerified: Boolean(captchaToken.value)
  })
  if (!validation.ok) {
    setFeedback(false, validation.message)
    return
  }
  submitting.value = true
  setFeedback(true, '')

  const result = await login({
    username: form.username,
    password: form.password,
    captchaToken: captchaToken.value
  })

  if (!result.ok) {
    submitting.value = false
    resetCaptcha()
    setFeedback(false, result.message || '登录失败，请稍后再试。')
    return
  }

  const currentUser = JSON.parse(localStorage.getItem('yc_user') || 'null')
  if (loginMode.value === 'admin' && currentUser?.role !== 'admin') {
    await logout()
    submitting.value = false
    resetCaptcha()
    setFeedback(false, '当前账号不是管理员账号，请切换到“用户登录”。')
    return
  }

  if (rememberMe.value) {
    localStorage.setItem(LOGIN_HINT_KEY, JSON.stringify({
      username: form.username,
      mode: loginMode.value
    }))
  } else {
    localStorage.removeItem(LOGIN_HINT_KEY)
  }

  setFeedback(true, '登录成功，正在进入系统...')
  submitting.value = false
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  if (loginMode.value === 'admin') {
    router.push('/ceramics/admin/collectibles')
    return
  }
  router.push(redirect || '/ceramics/user-center')
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  color: #f8f2e9;
  background: #1e1a16;
}

.bg-layer {
  position: absolute;
  inset: 0;
  background-image: url('/vcg-login-bg.webp');
  background-size: cover;
  background-position: center;
  transform: scale(1.02);
}

.vignette {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(100deg, rgba(24, 18, 14, 0.78) 0%, rgba(24, 18, 14, 0.52) 36%, rgba(24, 18, 14, 0.34) 100%);
}

.login-main {
  position: relative;
  z-index: 1;
  width: min(1260px, 94vw);
  margin: 0 auto;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.1fr minmax(350px, 430px);
  gap: 40px;
  align-items: center;
  padding: 36px 0;
}

.brand-side {
  max-width: 620px;
}

.brand-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border-radius: 999px;
  padding: 8px 14px 8px 10px;
  border: 1px solid rgba(255, 241, 220, 0.34);
  background: rgba(255, 255, 255, 0.09);
  backdrop-filter: blur(4px);
}

.chip-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  object-fit: cover;
  display: block;
}

.chip-text {
  font-family: var(--ym-font-display);
  font-size: 1.08rem;
  letter-spacing: 0.03em;
}

.brand-kicker {
  margin-top: 20px;
  letter-spacing: 0.26em;
  text-transform: uppercase;
  font-size: 0.76rem;
  color: rgba(255, 236, 208, 0.78);
}

.brand-side h1 {
  margin-top: 10px;
  font-family: var(--ym-font-display);
  font-size: clamp(2rem, 4.6vw, 3.6rem);
  line-height: 1.2;
  color: #fff7ec;
}

.brand-desc {
  margin-top: 14px;
  font-size: clamp(1rem, 1.5vw, 1.14rem);
  line-height: 1.9;
  color: rgba(255, 238, 214, 0.9);
  max-width: 560px;
}

.brand-actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  border-radius: 999px;
  border: 1px solid rgba(255, 242, 222, 0.48);
  padding: 10px 18px;
  text-decoration: none;
  color: #fff4e4;
  background: rgba(255, 255, 255, 0.11);
  transition: all 0.2s ease;
}

.ghost-btn.muted {
  color: rgba(255, 244, 228, 0.88);
  border-color: rgba(255, 242, 222, 0.3);
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.2);
}

.form-side {
  justify-self: end;
  width: 100%;
}

.login-card {
  border-radius: 22px;
  border: 1px solid rgba(255, 242, 222, 0.34);
  background: linear-gradient(145deg, rgba(255, 247, 234, 0.82), rgba(255, 240, 220, 0.7));
  box-shadow: 0 18px 56px rgba(0, 0, 0, 0.24);
  backdrop-filter: blur(8px);
  padding: 28px 24px;
  color: #4f3826;
}

.card-head {
  text-align: center;
}

.card-head h2 {
  font-family: var(--ym-font-display);
  font-size: 2rem;
  color: #553a28;
}

.card-head p {
  margin-top: 8px;
  color: #7c5c45;
  font-size: 0.92rem;
}

.mode-tabs {
  margin-top: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(124, 92, 69, 0.2);
  border-radius: 12px;
  padding: 4px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.auth-tabs {
  margin-top: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(124, 92, 69, 0.2);
  border-radius: 12px;
  padding: 4px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
}

.auth-tab,
.mode-tab {
  border: none;
  border-radius: 8px;
  padding: 10px 6px;
  background: transparent;
  color: #7a5b44;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}

.auth-tab.active,
.mode-tab.active {
  background: rgba(181, 68, 46, 0.14);
  color: #6a4027;
}

.login-form {
  margin-top: 16px;
  display: grid;
  gap: 12px;
}

.login-form label {
  display: grid;
  gap: 6px;
}

.login-form label span {
  font-size: 0.84rem;
  color: #7b5b44;
}

.login-form input[type='text'],
.login-form input[type='password'] {
  border-radius: 12px;
  border: 1px solid rgba(124, 92, 69, 0.2);
  background: rgba(245, 247, 250, 0.92);
  color: #3c2d22;
}

.form-meta {
  margin-top: 2px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.captcha-state {
  margin: 0;
  border-radius: 9px;
  padding: 8px 10px;
  font-size: 0.84rem;
}

.captcha-state.ok {
  color: #2f6a42;
  border: 1px solid rgba(47, 106, 66, 0.24);
  background: rgba(47, 106, 66, 0.09);
}

.captcha-state.pending {
  color: #7b5b44;
  border: 1px dashed rgba(124, 92, 69, 0.28);
  background: rgba(255, 255, 255, 0.42);
}

.captcha-dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(22, 16, 12, 0.62);
}

.captcha-dialog {
  position: relative;
  width: min(94vw, 460px);
  border-radius: 16px;
  border: 1px solid rgba(255, 242, 222, 0.34);
  background: rgba(255, 247, 234, 0.96);
  box-shadow: 0 20px 64px rgba(0, 0, 0, 0.34);
  padding: 18px;
  color: #4f3826;
}

.captcha-dialog .captcha-state {
  margin-top: 10px;
}

.captcha-close {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: rgba(79, 56, 38, 0.1);
  color: #5c3d2b;
  cursor: pointer;
  font-size: 1.25rem;
  line-height: 1;
}

.remember {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 0.86rem;
  color: #6f533f;
}

.remember input {
  width: 14px;
  height: 14px;
}

.meta-link {
  font-size: 0.86rem;
  color: #8f6545;
  text-decoration: none;
}

.meta-link:hover {
  text-decoration: underline;
}

.switch-link {
  border: none;
  background: transparent;
  color: #8f6545;
  font-size: 0.86rem;
  cursor: pointer;
  padding: 0;
}

.switch-link:hover {
  text-decoration: underline;
}

.register-tip {
  color: #7b5b44;
  font-size: 0.84rem;
}

.submit-btn {
  margin-top: 4px;
  border: none;
  border-radius: 12px;
  height: 48px;
  background: linear-gradient(135deg, #cf8b5e, #b5442e);
  color: #fff7ef;
  font-size: 1.03rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.submit-btn:not(:disabled):hover {
  transform: translateY(-1px);
  opacity: 0.94;
}

.submit-btn:disabled {
  cursor: not-allowed;
  opacity: 0.74;
}

.feedback {
  margin-top: 2px;
  padding: 9px 10px;
  border-radius: 9px;
  font-size: 0.88rem;
}

.feedback.ok {
  color: #2f6a42;
  border: 1px solid rgba(47, 106, 66, 0.24);
  background: rgba(47, 106, 66, 0.09);
}

.feedback.error {
  color: #963c2e;
  border: 1px solid rgba(150, 60, 46, 0.24);
  background: rgba(150, 60, 46, 0.08);
}

.card-foot {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed rgba(124, 92, 69, 0.24);
}

.card-foot p {
  text-align: center;
  font-size: 0.82rem;
  color: #7b5b44;
}

button:focus-visible,
input:focus-visible,
a:focus-visible {
  outline: 2px solid rgba(181, 68, 46, 0.56);
  outline-offset: 2px;
}

@media (max-width: 1080px) {
  .login-main {
    grid-template-columns: 1fr;
    gap: 20px;
    align-items: start;
    padding-top: 52px;
  }

  .brand-side {
    max-width: none;
  }

  .form-side {
    justify-self: stretch;
  }

  .login-card {
    max-width: 520px;
  }
}

@media (max-width: 620px) {
  .login-main {
    width: min(94vw, 560px);
    padding-top: 30px;
  }

  .brand-side h1 {
    font-size: 1.92rem;
  }

  .brand-actions {
    width: 100%;
    flex-direction: column;
  }

  .ghost-btn {
    width: 100%;
  }

  .login-card {
    padding: 22px 16px;
  }

  .card-head h2 {
    font-size: 1.75rem;
  }

  .form-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .register-tip {
    margin-bottom: 4px;
  }
}
</style>
