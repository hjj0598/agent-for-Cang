<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">图片分析</h2>
        <p class="page-desc">上传图片后，AI 会识别画面内容并提取可见文字。</p>
      </div>
      <span class="tag">Qwen-VL</span>
    </div>

    <section class="analysis-steps" aria-label="图片分析流程">
      <div class="analysis-step active">
        <span>01</span>
        <div>
          <strong>上传图片</strong>
          <small>选择或拖入文件</small>
        </div>
      </div>
      <div class="analysis-line"></div>
      <div class="analysis-step">
        <span>02</span>
        <div>
          <strong>AI 识别</strong>
          <small>理解画面与文字</small>
        </div>
      </div>
      <div class="analysis-line"></div>
      <div class="analysis-step">
        <span>03</span>
        <div>
          <strong>确认记忆</strong>
          <small>审核后写入档案</small>
        </div>
      </div>
    </section>

    <div class="image-workspace">
      <section class="panel upload-panel">
        <div class="panel-title-row">
          <h3>图片输入</h3>
          <span class="count-text">JPG / PNG / WEBP / BMP / GIF</span>
        </div>

        <label
          class="drop-zone"
          :class="{ active: previewUrl, dragging: dragging }"
          @dragenter.prevent="onDragEnter"
          @dragover.prevent="onDragOver"
          @dragleave.prevent="onDragLeave"
          @drop.prevent="onDrop"
        >
          <input ref="fileInput" type="file" accept="image/*" @change="onFileChange">
          <template v-if="previewUrl">
            <img class="preview-image" :src="previewUrl" alt="待分析图片">
            <span class="preview-mask">点击或拖入新图片替换</span>
          </template>
          <template v-else>
            <strong>{{ dragging ? '松开即可选择图片' : '选择或拖入图片' }}</strong>
            <span>支持 JPG / PNG / WEBP / BMP / GIF，单张不超过 10MB</span>
          </template>
        </label>

        <div v-if="file" class="file-meta">
          <div>
            <strong>{{ file.name }}</strong>
            <span>{{ file.type || '未知类型' }}</span>
          </div>
          <span>{{ formatSize(file.size) }}</span>
        </div>

        <div class="template-panel">
          <div class="panel-title-row compact">
            <h3>快捷分析</h3>
            <span class="count-text">选择后可继续编辑提示词</span>
          </div>
          <div class="template-grid">
            <button
              v-for="template in promptTemplates"
              :key="template.title"
              type="button"
              class="template-btn"
              :disabled="loading"
              @click="applyTemplate(template)"
            >
              <strong>{{ template.title }}</strong>
              <span>{{ template.desc }}</span>
            </button>
          </div>
        </div>

        <textarea
          v-model="prompt"
          class="textarea prompt-input"
          placeholder="例如：请分析这张图里有什么，并提取图片中的文字。"
        />

        <div class="action-row">
          <button class="btn secondary" type="button" :disabled="loading" @click="chooseFile">
            {{ file ? '重新选择' : '选择图片' }}
          </button>
          <button class="btn secondary" type="button" :disabled="loading || !file" @click="resetFileInput">
            清空图片
          </button>
          <button class="btn" type="button" :disabled="loading || !file" @click="submitAnalyze">
            {{ loading ? '分析中...' : '开始分析' }}
          </button>
        </div>

        <div v-if="message" :class="['message', messageType]">{{ message }}</div>

        <section class="memory-extraction">
          <div class="panel-title-row compact">
            <div>
              <h3>提取为人物记忆</h3>
              <p class="section-hint">AI 只生成候选内容，确认后才会写入人物记忆。</p>
            </div>
            <span class="tag">可确认记忆</span>
          </div>

          <div v-if="persons.length === 0" class="extraction-empty">
            <strong>还没有可关联的人物</strong>
            <span>先创建人物，再把聊天截图或照片提取成记忆。</span>
            <router-link class="btn secondary small" to="/persons">去创建人物</router-link>
          </div>

          <template v-else>
            <div class="extraction-toolbar">
              <label>
                关联人物
                <select v-model="selectedPersonId" class="select" :disabled="memoryExtractionLoading">
                  <option value="">请选择人物</option>
                  <option v-for="person in persons" :key="person.id" :value="String(person.id)">
                    {{ person.name }}{{ person.relation ? ` · ${person.relation}` : '' }}
                  </option>
                </select>
              </label>
              <button
                class="btn secondary"
                type="button"
                :disabled="memoryExtractionLoading || loading || !file || !selectedPersonId"
                @click="extractMemories"
              >
                {{ memoryExtractionLoading ? '提取中...' : '提取候选记忆' }}
              </button>
            </div>

            <div v-if="memoryExtractionLoading" class="extraction-empty">
              <strong>视觉模型正在整理记忆</strong>
              <span>它会尽量只提取图片中明确出现的信息。</span>
            </div>

            <div
              v-else-if="memoryExtractionDone && memoryCandidates.length === 0 && savedMemoryCount > 0"
              class="extraction-empty extraction-success"
            >
              <strong>候选记忆已全部保存</strong>
              <span>本次已确认并保存 {{ savedMemoryCount }} 条人物记忆，可以在人物详情中查看。</span>
            </div>

            <div v-else-if="memoryExtractionDone && memoryCandidates.length === 0" class="extraction-empty">
              <strong>没有得到可直接保存的候选记忆</strong>
              <pre v-if="memoryExtraction.rawAnswer">{{ memoryExtraction.rawAnswer }}</pre>
              <span v-else>可以尝试上传更清晰的聊天截图，或先使用右侧普通分析。</span>
            </div>

            <div v-else-if="memoryCandidates.length > 0" class="candidate-list">
              <article v-for="(candidate, index) in memoryCandidates" :key="index" class="candidate-item">
                <label class="candidate-check">
                  <input v-model="candidate.selected" type="checkbox">
                  <span>保存这条</span>
                </label>
                <div class="candidate-fields">
                  <select v-model="candidate.memoryType" class="select">
                    <option value="HOBBY">HOBBY 爱好</option>
                    <option value="QUOTE">QUOTE 说过的话</option>
                    <option value="TRAIT">TRAIT 性格特点</option>
                    <option value="DISLIKE">DISLIKE 不喜欢</option>
                    <option value="DATE">DATE 重要日期</option>
                    <option value="NOTE">NOTE 备注</option>
                  </select>
                  <textarea v-model="candidate.content" class="textarea" />
                  <small>模型可信度：{{ formatConfidence(candidate.confidence) }}</small>
                </div>
              </article>

              <div class="candidate-actions">
                <span>已选择 {{ selectedCandidateCount }} / {{ memoryCandidates.length }} 条</span>
                <button class="btn" type="button" :disabled="memoryLoading || selectedCandidateCount === 0" @click="saveCandidates">
                  {{ memoryLoading ? '保存中...' : '保存选中的记忆' }}
                </button>
              </div>
            </div>
          </template>
        </section>
      </section>

      <section class="panel result-panel">
        <div class="panel-title-row">
          <h3>分析结果</h3>
          <span v-if="result.model" class="tag">{{ result.model }}</span>
        </div>

        <div v-if="loading" class="result-empty result-loading">
          <div class="loading-orbit"><span></span></div>
          <strong>AI 正在看图</strong>
          <span>图片越大，等待时间可能越长。</span>
        </div>

        <div v-else-if="result.answer" class="answer-box">
          <div class="answer-meta">
            <strong>{{ result.filename || '图片分析结果' }}</strong>
            <button class="btn secondary small" type="button" @click="copyAnswer">复制结果</button>
          </div>
          <pre>{{ result.answer }}</pre>
        </div>

        <div v-else class="result-empty">
          <strong>暂无分析结果</strong>
          <span>选择图片后开始分析。</span>
        </div>
      </section>
    </div>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import { analyzeImage, extractMemoryCandidates } from '@/api/image'
