import { request } from '@/utils/request'

export function listFeedback(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  return request(`/api/feedback?${query.toString()}`)
}

export function submitFeedback(data) {
  return request('/api/feedback', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}
