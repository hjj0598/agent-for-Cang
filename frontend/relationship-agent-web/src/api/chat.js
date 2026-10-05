import { getToken } from '@/store/user'
import { handleUnauthorized } from '@/utils/request'

export async function chatStream(memoryId, message, onChunk) {
  const params = new URLSearchParams({
    memoryId,
    message
  })

  const response = await fetch(`/api/ai/chat?${params.toString()}`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${getToken()}`
    }
  })

  if (!response.ok) {
    if (response.status === 401) {
      handleUnauthorized()
    }
    const error = new Error(`聊天接口请求失败：${response.status}`)
    error.authExpired = response.status === 401
    throw error
  }

  if (!response.body) {
    const text = await response.text()
    onChunk(text)
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    const chunk = decoder.decode(value, { stream: true })
    onChunk(formatStreamChunk(chunk))
  }
}

function formatStreamChunk(chunk) {
  return chunk
    .split('\n')
    .map(line => line.startsWith('data:') ? line.slice(5).trimStart() : line)
    .filter(line => line !== '[DONE]')
    .join('\n')
}
