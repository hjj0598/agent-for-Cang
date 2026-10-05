import { request } from '@/utils/request'

export function getAdminStats() {
  return request('/api/dashboard/admin/stats')
}

export function listAdminUsers(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10,
    keyword: params.keyword || ''
  })

  return request(`/api/admin/users?${query.toString()}`)
}

export function getAdminUser(id) {
  return request(`/api/admin/users/${id}`)
}

export function updateAdminUserRole(id, role) {
  return request(`/api/admin/users/${id}/role`, {
    method: 'PUT',
    body: JSON.stringify({ role })
  })
}

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

export function updateFeedbackStatus(id, status) {
  return request(`/api/feedback/admin/${id}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status })
  })
}

export function pageAdminOperationLogs(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10,
    keyword: params.keyword || ''
  })

  if (params.userId) query.set('userId', params.userId)

  return request(`/api/operation-logs/admin/page?${query.toString()}`)
}
