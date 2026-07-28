const KNOWLEDGE_CHUNKS = [
  {
    id: 'DOC-001',
    source: '柴烧知识库',
    section: '柴烧知识库定位',
    content: '柴烧知识库按基础认知、工艺知识、美学鉴赏、非遗传承四类组织，支持图文解读与知识问答。'
  },
  {
    id: 'DOC-002',
    source: '柴烧知识库',
    section: '窑炉结构讲解',
    content: '窑炉关键部位包括投柴孔、火道、窑门、窑位、烟囱。讲解重点是温度曲线、落灰规律与釉变原理。'
  },
  {
    id: 'DOC-003',
    source: '柴烧知识库',
    section: '柴烧工艺流程',
    content: '柴烧流程可概括为泥坯入窑、分层摆窑、投柴烧制、控温烧窑、开窑出瓷。高温阶段落灰成釉是核心。'
  },
  {
    id: 'DOC-004',
    source: '柴烧知识库',
    section: '鉴赏识别维度',
    content: 'AI 鉴赏可从落灰厚薄、火痕走向、肌理层次、釉色分布等维度分析，并生成可视化报告。'
  },
  {
    id: 'DOC-005',
    source: '柴烧知识库',
    section: '木材与窑位影响',
    content: '松木油脂较高，容易形成金斑与焦糖色调；窑位温场差异会直接影响火痕、落灰和窑变结果。'
  },
  {
    id: 'GUIDE-001',
    source: '柴烧导览知识',
    section: '柴烧八步工艺',
    content: '八步工艺包括取土筛选、练泥醒泥、成型修坯、阴干预烧、施釉装窑、投柴烧制、停火冷却、出窑评估。'
  },
  {
    id: 'GUIDE-002',
    source: '柴烧导览知识',
    section: '装窑与投柴',
    content: '装窑时要根据器型和设计意图匹配窑位。投柴阶段需按火路与温场动态调节节律。'
  },
  {
    id: 'GUIDE-003',
    source: '柴烧导览知识',
    section: '作品鉴赏要点',
    content: '鉴赏器物可从器型比例、胎体质感、火痕方向、落灰层次与窑变色域五个方向综合判断。'
  },
  {
    id: 'ART-001',
    source: '非遗传承人资料',
    section: '龙志雄技艺特色',
    content: '龙志雄强调古法柴烧与自然肌理，关注火与土的关系，作品呈现落灰釉和自然火痕，追求入窑一色出窑万彩。'
  },
  {
    id: 'ART-002',
    source: '非遗传承人资料',
    section: '本土文化与工艺',
    content: '曲江柴烧与马坝在地文化相关，器型灵感可追溯到石峡文化陶器，体现非遗传承与在地创新。'
  },
  {
    id: 'ART-003',
    source: '非遗传承人资料',
    section: '师承脉络与传承实践',
    content: '龙志雄1998年系统学习曲江柴烧核心技艺，2005年创立工作室，2019年在马坝人遗址景区建设非遗工作站，形成生产、展示、传习一体化传承路径。'
  },
  {
    id: 'NOTE-001',
    source: '李晶老师笔记',
    section: '柴烧原理',
    content: '柴烧以木柴为燃料，温度常需达到1200摄氏度以上。木灰熔融并与陶土成分反应形成自然落灰釉。'
  },
  {
    id: 'NOTE-002',
    source: '李晶老师笔记',
    section: '柴烧审美特征',
    content: '柴烧作品强调火痕和灰釉形成的自然纹路，受薪柴材质、烧制时长、坯体位置和温差共同影响。'
  },
  {
    id: 'EXT-001',
    source: '钦州市文广旅局（2020-09-01）',
    section: '大风江古灶开窑叙事',
    content: '大风江古灶开窑案例强调坭兴陶“土、火、柴、窑”的协同逻辑，围绕古法柴烧、现场开窑、器物打磨与就地展售形成完整传播链路。'
  },
  {
    id: 'EXT-002',
    source: '钦州市文广旅局（2020-09-01）',
    section: '坭兴陶材料与工艺分工',
    content: '坭兴陶叙事提及“东西陶土双料混合”：东岸五花泥偏软，西岸紫红泥偏硬；并由家族分工覆盖陶土把控、烧制与销售环节。'
  },
  {
    id: 'EXT-003',
    source: '台灣陶藝聚落（主题报道）',
    section: '在地土矿与柴烧路径',
    content: '台湾柴烧案例强调“在地土矿 + 在地薪柴 + 持温策略”的地方性价值，认为土料杂质、矿物组成与烧成节律共同塑造不可复制的地方器物样貌。'
  },
  {
    id: 'EXT-004',
    source: '台灣陶藝聚落（主题报道）',
    section: '小型快速柴窑实践',
    content: '案例提及小型快速柴窑与鼓风协作可缩短烧成周期，但仍需通过持温与覆烧控制前后温差，核心仍是对土性、窑性与火路的长期经验。'
  },
  {
    id: 'EXT-005',
    source: '添興窯陶藝村（營業項目）',
    section: '蛇窑与产品体系',
    content: '添兴窑以“老蛇窑柴烧”为核心文化资产，同时形成生活陶艺品、柴烧系列、竹碳陶、璞真烧等分层产品体系，兼顾展示、体验与销售。'
  },
  {
    id: 'EXT-006',
    source: '添興窯陶藝村（營業項目）',
    section: '传统窑艺的当代转化',
    content: '页面展示“园区参观 + 陶艺体验 + 产品目录 + 活动运营”一体化路径，为传统窑艺从工艺展示转向文旅研学与生活消费提供可借鉴模式。'
  },
  {
    id: 'PAPER-001',
    source: '《现代柴烧技术机理研究》',
    section: '落灰成釉机理框架',
    content: '文献以“裸坯入窑、落灰成釉、满釉出窑”为机理主线，讨论现代柴烧与高温釉形成关系，可用于解释导览中的温度段与落灰变化。'
  },
  {
    id: 'PAPER-002',
    source: '《浅析柴烧艺术及其美学特点》',
    section: '柴烧美学维度',
    content: '研究指出柴烧审美来自手工介入与自然随机性叠加，可从历史发展、烧成影响因素和美学价值三条线组织展示文案。'
  },
  {
    id: 'PAPER-003',
    source: '《浅论柴烧工艺的传承与发展》',
    section: '传承与创新路径',
    content: '文献从窑型演变、技术多元化与市场价值阐述柴烧传承，强调“守正工艺 + 当代转化 + 市场沟通”三位一体的可持续发展策略。'
  },
  {
    id: 'PAPER-004',
    source: '《景德镇市无釉柴烧市场现状调查研究》',
    section: '无釉柴烧市场观察',
    content: '该类研究可用于补充“无釉柴烧”在消费端的认知差异与市场分层，为前端的用户教育、分类标签和活动策划提供依据。'
  }
]

