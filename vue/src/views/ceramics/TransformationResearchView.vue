<template>
  <CeramicsPageShell
    kicker="ABOUT"
    title="关于"
    description="汇总项目调研证据、成果框架与页面转译逻辑，作为“关于”板块中的实践成果页。"
    :sub-nav-items="aboutNavItems"
    :alt="true"
  >
    <section class="evidence-banner">
      <p class="banner-kicker">FIELD EVIDENCE</p>
      <h3>从“马坝人遗址”到“曲江柴烧工作站”的一线证据链</h3>
      <p>
        调研内容来自《李晶老师笔记》《非遗传承人、匠人信息》与项目设计方案，围绕“历史源流、工艺机制、传承主体、公众认知”
        四条线索建立可核验证据。
      </p>
      <div class="source-tags">
        <span>6000年前石峡文化陶器遗存</span>
        <span>2018年录入韶关市非遗项目</span>
        <span>龙志雄 / 李晶一线访谈资料</span>
      </div>
    </section>

    <section class="framework-grid">
      <article class="framework-card">
        <h3>“一库一脑一平台一链”项目框架</h3>
        <div class="framework-list">
          <article v-for="item in frameworkPillars" :key="item.title" class="framework-item">
            <p class="framework-title">{{ item.title }}</p>
            <p>{{ item.desc }}</p>
            <span>{{ item.value }}</span>
          </article>
        </div>
      </article>
    </section>

    <section class="research-grid">
      <article class="info-card">
        <h3>调研资料结构</h3>
        <p>按“文本资料 + 人物访谈 + 场地观察 + 传播反馈”四类归档，所有结论均可追溯到原始材料。</p>
        <ul>
          <li>文献线：柴烧历史源流、窑火工艺机理、非遗政策定位</li>
          <li>人物线：传承人龙志雄、教育推广老师李晶口述材料</li>
          <li>场地线：马坝人遗址公园内非遗工作站实地观察记录</li>
          <li>传播线：问卷数据与受众反馈，验证公众认知变化</li>
        </ul>
      </article>

      <article class="info-card stat-card">
        <h3>关键调研指标</h3>
        <div class="stat-list">
          <div v-for="item in metrics" :key="item.label" class="stat-item">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </article>
    </section>

    <section class="insight-card">
      <h3>核心调研发现</h3>
      <div class="insight-grid">
        <article v-for="item in insights" :key="item.title" class="insight-item">
          <p class="insight-title">{{ item.title }}</p>
          <p>{{ item.desc }}</p>
        </article>
      </div>
    </section>

    <section class="timeline-card">
      <h3>调研执行节奏</h3>
      <div class="timeline">
        <article v-for="phase in phases" :key="phase.title" class="timeline-item">
          <span class="phase">{{ phase.stage }}</span>
          <h4>{{ phase.title }}</h4>
          <p>{{ phase.desc }}</p>
          <span class="deliverable">{{ phase.deliverable }}</span>
        </article>
      </div>
    </section>

    <section class="sources-card">
      <h3>资料来源与页面映射</h3>
      <div class="sources-grid">
        <article v-for="item in evidenceSources" :key="item.source" class="source-item">
          <p class="source-name">{{ item.source }}</p>
          <p>{{ item.usedFor }}</p>
        </article>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { aboutNavItems } from './navs'

const metrics = [
  { value: '63.44%', label: '此前不了解马坝柴烧' },
  { value: '70.97%', label: '存在明确了解意愿' },
  { value: '76.88%', label: '认可工艺独特性' },
  { value: '93%+', label: '认可在地文化价值' }
]

const frameworkPillars = [
  {
    title: '一库：柴烧数字基因库',
    desc: '沉淀传承人动作捕捉、窑内温场、釉色图谱等基础数据，形成长期可复用资料底座。',
    value: '关键词：动作数据 / 传感数据 / 图像图谱'
  },
  {
    title: '一脑：柴烧 AI 大脑',
    desc: '将辅助设计、数字孪生窑炉与鉴赏分析整合为一个解释层，提高公众理解效率。',
    value: '关键词：AI鉴证 / 预测提示 / 文案转译'
  },
  {
    title: '一平台：窑脉数字博物馆',
    desc: '以 3D 导览、问答中枢、云窑工坊承接浏览行为，降低专业门槛并提升停留时长。',
    value: '关键词：3D秘境 / 云窑工坊 / AI解说'
  },
  {
    title: '一链：乡村振兴产业链',
    desc: '把线上传播导流到线下研学、文旅体验、版权交易与品牌合作，形成可持续运营。',
    value: '关键词：研学旅游 / 品牌溢价 / 社区合伙'
  }
]

