<template>
  <AppLayout>
    <div class="page-header admin-header">
      <div class="admin-title-block">
        <img class="admin-logo animated-logo" src="@/assets/brand-logo.png" alt="战狼公司标识">
        <div>
          <h2 class="page-title">战狼公司管理台</h2>
          <p class="page-desc">管理用户权限、处理意见反馈，并查看全站运行数据。</p>
        </div>
      </div>
      <button class="btn secondary" type="button" :disabled="loading" @click="reloadAll">
        {{ loading ? '刷新中...' : '刷新后台数据' }}
      </button>
    </div>

    <section class="admin-hero">
      <div>
        <span class="tag">Admin Workspace</span>
        <h3>权限、反馈、日志和数据，都集中在这里处理。</h3>
        <p>普通用户无法访问本页面的接口，后端会通过 JWT 里的角色和 @AdminOnly 统一拦截。</p>
      </div>
      <strong>We will must pop on the street</strong>
    </section>

    <div v-if="message" :class="['message', messageType]">{{ message }}</div>

    <section class="stats-grid">
      <article v-for="item in statCards" :key="item.label" class="stat-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.desc }}</p>
      </article>
    </section>

    <section class="panel admin-panel">
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.value"
          type="button"
          :class="['tab-btn', { active: activeTab === tab.value }]"
          @click="switchTab(tab.value)"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="activeTab === 'users'" class="tab-section">
        <div class="panel-title-row">
          <h3>用户管理</h3>
          <span class="count-text">共 {{ users.total }} 个用户</span>
        </div>

        <form class="filter-row" @submit.prevent="searchUsers">
          <input v-model="users.keyword" class="input" placeholder="按用户名或昵称搜索">
          <button class="btn" type="submit" :disabled="loading">搜索</button>
          <button class="btn secondary" type="button" :disabled="loading || !users.keyword" @click="clearUserSearch">
            清空
          </button>
        </form>

        <div class="table-wrap">
          <table class="admin-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>用户名</th>
                <th>昵称</th>
                <th>角色</th>
                <th>创建时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users.rows" :key="user.id">
                <td>{{ user.id }}</td>
                <td>{{ user.username }}</td>
                <td>{{ user.nickname || '-' }}</td>
                <td>
                  <span :class="['role-pill', user.role === 'ADMIN' ? 'admin' : 'user']">
                    {{ user.role || 'USER' }}
                  </span>
                </td>
                <td>{{ formatDate(user.createdAt) }}</td>
                <td>
                  <button
                    class="btn secondary small"
                    type="button"
                    :disabled="loading || user.username === 'admin'"
                    @click="toggleUserRole(user)"
                  >
                    {{ user.role === 'ADMIN' ? '设为 USER' : '设为 ADMIN' }}
                  </button>
                </td>
              </tr>
              <tr v-if="users.rows.length === 0">
                <td colspan="6" class="empty-cell">暂无用户数据</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="pager">
          <button class="btn secondary" type="button" :disabled="loading || users.page <= 1" @click="changeUsersPage(users.page - 1)">
            上一页
          </button>
          <span>第 {{ users.page }} / {{ usersTotalPages }} 页</span>
          <button class="btn secondary" type="button" :disabled="loading || users.page >= usersTotalPages" @click="changeUsersPage(users.page + 1)">
            下一页
          </button>
        </div>
      </div>

      <div v-else-if="activeTab === 'feedback'" class="tab-section">
        <div class="panel-title-row">
          <h3>意见处理</h3>
          <span class="count-text">共 {{ feedback.total }} 条意见</span>
        </div>

        <form class="filter-row feedback-filter" @submit.prevent="searchFeedback">
          <input v-model="feedback.userId" class="input" placeholder="用户ID，可不填">
          <select v-model="feedback.category" class="select">
            <option value="">全部分类</option>
            <option value="FEATURE">功能建议</option>
            <option value="BUG">问题反馈</option>
            <option value="EXPERIENCE">体验优化</option>
            <option value="AI">AI建议</option>
            <option value="OTHER">其他</option>
          </select>
          <select v-model="feedback.status" class="select">
            <option value="">全部状态</option>
            <option value="SUBMITTED">已提交</option>
            <option value="REVIEWED">已查看</option>
            <option value="DONE">已处理</option>
          </select>
          <button class="btn" type="submit" :disabled="loading">筛选</button>
          <button class="btn secondary" type="button" :disabled="loading" @click="clearFeedbackFilter">清空</button>
        </form>

        <article v-for="item in feedback.rows" :key="item.id" class="feedback-card">
          <div class="feedback-card-head">
            <div>
              <span class="tag">{{ categoryText(item.category) }}</span>
              <strong>{{ userDisplayName(item) }} 的意见</strong>
            </div>
            <small>{{ statusText(item.status) }} · {{ formatDate(item.createdAt) }}</small>
          </div>
          <p>{{ item.content }}</p>
          <div class="feedback-actions">
            <button class="btn secondary small" type="button" :disabled="loading || item.status === 'REVIEWED'" @click="changeFeedbackStatus(item, 'REVIEWED')">
              标记已查看
            </button>
            <button class="btn small" type="button" :disabled="loading || item.status === 'DONE'" @click="changeFeedbackStatus(item, 'DONE')">
              标记已处理
            </button>
          </div>
        </article>

        <div v-if="feedback.rows.length === 0" class="empty-text">暂无意见数据</div>

        <div class="pager">
          <button class="btn secondary" type="button" :disabled="loading || feedback.page <= 1" @click="changeFeedbackPage(feedback.page - 1)">
            上一页
          </button>
          <span>第 {{ feedback.page }} / {{ feedbackTotalPages }} 页</span>
          <button class="btn secondary" type="button" :disabled="loading || feedback.page >= feedbackTotalPages" @click="changeFeedbackPage(feedback.page + 1)">
            下一页
          </button>
        </div>
      </div>

      <div v-else class="tab-section">
        <div class="panel-title-row">
          <h3>全站操作日志</h3>
          <span class="count-text">共 {{ logs.total }} 条日志</span>
        </div>

        <form class="filter-row" @submit.prevent="searchLogs">
          <input v-model="logs.keyword" class="input" placeholder="按操作类型或内容搜索">
          <input v-model="logs.userId" class="input compact-input" placeholder="用户ID">
          <button class="btn" type="submit" :disabled="loading">搜索</button>
          <button class="btn secondary" type="button" :disabled="loading" @click="clearLogSearch">清空</button>
        </form>

        <article v-for="log in logs.rows" :key="log.id" class="log-item">
          <span>{{ log.operationType }}</span>
          <strong>{{ log.operationContent }}</strong>
          <small>{{ userDisplayName(log) }} · {{ formatDate(log.createdAt) }}</small>
        </article>
        <div v-if="logs.rows.length === 0" class="empty-text">暂无操作日志</div>

        <div class="pager">
          <button class="btn secondary" type="button" :disabled="loading || logs.page <= 1" @click="changeLogsPage(logs.page - 1)">
            上一页
          </button>
          <span>第 {{ logs.page }} / {{ logsTotalPages }} 页</span>
          <button class="btn secondary" type="button" :disabled="loading || logs.page >= logsTotalPages" @click="changeLogsPage(logs.page + 1)">
            下一页
          </button>
        </div>
      </div>
    </section>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import {
  getAdminStats,
  listAdminFeedback,
  listAdminUsers,
  pageAdminOperationLogs,
  updateAdminUserRole,
  updateFeedbackStatus
} from '@/api/admin'

