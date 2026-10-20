import { request } from '@/utils/request'

// 分页查询知识库内容，并支持关键词筛选。
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

// 添加一条纯文本知识。
export function addTextKnowledge(data) {
  return request('/api/knowledge/text', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 根据知识 ID 获取知识详情。
export function getKnowledgeDetail(id) {
  return request(`/api/knowledge/${id}`)
}

// 获取指定知识经过切分后的文本块。
export function getKnowledgeChunks(id) {
  return request(`/api/knowledge/${id}/chunks`)
}

// 更新指定知识的内容。
export function updateKnowledge(id, data) {
  return request(`/api/knowledge/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

// 删除指定知识。
export function deleteKnowledge(id) {
  return request(`/api/knowledge/${id}`, {
    method: 'DELETE'
  })
}

// 上传文件并将其导入知识库。
export function uploadKnowledge(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request('/api/knowledge/upload', {
    method: 'POST',
    body: formData
  })
}
