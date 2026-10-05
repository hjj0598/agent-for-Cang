import { request } from '@/utils/request'

export function listRecentOperationLogs(limit = 10) {
  return request(`/api/operation-logs/recent?limit=${limit}`)
}
