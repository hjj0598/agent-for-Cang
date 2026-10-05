<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">首页控制台</h2>
        <p class="page-desc">查看当前账号的人物、知识库、聊天和最近操作情况。</p>
      </div>
      <button class="btn secondary" type="button" :disabled="loading" @click="loadDashboard">
        {{ loading ? '刷新中...' : '刷新数据' }}
      </button>
    </div>

    <section class="hero-panel">
      <div class="hero-copy">
        <span class="tag">AI Agent Workspace</span>
        <h3>让 AI 带着记忆回答，而不是从零开始聊天。</h3>
        <p>人物记忆负责“人”，知识库负责“资料”，聊天历史负责“上下文”。</p>
        <strong class="slogan">We will must pop on the street</strong>
      </div>
      <div class="hero-media">
        <BrandVideo compact />
      </div>
    </section>

    <div v-if="message" class="message error">{{ message }}</div>

    <section class="stats-grid">
      <article class="stat-card" :class="{ 'stat-loading': loading && !statsLoaded }">
        <div class="stat-card-top">
          <span>人物数量</span>
          <span class="stat-icon">人</span>
        </div>
        <strong>{{ stats.personCount }}</strong>
        <p>当前账号管理的人物对象。</p>
      </article>
      <article class="stat-card" :class="{ 'stat-loading': loading && !statsLoaded }">
        <div class="stat-card-top">
          <span>知识库数量</span>
          <span class="stat-icon">知</span>
        </div>
        <strong>{{ stats.knowledgeCount }}</strong>
        <p>手动添加或上传的知识文档。</p>
      </article>
      <article class="stat-card" :class="{ 'stat-loading': loading && !statsLoaded }">
        <div class="stat-card-top">
          <span>聊天会话</span>
          <span class="stat-icon">聊</span>
        </div>
        <strong>{{ stats.chatSessionCount }}</strong>
        <p>按照 memoryId 保存的历史会话。</p>
      </article>
      <article class="stat-card" :class="{ 'stat-loading': loading && !statsLoaded }">
        <div class="stat-card-top">
          <span>聊天消息</span>
          <span class="stat-icon">AI</span>
        </div>
        <strong>{{ stats.chatMessageCount }}</strong>
        <p>用户消息和 AI 回复都会保存。</p>
      </article>
    </section>

    <section class="panel reminder-panel">
      <div class="panel-title-row">
        <div>
          <h3>关系维护提醒</h3>
          <p class="section-hint">根据人物资料和重要日期，给出下一步维护建议。</p>
        </div>
        <span class="tag">{{ reminders.length }} 条</span>
      </div>

      <div v-if="reminders.length === 0" class="empty-text">
        暂无提醒。可以在人物详情中补充重要日期或最近的关系信息。
      </div>
      <div v-else class="reminder-list">
        <article v-for="reminder in reminders" :key="reminder.reminderType + '-' + reminder.personId + '-' + reminder.title" class="reminder-item">
          <div class="reminder-icon">{{ reminder.reminderType === 'DATE' ? '日' : '更' }}</div>
          <div class="reminder-content">
            <div class="reminder-head">
              <strong>{{ reminder.personName }} · {{ reminder.title }}</strong>
              <small>{{ formatDate(reminder.referenceTime) }}</small>
            </div>
            <p>{{ reminder.detail }}</p>
            <div v-if="reminder.eventDate || reminder.source" class="reminder-meta">
              <span v-if="reminder.eventDate">识别日期：{{ reminder.eventDate }}</span>
              <span v-if="reminder.source">来源：{{ reminder.source }}</span>
            </div>
          </div>
          <router-link class="btn secondary small" :to="'/persons/' + reminder.personId">
            查看人物
          </router-link>
        </article>
      </div>
    </section>

    <section class="panel suggestion-panel">
      <div class="panel-title-row">
        <div>
          <h3>AI 关系维护建议</h3>
          <p class="section-hint">根据人物记忆、兴趣、重要日期和资料更新时间生成下一步行动。</p>
        </div>
        <button class="btn secondary small" type="button" :disabled="suggestionLoading" @click="loadSuggestions">
          {{ suggestionLoading ? '生成中...' : '重新生成' }}
        </button>
      </div>

      <div v-if="suggestionLoading" class="suggestion-empty">
        <strong>AI 正在整理关系上下文</strong>
        <span>正在读取人物记忆、重要日期和资料更新时间，请稍候。</span>
      </div>
      <div v-else-if="suggestionError" class="suggestion-empty suggestion-error">
        <strong>建议暂时没有生成成功</strong>
        <span>{{ suggestionError }}</span>
        <button class="btn secondary small" type="button" @click="loadSuggestions">重新生成</button>
      </div>
      <div v-else-if="suggestions.length === 0" class="empty-text">
        暂无可生成的建议。可以先补充人物爱好、重要日期或最近联系情况。
      </div>
      <div v-else class="suggestion-list">
        <article v-for="suggestion in suggestions" :key="suggestion.personId + '-' + suggestion.title" class="suggestion-item">
          <div :class="['priority-mark', 'priority-' + suggestion.priority]">
            {{ priorityText(suggestion.priority) }}
          </div>
          <div class="suggestion-content">
            <div class="suggestion-head">
              <strong>{{ suggestion.personName }} · {{ suggestion.title }}</strong>
              <span class="tag">{{ suggestion.generatedBy === 'AI' ? 'AI 生成' : '规则兜底' }}</span>
            </div>
            <p>{{ suggestion.reason }}</p>
            <div class="suggestion-action">下一步：{{ suggestion.action }}</div>
          </div>
          <router-link v-if="suggestion.personId" class="btn secondary small" :to="'/persons/' + suggestion.personId">
            查看人物
          </router-link>
        </article>
      </div>
    </section>

    <div class="dashboard-grid">
      <section class="panel quick-panel">
        <div class="panel-title-row">
          <h3>常用入口</h3>
          <span class="tag">工作流</span>
        </div>
        <div class="quick-grid">
          <router-link class="quick-link" to="/chat">
            <strong>开始聊天</strong>
            <span>让 AI 同时使用人物记忆、聊天历史和知识库回答。</span>
          </router-link>
          <router-link class="quick-link" to="/persons">
            <strong>管理人物</strong>
            <span>新增人物，再补充爱好、语录、性格和备注。</span>
          </router-link>
          <router-link class="quick-link" to="/knowledge">
            <strong>维护知识库</strong>
            <span>上传 TXT/PDF，或手动录入资料并进入向量检索。</span>
          </router-link>
        </div>
      </section>

      <section class="panel log-panel">
        <div class="panel-title-row">
          <h3>最近操作</h3>
          <span class="tag">operation_log</span>
        </div>
        <div v-if="logs.length === 0" class="empty-text">暂无操作记录</div>
        <article v-for="log in logs" :key="log.id" class="log-item">
          <span>{{ log.operationType }}</span>
          <strong>{{ log.operationContent }}</strong>
          <small>{{ formatDate(log.createdAt) }}</small>
        </article>
      </section>
    </div>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import BrandVideo from '@/components/BrandVideo.vue'
