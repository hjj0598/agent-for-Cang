<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ person.name || '人物详情' }}</h2>
        <p class="page-desc">{{ person.description || '暂无备注' }}</p>
      </div>
      <router-link class="btn secondary" to="/persons">返回列表</router-link>
    </div>

    <section class="person-detail-banner">
      <div class="detail-avatar">{{ (person.name || '人').slice(0, 1) }}</div>
      <div class="detail-banner-copy">
        <span class="detail-kicker">RELATIONSHIP PROFILE</span>
        <strong>{{ person.name || '人物详情' }}</strong>
        <span>{{ person.relation || '关系暂未设置' }} · {{ memories.length }} 条长期记忆</span>
      </div>
      <div class="detail-banner-state">
        <span class="state-dot"></span>
        <span>档案已连接</span>
      </div>
    </section>

    <div class="grid two">
      <section class="panel">
        <h3>人物信息</h3>
        <form class="person-form" @submit.prevent="savePerson">
          <label>
            姓名
            <input v-model="person.name" class="input" placeholder="请输入姓名">
          </label>
          <label>
            关系
            <input v-model="person.relation" class="input" placeholder="请输入关系">
          </label>
          <label>
            备注
            <textarea v-model="person.description" class="textarea" placeholder="请输入人物备注" />
          </label>
          <div class="actions">
            <button class="btn" type="submit" :disabled="loading">
              {{ loading ? '保存中...' : '保存修改' }}
            </button>
            <button class="btn danger" type="button" :disabled="loading" @click="deleteCurrentPerson">
              删除人物
            </button>
          </div>
        </form>
      </section>

      <section class="panel">
        <h3>新增记忆</h3>
        <MemoryForm
          v-model="form"
          :submitting="memoryLoading"
          @submit="saveMemory"
          @reset="resetForm"
        />
      </section>

      <section class="panel memory-panel">
        <div class="panel-title-row">
          <h3>记忆列表</h3>
          <span class="count-text">共 {{ filteredMemories.length }} 条</span>
        </div>
        <div class="filter-row">
          <select v-model="type" class="select">
            <option value="">全部类型</option>
            <option value="HOBBY">HOBBY 爱好</option>
            <option value="QUOTE">QUOTE 说过的话</option>
            <option value="TRAIT">TRAIT 性格特点</option>
            <option value="DISLIKE">DISLIKE 不喜欢</option>
            <option value="DATE">DATE 重要日期</option>
            <option value="NOTE">NOTE 备注</option>
          </select>
        </div>

        <div v-if="message" :class="['message', messageType]">{{ message }}</div>
        <div v-if="memoryLoading && memories.length === 0" class="empty-text">正在加载记忆...</div>
        <div v-else-if="filteredMemories.length === 0" class="empty-text">暂无记忆数据</div>

        <article v-for="memory in filteredMemories" :key="memory.id" class="memory-item">
          <span class="tag">{{ memoryTypeText(memory.memoryType) }}</span>
          <div class="memory-content">
            <p>{{ memory.content }}</p>
            <div class="memory-source">
              <span class="source-badge">{{ memory.sourceType || sourceTypeText(memory.source) }}</span>
              <span v-if="memory.sourceReference">{{ memory.sourceReference }}</span>
              <span>记录于 {{ formatDate(memory.createdAt) }}</span>
            </div>
          </div>
          <button class="btn danger" :disabled="memoryLoading" @click="removeMemory(memory.id)">删除</button>
        </article>
      </section>
    </div>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import MemoryForm from '@/components/MemoryForm.vue'
import { deletePerson, getPerson, updatePerson } from '@/api/person'
import { addMemory, deleteMemory, listMemories } from '@/api/memory'

