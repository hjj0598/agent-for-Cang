import { request } from '@/utils/request'

export function getDashboardStats() {
  return request('/api/dashboard/stats')
}

export function getRelationshipReminders() {
  return request('/api/dashboard/reminders')
}

export function getRelationshipSuggestions() {
  return request('/api/dashboard/suggestions')
}
