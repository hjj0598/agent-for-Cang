import { request } from '@/utils/request'

export function login(data) {
  return request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function register(data) {
  return request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function forgotCode(data) {
  return request('/api/auth/forgot-code', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function resetPassword(data) {
  return request('/api/auth/reset-password', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}
