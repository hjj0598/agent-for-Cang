const TOKEN_KEY = 'token'
const AUTH_EXPIRED_MESSAGE_KEY = 'auth-expired-message'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function setAuthExpiredMessage(message) {
  localStorage.setItem(
    AUTH_EXPIRED_MESSAGE_KEY,
    message || '登录已过期，请重新登录'
  )
}

export function consumeAuthExpiredMessage() {
  const message = localStorage.getItem(AUTH_EXPIRED_MESSAGE_KEY)
  localStorage.removeItem(AUTH_EXPIRED_MESSAGE_KEY)
  return message
}
