import { request } from '@/utils/request'

export function listPersons(params = {}) {
  const query = new URLSearchParams({
    page: params.page || 1,
    pageSize: params.pageSize || 10
  })

  return request(`/api/persons?${query.toString()}`)
}

export function addPerson(data) {
  return request('/api/persons', {
    method: 'POST',
    body: JSON.stringify(data)
  })
}

export function getPerson(id) {
  return request(`/api/persons/${id}`)
}

export function updatePerson(id, data) {
  return request(`/api/persons/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data)
  })
}

export function deletePerson(id) {
  return request(`/api/persons/${id}`, {
    method: 'DELETE'
  })
}