import { getDashboardStats, getRelationshipReminders, getRelationshipSuggestions } from '@/api/dashboard'
import { listRecentOperationLogs } from '@/api/operationLog'

export default {
  name: 'DashboardView',
  components: {
    AppLayout,
    BrandVideo
  },
  data() {
    return {
      loading: false,
      message: '',
      stats: {
        personCount: 0,
        knowledgeCount: 0,
        chatSessionCount: 0,
        chatMessageCount: 0
      },
      reminders: [],
      suggestions: [],
      suggestionLoading: false,
      suggestionError: '',
      statsLoaded: false,
      logs: []
    }
  },
  created() {
    this.loadDashboard()
  },
  methods: {
    async loadDashboard() {
      this.loading = true
      this.message = ''
      try {
        const [statsResult, logsResult, remindersResult] = await Promise.all([
          getDashboardStats(),
          listRecentOperationLogs(10),
          getRelationshipReminders()
        ])

        if (statsResult.code !== 1) {
          this.message = statsResult.msg || '加载统计失败'
          return
        }

        this.stats = {
          ...this.stats,
          ...(statsResult.data || {})
        }
        this.statsLoaded = true

        this.logs = logsResult.code === 1 ? (logsResult.data || []) : []
        this.reminders = remindersResult.code === 1 ? (remindersResult.data || []) : []
        this.loadSuggestions()
      } catch (error) {
        this.message = '请求失败，请确认后端服务已经启动'
      } finally {
        this.loading = false
      }
    },
    async loadSuggestions() {
      this.suggestionLoading = true
      this.suggestionError = ''
      try {
        const result = await getRelationshipSuggestions()
        if (result.code !== 1) {
          this.suggestions = []
          this.suggestionError = result.msg || '模型暂时没有返回建议'
          return
        }
        this.suggestions = result.data || []
      } catch (error) {
        this.suggestions = []
        this.suggestionError = '请确认后端服务和模型接口正常，再点击重试。'
      } finally {
        this.suggestionLoading = false
      }
    },
    priorityText(priority) {
      const map = {
        HIGH: '优先',
        MEDIUM: '建议',
        LOW: '可选'
      }
      return map[priority] || '建议'
    },
    formatDate(dateText) {
      if (!dateText) return '暂无时间'
      return dateText.replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.hero-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 540px);
  gap: 22px;
  align-items: stretch;
  margin-bottom: 16px;
  padding: 26px;
  border-radius: 8px;
  color: #ffffff;
  background:
    linear-gradient(135deg, #11151d, #252b35 64%, #3a1b20),
    #11151d;
  box-shadow: 0 22px 60px rgba(17, 24, 39, 0.16);
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-panel h3 {
  max-width: 680px;
  margin: 14px 0 0;
  font-size: 28px;
  line-height: 1.35;
}

.hero-panel p {
  margin: 10px 0 0;
  color: #d1d5db;
  line-height: 1.7;
}

.slogan {
  display: block;
  margin-top: 18px;
  color: #ffffff;
  font-size: 24px;
  line-height: 1.2;
}

.hero-media {
  min-width: 0;
  align-self: stretch;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.stat-card-top > span:first-child {
  color: #6b7280;
  font-size: 14px;
}

.stat-icon {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 8px;
  color: #b91c1c;
  background: #fff1f2;
  font-size: 11px;
  font-weight: 800;
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 36px;
  line-height: 1;
}

.stat-loading strong {
  color: transparent;
  border-radius: 6px;
  background: linear-gradient(90deg, #eef0f3 25%, #f8f9fa 50%, #eef0f3 75%);
  background-size: 220% 100%;
  animation: dashboard-shimmer 1.2s infinite;
}

@keyframes dashboard-shimmer {
  from {
    background-position: 220% 0;
  }

  to {
    background-position: -220% 0;
  }
}

.stat-card p {
  margin: 12px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.reminder-panel {
  margin-top: 16px;
}

.section-hint {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.reminder-list {
  display: grid;
  gap: 10px;
}

.reminder-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.reminder-icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, var(--brand), var(--brand-dark));
  font-size: 13px;
  font-weight: 800;
}

.reminder-content {
  min-width: 0;
}

.reminder-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reminder-head strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reminder-head small,
.reminder-content p {
  color: #6b7280;
}

.reminder-content p {
  margin: 5px 0 0;
  line-height: 1.5;
}

.reminder-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 7px;
  color: #9ca3af;
  font-size: 12px;
}

.btn.small {
  min-height: 34px;
  padding: 7px 11px;
  font-size: 13px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 16px;
  margin-top: 16px;
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

.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.quick-link {
  display: grid;
  gap: 8px;
  min-height: 128px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.quick-link:hover {
  transform: translateY(-2px);
  border-color: rgba(239, 52, 61, 0.32);
  background: #ffffff;
}

.quick-link strong {
  color: #1f2937;
  font-size: 17px;
}

.quick-link span {
  color: #6b7280;
  line-height: 1.6;
}

.log-panel {
  align-self: start;
}

.log-item {
  display: grid;
  gap: 5px;
  padding: 12px 0;
  border-bottom: 1px solid #e5e7eb;
}

.log-item:last-child {
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

.log-item small,
.empty-text {
  color: #6b7280;
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

.suggestion-panel {
  margin-top: 16px;
}

.suggestion-list {
  display: grid;
  gap: 10px;
}

.suggestion-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.priority-mark {
  display: grid;
  place-items: center;
  width: 42px;
  min-height: 34px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 800;
}

.priority-HIGH {
  color: #991b1b;
  background: #fee2e2;
}

.priority-MEDIUM {
  color: #92400e;
  background: #fef3c7;
}

.priority-LOW {
  color: #1d4ed8;
  background: #dbeafe;
}

.suggestion-content {
  min-width: 0;
}

.suggestion-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.suggestion-head strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggestion-content p {
  margin: 6px 0 0;
  color: #6b7280;
  line-height: 1.5;
}

.suggestion-action {
  margin-top: 8px;
  color: #374151;
  line-height: 1.5;
}

.suggestion-empty {
  display: grid;
  gap: 7px;
  padding: 16px;
  border: 1px dashed #d5d9e1;
  border-radius: 8px;
  color: #6b7280;
  background: #f9fafb;
}

.suggestion-empty strong {
  color: #1f2937;
}

.suggestion-error {
  border-color: #fecaca;
  background: #fff7f7;
}

.suggestion-error strong {
  color: #991b1b;
}

@media (max-width: 1200px) {
  .hero-panel,
  .dashboard-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 1000px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .reminder-item {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .reminder-item .btn {
    grid-column: 2;
    justify-self: start;
  }

  .reminder-head {
    display: grid;
    gap: 3px;
  }

  .suggestion-item {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .suggestion-item .btn {
    grid-column: 2;
    justify-self: start;
  }

  .suggestion-head {
    display: grid;
    justify-content: start;
    gap: 5px;
  }
}
</style>
