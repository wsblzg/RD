import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('./AiCreationStudyView.vue', import.meta.url), 'utf8')

assert.doesNotMatch(source, /reveal="manual"/)
assert.doesNotMatch(source, /viewer\.src\s*=/)
assert.doesNotMatch(source, /viewer\.setAttribute\(['"]src['"]/)
