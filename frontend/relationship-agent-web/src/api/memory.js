import { request } from '@/utils/request'

// 获取指定人物关联的全部记忆。
export function listMemories(personId) {
  return request(`/api/persons/${personId}/memories`)
}

// 为指定人物添加一条记忆。
export function addMemory(personId, data) {
  return request(`/api/persons/${personId}/memories`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 删除指定人物的一条记忆。
export function deleteMemory(personId, memoryId) {
  return request(`/api/persons/${personId}/memories/${memoryId}`, {
    method: 'DELETE'
  })
}

// 根据人物姓名搜索相关记忆。
export function searchMemoriesByPersonName(personName) {
  const params = new URLSearchParams({
    personName
  })

  return request(`/api/person-memories/search?${params.toString()}`)
}
