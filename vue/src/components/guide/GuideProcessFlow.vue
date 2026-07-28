<template>
  <article class="process-panel">
    <header class="panel-head">
      <p class="eyebrow">PROCESS · 工艺图解</p>
      <h3>从泥到器 — 柴烧八步工艺</h3>
      <p>按“土性-含水-温场-气氛”四条机理线拆分工艺节点，并同步标记每一步的高频操作风险点。</p>
    </header>

    <div class="process-visuals" aria-label="工艺现场图">
      <figure v-for="item in processVisuals" :key="item.title" class="visual-card">
        <img :src="item.src" :alt="item.title" loading="lazy" />
        <figcaption>
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </figcaption>
      </figure>
    </div>

    <!-- 横向时间线 -->
    <div class="timeline-wrap" aria-label="工艺流程时间线">
      <div class="timeline-track">
        <div
          v-for="(item, index) in steps"
          :key="item.no"
          class="timeline-node"
          :class="{ active: activeStep === index }"
          role="button"
          tabindex="0"
          @click="activeStep = index"
          @keydown.enter.prevent="activeStep = index"
          @keydown.space.prevent="activeStep = index"
        >
          <div class="node-circle">
            <span class="node-no">{{ item.no }}</span>
          </div>
          <span class="node-name">{{ item.name }}</span>
          <div v-if="index < steps.length - 1" class="node-line"></div>
        </div>
      </div>
    </div>

    <!-- 步骤详情 -->
    <div class="step-detail" v-if="currentStep">
      <div class="step-detail-head">
        <span class="step-no">{{ currentStep.no }}</span>
        <div>
          <h4>{{ currentStep.name }}</h4>
          <p class="step-phase">{{ currentStep.phase }}</p>
        </div>
      </div>
      <div class="detail-meta">
        <span>建议时长：{{ currentStep.duration }}</span>
        <span>关键控制：{{ currentStep.keyPoint }}</span>
      </div>
      <p class="step-desc">{{ currentStep.desc }}</p>
      <p class="step-tip">{{ currentStep.tip }}</p>
      <div class="step-actions">
        <button type="button" class="step-btn" :disabled="activeStep === 0" @click="prevStep">上一步</button>
        <button type="button" class="step-btn primary" :disabled="activeStep === steps.length - 1" @click="nextStep">下一步</button>
      </div>
    </div>

    <!-- 步骤网格（全览） -->
    <div class="step-grid">
      <article
        v-for="(item, index) in steps"
        :key="`${item.no}-grid`"
        class="step-card"
        :class="{ active: activeStep === index }"
        @click="activeStep = index"
      >
        <span class="card-no">{{ item.no }}</span>
        <h4>{{ item.name }}</h4>
        <p>{{ item.desc }}</p>
      </article>
    </div>
  </article>
</template>

<script setup>
import { computed, ref } from 'vue'

const activeStep = ref(0)

const processVisuals = [
  {
    title: '泥料压实与整形',
    desc: '通过压力辅助工具控制坯体密度，减少后续开裂风险。',
    src: '/vcg-process-pressing.webp'
  },
  {
    title: '手工修坯与刻饰',
    desc: '沿器口线条修整并雕刻纹样，形成器物细节节奏。',
    src: '/vcg-process-carving.webp'
  }
]

