<template>
  <CeramicsPageShell
    kicker="ABOUT"
    title="关于"
    description="由内容、设计、技术与研学实践共同支撑，让非遗表达更接近真实场景。"
    :sub-nav-items="aboutNavItems"
  >
    <section class="team-grid">
      <article class="team-card">
        <h3>项目负责人</h3>
        <p>黄烨玲</p>
        <p>联系电话：18128238105</p>
        <p>邮箱：3677941411@qq.com</p>
        <p class="desc">统筹项目方向、展示叙事与成果落地，推进非遗内容的数字化表达。</p>
      </article>
      <article class="team-card">
        <h3>指导教师</h3>
        <p>钟阳莉老师</p>
        <p>负责项目方向把控与内容审校。</p>
        <p class="desc">提供学术规范、传播叙事与非遗研究方法支持。</p>
      </article>
      <article class="team-card">
        <h3>合作方向与单位</h3>
        <p>校地联动、非遗传承人访谈、线下实践活动组织。</p>
        <p class="desc">覆盖课程实践、实地调研、活动执行与内容再创作。</p>
      </article>
    </section>

    <section class="role-card">
      <h3>团队分工示意</h3>
      <div class="role-grid">
        <article><strong>内容组</strong><p>梳理导览文案、匠人故事与柴烧知识素材。</p></article>
        <article><strong>设计组</strong><p>建立视觉风格、页面节奏与用户阅读体验。</p></article>
        <article><strong>体验组</strong><p>实现页面呈现、交互反馈与多终端访问体验。</p></article>
        <article><strong>服务组</strong><p>支撑用户账户、作品沉淀、订单与管理流程。</p></article>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { aboutNavItems } from './navs'
</script>

<style scoped>
.team-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.team-card {
  border: 1px solid rgba(255, 245, 230, 0.38);
  border-radius: 18px;
  padding: 20px;
  background: rgba(255, 248, 236, 0.72);
  backdrop-filter: blur(4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.team-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1rem;
  color: #4b3829;
}

.team-card p {
  margin-top: 8px;
  color: #5b4634;
  line-height: 1.75;
}

.desc {
  margin-top: 12px;
  font-size: 0.88rem;
  color: #6f5642;
  padding-top: 10px;
  border-top: 1px dashed var(--ym-border-strong);
}

.role-card {
  margin-top: 14px;
  border: 1px solid rgba(255, 245, 230, 0.38);
  border-radius: 18px;
  padding: 22px;
  background: rgba(255, 248, 236, 0.72);
  backdrop-filter: blur(4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.role-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
  color: #4b3829;
}

.role-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.role-grid article {
  border: 1px solid rgba(255, 245, 230, 0.48);
  border-radius: 12px;
  padding: 14px;
  background: rgba(255, 252, 246, 0.62);
  backdrop-filter: blur(3px);
}

.role-grid strong {
  display: block;
  color: #6a4a31;
  font-family: var(--ym-font-display);
  margin-bottom: 6px;
}

.role-grid p {
  font-size: 0.86rem;
  line-height: 1.75;
  color: #5b4634;
}

@media (max-width: 980px) {
  .team-grid,
  .role-grid {
    grid-template-columns: 1fr;
  }
}
</style>
