import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const source = readFileSync(new URL('./collectiblesApi.js', import.meta.url), 'utf8')

assert.match(source, /collectibleHttp\.get\('\/api\/points\/summary'\)/)
assert.match(source, /collectibleHttp\.post\('\/api\/points\/recharges'/)
assert.match(source, /collectibleHttp\.get\('\/api\/points\/recharges'/)
assert.match(source, /collectibleHttp\.post\(`\/api\/points\/recharges\/\$\{id\}\/mark-paid`\)/)
