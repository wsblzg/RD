export const DIALECT_PRESETS = [
  {
    key: 'mandarin',
    label: '普通话',
    short: '普',
    region: '标准普通话',
    voiceId: 'CAP_4193',
    ttsSample: '16000',
    color: '#c41e3a',
    description: '标准普通话讲解员，音色浑厚亲和'
  },
  {
    key: 'beijing',
    label: '京味儿',
    short: '京',
    region: '北京',
    voiceId: 'LMV_10577',
    ttsSample: '24000',
    color: '#b94e48',
    description: '北京腔讲解音色'
  },
  {
    key: 'qingdao',
    label: '山东青岛话',
    short: '鲁',
    region: '山东青岛',
    voiceId: 'LMV_10575',
    ttsSample: '24000',
    color: '#2e8b57',
    description: '胶辽官话讲解音色'
  },
  {
    key: 'henan',
    label: '河南当地话',
    short: '豫',
    region: '河南',
    voiceId: 'LMV_10568',
    ttsSample: '24000',
    color: '#b8860b',
    description: '中原官话讲解音色'
  },
  {
    key: 'cantonese',
    label: '粤语',
    short: '粤',
    region: '广东',
    voiceId: 'LITE_lengdan_xiongzhang',
    ttsSample: '16000',
    ttsLan: 'Chinese,Yue',
    useDynamicToken: true,
    color: '#a02c28',
    description: '粤语男声讲解音色'
  },
  {
    key: 'guangpu',
    label: '广西白话',
    short: '桂',
    region: '广西',
    voiceId: 'LMV_10570',
    ttsSample: '24000',
    color: '#3e8e7e',
    description: '广西口音普通话讲解音色'
  }
]

export const DEFAULT_DIALECT_KEY = 'mandarin'

export function getDialectByKey(key) {
  return DIALECT_PRESETS.find((dialect) => dialect.key === key) || DIALECT_PRESETS[0]
}
