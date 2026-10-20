import { request } from '@/utils/request'

// 获取当前登录用户的信息。
export function getCurrentUser() {
  return request('/api/user/me')
}

// 更新当前用户的昵称。
export function updateNickname(data) {
  return request('/api/user/nickname', {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

// 修改当前用户的登录密码。
export function updatePassword(data) {
  return request('/api/user/password', {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}
