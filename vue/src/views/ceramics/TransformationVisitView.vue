<template>
  <CeramicsPageShell
    kicker="ABOUT"
    title="关于"
    description="把工作站到访、活动组织与预约方式归入“关于”板块，统一展示项目参与路径。"
    :sub-nav-items="aboutNavItems"
    :alt="true"
  >
    <section class="station-card">
      <p class="station-kicker">SITE BASE</p>
      <h3>曲江柴烧陶艺非物质文化遗产工作站（马坝人遗址公园内）</h3>
      <p>
        场地兼具“创作基地 + 展示空间 + 研学课堂”三重功能，可承接公众导览、亲子陶艺、妇女技能培训等不同类型活动。
      </p>
      <div class="station-tags">
        <span>非遗展示</span>
        <span>陶艺实操</span>
        <span>公益培训</span>
        <span>研学接待</span>
      </div>
    </section>

    <section class="visit-grid">
      <article class="info-card">
        <h3>推荐参访动线</h3>
        <ol class="flow-list">
          <li>展厅导览：了解曲江柴烧历史源流、器型谱系与工艺特征。</li>
          <li>工艺讲解：围绕“泥、火、柴、窑”关系说明柴烧成败关键。</li>
          <li>陶艺体验：以水杯、茶盏等基础器型进行实操上手。</li>
          <li>访后沉淀：收集体验反馈，沉淀问卷与传播素材。</li>
        </ol>
      </article>

      <article class="info-card">
        <h3>重点活动样态</h3>
        <div class="activity-list">
          <article v-for="item in activities" :key="item.title" class="activity-item">
            <p class="activity-title">{{ item.title }}</p>
            <p>{{ item.desc }}</p>
            <span>{{ item.value }}</span>
          </article>
        </div>
      </article>
    </section>

    <section class="ops-grid">
      <article class="ops-card">
        <h3>到访执行包</h3>
        <div class="ops-list">
          <article v-for="item in visitPackages" :key="item.title" class="ops-item">
            <p class="ops-title">{{ item.title }}</p>
            <p>{{ item.desc }}</p>
            <span>{{ item.value }}</span>
          </article>
        </div>
      </article>

      <article class="ops-card flow-card">
        <h3>预约与接待 SOP</h3>
        <ol class="booking-list">
          <li v-for="item in bookingFlow" :key="item.title">
            <p class="step-title">{{ item.title }}</p>
            <p>{{ item.desc }}</p>
          </li>
        </ol>
      </article>
    </section>

    <section class="contact-card">
      <h3>联系与预约信息</h3>
      <div class="contact-grid">
        <p><strong>工作站地址</strong>广东省韶关市曲江区马坝人遗址公园内（风度书房后）</p>
        <p><strong>传承人联系</strong>龙志雄 13726566173</p>
        <p><strong>教学预约</strong>李老师 13827953005</p>
        <p><strong>建议接待时段</strong>周三至周日 09:30-17:30</p>
        <p><strong>单批建议规模</strong>10-30 人（研学团队建议分批）</p>
        <p><strong>适配形式</strong>亲子研学 / 校内课程 / 公益技能培训</p>
      </div>
    </section>

    <section class="conversion-card">
      <h3>线下转化建议指标</h3>
      <div class="conversion-grid">
        <article v-for="item in conversionMetrics" :key="item.label" class="conversion-item">
          <strong>{{ item.value }}</strong>
          <p>{{ item.label }}</p>
          <span>{{ item.desc }}</span>
        </article>
      </div>
    </section>
  </CeramicsPageShell>
</template>

<script setup>
import CeramicsPageShell from '@/components/CeramicsPageShell.vue'
import { aboutNavItems } from './navs'

const activities = [
  {
    title: '亲子陶艺培训',
    desc: '以“展厅认知 + 共同创作”模式，让家庭在协作中理解非遗工艺逻辑。',
    value: '案例：2021 曲江区柴烧陶艺亲子培训班'
  },
  {
    title: '妇女公益技能班',
    desc: '面向妇女群体开展手工创业就业培训，强化可迁移的手作技能。',
    value: '案例：曲江区妇联柴烧陶艺公益培训'
  },
  {
    title: '公众导览讲解',
    desc: '围绕历史源流、工艺步骤与器型审美进行分层讲解，降低理解门槛。',
    value: '输出：可复用的导览话术与教学脚本'
  }
]

const visitPackages = [
  {
    title: '课前准备包',
    desc: '提供背景简报、核心术语卡、线路说明，降低首次到访理解门槛。',
    value: '包含：石峡文化线索 / 柴烧基础概念 / 场地动线图'
  },
  {
    title: '现场体验包',
    desc: '按“导览-讲解-实操-反馈”四段式执行，保证每批次活动节奏一致。',
    value: '包含：讲解脚本 / 实操任务单 / 安全与材料清单'
  },
  {
    title: '返程传播包',
    desc: '活动结束后发放图文模板与短视频话术，促进二次传播与复访预约。',
    value: '包含：朋友圈文案 / 短视频标题建议 / 二次预约入口'
  }
]

const bookingFlow = [
  {
    title: '步骤1：提交需求',
    desc: '明确团队类型、到访人数、期望时段与核心目标（导览/研学/培训）。'
  },
  {
    title: '步骤2：24小时内返传排期',
    desc: '根据工作站接待能力输出建议日期、批次拆分与活动时长。'
  },
  {
    title: '步骤3：确认执行清单',
    desc: '确认交通、材料、讲解侧重点与现场负责人，避免临场信息缺失。'
  },
  {
    title: '步骤4：活动后沉淀',
    desc: '收集反馈问卷与图像素材，用于后续传播和下一轮活动优化。'
  }
]

