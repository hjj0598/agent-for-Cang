import { getToken } from '@/store/user'
import { handleUnauthorized } from '@/utils/request'

// 发起流式聊天请求，并通过 onChunk 逐段返回模型回复。
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

// 清理服务端事件流标记，将有效文本整理后交给页面展示。
function formatStreamChunk(chunk) {
  return chunk
    .split('\n')
    .map(line => line.startsWith('data:') ? line.slice(5).trimStart() : line)
    .filter(line => line !== '[DONE]')
    .join('\n')
}
