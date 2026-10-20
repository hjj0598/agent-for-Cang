import { request } from '@/utils/request'

// 获取首页仪表盘的统计数据。
export function getDashboardStats() {
  return request('/api/dashboard/stats')
}

// 获取当前用户的关系提醒。
export function getRelationshipReminders() {
  return request('/api/dashboard/reminders')
}

// 获取当前用户的关系维护建议。
export function getRelationshipSuggestions() {
  return request('/api/dashboard/suggestions')
}
