<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">AI 聊天</h2>
        <p class="page-desc">让 Agent 使用人物记忆、聊天历史和知识库一起回答。</p>
      </div>
      <div class="session-id">memoryId：{{ memoryId }}</div>
    </div>

    <section class="chat-layout">
      <aside class="panel session-panel">
        <button class="btn full-btn" @click="newSession">新建会话</button>

        <div class="section-title-row">
          <div class="quick-title">历史会话（{{ sessionTotal }}）</div>
          <button class="refresh-btn" type="button" :disabled="sessionLoading" @click="resetSessions">
            {{ sessionLoading ? '刷新中...' : '刷新' }}
          </button>
        </div>

        <form class="history-search" @submit.prevent="searchHistory">
          <input v-model="historyKeyword" class="history-search-input" placeholder="搜索历史消息">
          <button type="submit" :disabled="sessionLoading">搜索</button>
        </form>

        <div v-if="searched && searchResults.length === 0" class="empty-session search-empty">
          没有搜索到相关历史消息
        </div>

        <div v-if="searchResults.length > 0" class="search-results">
          <div class="search-title">搜索结果</div>
          <button
            v-for="result in searchResults"
            :key="result.messageId"
            class="search-result"
            type="button"
            @click="openSearchResult(result)"
          >
            <span>{{ result.sessionTitle }}</span>
            <small>{{ result.role === 'user' ? '用户' : 'AI' }} · {{ previewText(result.content) }}</small>
          </button>
          <button class="clear-search" type="button" @click="clearSearch">清空搜索</button>
        </div>

        <div class="session-list">
          <div v-if="sessionLoading && sessions.length === 0" class="empty-session">正在加载历史会话...</div>
          <div v-else-if="sessions.length === 0" class="empty-session">暂无历史会话</div>
          <div
            v-for="item in sessions"
            :key="item.id"
            class="session-item"
            :class="{ active: item.memoryId === memoryId }"
          >
            <template v-if="editingSessionId === item.id">
              <input
                v-model="editingTitle"
                class="session-title-input"
                @keydown.enter.prevent="saveSessionTitle(item)"
                @keydown.esc.prevent="cancelRename"
              >
              <button class="session-save" type="button" :disabled="sending" @click="saveSessionTitle(item)">
                保存
              </button>
              <button class="session-cancel" type="button" :disabled="sending" @click="cancelRename">
                取消
              </button>
            </template>
            <template v-else>
              <button class="session-open" type="button" @click="loadSession(item)">
                <span>{{ item.title }}</span>
                <small>{{ formatDate(item.updatedAt) }}</small>
              </button>
              <button class="session-rename" type="button" :disabled="sending" @click="startRename(item)">
                重命名
              </button>
              <button class="session-delete" type="button" :disabled="sending" @click="removeSession(item)">
                删除
              </button>
            </template>
          </div>
        </div>

        <button
          class="load-more"
          type="button"
          :disabled="sessionLoading || !hasMoreSessions"
          @click="loadMoreSessions"
        >
          {{ hasMoreSessions ? (sessionLoading ? '加载中...' : '加载更多') : '已加载全部' }}
        </button>

        <details class="prompt-section">
          <summary>快捷提示</summary>
          <button v-for="item in prompts" :key="item" class="prompt-btn" @click="message = item">
            {{ item }}
          </button>
        </details>
      </aside>

      <section class="panel chat-panel">
        <div class="chat-panel-header">
          <div>
            <span class="chat-kicker">CONTEXT-AWARE AGENT</span>
            <strong>{{ currentSessionTitle }}</strong>
          </div>
          <div class="chat-runtime">
            <span class="runtime-dot" :class="{ active: sending }"></span>
            <span>{{ sending ? 'AI 正在生成' : '会话已就绪' }}</span>
          </div>
        </div>
        <div ref="messages" class="messages">
          <ChatMessage
            v-for="item in messages"
            :key="item.id"
            :role="item.role"
            :content="item.content"
            @open-source="openSource"
          />
        </div>
        <form class="composer" @submit.prevent="send">
          <textarea
            v-model="message"
            class="textarea"
            :disabled="sending"
            placeholder="例如：根据知识库，JJKING 适合聊什么？"
            @keydown.enter.exact.prevent="send"
          />
          <div class="composer-side">
            <small>{{ message.length }} / 2000</small>
            <button class="btn" type="submit" :disabled="sending">
              {{ sending ? '回复中...' : '发送' }}
            </button>
          </div>
        </form>
      </section>
    </section>

    <div v-if="sourceDialog.visible" class="dialog-mask" @click.self="closeSource">
      <section class="dialog">
        <div class="dialog-header">
          <h3>参考来源原文</h3>
          <button class="dialog-close" type="button" @click="closeSource">关闭</button>
        </div>
        <div class="source-view">
          <div class="source-meta">
            <strong>{{ sourceDialog.document.title }}</strong>
            <span>{{ sourceDialog.document.source || '未知来源' }} · {{ formatDate(sourceDialog.document.createdAt) }}</span>
          </div>
          <pre>{{ sourceDialog.document.content }}</pre>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import ChatMessage from '@/components/ChatMessage.vue'