const insights = [
  {
    title: '历史连续性可被证实',
    desc: '笔记材料明确指出曲江地区从石峡文化陶器遗存到宋代窑址群，形成持续性的制陶文化线索。'
  },
  {
    title: '工艺复杂度高于公众认知',
    desc: '柴烧受木材、窑位、温场与地理气候共同影响，存在“一窑穷，一窑富”的高不确定性特征。'
  },
  {
    title: '传承与教育双主体并行',
    desc: '龙志雄侧重古法守正与窑炉实践，李晶侧重公众教学与研学推广，构成“生产+传播”双轮结构。'
  },
  {
    title: '技术介入点清晰',
    desc: 'AI 可优先落在工艺讲解、鉴赏解释、传播转译三处，减少“看不懂柴烧”导致的传播断层。'
  }
]

const phases = [
  {
    stage: '阶段一',
    title: '文献与资料基线构建',
    desc: '完成设计方案、非遗笔记、匠人信息三类文本归档，提取工艺术语与历史节点。',
    deliverable: '交付：资料索引表 + 术语清单'
  },
  {
    stage: '阶段二',
    title: '工作站实地观察',
    desc: '围绕马坝人遗址公园内工作站，记录展陈内容、教学场景、到访路径与接待流程。',
    deliverable: '交付：场地观察记录 + 图像素材映射'
  },
  {
    stage: '阶段三',
    title: '访谈与问卷交叉验证',
    desc: '结合传承人口述与公众问卷，核对“工艺价值”与“传播难点”是否一致。',
    deliverable: '交付：关键结论卡片 + 证据出处标注'
  },
  {
    stage: '阶段四',
    title: '成果转译到前端页面',
    desc: '将可核验内容转成导览叙事、指标看板与研学话术，形成可复用展示模板。',
    deliverable: '交付：关于板块实践成果页文案包'
  }
]

const evidenceSources = [
  {
    source: '《李晶老师笔记》',
    usedFor: '用于历史源流、教学活动样态、工作站实践路径提取。'
  },
  {
    source: '《非遗传承人、匠人信息》',
    usedFor: '用于传承人角色分工、联系方式与线下活动组织方式。'
  },
  {
    source: '《窑脉薪传——网页设计方案》',
    usedFor: '用于三大核心模块、配色体系与传播闭环设计依据。'
  },
  {
    source: '《项目总结融合稿》',
    usedFor: '用于“一库一脑一平台一链”与乡村振兴价值框架。'
  },
  {
    source: '钦州市文广旅局《传承古灶遗风，光大坭兴文化》（2020-09-01）',
    usedFor: '用于“开窑叙事、土火柴窑协同逻辑、古灶活动传播链路”外部案例补充。'
  },
  {
    source: '台灣陶藝聚落（主题报道/臺灣柴燒栏目）',
    usedFor: '用于“在地土矿、快速柴窑、器用美学与地方生活连接”方法论补充。'
  },
  {
    source: '添興窯陶藝村（營業項目）',
    usedFor: '用于“蛇窑品牌化、体验教学与产品目录运营”结构化案例补充。'
  },
  {
    source: '《现代柴烧技术机理研究》《浅析柴烧艺术及其美学特点》等论文',
    usedFor: '用于“落灰成釉机理、柴烧美学维度、传承与市场协同”学术证据补充。'
  }
]
</script>

<style scoped>
.evidence-banner,
.research-grid {
  margin-top: 0;
}

.evidence-banner {
  border: 1px solid rgba(169, 106, 74, 0.38);
  border-radius: 6px 24px 6px 24px;
  padding: 20px 22px;
  background:
    linear-gradient(140deg, rgba(255, 247, 233, 0.96), rgba(246, 234, 217, 0.82));
  box-shadow: 0 14px 30px rgba(58, 43, 30, 0.07);
}

.banner-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.2em;
  color: var(--ym-text-muted);
}

.evidence-banner h3 {
  margin-top: 10px;
  font-family: var(--ym-font-display);
  font-size: 1.34rem;
}

.evidence-banner > p {
  margin-top: 10px;
  color: var(--ym-text-secondary);
  line-height: 1.84;
}

