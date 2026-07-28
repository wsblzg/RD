<template>
  <header class="site-header" :class="{ scrolled: isScrolled }">
    <div class="header-inner">
      <button class="brand" type="button" @click="goRoute(buildPath('/home'))">
        <img class="brand-logo" src="/logo.webp" alt="窑创未来logo" />
        <span class="brand-text">柴智云</span>
      </button>

      <nav :class="['main-nav', { open: showMobileMenu }]" aria-label="主导航">
        <button
          v-for="item in navItems"
          :key="item.label"
          type="button"
          class="nav-link"
          :class="{ active: isActive(item) }"
          @click="goRoute(item.to)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="account-area">
        <button
          v-if="isAdmin"
          type="button"
          class="admin-entry-btn"
          @click="goRoute(buildPath('/admin/features'))"
        >
          后台管理
        </button>
        <template v-if="isLoggedIn">
          <div ref="accountMenuRef" class="account-dropdown">
            <button
              class="account-pill"
              type="button"
              :aria-expanded="accountMenuOpen ? 'true' : 'false'"
              aria-haspopup="menu"
              @click="toggleAccountMenu"
            >
              <span class="account-avatar">{{ userInitial }}</span>
              <span class="account-meta">
                <strong>{{ userDisplayName }}</strong>
                <small>{{ isAdmin ? '管理员' : '个人中心' }}</small>
              </span>
            </button>
            <transition name="menu-fade">
              <div v-if="accountMenuOpen" class="account-menu" role="menu" aria-label="个人菜单">
                <button
                  v-for="item in accountMenuItems"
                  :key="item.key"
                  type="button"
                  class="menu-item"
                  role="menuitem"
                  @click="goRoute(item.to)"
                >
                  <span>{{ item.label }}</span>
                  <small>{{ item.hint }}</small>
                </button>
                <button type="button" class="menu-item danger" role="menuitem" @click="handleLogout">
                  <span>退出登录</span>
                  <small>清除当前账号登录状态</small>
                </button>
              </div>
            </transition>
          </div>
        </template>
        <button v-else class="login-btn" type="button" @click="goRoute(buildPath('/user-login'))">登录</button>
      </div>

      <button
        class="menu-toggle"
        type="button"
        :aria-expanded="showMobileMenu ? 'true' : 'false'"
        aria-label="toggle navigation"
        @click="showMobileMenu = !showMobileMenu"
      >
        <span :class="{ open: showMobileMenu }"></span>
        <span :class="{ open: showMobileMenu }"></span>
        <span :class="{ open: showMobileMenu }"></span>
      </button>
    </div>
  </header>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { collectiblesAuthAPI } from '@/utils/collectiblesApi'

const route = useRoute()
const router = useRouter()
const showMobileMenu = ref(false)
const accountMenuOpen = ref(false)
const accountMenuRef = ref(null)
const isScrolled = ref(false)
const currentUser = ref(null)
let scrollRafId = null
const CERAMICS_BASE = '/ceramics'
const buildPath = (path) => `${CERAMICS_BASE}${path}`
const normalizedPath = computed(() => {
  const path = String(route.path || '')
  if (!path.startsWith(CERAMICS_BASE)) return path || '/'
  const stripped = path.replace(/^\/ceramics(?=\/|$)/, '')
  return stripped || '/'
})

const navItems = [
  { label: '首页', to: buildPath('/home'), group: '/home' },
  { label: '柴烧导览', to: buildPath('/guide/kiln'), group: '/guide' },
  { label: '智鉴中枢', to: buildPath('/intelligence/qa'), group: '/intelligence' },
  { label: '数字藏品馆', to: buildPath('/collections/catalog'), group: '/collections' },
  { label: '文创商城', to: buildPath('/shop'), group: '/shop' },
  { label: '窑火造场', to: buildPath('/ai-creation'), group: '/ai-creation' },
  { label: '社区广场', to: buildPath('/community'), group: '/community' }
]

const isActive = (item) => normalizedPath.value === item.group || normalizedPath.value.startsWith(`${item.group}/`)
const goRoute = (target) => {
  showMobileMenu.value = false
  accountMenuOpen.value = false
  router.push(target)
}

const readUser = () => {
  const raw = localStorage.getItem('yc_user')
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch (error) {
    return null
  }
}

const syncUser = () => {
  currentUser.value = readUser()
}