import { addMemory } from '@/api/memory'
import { listPersons } from '@/api/person'

export default {
  name: 'ImageAnalyzeView',
  components: {
    AppLayout
  },
  data() {
    return {
      file: null,
      previewUrl: '',
      dragging: false,
      prompt: '请用中文分析这张图片，说明图片中的主要内容；如果图片里有文字，请尽量提取出来。',
      promptTemplates: [
        {
          title: '识别内容',
          desc: '概括画面主体',
          prompt: '请用中文详细分析这张图片：先说明图片里的主要对象、场景和动作，再总结这张图想表达的信息。'
        },
        {
          title: '提取文字',
          desc: 'OCR 识别',
          prompt: '请提取这张图片中所有能看清的文字，尽量保持原有顺序；如果有表格或分段，也请按结构整理。'
        },
        {
          title: '截图排错',
          desc: '分析报错截图',
          prompt: '这是一张软件或网页截图。请识别截图中的错误信息、异常提示或关键界面内容，并给出可能原因和解决建议。'
        },
        {
          title: '聊天截图',
          desc: '总结对话重点',
          prompt: '这是一张聊天截图。请提取对话里的关键信息，总结双方表达的意思，并指出需要重点关注的内容。'
        },
        {
          title: '商品文案',
          desc: '生成介绍',
          prompt: '请根据这张图片生成一段商品或内容介绍文案，要求自然、有吸引力，并列出3个可用于发布的标题。'
        }
      ],
      result: {
        answer: '',
        model: '',
        filename: ''
      },
      persons: [],
      selectedPersonId: '',
      memoryExtractionLoading: false,
      memoryExtractionDone: false,
      memoryCandidates: [],
      memoryExtraction: {
        personName: '',
        rawAnswer: ''
      },
      savedMemoryCount: 0,
      memoryLoading: false,
      loading: false,
      message: '',
      messageType: 'error'
    }
  },
  computed: {
    selectedPersonName() {
      const person = this.persons.find(item => String(item.id) === String(this.selectedPersonId))
      return person ? person.name : ''
    },
    selectedCandidateCount() {
      return this.memoryCandidates.filter(item =>
        item.selected && item.content && item.content.trim()
      ).length
    }
  },
  created() {
    this.loadPersons()
  },
  beforeDestroy() {
    this.clearPreview()
  },
  methods: {
    async loadPersons() {
      try {
        const result = await listPersons({ page: 1, pageSize: 50 })
        if (result.code === 1) {
          this.persons = (result.data && result.data.rows) || []
        }
      } catch (error) {
        this.persons = []
      }
    },
    chooseFile() {
      if (this.loading) return
      this.$refs.fileInput.click()
    },
    onFileChange(event) {
      const selectedFile = event.target.files && event.target.files[0]
      if (!selectedFile) return
      this.handleSelectedFile(selectedFile)
    },
    onDragEnter() {
      if (this.loading) return
      this.dragging = true
    },
    onDragOver() {
      if (this.loading) return
      this.dragging = true
    },
    onDragLeave(event) {
      if (event.currentTarget.contains(event.relatedTarget)) return
      this.dragging = false
    },
    onDrop(event) {
      this.dragging = false
      if (this.loading) return

      const selectedFile = event.dataTransfer.files && event.dataTransfer.files[0]
      if (!selectedFile) return
      this.handleSelectedFile(selectedFile)
    },
    handleSelectedFile(selectedFile) {
      const allowTypes = ['image/jpeg', 'image/png', 'image/webp', 'image/bmp', 'image/gif']

      if (!selectedFile.type || !allowTypes.includes(selectedFile.type)) {
        this.showMessage('当前只支持 JPG、PNG、WEBP、BMP、GIF 图片')
        this.resetFileInput()
        return
      }

      if (selectedFile.size > 10 * 1024 * 1024) {
        this.showMessage('图片不能超过 10MB')
        this.resetFileInput()
        return
      }

      this.clearPreview()
      this.file = selectedFile
      this.previewUrl = URL.createObjectURL(selectedFile)
      this.result = {
        answer: '',
        model: '',
        filename: ''
      }
      this.clearMemoryExtraction()
      this.showMessage('图片已选择，可以开始分析', 'success')
    },
    applyTemplate(template) {
      this.prompt = template.prompt
      this.showMessage(`已切换为「${template.title}」模板`, 'success')
    },
    async submitAnalyze() {
      if (!this.file) {
        this.showMessage('请先选择图片')
        return
      }

      this.loading = true
      this.message = ''

      try {
        const result = await analyzeImage(this.file, this.prompt)
        if (result.code !== 1) {
          this.showMessage(result.msg || '图片分析失败')
          return
        }

        this.result = result.data || {
          answer: '',
          model: '',
          filename: ''
        }
        this.showMessage('图片分析完成', 'success')
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async extractMemories() {
      if (!this.file) {
        this.showMessage('请先选择图片')
        return
      }

      if (!this.selectedPersonId) {
        this.showMessage('请先选择要关联的人物')
        return
      }

      this.memoryExtractionLoading = true
      this.memoryExtractionDone = false
      this.memoryCandidates = []
      this.memoryExtraction = {
        personName: this.selectedPersonName,
        rawAnswer: ''
      }
      this.savedMemoryCount = 0

      try {
        const result = await extractMemoryCandidates(this.file, this.selectedPersonName)
        if (result.code !== 1) {
          this.showMessage(result.msg || '提取候选记忆失败')
          return
        }

        const data = result.data || {}
        const fallback = this.parseRawMemoryAnswer(data.rawAnswer)
        const candidates = Array.isArray(data.candidates) && data.candidates.length > 0
          ? data.candidates
          : fallback.candidates

        this.memoryExtraction = {
          personName: data.personName || fallback.personName || this.selectedPersonName,
          rawAnswer: data.rawAnswer || ''
        }
        this.memoryCandidates = candidates.map(item => ({
          memoryType: this.normalizeMemoryType(item.memoryType),
          content: item.content || '',
          confidence: item.confidence,
          selected: true
        }))
        this.memoryExtractionDone = true
        this.showMessage(
          this.memoryCandidates.length > 0
            ? `已提取 ${this.memoryCandidates.length} 条候选记忆，请确认后保存`
            : '没有提取到可直接保存的候选记忆',
          this.memoryCandidates.length > 0 ? 'success' : 'error'
        )
      } catch (error) {
        this.showMessage('请求失败，请确认后端视觉模型配置正确')
      } finally {
        this.memoryExtractionLoading = false
      }
    },
    async saveCandidates() {
      const targets = this.memoryCandidates.filter(item =>
        item.selected && item.content && item.content.trim()
      )

      if (!this.selectedPersonId || targets.length === 0) {
        this.showMessage('请至少选择一条有效记忆')
        return
      }

      this.memoryLoading = true
      let savedCount = 0

      try {
        for (const candidate of targets) {
          const result = await addMemory(this.selectedPersonId, {
            memoryType: candidate.memoryType || 'NOTE',
            content: candidate.content.trim(),
            source: this.buildMemorySource(candidate)
          })

          if (result.code !== 1) {
            throw new Error(result.msg || '保存人物记忆失败')
          }
          savedCount += 1
        }

        this.memoryCandidates = this.memoryCandidates.filter(item => !targets.includes(item))
        this.savedMemoryCount += savedCount
        this.showMessage(`已确认并保存 ${savedCount} 条人物记忆`, 'success')
      } catch (error) {
        this.showMessage(error.message || '保存人物记忆失败')
      } finally {
        this.memoryLoading = false
      }
    },
    buildMemorySource(candidate) {
      const filename = this.result.filename || this.file.name
      const confidence = Number(candidate.confidence)
      const confidenceText = Number.isNaN(confidence)
        ? ''
        : ` · 模型可信度：${this.formatConfidence(confidence)}`

      return `图片提取：${filename}${confidenceText}`.slice(0, 100)
    },
    async copyAnswer() {
      if (!this.result.answer) return

      try {
        await navigator.clipboard.writeText(this.result.answer)
        this.showMessage('分析结果已复制', 'success')
      } catch (error) {
        this.showMessage('复制失败，请手动选择结果内容')
      }
    },
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
    },
    clearPreview() {
      if (this.previewUrl) {
        URL.revokeObjectURL(this.previewUrl)
      }
      this.previewUrl = ''
    },
    resetFileInput() {
      this.file = null
      this.clearPreview()
      this.result = {
        answer: '',
        model: '',
        filename: ''
      }
      this.clearMemoryExtraction()
      this.dragging = false
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = ''
      }
    },
    clearMemoryExtraction() {
      this.memoryExtractionDone = false
      this.memoryCandidates = []
      this.memoryExtraction = {
        personName: '',
        rawAnswer: ''
      }
      this.savedMemoryCount = 0
    },
    parseRawMemoryAnswer(rawAnswer) {
      if (!rawAnswer || !String(rawAnswer).trim()) {
        return {
          personName: '',
          candidates: []
        }
      }

      let text = String(rawAnswer).trim()

      try {
        const direct = JSON.parse(text)
        if (Array.isArray(direct)) {
          text = direct.map(item => {
            if (typeof item === 'string') return item
            return item && (item.text || item.content || '')
          }).join('')
        }
      } catch (error) {
        // 模型返回的内容通常是 Markdown 代码块，继续按文本提取 JSON。
      }

      text = text
        .replace(/^```(?:json)?\s*/i, '')
        .replace(/\s*```$/i, '')
        .trim()

      const start = text.indexOf('{')
      if (start < 0) {
        return {
          personName: '',
          candidates: []
        }
      }

      let depth = 0
      let inString = false
      let escaped = false
      let end = -1

      for (let index = start; index < text.length; index += 1) {
        const current = text[index]

        if (inString) {
          if (escaped) {
            escaped = false
          } else if (current === '\\') {
            escaped = true
          } else if (current === '"') {
            inString = false
          }
          continue
        }

        if (current === '"') {
          inString = true
        } else if (current === '{') {
          depth += 1
        } else if (current === '}') {
          depth -= 1
          if (depth === 0) {
            end = index + 1
            break
          }
        }
      }

      if (end < 0) {
        return {
          personName: '',
          candidates: []
        }
      }

      try {
        const root = JSON.parse(text.slice(start, end))
        const payload = root.data && typeof root.data === 'object' ? root.data : root
        const memories = payload.memories || payload.candidates || payload.items || []

        return {
          personName: payload.personName || '',
          candidates: Array.isArray(memories)
            ? memories
                .filter(item => item && item.content && String(item.content).trim())
                .slice(0, 8)
                .map(item => ({
                  memoryType: this.normalizeMemoryType(item.memoryType),
                  content: String(item.content).trim(),
                  confidence: item.confidence
                }))
            : []
        }
      } catch (error) {
        return {
          personName: '',
          candidates: []
        }
      }
    },
    normalizeMemoryType(value) {
      const type = String(value || '').trim().toUpperCase()
      const mapping = {
        HOBBY: 'HOBBY',
        QUOTE: 'QUOTE',
        TRAIT: 'TRAIT',
        DISLIKE: 'DISLIKE',
        DATE: 'DATE',
        NOTE: 'NOTE',
        爱好: 'HOBBY',
        兴趣: 'HOBBY',
        偏好: 'HOBBY',
        语录: 'QUOTE',
        原话: 'QUOTE',
        性格: 'TRAIT',
        性格特点: 'TRAIT',
        不喜欢: 'DISLIKE',
        讨厌: 'DISLIKE',
        日期: 'DATE',
        重要日期: 'DATE',
        生日: 'DATE',
        纪念日: 'DATE'
      }
      return mapping[type] || 'NOTE'
    },
    formatConfidence(value) {
      const number = Number(value)
      if (Number.isNaN(number)) return '待确认'
      return `${Math.round(Math.max(0, Math.min(1, number)) * 100)}%`
    },
    formatSize(size) {
      if (!size && size !== 0) return ''
      if (size < 1024) return `${size} B`
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
      return `${(size / 1024 / 1024).toFixed(2)} MB`
    }
  }
}
</script>

