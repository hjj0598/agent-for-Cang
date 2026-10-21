import {
  clearToken,
  getToken,
  setAuthExpiredMessage
} from '@/store/user'

const BASE_URL = process.env.VUE_APP_API_BASE_URL || ''
const PUBLIC_AUTH_PATHS = [
  '/api/auth/login',
  '/api/auth/register',
  '/api/auth/forgot-code',
  '/api/auth/reset-password'
]

let redirectingToLogin = false

function isPublicAuthRequest(url) {
  const path = String(url || '').split('?')[0]
  return PUBLIC_AUTH_PATHS.includes(path)
}

export function handleUnauthorized() {
  // 多个请求同时返回 401 时只触发一次登录跳转。
  if (redirectingToLogin || window.location.pathname === '/login') {
    return
  }

  redirectingToLogin = true
  clearToken()
  setAuthExpiredMessage('登录已过期，请重新登录')

  const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`
  window.dispatchEvent(new CustomEvent('auth-expired', {
    detail: {
      redirect: currentPath && currentPath !== '/login'
        ? currentPath
        : '/chat'
    }
  }))
}

export function resetAuthRedirectState() {
  redirectingToLogin = false
}

export async function request(url, options = {}) {
  const token = getToken()
  const headers = {
    ...(options.headers || {})
  }

  // 上传文件交给浏览器设置 multipart 边界，其余请求使用 JSON。
  if (!(options.body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(BASE_URL + url, {
    ...options,
    headers
  })

  let result
  try {
    result = await response.json()
  } catch (error) {
    result = {
      code: 0,
      msg: `请求失败（HTTP ${response.status}）`
    }
  }

  // 公开认证接口的 401 属于表单错误，不代表当前会话过期。
  if (response.status === 401 && !isPublicAuthRequest(url)) {
    handleUnauthorized()
    return {
      ...(result || {}),
      code: 0,
      msg: '登录已过期，请重新登录',
      authExpired: true
    }
  }

  return result
}