const steps = [
  {
    no: '01', name: '取土筛选', phase: '泥料准备阶段',
    desc: '筛除杂质并控制颗粒级配，建立稳定泥料基础。',
    tip: '机理：矿物组成与颗粒级配决定烧结温度窗口和发色基调。风险点：筛分不彻底会残留石英粗粒，后续易出现爆点与针孔。',
    duration: '1-2 天',
    keyPoint: '矿物组成与颗粒级配'
  },
  {
    no: '02', name: '练泥醒泥', phase: '泥料准备阶段',
    desc: '通过反复揉练与静置提升泥料均匀性和可塑性。',
    tip: '机理：醒泥让水分在泥团内部重新分布，改善可塑性并释放封闭气泡。风险点：含水率偏高会塌坯，偏低则拉坯起裂。',
    duration: '1-3 天',
    keyPoint: '含水率窗口与排气'
  },
  {
    no: '03', name: '成型修坯', phase: '成型阶段',
    desc: '拉坯或手塑成型后进行修整，确立器物结构。',
    tip: '机理：壁厚一致性决定干燥收缩应力是否均衡。风险点：口沿与腹部厚薄差过大，预烧前就会出现应力裂纹。',
    duration: '1-2 天',
    keyPoint: '壁厚梯度与应力均衡'
  },
  {
    no: '04', name: '阴干预烧', phase: '干燥阶段',
    desc: '控制干燥节奏，降低开裂风险并完成素烧准备。',
    tip: '机理：坯体在100℃前主要完成自由水排出，300℃附近开始有机物分解。风险点：风口直吹或暴晒会造成表里收缩不同步，引发暗裂。',
    duration: '3-7 天',
    keyPoint: '收缩曲线与预热坡度'
  },
  {
    no: '05', name: '施釉装窑', phase: '入窑阶段',
    desc: '结合器型与窑位规划施釉策略和装窑方案。',
    tip: '机理：窑位决定火焰冲刷、落灰厚度与局部氧化还原环境。风险点：装窑间距不足会形成阴影烧成，导致欠火或粘连。',
    duration: '0.5-1 天',
    keyPoint: '窑位温场映射'
  },
  {
    no: '06', name: '投柴烧制', phase: '烧成阶段',
    desc: '依据火路与温场动态投柴，形成窑变核心条件。',
    tip: '机理：升温斜率与投柴节律共同塑造还原气氛和灰釉熔融状态。风险点：连续重柴导致温场剧烈波动，会引发釉面失稳或局部过烧。',
    duration: '2-4 天',
    keyPoint: '升温斜率与还原节律'
  },
  {
    no: '07', name: '停火冷却', phase: '冷却阶段',
    desc: '自然降温避免热冲击，保障器物完整度。',
    tip: '机理：降温阶段晶相重排与玻化层收缩同步进行。风险点：过早开窑会触发热震龟裂，尤其在厚胎器与大件器型上更明显。',
    duration: '2-5 天',
    keyPoint: '降温斜率与热应力释放'
  },
  {
    no: '08', name: '出窑评估', phase: '评估阶段',
    desc: '记录窑位与烧成结果，形成下一轮工艺反馈。',
    tip: '机理：将器物表征反映到窑位、火程和土配比，才能形成可复用工艺模型。风险点：只看成品不做参数还填，会导致下一窑重复试错。',
    duration: '0.5 天',
    keyPoint: '窑位-火程-釉效还原'
  }
]

const currentStep = computed(() => steps[activeStep.value] || steps[0])
const prevStep = () => {
  if (activeStep.value > 0) activeStep.value -= 1
}
const nextStep = () => {
  if (activeStep.value < steps.length - 1) activeStep.value += 1
}
</script>

<style scoped>
.process-panel {
  --accent: var(--ym-accent);
  --accent-rgb: var(--ym-accent-rgb);
  --border: var(--ym-border);
  --border-strong: var(--ym-border-strong);
  --text: var(--ym-text);
  --text-secondary: var(--ym-text-secondary);
  --text-muted: var(--ym-text-muted);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 24px;
  background: rgba(255, 250, 240, 0.94);
  display: grid;
  gap: 22px;
}

.panel-head h3 {
  font-family: var(--ym-font-display);
  font-size: 1.3rem;
  margin: 8px 0 8px;
}

.panel-head p {
  color: var(--text-secondary);
  line-height: 1.75;
  font-size: 0.93rem;
}

.eyebrow {
  font-size: 0.72rem;
  color: var(--text-muted);
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.process-visuals {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.visual-card {
  margin: 0;
  border: 1px solid var(--border);
  border-radius: 12px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.78);
}

.visual-card img {
  width: 100%;
  aspect-ratio: 16/9;
  object-fit: cover;
  display: block;
}

.visual-card figcaption {
  padding: 10px 12px;
  display: grid;
  gap: 3px;
}

.visual-card strong {
  font-size: 0.88rem;
  color: var(--text);
}

.visual-card span {
  font-size: 0.8rem;
  line-height: 1.7;
  color: var(--text-secondary);
}

/* ── Timeline ── */
.timeline-wrap {
  overflow-x: auto;
  padding-bottom: 8px;
}

.timeline-track {
  display: flex;
  align-items: flex-start;
  gap: 0;
  min-width: max-content;
  padding: 16px 8px 8px;
}

.timeline-node {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 4px;
  min-width: 80px;
}

.node-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--border-strong);
  background: rgba(255, 250, 240, 0.92);
  display: grid;
  place-items: center;
  transition: all 0.22s ease;
  z-index: 1;
}

