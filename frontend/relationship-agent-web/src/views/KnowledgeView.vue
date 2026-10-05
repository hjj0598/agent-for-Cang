<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">知识库管理</h2>
        <p class="page-desc">管理当前账号的私有知识，聊天时会按用户隔离检索。</p>
      </div>
      <button class="btn secondary" type="button" :disabled="loading" @click="reloadKnowledge">
        {{ loading ? '刷新中...' : '刷新列表' }}
      </button>
    </div>

    <section class="knowledge-hero">
      <div>
        <span class="hero-kicker">PRIVATE KNOWLEDGE WORKSPACE</span>
        <h3>把资料变成 AI 可以检索的上下文。</h3>
        <p>上传文档或手动录入内容，系统会同步切分并写入向量库。</p>
      </div>
      <div class="knowledge-flow">
        <span>文档</span>
        <i>→</i>
        <span>切片</span>
        <i>→</i>
        <span>向量检索</span>
      </div>
    </section>

    <div class="grid two">
      <section class="panel">
        <div class="panel-title-row">
          <h3>文件上传</h3>
          <span class="tag">PDF / TXT</span>
        </div>
        <UploadPanel :disabled="loading" @selected="onFileSelected" />
      </section>

      <section class="panel">
        <h3>手动新增知识</h3>
        <form class="knowledge-form" @submit.prevent="addKnowledge">
          <input v-model="form.title" class="input" placeholder="标题，例如：和 JJKING 的相处建议">
          <textarea
            v-model="form.content"
            class="textarea"
            placeholder="内容，例如：JJKING 喜欢音乐、旅行和有仪式感的安排。"
          />
          <button class="btn" type="submit" :disabled="loading">
            {{ loading ? '保存并入库中...' : '保存到知识库' }}
          </button>
        </form>
      </section>
    </div>

    <div v-if="message" :class="['message', messageType]">{{ message }}</div>

    <section class="panel document-panel">
      <div class="panel-title-row">
        <h3>知识库文档</h3>
        <span class="count-text">共 {{ total }} 条</span>
      </div>

      <form class="search-form" @submit.prevent="searchByTitle">
        <input v-model="keyword" class="input" placeholder="按标题搜索知识库">
        <button class="btn" type="submit" :disabled="loading">搜索</button>
        <button class="btn secondary" type="button" :disabled="loading || !keyword" @click="clearSearch">
          清空
        </button>
      </form>

      <div v-if="loading && documents.length === 0" class="empty-text">正在加载知识库...</div>
      <div v-else-if="documents.length === 0" class="empty-text">暂无知识文档</div>

      <article v-for="doc in documents" :key="doc.id" class="doc-item">
        <div class="doc-main">
          <strong>{{ doc.title }}</strong>
          <p>{{ previewContent(doc.content) }}</p>
          <small>{{ doc.source || '手动添加' }} · {{ formatDate(doc.createdAt) }}</small>
        </div>
        <div class="doc-actions">
          <span class="tag vector-tag"><span class="status-dot"></span>已入向量库</span>
          <button class="btn secondary" type="button" :disabled="loading" @click="openDetail(doc.id)">
            查看原文
          </button>
          <button class="btn secondary" type="button" :disabled="loading" @click="openChunks(doc.id)">
            查看切片
          </button>
          <button class="btn secondary" type="button" :disabled="loading" @click="openEdit(doc.id)">
            编辑
          </button>
          <button class="btn danger" type="button" :disabled="loading" @click="removeKnowledge(doc.id)">
            删除
          </button>
        </div>
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

    <div v-if="dialog.visible" class="dialog-mask" @click.self="closeDialog">
      <section class="dialog">
        <div class="dialog-header">
          <h3>{{ dialogTitle }}</h3>
          <button class="dialog-close" type="button" @click="closeDialog">关闭</button>
        </div>

        <div v-if="dialog.mode === 'view'" class="source-view">
          <div class="source-meta">
            <strong>{{ dialog.document.title }}</strong>
            <span>{{ dialog.document.source || '手动添加' }} · {{ formatDate(dialog.document.createdAt) }}</span>
          </div>
          <pre>{{ dialog.document.content }}</pre>
        </div>

        <div v-else-if="dialog.mode === 'chunks'" class="chunk-view">
          <div v-if="chunks.length === 0" class="empty-text">暂无切片数据</div>
          <article v-for="chunk in chunks" :key="chunk.id" class="chunk-item">
            <div class="chunk-meta">
              <strong>第 {{ chunk.chunkIndex + 1 }} 段</strong>
              <span>embeddingId：{{ chunk.embeddingId }}</span>
            </div>
            <pre>{{ chunk.content }}</pre>
          </article>
        </div>

        <form v-else class="edit-form" @submit.prevent="submitEdit">
          <label>
            标题
            <input v-model="editForm.title" class="input" placeholder="请输入知识标题">
          </label>
          <label>
            内容
            <textarea v-model="editForm.content" class="textarea edit-content" placeholder="请输入知识内容" />
          </label>
          <div class="dialog-actions">
            <button class="btn secondary" type="button" @click="closeDialog">取消</button>
            <button class="btn" type="submit" :disabled="loading">
              {{ loading ? '保存并重新入库中...' : '保存并重新向量化' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import UploadPanel from '@/components/UploadPanel.vue'
import {
  addTextKnowledge,
  deleteKnowledge,
  getKnowledgeChunks,
  getKnowledgeDetail,
  listKnowledge,
  updateKnowledge,
  uploadKnowledge
} from '@/api/knowledge'

const MAX_KNOWLEDGE_FILE_SIZE = 70 * 1024 * 1024

export default {
  name: 'KnowledgeView',
  components: {
    AppLayout,
    UploadPanel
  },
  data() {
    return {
      form: {
        title: '',
        content: ''
      },
      keyword: '',
      documents: [],
      chunks: [],
      page: 1,
      pageSize: 8,
      total: 0,
      loading: false,
      message: '',
      messageType: 'error',
      dialog: {
        visible: false,
        mode: 'view',
        document: {}
      },
      editForm: {
        id: null,
        title: '',
        content: ''
      }
    }
  },
  computed: {
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    dialogTitle() {
      if (this.dialog.mode === 'edit') return '编辑知识文档'
      if (this.dialog.mode === 'chunks') return '查看知识切片'
      return '查看原文'
    }
  },
  created() {
    this.loadKnowledge()
  },
  methods: {
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
    },
    async loadKnowledge() {
      this.loading = true
      try {
        const result = await listKnowledge({
          page: this.page,
          pageSize: this.pageSize,
          keyword: this.keyword.trim()
        })
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载知识库失败')
          return
        }

        const pageData = result.data || {}
        this.documents = pageData.rows || []
        this.total = pageData.total || 0
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    reloadKnowledge() {
      this.loadKnowledge()
    },
    searchByTitle() {
      this.page = 1
      this.loadKnowledge()
    },
    clearSearch() {
      this.keyword = ''
      this.page = 1
      this.loadKnowledge()
    },
    async onFileSelected(file) {
      if (!file) return

      const fileName = file.name || ''
      const lowerName = fileName.toLowerCase()
      if (!lowerName.endsWith('.txt') && !lowerName.endsWith('.pdf')) {
        this.showMessage('当前只支持上传 TXT 和 PDF 文件')
        return
      }

      if (file.size > MAX_KNOWLEDGE_FILE_SIZE) {
        this.showMessage('上传文件不能超过 70MB')
        return
      }

      this.loading = true
      this.showMessage(`正在上传 ${fileName}...`, 'success')
      try {
        const result = await uploadKnowledge(file)
        if (result.code !== 1) {
          this.showMessage(result.msg || '上传失败')
          return
        }

        this.showMessage(`${fileName} 已上传，并已同步写入向量库`, 'success')
        this.page = 1
        await this.loadKnowledge()
      } catch (error) {
        this.showMessage('上传失败，请确认后端服务已经启动，并且文件内容可以读取')
      } finally {
        this.loading = false
      }
    },
    async addKnowledge() {
      if (!this.form.title.trim()) {
        this.showMessage('请先填写标题')
        return
      }
      if (!this.form.content.trim()) {
        this.showMessage('请先填写内容')
        return
      }

      this.loading = true
      try {
        const result = await addTextKnowledge(this.form)
        if (result.code !== 1) {
          this.showMessage(result.msg || '添加知识失败')
          return
        }

        this.showMessage('知识已保存，并已同步写入向量库', 'success')
        this.page = 1
        this.form = {
          title: '',
          content: ''
        }
        await this.loadKnowledge()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async removeKnowledge(id) {
      if (!window.confirm('确认删除这条知识吗？')) return

      this.loading = true
      try {
        const result = await deleteKnowledge(id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '删除知识失败')
          return
        }

        this.showMessage('知识已删除', 'success')
        if (this.documents.length === 1 && this.page > 1) {
          this.page = this.page - 1
        }
        await this.loadKnowledge()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async openDetail(id) {
      this.loading = true
      try {
        const result = await getKnowledgeDetail(id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载原文失败')
          return
        }

        this.dialog = {
          visible: true,
          mode: 'view',
          document: result.data || {}
        }
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async openChunks(id) {
      this.loading = true
      try {
        const result = await getKnowledgeChunks(id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载切片失败')
          return
        }

        this.chunks = result.data || []
        this.dialog = {
          visible: true,
          mode: 'chunks',
          document: {}
        }
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async openEdit(id) {
      this.loading = true
      try {
        const result = await getKnowledgeDetail(id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载文档失败')
          return
        }

        const document = result.data || {}
        this.editForm = {
          id: document.id,
          title: document.title || '',
          content: document.content || ''
        }
        this.dialog = {
          visible: true,
          mode: 'edit',
          document
        }
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    closeDialog() {
      if (this.loading) return
      this.resetDialog()
    },
    resetDialog() {
      this.dialog = {
        visible: false,
        mode: 'view',
        document: {}
      }
      this.editForm = {
        id: null,
        title: '',
        content: ''
      }
      this.chunks = []
    },
    async submitEdit() {
      if (!this.editForm.title.trim()) {
        this.showMessage('请先填写标题')
        return
      }

      if (!this.editForm.content.trim()) {
        this.showMessage('请先填写内容')
        return
      }

      this.loading = true
      try {
        const result = await updateKnowledge(this.editForm.id, {
          title: this.editForm.title,
          content: this.editForm.content
        })

        if (result.code !== 1) {
          this.showMessage(result.msg || '编辑知识失败')
          return
        }

        this.showMessage('知识已更新，并已重新写入向量库', 'success')
        this.resetDialog()
        await this.loadKnowledge()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    previewContent(content) {
      if (!content) return '暂无内容'
      const cleanContent = content.replace(/\s+/g, ' ').trim()
      if (cleanContent.length <= 160) return cleanContent
      return `${cleanContent.slice(0, 160)}...`
    },
    formatDate(dateText) {
      if (!dateText) return '暂无时间'
      return dateText.replace('T', ' ').slice(0, 19)
    },
    async changePage(page) {
      if (page < 1 || page > this.totalPages) return
      this.page = page
      await this.loadKnowledge()
    }
  }
}
</script>

<style scoped>
h3 {
  margin: 0 0 16px;
}

.knowledge-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
  padding: 22px 24px;
  border: 1px solid rgba(239, 52, 61, 0.18);
  border-radius: 8px;
  color: #ffffff;
  background:
    linear-gradient(135deg, rgba(17, 21, 29, 0.98), rgba(37, 43, 53, 0.98)),
    #11151d;
  box-shadow: 0 20px 52px rgba(17, 24, 39, 0.14);
}

.knowledge-hero h3 {
  margin: 9px 0 0;
  font-size: 24px;
  line-height: 1.3;
}

.knowledge-hero p {
  margin: 8px 0 0;
  color: #cbd5e1;
  line-height: 1.6;
}

.hero-kicker {
  color: #fda4af;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.knowledge-flow {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 auto;
  color: #ffffff;
  font-size: 13px;
  font-weight: 800;
}

.knowledge-flow span {
  padding: 9px 11px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
}

.knowledge-flow i {
  color: #fda4af;
  font-style: normal;
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

.knowledge-form {
  display: grid;
  gap: 12px;
}

.search-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
  margin-bottom: 16px;
}

.message {
  margin-top: 16px;
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

.document-panel {
  margin-top: 16px;
}

.count-text {
  color: #6b7280;
  font-size: 14px;
}

.doc-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 15px 0;
  border-bottom: 1px solid #e5e7eb;
}

.doc-item:last-child {
  border-bottom: 0;
}

.doc-main {
  min-width: 0;
}

.doc-main strong {
  font-size: 16px;
}

.doc-item p {
  margin: 8px 0 0;
  color: #6b7280;
  line-height: 1.6;
  word-break: break-word;
}

.doc-item small,
.empty-text {
  display: block;
  margin-top: 8px;
  color: #6b7280;
}

.doc-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 250px;
  flex-wrap: wrap;
}

.vector-tag {
  gap: 6px;
  color: #047857;
  background: #d1fae5;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.12);
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
  width: min(920px, 100%);
  max-height: min(780px, calc(100vh - 48px));
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

.source-view,
.chunk-view {
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

.source-view pre,
.chunk-item pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  color: #374151;
  font-family: inherit;
}

.chunk-item {
  display: grid;
  gap: 10px;
  padding: 14px;
  margin-bottom: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.chunk-meta {
  display: grid;
  gap: 4px;
}

.chunk-meta span {
  color: #6b7280;
  font-size: 12px;
  word-break: break-all;
}

.edit-form {
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  gap: 14px;
  padding-top: 14px;
}

.edit-form label {
  display: grid;
  gap: 7px;
  color: #374151;
  font-weight: 700;
}

.edit-content {
  min-height: 320px;
  max-height: 460px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 800px) {
  .knowledge-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .search-form,
  .doc-item {
    display: grid;
    grid-template-columns: 1fr;
  }

  .doc-actions {
    justify-content: flex-start;
    min-width: 0;
  }

  .dialog-mask {
    padding: 12px;
  }

  .dialog-actions {
    justify-content: stretch;
  }
}
</style>
