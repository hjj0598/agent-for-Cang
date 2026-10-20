import { request } from '@/utils/request'

// 获取指定数量的最近操作日志。
export function listRecentOperationLogs(limit = 10) {
  return request(`/api/operation-logs/recent?limit=${limit}`)
}