const RETRIEVAL_TERMS = [
  '柴烧',
  '窑炉',
  '龙窑',
  '投柴',
  '火道',
  '窑门',
  '窑位',
  '烟囱',
  '工艺',
  '流程',
  '步骤',
  '装窑',
  '烧制',
  '开窑',
  '落灰',
  '灰釉',
  '火痕',
  '窑变',
  '釉色',
  '肌理',
  '器型',
  '胎体',
  '松木',
  '木材',
  '温度',
  '1200',
  '1300',
  '坭兴陶',
  '大风江古灶',
  '六艺古法',
  '五花泥',
  '紫红泥',
  '无釉',
  '在地土矿',
  '北埔',
  '老蛇窑',
  '竹碳陶',
  '璞真烧',
  '有机陶',
  '台湾柴烧',
  '主题报道',
  '园区参观',
  '陶艺体验',
  '文旅',
  '产品体系',
  '传承',
  '匠人',
  '龙志雄',
  '李晶',
  '曲江',
  '马坝',
  '石峡文化',
  '鉴赏',
  '报告',
  '非遗',
  '研学'
]

const normalizeText = (text) => (text || '').toLowerCase().trim()

const splitFallbackTokens = (query) => {
  const normalized = normalizeText(query)
  const parts = normalized.match(/[a-z0-9]+|[\u4e00-\u9fa5]{2,}/g)
  if (!parts) return []
  return [...new Set(parts.filter(Boolean))]
}

const extractQueryTokens = (query) => {
  const normalized = normalizeText(query)
  const matchedTerms = RETRIEVAL_TERMS.filter((term) => normalized.includes(term))
  const fallback = splitFallbackTokens(normalized)
  return [...new Set([...matchedTerms, ...fallback])]
}

const countOccurrences = (text, token) => {
  if (!token) return 0
  let count = 0
  let index = 0
  while (index >= 0) {
    index = text.indexOf(token, index)
    if (index === -1) break
    count += 1
    index += token.length
  }
  return count
}

const buildSearchText = (chunk) => normalizeText(`${chunk.source} ${chunk.section} ${chunk.content}`)

const scoreChunk = (chunk, tokens) => {
  const searchText = buildSearchText(chunk)
  let score = 0
  tokens.forEach((token) => {
    const occurrences = countOccurrences(searchText, token)
    if (occurrences > 0) {
      score += occurrences * (token.length >= 3 ? 4 : 2)
      if (chunk.section.includes(token) || chunk.source.includes(token)) {
        score += 3
      }
    }
  })
  return score
}

const buildReferenceLabel = (chunk) => `${chunk.source} · ${chunk.section}`

const dedupeById = (list) => {
  const seen = new Set()
  return list.filter((item) => {
    if (seen.has(item.id)) return false
    seen.add(item.id)
    return true
  })
}

export const retrieveKnowledge = (question, options = {}) => {
  const limit = Number.isFinite(options.limit) ? Math.max(1, options.limit) : 4
  const tokens = extractQueryTokens(question)
  const ranked = KNOWLEDGE_CHUNKS
    .map((chunk) => ({ chunk, score: scoreChunk(chunk, tokens) }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score)
    .slice(0, limit)
    .map((item) => item.chunk)

  const chunks = ranked.length ? ranked : KNOWLEDGE_CHUNKS.slice(0, Math.min(limit, 2))
  const references = dedupeById(chunks).map(buildReferenceLabel)
  return {
    tokens,
    chunks,
    references
  }
}

export const formatKnowledgeForPrompt = (chunks = []) => {
  if (!Array.isArray(chunks) || chunks.length === 0) {
    return '未检索到可用知识片段。'
  }
  return chunks
    .map((chunk, index) => {
      const title = `[参考${index + 1}] ${chunk.source} / ${chunk.section}`
      return `${title}\n${chunk.content}`
    })
    .join('\n\n')
}
