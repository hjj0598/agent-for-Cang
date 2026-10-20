import { request } from '@/utils/request'

// 分页获取人物关系列表。
export function listPersons(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  return request(`/api/persons?${query.toString()}`)
}

// 新增一个人物关系。
export function addPerson(data) {
  return request('/api/persons', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

// 根据人物 ID 获取详细信息。
export function getPerson(id) {
  return request(`/api/persons/${id}`)
}

// 更新指定人物的信息。
export function updatePerson(id, data) {
  return request(`/api/persons/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

// 删除指定人物关系。
export function deletePerson(id) {
  return request(`/api/persons/${id}`, {
    method: 'DELETE'
  })
}
