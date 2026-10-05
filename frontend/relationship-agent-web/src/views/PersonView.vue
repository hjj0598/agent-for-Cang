<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">人物管理</h2>
        <p class="page-desc">管理朋友、家人、同事等人物，并支持按人物姓名搜索记忆。</p>
      </div>
      <button class="btn" @click="showForm = !showForm">
        {{ showForm ? '收起表单' : '新增人物' }}
      </button>
    </div>

    <section class="person-summary-strip">
      <div class="summary-mark">人</div>
      <div class="summary-copy">
        <strong>你的关系档案</strong>
        <span>人物资料、长期记忆和 AI 维护建议都从这里开始。</span>
      </div>
      <div class="summary-stat">
        <strong>{{ total }}</strong>
        <span>已建立人物</span>
      </div>
      <div class="summary-stat">
        <strong>{{ memorySearched ? memoryResults.length : '—' }}</strong>
        <span>{{ memorySearched ? '搜索结果' : '最近搜索' }}</span>
      </div>
    </section>

    <section v-if="showForm" class="panel form-panel">
      <div class="form-panel-header">
        <div>
          <span class="section-kicker">NEW RELATIONSHIP PROFILE</span>
          <h3>新增人物</h3>
          <p>先建立基础档案，后续可以继续补充记忆、资料和 AI 维护建议。</p>
        </div>
        <span class="draft-state"><span class="draft-dot"></span>档案草稿</span>
      </div>

      <div class="person-form-layout">
        <form class="person-form" @submit.prevent="submitPerson">
          <div class="form-field-grid">
            <label class="form-field">
              <span>姓名</span>
              <small>作为人物搜索和记忆关联的主名称</small>
              <input v-model="form.name" class="input" autocomplete="off" placeholder="例如：JJKING">
            </label>
            <label class="form-field">
              <span>关系</span>
              <small>帮助 AI 理解你们之间的关系</small>
              <input v-model="form.relation" class="input" placeholder="例如：重要的人、朋友、同学">
            </label>
          </div>
          <label class="form-field">
            <span>备注</span>
            <small>可以记录爱好、性格、相处提醒等信息</small>
            <textarea
              v-model="form.description"
              class="textarea"
              maxlength="500"
              placeholder="例如：喜欢音乐、重视仪式感，适合在周末主动联系。"
            />
            <em>{{ form.description.length }} / 500</em>
          </label>
          <div class="form-actions">
            <span>保存后即可在人物详情中继续添加长期记忆。</span>
            <button class="btn" type="submit" :disabled="loading">
              <span class="button-mark">{{ loading ? '…' : '+' }}</span>
              {{ loading ? '保存中...' : '保存人物' }}
            </button>
          </div>
        </form>

        <aside class="person-preview">
          <span class="preview-kicker">PROFILE PREVIEW</span>
          <div class="preview-avatar">{{ formInitial }}</div>
          <strong>{{ form.name || 'JJKING' }}</strong>
          <span class="preview-relation">{{ form.relation || '关系暂未设置' }}</span>
          <p>{{ form.description || '填写备注后，这里会实时展示人物档案摘要。' }}</p>
          <div class="preview-footer">
            <span class="preview-status"></span>
            <span>可被 AI 记忆和检索</span>
          </div>
        </aside>
      </div>
    </section>

    <div v-if="message" :class="['message', messageType]">{{ message }}</div>

    <section class="panel memory-search-panel">
      <div class="panel-title-row">
        <h3>按人物搜索记忆</h3>
        <span class="tag">person_memory</span>
      </div>
      <form class="search-form" @submit.prevent="searchMemories">
        <input v-model="memoryKeyword" class="input" placeholder="输入人物姓名，例如：JJKING">
        <button class="btn" type="submit" :disabled="memorySearching">搜索记忆</button>
        <button class="btn secondary" type="button" :disabled="memorySearching || !memoryKeyword" @click="clearMemorySearch">
          清空
        </button>
      </form>

      <div v-if="memorySearched && memoryResults.length === 0" class="empty-text">
        没有搜索到这个人的记忆。可以先新增人物，再进入详情添加记忆。
      </div>
      <div v-if="memoryResults.length > 0" class="memory-result-list">
        <article v-for="memory in memoryResults" :key="memory.id" class="memory-result">
          <span class="tag">{{ memoryTypeText(memory.memoryType) }}</span>
          <p>{{ memory.content }}</p>
          <small>
            来源：{{ memory.sourceType || sourceTypeText(memory.source) }}
            <span v-if="memory.sourceReference"> · {{ memory.sourceReference }}</span>
            · {{ formatDate(memory.createdAt) }}
          </small>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="toolbar">
        <input v-model="keyword" class="input" placeholder="在当前页搜索人物姓名、关系或备注">
        <span class="count-text">共 {{ total }} 条</span>
      </div>

      <div v-if="loading && persons.length === 0" class="empty-text">正在加载人物列表...</div>
      <div v-else-if="filteredPersons.length === 0" class="empty-text">暂无人物数据</div>

      <div class="person-grid">
        <article v-for="person in filteredPersons" :key="person.id" class="person-card">
          <div class="person-card-head">
            <div class="person-avatar">{{ (person.name || '人').slice(0, 1) }}</div>
            <div>
            <h3>{{ person.name }}</h3>
            <span class="tag">{{ person.relation || '未填写关系' }}</span>
            </div>
          </div>
          <p>{{ person.description || '暂无备注' }}</p>
          <div class="card-actions">
            <router-link class="btn secondary" :to="'/persons/' + person.id">查看详情</router-link>
            <button class="btn danger" @click="removePerson(person.id)">删除</button>
          </div>
        </article>
      </div>

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
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import { addPerson, deletePerson, listPersons } from '@/api/person'
import { searchMemoriesByPersonName } from '@/api/memory'

