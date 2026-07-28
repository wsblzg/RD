<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <section class="footer-col brand-col">
        <div class="brand-head">
          <img class="brand-mark" src="/logo.webp" alt="窑创未来logo" />
          <h3>窑创未来</h3>
        </div>
        <p>AI 智焕柴烧陶瓷非遗数字导览馆</p>
        <p>面向公众介绍柴烧工艺、审美和在地文化，让导览、智鉴、收藏与线下参与自然连起来。</p>
        <div class="brand-tags">
          <span>非遗数字化</span>
          <span>智能导览</span>
          <span>文化传播</span>
        </div>
      </section>

      <section class="footer-col">
        <h3>平台能力</h3>
        <router-link to="/ceramics/guide/kiln">柴烧数字导览</router-link>
        <router-link to="/ceramics/intelligence/appraisal">AI 辅助鉴赏</router-link>
        <router-link to="/ceramics/intelligence/qa">柴烧知识问答</router-link>
        <router-link to="/ceramics/collections/catalog">数字藏品展示</router-link>
        <router-link to="/ceramics/transformation/research">实践成果展示</router-link>
      </section>

      <section class="footer-col">
        <h3>内容服务</h3>
        <router-link to="/ceramics/home">项目首页</router-link>
        <router-link to="/ceramics/guide/works">马坝风采</router-link>
        <router-link to="/ceramics/guide/process">制作步骤</router-link>
        <router-link :to="{ path: '/ceramics/user-center', query: { tab: 'collections' } }">我的收藏</router-link>
        <router-link to="/ceramics/about/project">项目档案</router-link>
      </section>

      <section class="footer-col">
        <h3>关于我们</h3>
        <router-link to="/ceramics/about/project">项目介绍</router-link>
        <router-link to="/ceramics/about/team">团队与合作</router-link>
        <router-link to="/ceramics/about/copyright">版权说明</router-link>
        <router-link to="/ceramics/community">社区广场</router-link>
        <router-link to="/ceramics/transformation/visit">到访与参与</router-link>
      </section>

    </div>
    <div class="footer-bottom">
      <p>© {{ currentYear }} 窑创未来. 图片视频素材如有侵权请联系删除。本站仅用于教学展示与非遗传播。</p>
    </div>
  </footer>
</template>

<script setup>
const currentYear = new Date().getFullYear()
</script>

<style scoped>
.site-footer {
  margin-top: 36px;
  background:
    radial-gradient(circle at 12% 20%, rgba(var(--ym-accent-rgb), 0.18), transparent 36%),
    radial-gradient(circle at 88% 80%, rgba(var(--ym-support-rgb), 0.14), transparent 34%),
    linear-gradient(180deg, #171513 0%, #0f0e0d 100%);
  color: rgba(255, 244, 230, 0.88);
  border-top: 1px solid rgba(var(--ym-gold-rgb), 0.32);
  position: relative;
  overflow: hidden;
}

.site-footer::before {
  content: '';
  position: absolute;
  inset: auto -10% -40% -10%;
  height: 70%;
  pointer-events: none;
  background:
    radial-gradient(circle at 22% 48%, rgba(var(--ym-gold-rgb), 0.28), transparent 46%),
    radial-gradient(circle at 76% 54%, rgba(var(--ym-accent-rgb), 0.2), transparent 52%);
  filter: blur(24px);
  opacity: 0.32;
}

.footer-inner {
  width: min(1260px, 94vw);
  margin: 0 auto;
  padding: 48px 0 28px;
  display: grid;
  grid-template-columns: 1.35fr repeat(3, minmax(0, 1fr));
  gap: 22px;
}

.footer-col h3 {
  margin-bottom: 14px;
  font-family: var(--ym-font-display);
  font-size: 1.02rem;
  color: rgba(255, 242, 215, 0.95);
  letter-spacing: 0.05em;
}

.footer-col p,
.footer-col a {
  display: block;
  color: rgba(244, 231, 209, 0.8);
  line-height: 1.85;
  text-decoration: none;
  font-size: 0.9rem;
}

.footer-col a:hover {
  color: rgba(var(--ym-gold-rgb), 0.95);
}

.brand-head {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.brand-head h3 {
  margin: 0;
}

.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  object-fit: cover;
  display: block;
  border: 1px solid rgba(var(--ym-gold-rgb), 0.64);
}

.brand-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.brand-tags span {
  border: 1px solid rgba(var(--ym-gold-rgb), 0.36);
  border-radius: 4px 12px 4px 12px;
  padding: 3px 9px;
  font-size: 0.74rem;
  color: rgba(255, 230, 186, 0.88);
  background: rgba(212, 175, 55, 0.08);
}

.mini-tip {
  margin-top: 8px;
  font-size: 0.8rem;
  color: rgba(248, 219, 167, 0.66);
}

.footer-bottom {
  border-top: 1px solid rgba(var(--ym-gold-rgb), 0.2);
  padding: 14px 0 16px;
}

.footer-bottom p {
  width: min(1260px, 94vw);
  margin: 0 auto;
  color: rgba(240, 220, 184, 0.64);
  font-size: 0.86rem;
}

@media (max-width: 1080px) {
  .footer-inner {
    grid-template-columns: 1.4fr repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .footer-inner {
    grid-template-columns: 1fr;
  }
}
</style>