<style scoped>
h3 {
  margin: 0;
}

.analysis-steps {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12px 30px rgba(17, 24, 39, 0.05);
}

.analysis-step {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  color: #9ca3af;
}

.analysis-step > span {
  display: grid;
  flex: 0 0 30px;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  color: #6b7280;
  background: #eef0f3;
  font-size: 11px;
  font-weight: 800;
}

.analysis-step.active {
  color: #111827;
}

.analysis-step.active > span {
  color: #ffffff;
  background: linear-gradient(135deg, var(--brand), var(--brand-dark));
  box-shadow: 0 8px 18px rgba(239, 52, 61, 0.2);
}

.analysis-step strong,
.analysis-step small {
  display: block;
}

.analysis-step strong {
  font-size: 13px;
}

.analysis-step small {
  margin-top: 2px;
  color: #9ca3af;
  font-size: 11px;
}

.analysis-line {
  flex: 1;
  height: 1px;
  min-width: 20px;
  background: #e5e7eb;
}

.image-workspace {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
  gap: 16px;
  align-items: start;
}

.panel-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-title-row.compact {
  margin-bottom: 10px;
}

.panel-title-row.compact h3 {
  font-size: 16px;
}

.count-text {
  color: #6b7280;
  font-size: 13px;
}

.drop-zone {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 320px;
  border: 1px dashed #c9ced8;
  border-radius: 8px;
  overflow: hidden;
  color: #6b7280;
  background:
    linear-gradient(135deg, rgba(239, 52, 61, 0.08), transparent 42%),
    #f9fafb;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
  cursor: pointer;
}