const conversionMetrics = [
  {
    value: '70%+',
    label: '首次到访后继续关注',
    desc: '衡量数字页面对线下体验后的持续吸引力。'
  },
  {
    value: '30%+',
    label: '活动后二次预约意向',
    desc: '衡量从“体验一次”到“持续参与”的转化能力。'
  },
  {
    value: '50%+',
    label: '内容二次传播触达',
    desc: '统计活动后图文/短视频分享带来的新增触达。'
  }
]
</script>

<style scoped>
.station-card {
  border: 1px solid rgba(169, 106, 74, 0.35);
  border-radius: 6px 22px 6px 22px;
  padding: 20px 22px;
  background: linear-gradient(145deg, rgba(255, 248, 236, 0.95), rgba(246, 234, 216, 0.8));
  box-shadow: 0 14px 28px rgba(58, 43, 30, 0.06);
}

.station-kicker {
  margin: 0;
  font-size: 0.72rem;
  letter-spacing: 0.2em;
  color: var(--ym-text-muted);
}

.station-card h3 {
  margin-top: 10px;
  font-family: var(--ym-font-display);
  font-size: 1.3rem;
}

.station-card > p {
  margin-top: 10px;
  color: var(--ym-text-secondary);
  line-height: 1.84;
}

.station-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.station-tags span {
  border-radius: 4px 10px 4px 10px;
  border: 1px solid rgba(var(--ym-accent-rgb), 0.35);
  padding: 4px 10px;
  font-size: 0.78rem;
  color: var(--ym-accent);
  background: rgba(var(--ym-accent-rgb), 0.08);
}

.visit-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.info-card,
.contact-card {
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

.flow-list {
  margin-top: 12px;
  padding-left: 20px;
  color: var(--ym-text-secondary);
  line-height: 1.92;
}

.activity-list {
  margin-top: 10px;
  display: grid;
  gap: 8px;
}

.activity-item {
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 4px 12px;
  background: rgba(255, 255, 255, 0.84);
  padding: 10px;
}

.activity-title {
  margin: 0;
  font-family: var(--ym-font-display);
  font-size: 0.96rem;
}

.activity-item p:nth-child(2) {
  margin-top: 6px;
  font-size: 0.88rem;
  line-height: 1.74;
  color: var(--ym-text-secondary);
}

.activity-item span {
  display: inline-flex;
  margin-top: 6px;
  font-size: 0.76rem;
  color: var(--ym-text-muted);
}

.contact-card {
  margin-top: 14px;
}

.contact-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
}

.contact-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.contact-grid p {
  margin: 0;
  padding: 12px 14px;
  border-radius: 4px 12px 4px 12px;
  border: 1px solid var(--ym-border);
  background: rgba(255, 255, 255, 0.82);
  font-size: 0.9rem;
  color: var(--ym-text-secondary);
  line-height: 1.6;
}

.contact-grid p strong {
  display: block;
  color: var(--ym-text);
  font-size: 0.78rem;
  letter-spacing: 0.04em;
  margin-bottom: 4px;
}

.ops-grid {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 14px;
}

.ops-card,
.conversion-card {
  border: 1px solid var(--ym-border);
  border-radius: 6px 20px 6px 20px;
  padding: 22px;
  background: rgba(255, 250, 240, 0.94);
}

.ops-card h3,
.conversion-card h3 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
}

.ops-list {
  margin-top: 12px;
  display: grid;
  gap: 10px;
}

.ops-item {
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 6px 14px;
  background: rgba(255, 255, 255, 0.84);
  padding: 12px;
}

.ops-title {
  margin: 0;
  font-family: var(--ym-font-display);
  font-size: 0.96rem;
}

.ops-item p:nth-child(2) {
  margin-top: 6px;
  font-size: 0.88rem;
  line-height: 1.72;
  color: var(--ym-text-secondary);
}

.ops-item span {
  margin-top: 6px;
  display: inline-flex;
  font-size: 0.76rem;
  color: var(--ym-text-muted);
}

.flow-card {
  border-radius: 16px 4px 20px 8px;
}

.booking-list {
  margin-top: 10px;
  padding-left: 20px;
  display: grid;
  gap: 8px;
}

.booking-list li {
  border-left: 2px solid rgba(var(--ym-support-rgb), 0.45);
  padding-left: 10px;
}

.step-title {
  margin: 0;
  font-family: var(--ym-font-display);
  font-size: 0.92rem;
}

.booking-list li p:last-child {
  margin-top: 6px;
  color: var(--ym-text-secondary);
  font-size: 0.86rem;
  line-height: 1.7;
}

.conversion-card {
  margin-top: 14px;
  border-radius: 14px 4px 18px 8px;
}

.conversion-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.conversion-item {
  border: 1px solid var(--ym-border);
  border-radius: 4px 12px 6px 14px;
  background: rgba(255, 255, 255, 0.84);
  padding: 12px;
}

.conversion-item strong {
  font-family: var(--ym-font-display);
  color: var(--ym-accent);
  font-size: 1.18rem;
}

.conversion-item p {
  margin-top: 6px;
  font-size: 0.9rem;
  color: var(--ym-text);
}

.conversion-item span {
  margin-top: 4px;
  display: inline-flex;
  font-size: 0.78rem;
  color: var(--ym-text-muted);
  line-height: 1.65;
}

@media (max-width: 860px) {
  .visit-grid,
  .ops-grid,
  .contact-grid,
  .conversion-grid {
    grid-template-columns: 1fr;
  }
}
</style>
