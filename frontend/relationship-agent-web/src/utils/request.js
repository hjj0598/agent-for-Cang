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
