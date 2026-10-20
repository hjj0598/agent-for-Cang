import { request } from '@/utils/request'

// 使用账号信息登录系统。
export function login(data) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 注册新用户。
export function register(data) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 发送找回密码所需的验证码。
export function forgotCode(data) {
  return request('/api/auth/forgot-code', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 使用验证码重置用户密码。
export function resetPassword(data) {
  return request('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}