const isLoggedIn = computed(() => Boolean(currentUser.value?.id))
const isAdmin = computed(() => currentUser.value?.role === 'admin')
const userDisplayName = computed(() => currentUser.value?.displayName || currentUser.value?.username || '用户')
const userInitial = computed(() => (userDisplayName.value || '用').slice(0, 1))
const accountMenuItems = computed(() => {
  const items = [
    { key: 'profile', label: '个人资料', hint: '查看账号信息与状态', to: { path: buildPath('/user-center'), query: { tab: 'profile' } } },
    { key: 'collections', label: '我的藏品', hint: '查看已获取的数字藏品', to: { path: buildPath('/user-center'), query: { tab: 'collections' } } },
    { key: 'orders', label: '我的订单', hint: '查看商城订单与发货状态', to: { path: buildPath('/user-center'), query: { tab: 'orders' } } },
    { key: 'posts', label: '我的文章', hint: '查看你发布的社区文章', to: { path: buildPath('/user-center'), query: { tab: 'posts' } } },
    { key: 'cart', label: '购物车', hint: '继续结算待购买商品', to: buildPath('/shop/cart') },
    { key: 'publish', label: '发布文章', hint: '发布活动纪实与工艺分享', to: buildPath('/community/publish') }
  ]
  if (isAdmin.value) {
    items.unshift({
      key: 'admin-orders',
      label: '订单审核',
      hint: '审核付款并处理发货',
      to: buildPath('/admin/shop/orders')
    })
    items.unshift({
      key: 'admin-shop',
      label: '商城管理',
      hint: '管理商品、库存与上下架',
      to: buildPath('/admin/shop/products')
    })
    items.unshift({
      key: 'admin',
      label: '后台管理',
      hint: '进入管理员上新与上下架面板',
      to: buildPath('/admin/collectibles')
    })
    items.unshift({
      key: 'admin-posts',
      label: '文章管理',
      hint: '集中管理社区文章内容',
      to: buildPath('/admin/community')
    })
    items.unshift({
      key: 'admin-features',
      label: '新增功能管理',
      hint: '查看新增能力后台入口',
      to: buildPath('/admin/features')
    })
  }
  return items
})

const toggleAccountMenu = () => {
  accountMenuOpen.value = !accountMenuOpen.value
}

const handleLogout = () => {
  collectiblesAuthAPI.logout()
  syncUser()
  showMobileMenu.value = false
  accountMenuOpen.value = false
  if (!route.path.endsWith('/user-login')) {
    router.push(buildPath('/user-login'))
  }
}

const updateScrolledState = () => {
  const nextValue = window.scrollY > 20
  if (nextValue === isScrolled.value) return
  isScrolled.value = nextValue
}

const handleScroll = () => {
  if (scrollRafId !== null) return
  scrollRafId = window.requestAnimationFrame(() => {
    scrollRafId = null
    updateScrolledState()
  })
}

const handleStorage = (event) => {
  if (!event.key || event.key === 'yc_user' || event.key === 'yc_token') {
    syncUser()
  }
}

const handleDocumentClick = (event) => {
  if (!accountMenuOpen.value) return
  const container = accountMenuRef.value
  if (!container) return
  if (container.contains(event.target)) return
  accountMenuOpen.value = false
}

const handleDocumentKeydown = (event) => {
  if (event.key === 'Escape') {
    accountMenuOpen.value = false
  }
}

watch(
  () => route.fullPath,
  () => {
    accountMenuOpen.value = false
    syncUser()
  }
)

onMounted(() => {
  syncUser()
  updateScrolledState()
  window.addEventListener('scroll', handleScroll, { passive: true })
  window.addEventListener('storage', handleStorage)
  window.addEventListener('click', handleDocumentClick)
  window.addEventListener('keydown', handleDocumentKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('storage', handleStorage)
  window.removeEventListener('click', handleDocumentClick)
  window.removeEventListener('keydown', handleDocumentKeydown)
  if (scrollRafId !== null) {
    window.cancelAnimationFrame(scrollRafId)
    scrollRafId = null
  }
})
</script>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(8px);
  background:
    linear-gradient(180deg, rgba(17, 17, 17, 0.9), rgba(7, 7, 7, 0.92));
  border-bottom: 1px solid rgba(255, 220, 160, 0.12);
  transition: background 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
}

.site-header::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.5;
  background:
    radial-gradient(circle at 14% 0%, rgba(255, 245, 200, 0.16), transparent 30%),
    radial-gradient(circle at 88% 36%, rgba(255, 231, 176, 0.08), transparent 28%);
  filter: blur(20px);
}

