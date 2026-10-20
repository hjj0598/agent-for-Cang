import { request } from '@/utils/request'

// 分页获取当前用户的聊天会话。
export function listChatSessions(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  return request(`/api/chat-sessions?${query.toString()}`)
}

// 获取指定会话中的全部聊天消息。
export function listChatMessages(sessionId) {
  return request(`/api/chat-sessions/${sessionId}/messages`)
}

// 根据关键词搜索聊天消息。
export function searchChatMessages(keyword) {
  const params = new URLSearchParams({
    keyword
  })

  return request(`/api/chat-sessions/search?${params.toString()}`)
}

// 修改指定聊天会话的标题。
export function updateChatSessionTitle(sessionId, data) {
  return request(`/api/chat-sessions/${sessionId}/title`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

// 删除指定聊天会话。
export function deleteChatSession(sessionId) {
  return request(`/api/chat-sessions/${sessionId}`, {
    method: 'DELETE'
  })
}
