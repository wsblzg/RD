<template>
  <CeramicsPageShell
    kicker="ABOUT"
    title="关于"
    description="补全项目可信度与背景信息，呈现版权说明。"
    :sub-nav-items="aboutNavItems"
  >
    <section class="copyright-grid">
      <article class="info-card">
        <h3>版权与素材声明</h3>
        <p>站内图文、模型与调研素材仅用于非遗传播、教学展示与项目答辩，不用于金融化或投机化用途。</p>
        <ul>
          <li>引用素材需标注来源与授权范围。</li>
          <li>用户上传内容须遵守国家法律法规与平台规范。</li>
          <li>如有侵权或授权争议，可通过项目邮箱反馈处理。</li>
        </ul>
      </article>
      <article class="info-card">
        <h3>使用边界</h3>
        <div class="rule-list">
          <div><strong>允许</strong><span>教学展示 / 学术答辩 / 公益传播</span></div>
          <div><strong>限制</strong><span>二次商业包装 / 未授权转载 / 误导性营销</span></div>
          <div><strong>禁止</strong><span>金融化交易、投机炒作、侵权牟利</span></div>
        </div>
      </article>
    </section>

    <section class="process-card">
      <h3>侵权反馈流程</h3>
      <ol>
        <li>邮件提交：说明素材位置、权属证明与处理诉求。</li>
        <li>平台初审：24小时内响应，必要时先行下线争议内容。</li>
        <li>协同核验：与内容提供方、运营方及指导团队核对授权链路。</li>
        <li>结果反馈：确认后恢复或永久下线，并更新素材台账。</li>
      </ol>
      <p class="tip">后续如需版权治理增强，可通过运营规范与法务流程单独建设，不并入本期技术主链路。</p>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { aboutNavItems } from './navs'
</script>

<style scoped>
.copyright-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.info-card,
.process-card {
  border: 1px solid rgba(255, 245, 230, 0.38);
  border-radius: 18px;
  padding: 22px;
  background: rgba(255, 248, 236, 0.72);
  backdrop-filter: blur(4px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

.info-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
  color: #4b3829;
}

.info-card p,
.info-card li {
  margin-top: 10px;
  color: #5b4634;
  line-height: 1.8;
}

.info-card ul {
  margin-top: 12px;
  padding-left: 18px;
}

.rule-list {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.rule-list div {
  border: 1px solid rgba(255, 245, 230, 0.48);
  border-radius: 12px;
  padding: 12px 14px;
  background: rgba(255, 252, 246, 0.62);
  backdrop-filter: blur(3px);
  display: grid;
  gap: 4px;
}

.rule-list strong {
  color: #6a4a31;
  font-size: 0.88rem;
  letter-spacing: 0.04em;
}

.rule-list span {
  font-size: 0.88rem;
  color: #5b4634;
}

.process-card {
  margin-top: 14px;
}

.process-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
  color: #4b3829;
}

.process-card ol {
  margin-top: 12px;
  padding-left: 18px;
  line-height: 2;
  color: #5b4634;
}

.tip {
  border-top: 1px dashed var(--ym-border-strong);
  padding-top: 12px;
  margin-top: 12px;
  font-size: 0.88rem;
  color: #6f5642;
  line-height: 1.75;
}

@media (max-width: 980px) {
  .copyright-grid {
    grid-template-columns: 1fr;
  }
}
</style>