.site-header.scrolled {
  background: rgba(7, 7, 7, 0.98);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.34);
  border-bottom-color: rgba(255, 220, 160, 0.12);
}

.header-inner {
  width: min(1260px, 94vw);
  margin: 0 auto;
  min-height: 86px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 22px;
  padding: 10px 0;
}

/* ── Brand ── */
.brand {
  border: none;
  background: transparent;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: var(--ym-text);
  padding: 0;
}

.brand-logo {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.12);
}

.brand-text {
  font-family: var(--ym-font-calligraphy-ma);
  font-size: 1.48rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--ym-gold);
}

/* ── Main Nav ── */
.main-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.nav-link {
  border: 1px solid var(--ym-border);
  background: rgba(17, 17, 17, 0.72);
  color: #ffe7b0;
  border-radius: 4px 12px 4px 12px;
  padding: 9px 18px;
  cursor: pointer;
  font-size: 1.02rem;
  font-weight: 600;
  transition: all 0.24s ease;
  white-space: nowrap;
  font-family: var(--ym-font-calligraphy-ma);
  letter-spacing: 0.08em;
  position: relative;
}

.nav-link::after {
  content: '';
  position: absolute;
  inset: auto 8px 6px;
  height: 1px;
  background: rgba(var(--ym-gold-rgb), 0.56);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform 0.24s ease;
}

.nav-link:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.34);
  background: rgba(var(--ym-accent-rgb), 0.08);
  color: #ffd36b;
  transform: translateY(-1px);
}

.nav-link:hover::after {
  transform: scaleX(1);
}

.nav-link.active {
  border-color: rgba(var(--ym-gold-rgb), 0.22);
  background:
    linear-gradient(140deg, rgba(var(--ym-accent-rgb), 0.16), rgba(var(--ym-gold-rgb), 0.08));
  box-shadow: inset 0 1px 0 rgba(255, 245, 200, 0.06);
  color: #ffd36b;
  font-weight: 600;
}

.nav-link:active {
  border-color: rgba(var(--ym-gold-rgb), 0.24);
  background: rgba(var(--ym-accent-rgb), 0.16);
  color: var(--ym-gold);
}

/* ── Account ── */
.account-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-entry-btn {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.38);
  border-radius: 5px 12px 5px 12px;
  background: rgba(17, 17, 17, 0.76);
  color: #ffe7b0;
  min-height: 42px;
  padding: 8px 14px;
  font-size: 0.84rem;
  font-family: var(--ym-font-calligraphy-ma);
  font-weight: 600;
  letter-spacing: 0.08em;
  cursor: pointer;
  transition: all 0.2s ease;
}

.admin-entry-btn:hover {
  background: rgba(var(--ym-accent-rgb), 0.1);
  border-color: rgba(var(--ym-accent-rgb), 0.42);
  color: #ffd36b;
}

.account-dropdown {
  position: relative;
}

.account-pill {
  border: 1px solid var(--ym-border);
  border-radius: 8px 18px 8px 18px;
  background: rgba(17, 17, 17, 0.82);
  min-height: 58px;
  padding: 8px 12px 8px 10px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: var(--ym-text);
  transition: all 0.2s ease;
}

.account-pill:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.34);
  background: rgba(17, 17, 17, 0.92);
}

.account-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 250px;
  border: 1px solid rgba(217, 177, 90, 0.34);
  border-radius: 8px 18px 8px 18px;
  background: linear-gradient(160deg, rgba(17, 17, 17, 0.98), rgba(5, 5, 5, 0.98));
  box-shadow: 0 18px 34px rgba(0, 0, 0, 0.48), 0 0 0 1px rgba(255, 220, 160, 0.08) inset;
  padding: 8px;
  display: grid;
  gap: 6px;
  z-index: 30;
}

.menu-item {
  border: 1px solid rgba(255, 220, 160, 0.1);
  border-radius: 6px 12px 6px 12px;
  background: rgba(8, 8, 8, 0.92);
  color: #f3efe8;
  padding: 8px 10px;
  text-align: left;
  display: grid;
  gap: 2px;
  cursor: pointer;
  font: inherit;
  transition: all 0.2s ease;
}

.menu-item span {
  font-size: 0.9rem;
  color: #ffe7b0;
  font-family: var(--ym-font-calligraphy-ma);
  font-weight: 600;
}

.menu-item small {
  font-size: 0.74rem;
  color: #9a8f84;
}

