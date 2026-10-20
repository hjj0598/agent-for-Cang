import { request } from '@/utils/request'

// 分页获取当前用户提交的反馈。
export function listFeedback(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  return request(`/api/feedback?${query.toString()}`)
}

// 提交一条新的用户反馈。
export function submitFeedback(data) {
  return request('/api/feedback', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}
