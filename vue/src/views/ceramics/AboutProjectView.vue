<template>
  <CeramicsPageShell
    kicker="ABOUT"
    title="关于"
    description="以数字化方式讲述柴烧陶瓷的工艺之美、传承脉络与当代表达。"
    :sub-nav-items="aboutNavItems"
  >
    <section class="about-grid">
      <article class="info-card">
        <h3>项目定位</h3>
        <p>
          本项目以“柴烧非遗数字化展示与智能化解读”为核心，构建集文化传播、专业导览、
          智能辅助、数字收藏、实践成果与到访参与于一体的综合型网页产品。
        </p>
        <div class="value-list">
          <div><strong>看见工艺</strong><span>大图叙事 + 3D窑炉导览</span></div>
          <div><strong>看懂柴烧</strong><span>AI辅助鉴赏 + 知识问答</span></div>
          <div><strong>持续沉淀</strong><span>数字藏品馆 + 运营机制</span></div>
        </div>
      </article>
      <article class="info-card">
        <h3>技术路线</h3>
        <ul>
          <li>体验层：以清晰的信息结构串联导览、鉴赏、商城、社区与研学服务。</li>
          <li>智能层：融合图像理解、知识问答与 3D 生成，辅助用户看见工艺细节。</li>
          <li>内容层：沉淀藏品、作品、文章与活动数据，让非遗传播可持续生长。</li>
          <li>媒体层：围绕高清图片与 3D 模型优化加载体验，兼顾桌面端与移动端访问。</li>
        </ul>
      </article>
    </section>

    <section class="evolve-card">
      <h3>版本演进</h3>
      <div class="evolve-list">
        <article><span>P0</span><p>完成非遗导览、数字藏品与核心页面体系建设。</p></article>
        <article><span>P1</span><p>拓展 AI 鉴赏、知识问答与个人作品沉淀能力。</p></article>
        <article><span>P2</span><p>完善研学预约、社区展示与文创商城的运营闭环。</p></article>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { aboutNavItems } from './navs'
</script>

<style scoped>
.about-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.info-card,
.evolve-card {
  border: 1px solid rgba(255, 245, 230, 0.38);
  border-radius: 18px;
  padding: 22px;
  background: rgba(255, 248, 236, 0.72);
  backdrop-filter: blur(4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.info-card h3 {
  margin-top: 12px;
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
  color: #4b3829;
}

.info-card h3:first-child {
  margin-top: 0;
}

.info-card p {
  margin-top: 10px;
  color: #5b4634;
  line-height: 1.8;
}

.info-card ul {
  margin-top: 12px;
  padding-left: 18px;
  color: #5b4634;
  line-height: 2;
}

.value-list {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.value-list div {
  border: 1px solid rgba(255, 245, 230, 0.48);
  border-radius: 12px;
  padding: 12px 14px;
  background: rgba(255, 252, 246, 0.62);
  backdrop-filter: blur(3px);
  display: grid;
  gap: 4px;
}

.value-list strong {
  color: #6a4a31;
  font-family: var(--ym-font-display);
}

.value-list span {
  color: #5b4634;
  font-size: 0.88rem;
}

.evolve-card {
  margin-top: 14px;
}

.evolve-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
  color: #4b3829;
}

.evolve-list {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.evolve-list article {
  border: 1px solid rgba(255, 245, 230, 0.48);
  border-radius: 14px;
  padding: 14px;
  background: rgba(255, 252, 246, 0.62);
  backdrop-filter: blur(3px);
}

.evolve-list span {
  display: inline-flex;
  border-radius: 999px;
  padding: 3px 10px;
  font-size: 0.76rem;
  color: #6a4a31;
  background: rgba(106, 74, 49, 0.12);
  border: 1px solid rgba(106, 74, 49, 0.24);
}

.evolve-list p {
  margin-top: 10px;
  color: #5b4634;
  font-size: 0.9rem;
  line-height: 1.75;
}

@media (max-width: 980px) {
  .about-grid,
  .evolve-list {
    grid-template-columns: 1fr;
  }
}
</style>