.source-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.source-tags span {
  border: 1px solid rgba(var(--ym-accent-rgb), 0.32);
  border-radius: 4px 12px 4px 12px;
  padding: 4px 10px;
  font-size: 0.78rem;
  color: var(--ym-accent);
  background: rgba(var(--ym-accent-rgb), 0.08);
}

.framework-grid {
  margin-top: 14px;
  display: block;
}

.framework-card {
  border: 1px solid var(--ym-border);
  border-radius: 4px 20px 8px 24px;
  padding: 20px;
  background: rgba(255, 250, 240, 0.94);
}

.framework-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.08rem;
}

.framework-list {
  margin-top: 12px;
  display: grid;
  gap: 9px;
}

.framework-item {
  border: 1px solid var(--ym-border);
  border-radius: 3px 12px 5px 14px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.86);
}

.framework-title {
  margin: 0;
  font-family: var(--ym-font-display);
  font-size: 0.94rem;
}

.framework-item p:nth-child(2) {
  margin-top: 6px;
  color: var(--ym-text-secondary);
  line-height: 1.72;
  font-size: 0.86rem;
}

.framework-item span {
  margin-top: 8px;
  display: inline-flex;
  font-size: 0.75rem;
  color: var(--ym-text-muted);
}

.research-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}

.info-card,
.insight-card,
.timeline-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 20px 6px 20px;
  padding: 22px;
  background: rgba(255, 250, 240, 0.94);
}

.info-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
}

.info-card p {
  margin-top: 10px;
  color: var(--ym-text-secondary);
  line-height: 1.8;
}

.info-card ul {
  margin-top: 12px;
  padding-left: 18px;
  color: var(--ym-text-secondary);
  line-height: 1.94;
}

.stat-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.stat-item {
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 4px 12px;
  padding: 12px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9), rgba(255, 247, 236, 0.74));
  display: grid;
  gap: 3px;
}

.stat-item strong {
  font-size: 1.3rem;
  color: var(--ym-accent);
  font-family: var(--ym-font-display);
}

.stat-item span {
  color: var(--ym-text-secondary);
  font-size: 0.88rem;
}

.insight-card {
  margin-top: 14px;
}

.insight-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
}

.insight-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.insight-item {
  border: 1px solid var(--ym-border);
  border-radius: 4px 14px 4px 14px;
  background: rgba(255, 255, 255, 0.84);
  padding: 12px;
}

.insight-title {
  margin: 0;
  font-family: var(--ym-font-display);
  font-size: 0.98rem;
}

.insight-item p:last-child {
  margin-top: 6px;
  color: var(--ym-text-secondary);
  line-height: 1.76;
  font-size: 0.88rem;
}

.timeline-card {
  margin-top: 14px;
}

.timeline-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
}

.timeline {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.timeline-item {
  border: 1px solid var(--ym-border);
  border-radius: 6px 14px 6px 14px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.86);
}

.phase {
  display: inline-flex;
  border-radius: 4px 10px 4px 10px;
  padding: 3px 10px;
  font-size: 0.76rem;
  color: var(--ym-accent);
  background: rgba(var(--ym-accent-rgb), 0.1);
  border: 1px solid rgba(var(--ym-accent-rgb), 0.2);
}

.timeline-item h4 {
  margin-top: 10px;
  font-size: 1rem;
}

.timeline-item p {
  margin-top: 6px;
  font-size: 0.9rem;
  color: var(--ym-text-secondary);
  line-height: 1.75;
}

.deliverable {
  margin-top: 8px;
  display: inline-flex;
  font-size: 0.76rem;
  color: var(--ym-text-muted);
}

.sources-card {
  margin-top: 14px;
  border: 1px solid var(--ym-border);
  border-radius: 14px 4px 18px 8px;
  padding: 20px;
  background: rgba(255, 250, 240, 0.93);
}

.sources-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.08rem;
}

.sources-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.source-item {
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 6px 14px;
  background: rgba(255, 255, 255, 0.84);
  padding: 12px;
}

.source-name {
  margin: 0;
  font-family: var(--ym-font-display);
  color: var(--ym-text);
  font-size: 0.92rem;
}

.source-item p:last-child {
  margin-top: 6px;
  color: var(--ym-text-secondary);
  line-height: 1.72;
  font-size: 0.86rem;
}

@media (max-width: 980px) {
  .framework-grid,
  .research-grid,
  .insight-grid,
  .timeline,
  .sources-grid {
    grid-template-columns: 1fr;
  }
}
</style>
