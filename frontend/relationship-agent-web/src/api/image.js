import { request } from '@/utils/request'

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
