import assert from 'node:assert/strict'
import { canSubmitLogin, hasLoginCredentials } from './loginCaptcha.js'

assert.equal(hasLoginCredentials({ username: '', password: '123456' }).ok, false)
assert.equal(hasLoginCredentials({ username: 'demo', password: '123456' }).ok, true)
assert.equal(canSubmitLogin({ username: 'demo', password: '123456', captchaVerified: false }).ok, false)
assert.equal(canSubmitLogin({ username: 'demo', password: '123456', captchaVerified: true }).ok, true)