export default {
  name: 'PersonView',
  components: {
    AppLayout
  },
  data() {
    return {
      keyword: '',
      memoryKeyword: '',
      memorySearching: false,
      memorySearched: false,
      memoryResults: [],
      showForm: false,
      loading: false,
      message: '',
      messageType: 'error',
      page: 1,
      pageSize: 8,
      total: 0,
      form: {
        name: '',
        relation: '',
        description: ''
      },
      persons: [],
      memoryTypeMap: {
        HOBBY: '爱好',
        QUOTE: '说过的话',
        TRAIT: '性格特点',
        DISLIKE: '不喜欢',
        DATE: '重要日期',
        NOTE: '备注'
      }
    }
  },
  computed: {
    formInitial() {
      return (this.form.name || 'J').slice(0, 1).toUpperCase()
    },
    totalPages() {
      return Math.max(1, Math.ceil(this.total / this.pageSize))
    },
    filteredPersons() {
      const key = this.keyword.trim()
      if (!key) return this.persons
      return this.persons.filter(item =>
        (item.name || '').includes(key) ||
        (item.relation || '').includes(key) ||
        (item.description || '').includes(key)
      )
    }
  },
  created() {
    this.loadPersons()
  },
  methods: {
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
    },
    resetForm() {
      this.form = {
        name: '',
        relation: '',
        description: ''
      }
    },
    async loadPersons() {
      this.loading = true
      try {
        const result = await listPersons({
          page: this.page,
          pageSize: this.pageSize
        })
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载人物失败')
          return
        }
        const pageData = result.data || {}
        this.persons = pageData.rows || []
        this.total = pageData.total || 0
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async submitPerson() {
      if (!this.form.name.trim()) {
        this.showMessage('请先填写姓名')
        return
      }

      this.loading = true
      try {
        const result = await addPerson(this.form)
        if (result.code !== 1) {
          this.showMessage(result.msg || '新增人物失败')
          return
        }

        this.showMessage('新增人物成功', 'success')
        this.resetForm()
        this.showForm = false
        this.page = 1
        await this.loadPersons()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async removePerson(id) {
      if (!window.confirm('确认删除这个人物吗？')) return

      this.loading = true
      try {
        const result = await deletePerson(id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '删除人物失败')
          return
        }

        this.showMessage('删除人物成功', 'success')
        if (this.persons.length === 1 && this.page > 1) {
          this.page = this.page - 1
        }
        await this.loadPersons()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async searchMemories() {
      const keyword = this.memoryKeyword.trim()
      if (!keyword) {
        this.clearMemorySearch()
        return
      }

      this.memorySearching = true
      this.memorySearched = true
      try {
        const result = await searchMemoriesByPersonName(keyword)
        if (result.code !== 1) {
          this.memoryResults = []
          this.showMessage(result.msg || '搜索记忆失败')
          return
        }

        this.memoryResults = result.data || []
      } catch (error) {
        this.memoryResults = []
        this.showMessage('搜索失败，请确认后端服务已经启动')
      } finally {
        this.memorySearching = false
      }
    },
    clearMemorySearch() {
      this.memoryKeyword = ''
      this.memorySearched = false
      this.memoryResults = []
    },
    async changePage(page) {
      if (page < 1 || page > this.totalPages) return
      this.page = page
      await this.loadPersons()
    },
    memoryTypeText(type) {
      return this.memoryTypeMap[type] || type || '备注'
    },
    sourceTypeText(source) {
      const value = String(source || '手动记录')
      if (value.startsWith('图片提取')) return '图片提取'
      if (value.includes('AI聊天') || value.includes('AI chat')) return 'AI 对话'
      if (value.startsWith('知识库')) return '知识库'
      return '手动记录'
    },
    formatDate(dateText) {
      if (!dateText) return '暂无时间'
      return dateText.replace('T', ' ').slice(0, 19)
    }
  }
}
</script>

<style scoped>
.form-panel,
.memory-search-panel {
  margin-bottom: 16px;
}

.form-panel {
  overflow: hidden;
  border-color: rgba(239, 52, 61, 0.18);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(255, 247, 248, 0.96)),
    #ffffff;
}

.form-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-kicker,
.preview-kicker {
  color: #b91c1c;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.form-panel-header h3 {
  margin: 7px 0 0;
  font-size: 23px;
}

.form-panel-header p {
  margin: 7px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.draft-state {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex: 0 0 auto;
  min-height: 30px;
  padding: 0 10px;
  border: 1px solid #fecdd3;
  border-radius: 999px;
  color: #9f1239;
  background: #fff1f2;
  font-size: 12px;
  font-weight: 700;
}

.draft-dot,
.preview-status {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #10b981;
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.12);
}

.person-form-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 22px;
  align-items: stretch;
}

.person-summary-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid rgba(239, 52, 61, 0.16);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(255, 241, 242, 0.92), rgba(255, 255, 255, 0.96)),
    #ffffff;
}