.drop-zone:hover {
  border-color: var(--brand);
  box-shadow: 0 14px 36px rgba(17, 24, 39, 0.08);
  transform: translateY(-1px);
}

.drop-zone.active {
  border-style: solid;
  background: #111827;
}

.drop-zone.dragging {
  border-color: var(--brand);
  background:
    linear-gradient(135deg, rgba(239, 52, 61, 0.14), transparent 42%),
    #ffffff;
  box-shadow: 0 0 0 4px rgba(239, 52, 61, 0.1), 0 18px 42px rgba(17, 24, 39, 0.1);
}

.drop-zone input {
  display: none;
}

.drop-zone strong {
  color: #111827;
  font-size: 20px;
}

.drop-zone span {
  margin-top: 8px;
  font-size: 14px;
}

.preview-image {
  width: 100%;
  height: 100%;
  max-height: 440px;
  object-fit: contain;
  background: #111827;
}

.preview-mask {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  display: flex;
  justify-content: center;
  min-height: 36px;
  align-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: rgba(17, 24, 39, 0.76);
  font-size: 13px;
  opacity: 0;
  transition: opacity 0.18s ease;
}

.drop-zone:hover .preview-mask,
.drop-zone.dragging .preview-mask {
  opacity: 1;
}

.file-meta {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin-top: 12px;
  padding: 11px 12px;
  border-radius: 8px;
  background: #f3f4f6;
}

