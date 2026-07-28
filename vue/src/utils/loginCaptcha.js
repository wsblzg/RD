export const hasLoginCredentials = ({ username, password }) => {
  if (!username || !password) {
    return { ok: false, message: '请输入账号和密码。' }
  }
  return { ok: true, message: '' }
}

export const canSubmitLogin = ({ username, password, captchaVerified }) => {
  const credentials = hasLoginCredentials({ username, password })
  if (!credentials.ok) {
    return credentials
  }
  if (!captchaVerified) {
    return { ok: false, message: '请先完成抓娃娃人机验证。' }
  }
  return { ok: true, message: '' }
}