.summary-mark,
.person-avatar {
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, var(--brand), var(--brand-dark));
  font-weight: 800;
}

.summary-mark {
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
}

.summary-copy {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.summary-copy strong {
  color: #111827;
  font-size: 14px;
}

.summary-copy span {
  overflow: hidden;
  color: #6b7280;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-stat {
  display: grid;
  min-width: 82px;
  margin-left: auto;
  padding-left: 14px;
  border-left: 1px solid #fecdd3;
}

.summary-stat + .summary-stat {
  margin-left: 0;
}

.summary-stat strong {
  color: #b91c1c;
  font-size: 20px;
}

.summary-stat span {
  color: #9ca3af;
  font-size: 11px;
}

.form-panel h3,
.panel-title-row h3 {
  margin: 0;
}

.person-form {
  display: grid;
  gap: 14px;
}

.form-field {
  display: grid;
  gap: 7px;
  color: #374151;
  font-weight: 700;
}

.form-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-field small {
  color: #9ca3af;
  font-size: 11px;
  font-weight: 500;
}

.form-field em {
  color: #9ca3af;
  font-size: 11px;
  font-style: normal;
  font-weight: 500;
  text-align: right;
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-top: 4px;
}

.form-actions > span {
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.5;
}

.button-mark {
  display: inline-grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 5px;
  font-size: 15px;
  line-height: 1;
}

.person-preview {
  display: flex;
  flex-direction: column;
  min-height: 280px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  color: #ffffff;
  background:
    radial-gradient(circle at 100% 0, rgba(239, 52, 61, 0.34), transparent 170px),
    linear-gradient(145deg, #11151d, #252b35);
  box-shadow: 0 18px 40px rgba(17, 24, 39, 0.14);
}

.person-preview .preview-kicker {
  color: #fda4af;
}

.preview-avatar {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  margin-top: 28px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, #ef343d, #c91f2b);
  box-shadow: 0 14px 28px rgba(239, 52, 61, 0.25);
  font-size: 24px;
  font-weight: 800;
}

.person-preview > strong {
  margin-top: 16px;
  overflow: hidden;
  font-size: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-relation {
  margin-top: 5px;
  color: #fda4af;
  font-size: 13px;
  font-weight: 700;
}

.person-preview p {
  display: -webkit-box;
  margin-top: 14px;
  overflow: hidden;
  color: #d1d5db;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.preview-footer {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: auto;
  padding-top: 16px;
  color: #bbf7d0;
  font-size: 12px;
}

.panel-title-row,
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.search-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 10px;
}

.count-text {
  flex: 0 0 auto;
  color: #6b7280;
  font-size: 14px;
}

.person-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.person-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  transition: transform 0.18s ease, background 0.18s ease;
}

.person-card:hover {
  transform: translateY(-2px);
  background: #ffffff;
}

h3 {
  margin: 0 0 8px;
}

.person-card-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.person-card-head h3 {
  margin-bottom: 6px;
}

.person-avatar {
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  background: linear-gradient(135deg, #111827, #374151);
}

p {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.memory-result-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.memory-result {
  display: grid;
  gap: 8px;
  padding: 12px;
}

.memory-result small,
.empty-text {
  color: #6b7280;
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

@media (max-width: 800px) {
  .person-form-layout {
    grid-template-columns: 1fr;
  }

  .person-preview {
    min-height: 238px;
  }

  .form-field-grid {
    grid-template-columns: 1fr;
  }

  .form-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .form-actions .btn {
    width: 100%;
  }

  .person-summary-strip {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .summary-copy {
    flex: 1 1 calc(100% - 54px);
  }

  .summary-stat {
    margin-left: 50px;
  }

  .toolbar,
  .search-form {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>