.file-meta div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.file-meta strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta span {
  flex: 0 0 auto;
  color: #6b7280;
}

.template-panel {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.template-btn {
  display: grid;
  gap: 5px;
  min-height: 74px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 11px;
  text-align: left;
  color: #374151;
  background: #ffffff;
  transition: border-color 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.template-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(239, 52, 61, 0.32);
  box-shadow: 0 10px 24px rgba(17, 24, 39, 0.07);
}

.template-btn strong {
  color: #111827;
  font-size: 14px;
}

.template-btn span {
  color: #6b7280;
  font-size: 12px;
}

.prompt-input {
  margin-top: 12px;
  min-height: 116px;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
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

.memory-extraction {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.memory-extraction h3 {
  margin: 0;
  font-size: 16px;
}

.section-hint {
  margin: 5px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.extraction-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 10px;
}

.extraction-toolbar label {
  display: grid;
  gap: 7px;
  color: #374151;
  font-size: 13px;
  font-weight: 700;
}

.extraction-empty {
  display: grid;
  gap: 7px;
  padding: 14px;
  border: 1px dashed #d5d9e1;
  border-radius: 8px;
  color: #6b7280;
  background: #f9fafb;
}

.extraction-empty strong {
  color: #1f2937;
}

.extraction-success {
  border-color: #a7f3d0;
  background: #ecfdf5;
}

.extraction-success strong {
  color: #047857;
}

.extraction-empty pre {
  max-height: 160px;
  margin: 4px 0 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.5;
  font-family: inherit;
}

.candidate-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.candidate-item {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.candidate-check {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding-top: 4px;
  color: #374151;
  font-size: 12px;
  white-space: nowrap;
}

.candidate-fields {
  display: grid;
  gap: 8px;
}

.candidate-fields .textarea {
  min-height: 76px;
}

.candidate-fields small {
  color: #6b7280;
}

.candidate-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.result-panel {
  min-height: 520px;
}

.result-empty {
  display: grid;
  place-items: center;
  align-content: center;
  min-height: 420px;
  color: #6b7280;
  text-align: center;
  border: 1px dashed #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
}

.result-loading {
  border-color: #fecdd3;
  background:
    linear-gradient(135deg, rgba(239, 52, 61, 0.06), transparent 55%),
    #fffafa;
}

.loading-orbit {
  position: relative;
  width: 48px;
  height: 48px;
  margin-bottom: 14px;
  border: 3px solid #fecdd3;
  border-top-color: var(--brand);
  border-radius: 999px;
  animation: image-spin 1s linear infinite;
}

.loading-orbit span {
  position: absolute;
  top: 7px;
  left: 7px;
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--brand);
}

@keyframes image-spin {
  to {
    transform: rotate(360deg);
  }
}

.result-empty strong {
  color: #111827;
  font-size: 20px;
}

.result-empty span {
  margin-top: 8px;
}

.answer-box {
  min-height: 420px;
}

.answer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.answer-meta strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn.small {
  min-height: 34px;
  padding: 7px 11px;
  font-size: 13px;
}

.answer-box pre {
  min-height: 386px;
  max-height: calc(100vh - 270px);
  margin: 0;
  padding: 16px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.75;
  color: #374151;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  font-family: inherit;
}

@media (max-width: 980px) {
  .image-workspace {
    grid-template-columns: 1fr;
  }

  .drop-zone {
    min-height: 260px;
  }
}

@media (max-width: 640px) {
  .analysis-steps {
    align-items: stretch;
    flex-direction: column;
  }

  .analysis-line {
    width: 1px;
    height: 14px;
    min-height: 14px;
    margin-left: 14px;
  }

  .action-row,
  .extraction-toolbar,
  .candidate-actions,
  .answer-meta,
  .file-meta {
    display: grid;
    grid-template-columns: 1fr;
  }

  .candidate-item {
    grid-template-columns: 1fr;
  }

  .template-grid {
    grid-template-columns: 1fr;
  }
}
</style>
