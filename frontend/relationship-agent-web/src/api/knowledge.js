import { request } from '@/utils/request'

export function listKnowledge(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  if (params.keyword) {
    query.append('keyword', params.keyword)
  }

  return request(`/api/knowledge?${query.toString()}`)
}

export function addTextKnowledge(data) {
  return request('/api/knowledge/text', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function getKnowledgeDetail(id) {
  return request(`/api/knowledge/${id}`)
}

export function getKnowledgeChunks(id) {
  return request(`/api/knowledge/${id}/chunks`)
}

export function updateKnowledge(id, data) {
  return request(`/api/knowledge/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

export function deleteKnowledge(id) {
  return request(`/api/knowledge/${id}`, {
    method: 'DELETE'
  })
}

export function uploadKnowledge(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request('/api/knowledge/upload', {
    method: 'POST',
    body: formData
  })
}
