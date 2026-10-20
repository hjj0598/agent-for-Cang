import { request } from '@/utils/request'

// 上传图片并根据可选提示词执行内容分析。
export function analyzeImage(file, prompt) {
  const formData = new FormData()
  formData.append('file', file)

  if (prompt && prompt.trim()) {
    formData.append('prompt', prompt.trim())
  }

  return request('/api/images/analyze', {
    method: 'POST',
    body: formData
  })
}

// 从上传图片中提取指定人物的记忆候选内容。
export function extractMemoryCandidates(file, personName) {
  const formData = new FormData()
  formData.append('file', file)

  if (personName && personName.trim()) {
    formData.append('personName', personName.trim())
  }

  return request('/api/images/memory-candidates', {
    method: 'POST',
    body: formData
  })
}
