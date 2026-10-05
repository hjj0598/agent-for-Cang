import { request } from '@/utils/request'

export function getCurrentUser() {
  return request('/api/user/me')
}

export function updateNickname(data) {
  return request('/api/user/nickname', {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

export function updatePassword(data) {
  return request('/api/user/password', {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}