export default {
  name: 'AdminView',
  components: {
    AppLayout
  },
  data() {
    return {
      loading: false,
      message: '',
      messageType: 'error',
      activeTab: 'users',
      tabs: [
        { label: '用户管理', value: 'users' },
        { label: '意见处理', value: 'feedback' },
        { label: '操作日志', value: 'logs' }
      ],
      stats: {
        userCount: 0,
        personCount: 0,
        knowledgeCount: 0,
        chatSessionCount: 0,
        chatMessageCount: 0,
        feedbackCount: 0,
        operationLogCount: 0
      },
      users: {
        page: 1,
        pageSize: 8,
        total: 0,
        keyword: '',
        rows: []
      },
      feedback: {
        page: 1,
        pageSize: 6,
        total: 0,
        userId: '',
        category: '',
        status: '',
        rows: []
      },
      logs: {
        page: 1,
        pageSize: 8,
        total: 0,
        userId: '',
        keyword: '',
        rows: []
      },
      categoryMap: {
        FEATURE: '功能建议',
        BUG: '问题反馈',
        EXPERIENCE: '体验优化',
        AI: 'AI建议',
        OTHER: '其他'
      },
      statusMap: {
        SUBMITTED: '已提交',
        REVIEWED: '已查看',
        DONE: '已处理'
      }
    }
  },
  computed: {
    statCards() {
      return [
        { label: '用户数量', value: this.stats.userCount, desc: '平台注册账号总数。' },
        { label: '人物数量', value: this.stats.personCount, desc: '全站创建的人物对象。' },
        { label: '知识库数量', value: this.stats.knowledgeCount, desc: '全站知识文档数量。' },
        { label: '聊天消息', value: this.stats.chatMessageCount, desc: '用户与 AI 的消息总量。' },
        { label: '意见数量', value: this.stats.feedbackCount, desc: '意见收集箱提交数量。' },
        { label: '操作日志', value: this.stats.operationLogCount, desc: '系统记录的操作行为。' }
      ]
    },
    usersTotalPages() {
      return Math.max(1, Math.ceil(this.users.total / this.users.pageSize))
    },
    feedbackTotalPages() {
      return Math.max(1, Math.ceil(this.feedback.total / this.feedback.pageSize))
    },
    logsTotalPages() {
      return Math.max(1, Math.ceil(this.logs.total / this.logs.pageSize))
    }
  },
  created() {
    this.reloadAll()
  },
  methods: {
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
    },
    async reloadAll() {
      this.loading = true
      this.message = ''
      try {
        await Promise.all([
          this.loadStats(),
          this.loadUsers(),
          this.loadFeedback(),
          this.loadLogs()
        ])
      } catch (error) {
        this.showMessage('加载管理员数据失败，请确认当前账号是管理员并且后端已经启动')
      } finally {
        this.loading = false
      }
    },
    async loadStats() {
      const result = await getAdminStats()
      if (result.code !== 1) {
        throw new Error(result.msg || '加载全站统计失败')
      }
      this.stats = {
        ...this.stats,
        ...(result.data || {})
      }
    },
    async loadUsers() {
      const result = await listAdminUsers({
        page: this.users.page,
        pageSize: this.users.pageSize,
        keyword: this.users.keyword.trim()
      })
      this.applyPageResult(result, this.users, '加载用户失败')
    },
    async loadFeedback() {
      const result = await listAdminFeedback({
        page: this.feedback.page,
        pageSize: this.feedback.pageSize,
        userId: this.feedback.userId.trim(),
        category: this.feedback.category,
        status: this.feedback.status
      })
      this.applyPageResult(result, this.feedback, '加载意见失败')
    },
    async loadLogs() {
      const result = await pageAdminOperationLogs({
        page: this.logs.page,
        pageSize: this.logs.pageSize,
        userId: this.logs.userId.trim(),
        keyword: this.logs.keyword.trim()
      })
      this.applyPageResult(result, this.logs, '加载日志失败')
    },
    applyPageResult(result, target, fallbackMessage) {
      if (result.code !== 1) {
        throw new Error(result.msg || fallbackMessage)
      }

      const pageData = result.data || {}
      target.rows = pageData.rows || []
      target.total = pageData.total || 0
    },
    switchTab(tab) {
      this.activeTab = tab
      this.message = ''
    },
    async searchUsers() {
      this.users.page = 1
      await this.runAction(() => this.loadUsers(), '用户列表已刷新')
    },
    async clearUserSearch() {
      this.users.keyword = ''
      this.users.page = 1
      await this.runAction(() => this.loadUsers(), '用户筛选已清空')
    },
    async changeUsersPage(page) {
      if (page < 1 || page > this.usersTotalPages) return
      this.users.page = page
      await this.runAction(() => this.loadUsers())
    },
    async toggleUserRole(user) {
      const nextRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN'
      if (!window.confirm(`确认把 ${user.username} 设置为 ${nextRole} 吗？`)) return

      await this.runAction(async () => {
        const result = await updateAdminUserRole(user.id, nextRole)
        if (result.code !== 1) {
          throw new Error(result.msg || '修改用户角色失败')
        }
        await Promise.all([this.loadUsers(), this.loadLogs()])
      }, `已将 ${user.username} 设置为 ${nextRole}`)
    },
    async searchFeedback() {
      this.feedback.page = 1
      await this.runAction(() => this.loadFeedback(), '意见列表已刷新')
    },
    async clearFeedbackFilter() {
      this.feedback.userId = ''
      this.feedback.category = ''
      this.feedback.status = ''
      this.feedback.page = 1
      await this.runAction(() => this.loadFeedback(), '意见筛选已清空')
    },
    async changeFeedbackPage(page) {
      if (page < 1 || page > this.feedbackTotalPages) return
      this.feedback.page = page
      await this.runAction(() => this.loadFeedback())
    },
    async changeFeedbackStatus(item, status) {
      await this.runAction(async () => {
        const result = await updateFeedbackStatus(item.id, status)
        if (result.code !== 1) {
          throw new Error(result.msg || '修改意见状态失败')
        }
        await Promise.all([this.loadFeedback(), this.loadStats(), this.loadLogs()])
      }, `意见已标记为${this.statusText(status)}`)
    },
    async searchLogs() {
      this.logs.page = 1
      await this.runAction(() => this.loadLogs(), '日志列表已刷新')
    },
    async clearLogSearch() {
      this.logs.keyword = ''
      this.logs.userId = ''
      this.logs.page = 1
      await this.runAction(() => this.loadLogs(), '日志筛选已清空')
    },
    async changeLogsPage(page) {
      if (page < 1 || page > this.logsTotalPages) return
      this.logs.page = page
      await this.runAction(() => this.loadLogs())
    },
    async runAction(action, successMessage) {
      this.loading = true
      this.message = ''
      try {
        await action()
        if (successMessage) {
          this.showMessage(successMessage, 'success')
        }
      } catch (error) {
        this.showMessage(error.message || '操作失败，请稍后再试')
      } finally {
        this.loading = false
      }
    },
    categoryText(category) {
      return this.categoryMap[category] || category || '其他'
    },
    statusText(status) {
      return this.statusMap[status] || status || '已提交'
    },
    userDisplayName(item) {
      if (item.nickname && item.username && item.nickname !== item.username) {
        return `${item.nickname}（${item.username}）`
      }

      if (item.username) {
        return item.username
      }

      if (item.nickname) {
        return item.nickname
      }

      return `用户 ${item.userId}`
    },
    formatDate(dateText) {
      if (!dateText) return '暂无时间'
      return dateText.replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.admin-header {
  align-items: center;
}

.admin-title-block {
  display: flex;
  align-items: center;
  gap: 14px;
}

.admin-logo {
  width: 58px;
  height: 58px;
  object-fit: contain;
  border-radius: 8px;
  background: #05070a;
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.18);
}

.admin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 16px;
  padding: 24px;
  border-radius: 8px;
  color: #ffffff;
  background:
    linear-gradient(135deg, #11151d, #252b35 64%, #3a1b20),
    #11151d;
  box-shadow: 0 22px 60px rgba(17, 24, 39, 0.16);
}

.admin-hero h3 {
  margin: 14px 0 0;
  font-size: 28px;
  line-height: 1.35;
}

.admin-hero p {
  margin: 10px 0 0;
  color: #d1d5db;
  line-height: 1.7;
}

.admin-hero strong {
  max-width: 280px;
  font-size: 24px;
  line-height: 1.25;
  text-align: right;
}

.message {
  margin-bottom: 16px;
  padding: 10px 12px;
  font-size: 14px;
}

.message.error {
  color: #991b1b;
  background: #fee2e2;
}

.message.success {
  color: #047857;
  background: #d1fae5;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  padding: 18px;
}

.stat-card span {
  color: #6b7280;
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  line-height: 1;
}

.stat-card p {
  margin: 10px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.admin-panel {
  display: grid;
  gap: 16px;
  min-width: 0;
}

.tabs {
  min-width: 0;
  display: flex;
  gap: 10px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e5e7eb;
}

.tab-btn {
  min-height: 38px;
  border: 0;
  border-radius: 8px;
  padding: 8px 14px;
  color: #374151;
  background: #eef0f3;
}

.tab-btn.active {
  color: #ffffff;
  background: linear-gradient(135deg, #ef343d, #c91f2b);
  box-shadow: 0 12px 26px rgba(239, 52, 61, 0.2);
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-title-row h3 {
  margin: 0;
}

.count-text,
.empty-text {
  color: #6b7280;
}

.filter-row {
  min-width: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  margin-bottom: 16px;
}

.feedback-filter {
  grid-template-columns: 160px 180px 180px auto auto;
}

.compact-input {
  max-width: 160px;
}

.table-wrap {
  min-width: 0;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.admin-table {
  width: 100%;
  min-width: 760px;
  border-collapse: collapse;
}

.tab-section {
  min-width: 0;
}

.admin-table th,
.admin-table td {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
  text-align: left;
  vertical-align: middle;
}

.admin-table th {
  color: #6b7280;
  background: #f9fafb;
  font-size: 13px;
}

.admin-table tr:last-child td {
  border-bottom: 0;
}

.empty-cell {
  color: #6b7280;
  text-align: center;
}

.role-pill {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.role-pill.admin {
  color: #b91c1c;
  background: #fff1f2;
}

.role-pill.user {
  color: #047857;
  background: #d1fae5;
}

.btn.small {
  min-height: 32px;
  padding: 7px 10px;
  font-size: 13px;
}

.feedback-card {
  display: grid;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
}

.feedback-card:last-of-type {
  border-bottom: 0;
}

.feedback-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.feedback-card-head > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.feedback-card-head small,
.log-item small {
  color: #6b7280;
}

.feedback-card p {
  margin: 0;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.feedback-actions {
  display: flex;
  gap: 10px;
}

.log-item {
  display: grid;
  gap: 6px;
  padding: 13px 0;
  border-bottom: 1px solid #e5e7eb;
}

.log-item:last-of-type {
  border-bottom: 0;
}

.log-item span {
  width: fit-content;
  padding: 3px 8px;
  border-radius: 999px;
  color: #b91c1c;
  background: #fff1f2;
  font-size: 12px;
  font-weight: 700;
}

.log-item strong {
  color: #1f2937;
  line-height: 1.5;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 16px;
  color: #6b7280;
  font-size: 14px;
}

@media (max-width: 1280px) {
  .stats-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .feedback-filter,
  .filter-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .admin-title-block,
  .admin-hero,
  .feedback-card-head,
  .feedback-card-head > div,
  .feedback-actions,
  .tabs,
  .pager {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-grid,
  .feedback-filter,
  .filter-row {
    grid-template-columns: 1fr;
  }

  .admin-hero strong {
    text-align: left;
  }
}
</style>
