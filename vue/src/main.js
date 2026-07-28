import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/main.css'

if (import.meta.env.PROD) {
  console.log = () => {}
  console.info = () => {}
  console.debug = () => {}
}

const purgeRegressionAuthCache = () => {
  const rawUser = localStorage.getItem('yc_user')
  if (!rawUser) return

  try {
    const user = JSON.parse(rawUser)
    const username = String(user?.username || '').trim()
    const displayName = String(user?.displayName || '').trim()
    const isRegressionAccount = username.startsWith('pwtest_') || /回归测试/.test(displayName)
    if (!isRegressionAccount) return
  } catch (error) {
    // 本地缓存已损坏时，一并清理避免影响登录态
  }

  localStorage.removeItem('yc_user')
  localStorage.removeItem('yc_token')
  localStorage.removeItem('yc_login_hint')
}

purgeRegressionAuthCache()

const app = createApp(App)

app.use(router)
app.mount('#app')
