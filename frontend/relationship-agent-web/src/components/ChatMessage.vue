<template>
  <div class="chat-message" :class="role">
    <div class="message-avatar">{{ role === 'user' ? '我' : 'AI' }}</div>
    <div class="message-body">
      <div class="message-role">{{ role === 'user' ? '用户' : '识忆承缘' }}</div>
      <div class="message-content">{{ displayContent }}</div>
      <div v-if="role !== 'user' && answerContent" class="message-tools">
        <button class="message-tool" type="button" @click="copyContent">
          {{ copied ? '已复制' : '复制回复' }}
        </button>
      </div>
      <div v-if="sources.length > 0" class="source-box">
        <div class="source-title">参考来源</div>
        <button
          v-for="source in sources"
          :key="source.raw"
          class="source-item"
          type="button"
          @click="$emit('open-source', source.documentId)"
        >
          <span>{{ source.text }}</span>
          <small v-if="source.documentId">查看原文</small>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatMessage',
  props: {
    role: {
      type: String,
      default: 'assistant'
    },
    content: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      copied: false
    }
  },
  computed: {
    sourceMarker() {
      const markers = ['Reference sources:', '参考来源：', '参考来源:']
      return markers
        .map(marker => ({
          marker,
          index: this.content.indexOf(marker)
        }))
        .filter(item => item.index !== -1)
        .sort((a, b) => a.index - b.index)[0] || null
    },
    answerContent() {
      if (!this.sourceMarker) {
        return this.content
      }

      return this.content.slice(0, this.sourceMarker.index).trimEnd()
    },
    displayContent() {
      if (this.answerContent) {
        return this.answerContent
      }

      return this.role === 'assistant' ? '正在生成回复…' : ''
    },
    sources() {
      if (!this.sourceMarker) {
        return []
      }

      return this.content
        .slice(this.sourceMarker.index + this.sourceMarker.marker.length)
        .split('\n')
        .map(line => line.trim())
        .filter(line => line)
        .map(line => {
          const match = line.match(/\[doc:(\d+)]/)
          const documentId = match ? Number(match[1]) : null
          const text = line.replace(/\[doc:\d+]\s*/, '')

          return {
            raw: line,
            documentId,
            text
          }
        })
    }
  },
  methods: {
    async copyContent() {
      if (!this.answerContent) return

      try {
        await navigator.clipboard.writeText(this.answerContent)
        this.copied = true
        window.setTimeout(() => {
          this.copied = false
        }, 1600)
      } catch (error) {
        this.copied = false
      }
    }
  }
}
</script>

<style scoped>
.chat-message {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex: 0 0 40px;
  height: 40px;
  display: grid;
  place-items: center;
  color: #ffffff;
  background: #111827;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
  box-shadow: 0 10px 22px rgba(17, 24, 39, 0.18);
}

.chat-message.user .message-avatar {
  background: linear-gradient(135deg, #ef343d, #c91f2b);
}

.message-body {
  max-width: min(720px, 76%);
  padding: 13px 15px;
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 10px 26px rgba(17, 24, 39, 0.06);
}

.chat-message.user .message-body {
  color: #ffffff;
  background: linear-gradient(135deg, #ef343d, #c91f2b);
  border-color: transparent;
}

.message-role {
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
}

.chat-message.user .message-role {
  color: rgba(255, 255, 255, 0.78);
}

.message-content {
  white-space: pre-wrap;
  line-height: 1.75;
}

.message-tools {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.message-tool {
  border: 0;
  padding: 2px 0;
  color: #9ca3af;
  background: transparent;
  font-size: 12px;
}

.message-tool:hover {
  color: #b91c1c;
}

.source-box {
  display: grid;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  background: #fff1f2;
}

.source-title {
  color: #b91c1c;
  font-size: 13px;
  font-weight: 800;
}

.source-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #ffe4e6;
  padding: 8px;
  text-align: left;
  color: #374151;
  background: #ffffff;
  border-radius: 8px;
  line-height: 1.5;
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.source-item:hover {
  transform: translateY(-1px);
  border-color: #fb7185;
}

.source-item small {
  flex: 0 0 auto;
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
}

@media (max-width: 720px) {
  .message-body {
    max-width: calc(100% - 52px);
  }
}
</style>
