<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">战狼公司意见收集箱</h2>
        <p class="page-desc">暂时只能提供意见，管理回复功能还在研发，完成后你可以去功能迭代里查看进度。</p>
      </div>
      <button class="btn secondary" type="button" :disabled="loading" @click="reloadFeedback">
        {{ loading ? '刷新中...' : '刷新记录' }}
      </button>
    </div>

    <section class="feedback-hero">
      <div>
        <span class="tag">Company Feedback</span>
        <h3>每一条意见都会被保存，并按当前登录用户隔离展示。</h3>
        <p>你可以选择分类，也可以选择自动分类，后端会根据内容做简单判断。</p>
      </div>
      <img src="@/assets/brand-logo.png" alt="品牌标识">
    </section>

    <div class="feedback-grid">
      <section class="panel submit-panel">
        <div class="panel-title-row">
          <h3>提交意见</h3>
          <span class="tag">管理回复研发中</span>
        </div>

        <form class="feedback-form" @submit.prevent="submit">
          <label>
            意见分类
            <select v-model="form.category" class="select">
              <option value="AUTO">自动分类</option>
              <option value="FEATURE">功能建议</option>
              <option value="BUG">问题反馈</option>
              <option value="EXPERIENCE">体验优化</option>
              <option value="AI">AI建议</option>
              <option value="OTHER">其他</option>
            </select>
          </label>

          <label>
            意见内容
            <textarea
              v-model="form.content"
              class="textarea feedback-textarea"
              maxlength="2000"
              placeholder="例如：希望聊天页面支持收藏重要回复，或者知识库上传后可以显示处理进度。"
            />
          </label>

          <div class="form-footer">
            <span>{{ form.content.length }} / 2000</span>
            <button class="btn" type="submit" :disabled="loading">
              {{ loading ? '提交中...' : '提交意见' }}
            </button>
          </div>
        </form>

        <div v-if="message" :class="['message', messageType]">{{ message }}</div>
      </section>

      <section class="panel history-panel">
        <div class="panel-title-row">
          <h3>我的意见记录</h3>
          <span class="count-text">共 {{ total }} 条</span>
        </div>

        <div v-if="loading && feedbackList.length === 0" class="empty-text">正在加载意见记录...</div>
        <div v-else-if="feedbackList.length === 0" class="empty-text">暂无意见记录</div>

        <article v-for="item in feedbackList" :key="item.id" class="feedback-item">
          <div class="item-head">
            <span class="tag">{{ categoryText(item.category) }}</span>
            <span :class="['status-pill', 'status-' + item.status]">
              {{ statusText(item.status) }}
            </span>
            <small>{{ formatDate(item.createdAt) }}</small>
          </div>
          <p>{{ item.content }}</p>
        </article>

        <div class="pager">
          <button class="btn secondary" type="button" :disabled="loading || page <= 1" @click="changePage(page - 1)">
            上一页
          </button>
          <span>第 {{ page }} / {{ totalPages }} 页</span>
          <button class="btn secondary" type="button" :disabled="loading || page >= totalPages" @click="changePage(page + 1)">
            下一页
          </button>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import { listFeedback, submitFeedback } from '@/api/feedback'

export default {
  name: 'FeedbackView',
  components: {
    AppLayout
  },
  data() {
    return {
      loading: false,
      message: '',
      messageType: 'error',
      page: 1,
      pageSize: 8,
      total: 0,
      feedbackList: [],
      form: {
        category: 'AUTO',
        content: ''
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
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    }
  },
  created() {
    this.loadFeedback()
  },
  methods: {
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
    },
    async loadFeedback() {
      this.loading = true
      try {
        const result = await listFeedback({
          page: this.page,
          pageSize: this.pageSize
        })

        if (result.code !== 1) {
          this.showMessage(result.msg || '加载意见记录失败')
          return
        }

        const pageData = result.data || {}
        this.feedbackList = pageData.rows || []
        this.total = pageData.total || 0
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    reloadFeedback() {
      this.loadFeedback()
    },
    async submit() {
      if (!this.form.content.trim()) {
        this.showMessage('请先填写意见内容')
        return
      }

      this.loading = true
      try {
        const result = await submitFeedback({
          category: this.form.category,
          content: this.form.content
        })

        if (result.code !== 1) {
          this.showMessage(result.msg || '提交意见失败')
          return
        }

        this.showMessage('意见提交成功，感谢你的反馈', 'success')
        this.form = {
          category: 'AUTO',
          content: ''
        }
        this.page = 1
        await this.loadFeedback()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async changePage(page) {
      if (page < 1 || page > this.totalPages) return
      this.page = page
      await this.loadFeedback()
    },
    categoryText(category) {
      return this.categoryMap[category] || category || '其他'
    },
    statusText(status) {
      return this.statusMap[status] || status || '已提交'
    },
    formatDate(dateText) {
      if (!dateText) return '暂无时间'
      return dateText.replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.feedback-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22px;
  margin-bottom: 16px;
  padding: 24px;
  border-radius: 8px;
  color: #ffffff;
  background:
    linear-gradient(135deg, #11151d, #252b35 64%, #3a1b20),
    #11151d;
  box-shadow: 0 22px 60px rgba(17, 24, 39, 0.16);
}

.feedback-hero h3 {
  max-width: 720px;
  margin: 14px 0 0;
  font-size: 28px;
  line-height: 1.35;
}

.feedback-hero p {
  margin: 10px 0 0;
  color: #d1d5db;
  line-height: 1.7;
}

.feedback-hero img {
  width: 112px;
  height: 112px;
  object-fit: contain;
  filter: drop-shadow(0 18px 38px rgba(0, 0, 0, 0.38));
}

.feedback-grid {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 16px;
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

.feedback-form {
  display: grid;
  gap: 14px;
}

.feedback-form label {
  display: grid;
  gap: 7px;
  color: #374151;
  font-weight: 700;
}

.feedback-textarea {
  min-height: 180px;
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #6b7280;
  font-size: 13px;
}

.message {
  margin-top: 14px;
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

.count-text,
.empty-text {
  color: #6b7280;
}

.feedback-item {
  display: grid;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px solid #e5e7eb;
}

.feedback-item:last-child {
  border-bottom: 0;
}

.item-head {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
}

.item-head small {
  margin-left: auto;
}

.item-head small {
  color: #6b7280;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.status-SUBMITTED {
  color: #9a3412;
  background: #ffedd5;
}

.status-REVIEWED {
  color: #1d4ed8;
  background: #dbeafe;
}

.status-DONE {
  color: #047857;
  background: #d1fae5;
}

.feedback-item p {
  margin: 0;
  color: #374151;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
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

@media (max-width: 1000px) {
  .feedback-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .feedback-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .form-footer,
  .item-head {
    align-items: flex-start;
    flex-direction: column;
  }

  .item-head small {
    margin-left: 0;
  }
}
</style>
