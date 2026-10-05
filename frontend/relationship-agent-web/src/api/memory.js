import { request } from '@/utils/request'

export function listMemories(personId) {
  return request(`/api/persons/${personId}/memories`)
}

export function addMemory(personId, data) {
  return request(`/api/persons/${personId}/memories`, {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function deleteMemory(personId, memoryId) {
  return request(`/api/persons/${personId}/memories/${memoryId}`, {
    method: 'DELETE'
  })
}

export function searchMemoriesByPersonName(personName) {
  const params = new URLSearchParams({
    personName
  })

  return request(`/api/person-memories/search?${params.toString()}`)
}