.menu-item:hover {
  border-color: rgba(255, 211, 107, 0.42);
  background: linear-gradient(135deg, rgba(217, 177, 90, 0.16), rgba(161, 75, 52, 0.1));
  box-shadow: 0 0 0 1px rgba(255, 220, 160, 0.08) inset;
}

.menu-item:hover span {
  color: #ffd36b;
}

.menu-item:hover small {
  color: #d6c5ad;
}

.menu-item.danger {
  border-color: rgba(212, 116, 97, 0.34);
  background: linear-gradient(135deg, rgba(212, 116, 97, 0.16), rgba(17, 17, 17, 0.94));
}

.menu-item.danger span {
  color: #e48d76;
}

.menu-item.danger small {
  color: #c69486;
}

.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  border: 1px solid rgba(var(--ym-gold-rgb), 0.2);
  background: rgba(var(--ym-gold-rgb), 0.1);
  display: grid;
  place-items: center;
  font-family: var(--ym-font-seal);
  color: var(--ym-gold);
  font-size: 1rem;
  font-weight: 700;
  flex-shrink: 0;
}

.account-meta {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.2;
}

.account-meta strong {
  font-size: 0.98rem;
  color: #ffe7b0;
  font-family: var(--ym-font-calligraphy-ma);
  font-weight: 600;
}

.account-meta small {
  margin-top: 3px;
  color: var(--ym-text-muted);
  font-size: 0.74rem;
}

.login-btn {
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 4px 12px;
  background: rgba(17, 17, 17, 0.76);
  color: #ffe7b0;
  padding: 9px 16px;
  cursor: pointer;
  font-size: 0.92rem;
  font-family: var(--ym-font-calligraphy-ma);
  font-weight: 600;
  letter-spacing: 0.08em;
  transition: all 0.2s ease;
}

.login-btn:hover {
  border-color: rgba(var(--ym-accent-rgb), 0.34);
  background: rgba(var(--ym-accent-rgb), 0.08);
  color: #ffd36b;
}

/* ── Mobile Toggle ── */
.menu-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  border: none;
  background: transparent;
  width: 38px;
  height: 38px;
  padding: 6px;
  cursor: pointer;
}

.menu-toggle span {
  display: block;
  width: 100%;
  height: 2px;
  background: var(--ym-gold);
  border-radius: 2px;
  transition: transform 0.24s ease, opacity 0.24s ease;
  transform-origin: center;
}

.menu-toggle span.open:nth-child(1) { transform: translateY(7px) rotate(45deg); }
.menu-toggle span.open:nth-child(2) { opacity: 0; }
.menu-toggle span.open:nth-child(3) { transform: translateY(-7px) rotate(-45deg); }

/* ── Focus ── */
.nav-link:focus-visible,
.account-pill:focus-visible,
.admin-entry-btn:focus-visible,
.login-btn:focus-visible,
.brand:focus-visible,
.menu-toggle:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 2px;
}

/* ── Responsive ── */
@media (max-width: 1120px) {
  .nav-link {
    padding: 9px 16px;
    font-size: 0.95rem;
  }
}

@media (max-width: 980px) {
  .header-inner {
    grid-template-columns: auto 1fr auto;
    min-height: 72px;
    padding: 8px 0;
  }

  .menu-toggle {
    display: flex;
    margin-left: 6px;
    order: 3;
  }

  .account-meta,
  .account-menu small {
    display: none;
  }

  .admin-entry-btn {
    min-height: 36px;
    padding: 6px 10px;
    font-size: 0.76rem;
  }

  .account-pill {
    min-height: 44px;
    padding: 4px 8px;
  }

  .account-avatar {
    width: 30px;
    height: 30px;
    font-size: 0.86rem;
    border-width: 1px;
  }

  .login-btn {
    padding: 7px 12px;
    font-size: 0.84rem;
  }

  .account-menu {
    right: -12px;
    width: min(80vw, 260px);
  }

  .main-nav {
    position: absolute;
    top: 72px;
    left: 0;
    right: 0;
    padding: 10px 16px 16px;
    border-bottom: 1px solid var(--ym-border);
    background: rgba(7, 7, 7, 0.98);
    backdrop-filter: blur(14px);
    display: none;
    flex-direction: column;
    align-items: stretch;
    gap: 4px;
  }

  .main-nav.open {
    display: flex;
  }

  .nav-link {
    border-radius: 12px;
    text-align: left;
    padding: 11px 14px;
    width: 100%;
  }
}

@media (max-width: 620px) {
  .brand-text {
    font-size: 1.03rem;
  }
}
</style>