export default {
  name: 'PersonDetailView',
  components: {
    AppLayout,
    MemoryForm
  },
  data() {
    return {
      person: {
        id: this.$route.params.id,
        name: '',
        relation: '',
        description: ''
      },
      loading: false,
      memoryLoading: false,
      message: '',
      messageType: 'error',
      type: '',
      form: {
        personName: '',
        memoryType: 'HOBBY',
        content: '',
        source: '手动记录'
      },
      memories: [],
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
    filteredMemories() {
      if (!this.type) return this.memories
      return this.memories.filter(item => item.memoryType === this.type)
    }
  },
  async created() {
    await this.loadPerson()
    await this.loadMemories()
  },
  methods: {
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
    },
    async loadPerson() {
      this.loading = true
      try {
        const result = await getPerson(this.$route.params.id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载人物失败')
          return
        }
        this.person = result.data
        this.resetForm()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async loadMemories() {
      this.memoryLoading = true
      try {
        const result = await listMemories(this.$route.params.id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载记忆失败')
          return
        }
        this.memories = result.data || []
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.memoryLoading = false
      }
    },
    async savePerson() {
      if (!this.person.name.trim()) {
        this.showMessage('请先填写姓名')
        return
      }

      this.loading = true
      try {
        const result = await updatePerson(this.person.id, {
          name: this.person.name,
          relation: this.person.relation,
          description: this.person.description
        })
        if (result.code !== 1) {
          this.showMessage(result.msg || '保存失败')
          return
        }
        this.showMessage('人物信息已保存', 'success')
        await this.loadPerson()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async deleteCurrentPerson() {
      if (!window.confirm('确认删除这个人物吗？')) return

      this.loading = true
      try {
        const result = await deletePerson(this.person.id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '删除失败')
          return
        }
        this.$router.push('/persons')
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async saveMemory() {
      if (!this.form.content.trim()) {
        this.showMessage('请先填写记忆内容')
        return
      }

      this.memoryLoading = true
      try {
        const result = await addMemory(this.person.id, {
          memoryType: this.form.memoryType,
          content: this.form.content,
          source: this.form.source || '手动记录'
        })
        if (result.code !== 1) {
          this.showMessage(result.msg || '保存记忆失败')
          return
        }

        this.showMessage('记忆已保存', 'success')
        this.resetForm()
        await this.loadMemories()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.memoryLoading = false
      }
    },
    resetForm() {
      this.form = {
        personName: this.person.name,
        memoryType: 'HOBBY',
        content: '',
        source: '手动记录'
      }
    },
    async removeMemory(id) {
      if (!window.confirm('确认删除这条记忆吗？')) return

      this.memoryLoading = true
      try {
        const result = await deleteMemory(this.person.id, id)
        if (result.code !== 1) {
          this.showMessage(result.msg || '删除记忆失败')
          return
        }

        this.showMessage('记忆已删除', 'success')
        await this.loadMemories()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.memoryLoading = false
      }
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
h3 {
  margin: 0 0 16px;
}

.person-detail-banner {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 8px;
  color: #ffffff;
  background:
    linear-gradient(135deg, rgba(17, 21, 29, 0.98), rgba(58, 27, 32, 0.98)),
    #11151d;
  box-shadow: 0 18px 44px rgba(17, 24, 39, 0.14);
}

.detail-avatar {
  display: grid;
  flex: 0 0 48px;
  width: 48px;
  height: 48px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  background: rgba(239, 52, 61, 0.86);
  font-size: 20px;
  font-weight: 800;
}

.detail-banner-copy {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.detail-kicker {
  color: #fda4af;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.detail-banner-copy strong {
  overflow: hidden;
  font-size: 19px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-banner-copy > span:last-child {
  color: #cbd5e1;
  font-size: 12px;
}

.detail-banner-state {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-left: auto;
  color: #bbf7d0;
  font-size: 12px;
}

.state-dot {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.12);
}

.filter-row {
  margin-bottom: 14px;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.count-text {
  color: #6b7280;
  font-size: 14px;
}

.memory-panel {
  grid-column: 1 / -1;
}

.person-form {
  display: grid;
  gap: 14px;
}

.person-form label {
  display: grid;
  gap: 7px;
  color: #374151;
  font-weight: 700;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
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

.memory-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #e5e7eb;
}

.memory-content {
  display: grid;
  gap: 4px;
}

.memory-content p {
  margin: 0;
  line-height: 1.6;
}

.memory-source,
.empty-text {
  color: #6b7280;
}

.memory-source {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  font-size: 12px;
}

.source-badge {
  padding: 3px 7px;
  border-radius: 999px;
  color: #991b1b;
  background: #fff1f2;
  font-weight: 700;
}

@media (max-width: 700px) {
  .person-detail-banner {
    align-items: flex-start;
  }

  .detail-banner-state {
    display: none;
  }

  .memory-item {
    grid-template-columns: 1fr;
  }
}
</style>