import { chatStream } from '@/api/chat'
import {
  deleteChatSession,
  listChatMessages,
  listChatSessions,
  searchChatMessages,
  updateChatSessionTitle
} from '@/api/chatHistory'
import { getKnowledgeDetail } from '@/api/knowledge'

export default {
  name: 'ChatView',
  components: {
    AppLayout,
    ChatMessage
  },
  data() {
    return {
      memoryId: 'session-' + Date.now(),
      message: '',
      sending: false,
      sessionLoading: false,
      sessionPage: 1,
      sessionPageSize: 10,
      sessionTotal: 0,
      editingSessionId: null,
      editingTitle: '',
      historyKeyword: '',
      searched: false,
      searchResults: [],
      sessions: [],
      sourceDialog: {
        visible: false,
        document: {}
      },
      messages: [
        {
          id: 1,
          role: 'assistant',
          content: '你好，我可以帮你记录人物记忆、查询人物信息，并结合知识库给建议。'
        }
      ],
      prompts: [
        '根据知识库，UPLOAD_PDF_TEST_20260728 是什么意思？',
        '查询人物姓名为 JJKING 的所有信息',
        '记住 JJKING 喜欢听音乐，类型是 HOBBY'
      ]
    }
  },
  computed: {
    currentSessionTitle() {
      const current = this.sessions.find(item => item.memoryId === this.memoryId)
      return current ? current.title : '新会话'
    },
    hasMoreSessions() {
      return this.sessions.length < this.sessionTotal
    }
  },
  created() {
    this.restoreLastSession()
  },
  methods: {
    newSession() {
      if (this.sending) return
      this.memoryId = 'session-' + Date.now()
      localStorage.removeItem('relationship-agent-active-session-id')
      localStorage.setItem('relationship-agent-active-memory-id', this.memoryId)
      this.messages = [
        {
          id: Date.now(),
          role: 'assistant',
          content: '新会话已创建。'
        }
      ]
    },
    async resetSessions() {
      this.sessionPage = 1
      await this.loadSessions(false)
    },
    async restoreLastSession() {
      this.sessionPage = 1
      await this.loadSessions(false)

      const savedSessionId = Number(localStorage.getItem('relationship-agent-active-session-id'))
      const savedMemoryId = localStorage.getItem('relationship-agent-active-memory-id')
      let target = this.findSavedSession(savedSessionId, savedMemoryId)

      while (!target && savedSessionId && this.hasMoreSessions) {
        this.sessionPage = this.sessionPage + 1
        await this.loadSessions(true)
        target = this.findSavedSession(savedSessionId, savedMemoryId)
      }

      if (!target && this.sessions.length > 0) {
        target = this.sessions[0]
      }

      if (target) {
        await this.loadSession(target)
      }
    },
    findSavedSession(savedSessionId, savedMemoryId) {
      return this.sessions.find(item =>
        (savedSessionId && item.id === savedSessionId) ||
        (savedMemoryId && item.memoryId === savedMemoryId)
      )
    },
    async loadSessions(append = false) {
      this.sessionLoading = true
      try {
        const result = await listChatSessions({
          page: this.sessionPage,
          pageSize: this.sessionPageSize
        })
        if (result.code === 1) {
          const pageData = result.data || {}
          const rows = pageData.rows || []
          this.sessions = append ? this.sessions.concat(rows) : rows
          this.sessionTotal = pageData.total || 0
        }
      } catch (error) {
        if (!append) {
          this.sessions = []
          this.sessionTotal = 0
        }
      } finally {
        this.sessionLoading = false
      }
    },
    async loadMoreSessions() {
      if (!this.hasMoreSessions || this.sessionLoading) return
      this.sessionPage = this.sessionPage + 1
      await this.loadSessions(true)
    },
    async loadSession(session) {
      if (this.sending) return

      this.memoryId = session.memoryId
      localStorage.setItem('relationship-agent-active-session-id', String(session.id))
      localStorage.setItem('relationship-agent-active-memory-id', session.memoryId)
      try {
        const result = await listChatMessages(session.id)
        if (result.code !== 1) return

        this.messages = (result.data || []).map(item => ({
          id: item.id,
          role: item.role,
          content: item.content
        }))

        if (this.messages.length === 0) {
          this.messages = [
            {
              id: Date.now(),
              role: 'assistant',
              content: '这个会话还没有消息。'
            }
          ]
        }

        this.scrollToBottom()
      } catch (error) {
        this.messages = [
          {
            id: Date.now(),
            role: 'assistant',
            content: '加载历史消息失败，请确认后端服务已经启动。'
          }
        ]
      }
    },
    async searchHistory() {
      const keyword = this.historyKeyword.trim()
      if (!keyword) {
        this.clearSearch()
        return
      }

      this.sessionLoading = true
      try {
        const result = await searchChatMessages(keyword)
        this.searched = true
        this.searchResults = result.code === 1 ? (result.data || []) : []
      } catch (error) {
        this.searched = true
        this.searchResults = []
      } finally {
        this.sessionLoading = false
      }
    },
    clearSearch() {
      this.historyKeyword = ''
      this.searched = false
      this.searchResults = []
    },
    async openSearchResult(result) {
      localStorage.setItem('relationship-agent-active-session-id', String(result.sessionId))
      localStorage.setItem('relationship-agent-active-memory-id', result.memoryId)
      await this.loadSession({
        id: result.sessionId,
        memoryId: result.memoryId
      })
    },
    startRename(session) {
      if (this.sending) return
      this.editingSessionId = session.id
      this.editingTitle = session.title || ''
    },
    cancelRename() {
      this.editingSessionId = null
      this.editingTitle = ''
    },
    async saveSessionTitle(session) {
      if (this.sending) return

      const cleanTitle = this.editingTitle.trim()
      if (!cleanTitle) return

      try {
        const result = await updateChatSessionTitle(session.id, {
          title: cleanTitle
        })

        if (result.code !== 1) return

        this.cancelRename()
        await this.resetSessions()
      } catch (error) {
        this.messages = [
          {
            id: Date.now(),
            role: 'assistant',
            content: '重命名会话失败，请确认后端服务已经启动。'
          }
        ]
      }
    },
    async removeSession(session) {
      if (this.sending) return
      if (!window.confirm('确认删除这个历史会话吗？')) return

      try {
        const result = await deleteChatSession(session.id)
        if (result.code !== 1) return

        if (session.memoryId === this.memoryId) {
          this.newSession()
        }

        await this.resetSessions()
      } catch (error) {
        this.messages = [
          {
            id: Date.now(),
            role: 'assistant',
            content: '删除历史会话失败，请确认后端服务已经启动。'
          }
        ]
      }
    },
    async openSource(documentId) {
      if (!documentId) return

      try {
        const result = await getKnowledgeDetail(documentId)
        if (result.code !== 1) return

        this.sourceDialog = {
          visible: true,
          document: result.data || {}
        }
      } catch (error) {
        this.messages.push({
          id: Date.now(),
          role: 'assistant',
          content: '打开参考来源失败，请确认后端服务已经启动。'
        })
      }
    },
    closeSource() {
      this.sourceDialog = {
        visible: false,
        document: {}
      }
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.messages
        if (el) {
          el.scrollTop = el.scrollHeight
        }
      })
    },
    async send() {
      if (!this.message.trim()) return
      const userText = this.message.trim()
      this.messages.push({
        id: Date.now(),
        role: 'user',
        content: userText
      })
      this.message = ''
      const assistantMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: ''
      }
      this.messages.push(assistantMessage)
      this.scrollToBottom()

      this.sending = true
      try {
        await chatStream(this.memoryId, userText, chunk => {
          assistantMessage.content += chunk
          this.scrollToBottom()
        })

        if (!assistantMessage.content.trim()) {
          assistantMessage.content = '没有收到回复，请稍后再试。'
        }
        await this.resetSessions()
        const currentSession = this.sessions.find(item => item.memoryId === this.memoryId)
        if (currentSession) {
          localStorage.setItem('relationship-agent-active-session-id', String(currentSession.id))
          localStorage.setItem('relationship-agent-active-memory-id', currentSession.memoryId)
        }
      } catch (error) {
        if (error && error.authExpired) {
          const messageIndex = this.messages.indexOf(assistantMessage)
          if (messageIndex >= 0) {
            this.messages.splice(messageIndex, 1)
          }
          return
        }
        assistantMessage.content = '聊天请求失败，请确认后端服务已经启动，并且你已经登录。'
      } finally {
        this.sending = false
      }
    },
    formatDate(dateText) {
      if (!dateText) return ''
      return dateText.replace('T', ' ').slice(0, 19)
    },
    previewText(content) {
      if (!content) return '暂无内容'
      const text = content.replace(/\s+/g, ' ').trim()
      if (text.length <= 36) return text
      return `${text.slice(0, 36)}...`
    }
  }
}
</script>

