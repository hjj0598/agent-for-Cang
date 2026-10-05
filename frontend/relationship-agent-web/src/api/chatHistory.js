import { request } from '@/utils/request'

export function listChatSessions(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  return request(`/api/chat-sessions?${query.toString()}`)
}

export function listChatMessages(sessionId) {
  return request(`/api/chat-sessions/${sessionId}/messages`)
}

export function searchChatMessages(keyword) {
  const params = new URLSearchParams({
    keyword
  })

  return request(`/api/chat-sessions/search?${params.toString()}`)
}

export function updateChatSessionTitle(sessionId, data) {
  return request(`/api/chat-sessions/${sessionId}/title`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

export function deleteChatSession(sessionId) {
  return request(`/api/chat-sessions/${sessionId}`, {
    method: 'DELETE'
  })
}
