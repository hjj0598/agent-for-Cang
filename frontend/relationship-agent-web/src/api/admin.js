import { request } from '@/utils/request'

// 获取管理员控制台的统计数据。
export function getAdminStats() {
  return request('/api/dashboard/admin/stats')
}

// 分页查询用户列表，并支持按关键词筛选。
export function listAdminUsers(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10,
    keyword: params.keyword || ''
  })

  return request(`/api/admin/users?${query.toString()}`)
}

// 根据用户 ID 获取用户详情。
export function getAdminUser(id) {
  return request(`/api/admin/users/${id}`)
}

// 更新指定用户的角色。
export function updateAdminUserRole(id, role) {
  return request(`/api/admin/users/${id}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role })
  })
}

// 分页查询全部反馈，并支持按用户、分类和状态筛选。
export function listAdminFeedback(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  if (params.userId) query.set('userId', params.userId)
  if (params.category) query.set('category', params.category)
  if (params.status) query.set('status', params.status)

  return request(`/api/feedback/admin/all?${query.toString()}`)
}

// 更新指定反馈的处理状态。
export function updateFeedbackStatus(id, status) {
  return request(`/api/feedback/admin/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status })
  })
}

// 分页查询管理员操作日志。
export function pageAdminOperationLogs(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10,
    keyword: params.keyword || ''
  })

  if (params.userId) query.set('userId', params.userId)

  return request(`/api/operation-logs/admin/page?${query.toString()}`)
}
