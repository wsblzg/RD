import { createRouter, createWebHistory } from 'vue-router'
const HomeView = () => import('../views/ceramics/HomeView.vue')
const GuideKilnView = () => import('../views/ceramics/GuideKilnView.vue')
const GuideWorksView = () => import('../views/ceramics/GuideWorksView.vue')
const GuideProcessView = () => import('../views/ceramics/GuideProcessView.vue')
const GuideArtisansView = () => import('../views/ceramics/GuideArtisansView.vue')
const CollectionsCatalogView = () => import('../views/ceramics/CollectionsCatalogView.vue')
const CollectionsDetailView = () => import('../views/ceramics/CollectionsDetailView.vue')
const ShopCatalogView = () => import('../views/ceramics/ShopCatalogView.vue')
const ShopDetailView = () => import('../views/ceramics/ShopDetailView.vue')
const ShopCartView = () => import('../views/ceramics/ShopCartView.vue')
const UserCenterView = () => import('../views/ceramics/UserCenterView.vue')
const IntelligenceAppraisalView = () => import('../views/ceramics/IntelligenceAppraisalView.vue')
const IntelligenceQaView = () => import('../views/ceramics/IntelligenceQaView.vue')
const AiCreationStudyView = () => import('../views/ceramics/AiCreationStudyView.vue')
const TransformationResearchView = () => import('../views/ceramics/TransformationResearchView.vue')
const TransformationVisitView = () => import('../views/ceramics/TransformationVisitView.vue')
const AboutProjectView = () => import('../views/ceramics/AboutProjectView.vue')
const AboutTeamView = () => import('../views/ceramics/AboutTeamView.vue')
const AboutCopyrightView = () => import('../views/ceramics/AboutCopyrightView.vue')
const UserLoginView = () => import('../views/ceramics/UserLoginView.vue')
const CommunitySquareView = () => import('../views/ceramics/CommunitySquareView.vue')
const CommunityPublishView = () => import('../views/ceramics/CommunityPublishView.vue')
const CommunityPostDetailView = () => import('../views/ceramics/CommunityPostDetailView.vue')
const AdminFeaturesView = () => import('../views/ceramics/AdminFeaturesView.vue')
const AdminCollectiblesView = () => import('../views/ceramics/AdminCollectiblesView.vue')
const AdminCommunityPostsView = () => import('../views/ceramics/AdminCommunityPostsView.vue')
const AdminShopProductsView = () => import('../views/ceramics/AdminShopProductsView.vue')
const AdminShopOrdersView = () => import('../views/ceramics/AdminShopOrdersView.vue')
const AdminPointsView = () => import('../views/ceramics/AdminPointsView.vue')
const CERAMICS_BASE = '/ceramics'
const withCeramics = (path) => `${CERAMICS_BASE}${path}`