.timeline-node.active .node-circle {
  border-color: var(--accent);
  background: var(--accent);
}

.node-no {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-muted);
  transition: color 0.22s ease;
}

.timeline-node.active .node-no {
  color: #fff7ea;
}

.node-name {
  font-size: 0.8rem;
  color: var(--text-secondary);
  text-align: center;
  transition: color 0.22s ease;
  white-space: nowrap;
}

.timeline-node.active .node-name {
  color: var(--accent);
  font-weight: 500;
}

.node-line {
  position: absolute;
  top: 20px;
  left: calc(50% + 20px);
  width: calc(100% - 16px);
  height: 2px;
  background: var(--border-strong);
  z-index: 0;
}

/* ── Step Detail ── */
.step-detail {
  border: 1px solid rgba(var(--accent-rgb), 0.24);
  border-radius: 14px;
  padding: 20px;
  background: rgba(var(--accent-rgb), 0.05);
  display: grid;
  gap: 10px;
}

.step-detail-head {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.step-no {
  font-family: var(--ym-font-display);
  font-size: 2rem;
  color: rgba(var(--accent-rgb), 0.3);
  line-height: 1;
  flex-shrink: 0;
}

.step-detail-head h4 {
  font-family: var(--ym-font-display);
  font-size: 1.1rem;
  margin-bottom: 3px;
}

.step-phase {
  font-size: 0.8rem;
  color: var(--accent);
  letter-spacing: 0.06em;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-meta span {
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  padding: 3px 9px;
  font-size: 0.78rem;
  color: var(--text-secondary);
  background: rgba(255, 250, 240, 0.88);
}

.step-desc {
  color: var(--text-secondary);
  line-height: 1.75;
}

.step-tip {
  font-size: 0.88rem;
  color: var(--text-muted);
  line-height: 1.7;
  border-top: 1px dashed var(--border-strong);
  padding-top: 10px;
}

.step-actions {
  margin-top: 4px;
  display: flex;
  gap: 8px;
}

.step-btn {
  border: 1px solid var(--border-strong);
  border-radius: 999px;
  background: rgba(255, 250, 240, 0.9);
  color: var(--text-secondary);
  padding: 6px 12px;
  font-size: 0.86rem;
  cursor: pointer;
}

.step-btn.primary {
  border-color: rgba(var(--accent-rgb), 0.4);
  background: rgba(var(--accent-rgb), 0.1);
  color: var(--accent);
}

.step-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ── Step Grid ── */
.step-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.step-card {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: rgba(255, 250, 240, 0.8);
  cursor: pointer;
  transition: all 0.2s ease;
}

.step-card:hover {
  border-color: rgba(var(--accent-rgb), 0.32);
  background: rgba(255, 250, 240, 0.98);
}

.step-card.active {
  border-color: rgba(var(--accent-rgb), 0.48);
  background: rgba(var(--accent-rgb), 0.08);
}

.card-no {
  font-size: 0.72rem;
  color: var(--accent);
  font-weight: 700;
  letter-spacing: 0.08em;
  display: block;
  margin-bottom: 5px;
}

.step-card h4 {
  font-size: 0.96rem;
  margin-bottom: 5px;
}

.step-card p {
  font-size: 0.84rem;
  color: var(--text-secondary);
  line-height: 1.65;
}

.timeline-node:focus-visible {
  outline: 2px solid var(--ym-focus);
  outline-offset: 4px;
  border-radius: 4px;
}

@media (max-width: 1020px) {
  .step-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 580px) {
  .process-visuals,
  .step-grid {
    grid-template-columns: 1fr;
  }

  .step-actions {
    flex-direction: column;
  }
}
</style>