<style scoped>
.chat-layout {
  display: grid;
  grid-template-columns: 390px minmax(0, 1fr);
  gap: 16px;
  height: calc(100vh - 112px);
  min-height: 560px;
}

.session-id {
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 8px 10px;
  color: #b91c1c;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  font-size: 13px;
}

.session-panel {
  align-self: start;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.full-btn {
  width: 100%;
}

.quick-title {
  margin: 16px 0 10px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 700;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.refresh-btn {
  margin-top: 12px;
  border: 0;
  color: #b91c1c;
  background: transparent;
  font-size: 12px;
}

.history-search {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-bottom: 10px;
}

.history-search-input {
  min-width: 0;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 9px;
  outline: none;
}

.history-search-input:focus {
  border-color: #ef343d;
  box-shadow: 0 0 0 3px rgba(239, 52, 61, 0.1);
}

.history-search button,
.clear-search,
.load-more {
  border: 0;
  border-radius: 8px;
  padding: 8px 10px;
  color: #ffffff;
  background: #111827;
  font-size: 12px;
}

.search-results {
  display: grid;
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  background: #fff1f2;
  max-height: 260px;
  overflow: auto;
}

.search-empty {
  margin-bottom: 10px;
}

.search-title {
  color: #b91c1c;
  font-size: 12px;
  font-weight: 800;
}

.search-result {
  display: grid;
  gap: 4px;
  border: 1px solid transparent;
  padding: 8px;
  text-align: left;
  border-radius: 8px;
  background: #ffffff;
}

.search-result:hover {
  border-color: #fb7185;
}

.search-result span {
  color: #1f2937;
  font-size: 13px;
  font-weight: 700;
}

.search-result small,
.empty-session {
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.clear-search {
  width: fit-content;
  height: 28px;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

.session-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  margin-bottom: 8px;
  color: #374151;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  transition: border-color 0.18s ease, transform 0.18s ease, background 0.18s ease;
}

.session-item:hover {
  transform: translateY(-1px);
  border-color: #fecdd3;
}

.session-item.active {
  border-color: #ef343d;
  background: #fff1f2;
}

.session-open {
  min-width: 0;
  flex: 1;
  display: grid;
  gap: 4px;
  border: 0;
  text-align: left;
  color: inherit;
  background: transparent;
}

.session-open span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 700;
}

.session-title-input {
  min-width: 0;
  flex: 1;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  padding: 7px 8px;
  outline: none;
}

.session-rename,
.session-delete,
.session-save,
.session-cancel {
  flex: 0 0 auto;
  border: 0;
  background: transparent;
  font-size: 12px;
}

.session-rename {
  color: #b91c1c;
}

.session-delete {
  color: #111827;
}

.session-save {
  color: #047857;
}

.session-cancel {
  color: #6b7280;
}

.load-more {
  flex: 0 0 auto;
  margin-top: 10px;
  color: #b91c1c;
  background: #fff1f2;
}

.prompt-section {
  flex: 0 0 auto;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.prompt-section summary {
  cursor: pointer;
  color: #6b7280;
  font-size: 13px;
  font-weight: 700;
}

.prompt-btn {
  width: 100%;
  padding: 10px;
  margin-top: 10px;
  text-align: left;
  color: #374151;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.prompt-btn:hover {
  border-color: #fecdd3;
  background: #ffffff;
}

.chat-panel {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-height: 0;
}

.chat-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 54px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.chat-kicker {
  display: block;
  color: #b91c1c;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.chat-panel-header strong {
  display: block;
  max-width: 520px;
  margin-top: 5px;
  overflow: hidden;
  color: #111827;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-runtime {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 12px;
}

.runtime-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.12);
}

.runtime-dot.active {
  background: #ef343d;
  box-shadow: 0 0 0 4px rgba(239, 52, 61, 0.12);
  animation: runtime-pulse 1.1s ease-in-out infinite;
}

@keyframes runtime-pulse {
  50% {
    opacity: 0.42;
    transform: scale(0.78);
  }
}

.messages {
  min-height: 420px;
  max-height: calc(100vh - 260px);
  overflow: auto;
  padding-right: 8px;
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  padding-top: 14px;
  border-top: 1px solid #e5e7eb;
}

.composer .textarea {
  min-height: 52px;
  max-height: 120px;
}

.composer-side {
  display: grid;
  align-content: end;
  justify-items: end;
  gap: 6px;
}

.composer-side small {
  color: #9ca3af;
  font-size: 11px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
}

.dialog {
  width: min(860px, 100%);
  max-height: min(760px, calc(100vh - 48px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  padding: 20px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.24);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #e5e7eb;
}

.dialog-header h3 {
  margin: 0;
}

.dialog-close {
  border: 0;
  color: #6b7280;
  background: transparent;
}

.source-view {
  min-height: 0;
  overflow: auto;
  padding-top: 14px;
}

.source-meta {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
}

.source-meta span {
  color: #6b7280;
  font-size: 13px;
}

.source-view pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #374151;
  font-family: inherit;
}

@media (max-width: 900px) {
  .chat-layout {
    grid-template-columns: 1fr;
    height: auto;
  }

  .session-panel {
    max-height: 620px;
  }

  .chat-panel-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .composer {
    grid-template-columns: 1fr;
  }
}
</style>