const routes = [
  {
    path: '/',
    redirect: withCeramics('/home')
  },
  {
    path: '/home',
    redirect: withCeramics('/home')
  },
  {
    path: '/new-home',
    redirect: withCeramics('/home')
  },
  {
    path: '/user-login',
    redirect: withCeramics('/user-login')
  },
  {
    path: '/login',
    redirect: withCeramics('/user-login')
  },
  {
    path: '/guide',
    redirect: withCeramics('/guide/kiln')
  },
  {
    path: '/guide/kiln',
    redirect: withCeramics('/guide/kiln')
  },
  {
    path: '/guide/works',
    redirect: withCeramics('/guide/works')
  },
  {
    path: '/guide/process',
    redirect: withCeramics('/guide/process')
  },
  {
    path: '/guide/artisans',
    redirect: withCeramics('/guide/artisans')
  },
  {
    path: '/collections',
    redirect: withCeramics('/collections/catalog')
  },
  {
    path: '/collections/catalog',
    redirect: withCeramics('/collections/catalog')
  },
  {
    path: '/collections/mine',
    redirect: withCeramics('/collections/mine')
  },
  {
    path: '/collections/item/:id',
    redirect: (to) => withCeramics(`/collections/item/${to.params.id}`)
  },
  {
    path: '/shop',
    redirect: withCeramics('/shop')
  },
  {
    path: '/shop/cart',
    redirect: withCeramics('/shop/cart')
  },
  {
    path: '/shop/product/:id',
    redirect: (to) => withCeramics(`/shop/product/${to.params.id}`)
  },
  {
    path: '/community',
    redirect: withCeramics('/community')
  },
  {
    path: '/community/publish',
    redirect: (to) => ({ path: withCeramics('/community/publish'), query: to.query })
  },
  {
    path: '/community/post/:id',
    redirect: (to) => withCeramics(`/community/post/${to.params.id}`)
  },
  {
    path: '/admin',
    redirect: withCeramics('/admin/features')
  },
  {
    path: '/admin/features',
    redirect: withCeramics('/admin/features')
  },
  {
    path: '/admin/collectibles',
    redirect: withCeramics('/admin/collectibles')
  },
  {
    path: '/admin/community',
    redirect: withCeramics('/admin/community')
  },
  {
    path: '/admin/shop/products',
    redirect: withCeramics('/admin/shop/products')
  },
  {
    path: '/admin/shop/orders',
    redirect: withCeramics('/admin/shop/orders')
  },
  {
    path: '/user-center',
    redirect: (to) => ({ path: withCeramics('/user-center'), query: to.query })
  },
  {
    path: '/intelligence',
    redirect: withCeramics('/intelligence/qa')
  },
  {
    path: '/intelligence/appraisal',
    redirect: withCeramics('/intelligence/appraisal')
  },
  {
    path: '/intelligence/qa',
    redirect: withCeramics('/intelligence/qa')
  },
  {
    path: '/ai-creation',
    redirect: withCeramics('/ai-creation')
  },
  {
    path: '/transformation',
    redirect: withCeramics('/about/practice')
  },
  {
    path: '/transformation/research',
    redirect: withCeramics('/about/practice')
  },
  {
    path: '/transformation/visit',
    redirect: withCeramics('/about/visit')
  },
  {
    path: '/about',
    redirect: withCeramics('/about/project')
  },
  {
    path: '/about/project',
    redirect: withCeramics('/about/project')
  },
  {
    path: '/about/team',
    redirect: withCeramics('/about/team')
  },
  {
    path: '/about/practice',
    redirect: withCeramics('/about/practice')
  },
  {
    path: '/about/visit',
    redirect: withCeramics('/about/visit')
  },
  {
    path: '/about/copyright',
    redirect: withCeramics('/about/copyright')
  },
  {
    path: CERAMICS_BASE,
    redirect: withCeramics('/home')
  },
  {
    path: withCeramics('/home'),
    name: 'CeramicsHome',
    component: HomeView,
    meta: {
      title: '首页'
    }
  },
  {
    path: withCeramics('/user-login'),
    component: UserLoginView,
    meta: { title: '用户登录' }
  },
  {
    path: withCeramics('/guide/kiln'),
    component: GuideKilnView,
    meta: { title: '柴烧导览 - 柴烧秘境' }
  },
  {
    path: withCeramics('/guide/works'),
    component: GuideWorksView,
    meta: { title: '柴烧导览 - 马坝风采' }
  },
  {
    path: withCeramics('/guide/process'),
    component: GuideProcessView,
    meta: { title: '柴烧导览 - 制作步骤' }
  },
  {
    path: withCeramics('/guide/artisans'),
    component: GuideArtisansView,
    meta: { title: '柴烧导览 - 非遗匠人' }
  },
  {
    path: withCeramics('/community'),
    component: CommunitySquareView,
    meta: { title: '社区广场' }
  },
  {
    path: withCeramics('/shop'),
    component: ShopCatalogView,
    meta: { title: '文创商城' }
  },
  {
    path: withCeramics('/shop/cart'),
    component: ShopCartView,
    meta: { title: '文创商城 - 购物车' }
  },
  {
    path: withCeramics('/shop/product/:id'),
    component: ShopDetailView,
    meta: { title: '文创商城 - 商品详情' }
  },
  {
    path: withCeramics('/community/publish'),
    component: CommunityPublishView,
    meta: { title: '社区发布 - 发布文章' }
  },
  {
    path: withCeramics('/community/post/:id'),
    component: CommunityPostDetailView,
    meta: { title: '社区文章详情' }
  },
  {
    path: withCeramics('/admin/features'),
    component: AdminFeaturesView,
    meta: { title: '后台管理 - 新增功能管理' }
  },
  {
    path: withCeramics('/admin/collectibles'),
    component: AdminCollectiblesView,
    meta: { title: '后台管理 - 藏品管理' }
  },
  {
    path: withCeramics('/admin/community'),
    component: AdminCommunityPostsView,
    meta: { title: '后台管理 - 文章管理' }
  },
  {
    path: withCeramics('/admin/shop/products'),
    component: AdminShopProductsView,
    meta: { title: '后台管理 - 商城商品' }
  },
  {
    path: withCeramics('/admin/shop/orders'),
    component: AdminShopOrdersView,
    meta: { title: '后台管理 - 商城订单' }
  },
  {
    path: withCeramics('/admin/points'),
    component: AdminPointsView,
    meta: { title: '后台管理 - 积分审核' }
  },
  {
    path: withCeramics('/collections/catalog'),
    component: CollectionsCatalogView,
    meta: { title: '数字藏品馆 - 藏品总览' }
  },
  {
    path: withCeramics('/user-center'),
    component: UserCenterView,
    meta: { title: '用户中心' }
  },
  {
    path: withCeramics('/collections/mine'),
    redirect: { path: withCeramics('/user-center'), query: { tab: 'collections' } }
  },
  {
    path: withCeramics('/collections/item/:id'),
    component: CollectionsDetailView,
    meta: { title: '数字藏品馆 - 藏品详情' }
  },
  {
    path: withCeramics('/intelligence/appraisal'),
    component: IntelligenceAppraisalView,
    meta: { title: '智鉴中枢 - AI辅助鉴赏' }
  },
  {
    path: withCeramics('/intelligence/qa'),
    component: IntelligenceQaView,
    meta: { title: '智鉴中枢 - 柴烧知识问答' }
  },
  {
    path: withCeramics('/ai-creation'),
    component: AiCreationStudyView,
    meta: { title: '窑火造物' }
  },
  {
    path: withCeramics('/transformation/research'),
    redirect: withCeramics('/about/practice')
  },
  {
    path: withCeramics('/transformation/visit'),
    redirect: withCeramics('/about/visit')
  },
  {
    path: withCeramics('/about/practice'),
    component: TransformationResearchView,
    meta: { title: '关于 - 实践成果' }
  },
  {
    path: withCeramics('/about/visit'),
    component: TransformationVisitView,
    meta: { title: '关于 - 到访参与' }
  },
  {
    path: withCeramics('/about/project'),
    component: AboutProjectView,
    meta: { title: '关于 - 项目介绍' }
  },
  {
    path: withCeramics('/about/team'),
    component: AboutTeamView,
    meta: { title: '关于 - 团队与合作' }
  },
  {
    path: withCeramics('/about/copyright'),
    component: AboutCopyrightView,
    meta: { title: '关于 - 版权说明' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: withCeramics('/home')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0, left: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 柴智云` : '柴智云'
  next()
})

router.afterEach((to, from) => {
  window.scrollTo(0, 0)
  document.documentElement.scrollTop = 0
  document.body.scrollTop = 0
})

export default router
