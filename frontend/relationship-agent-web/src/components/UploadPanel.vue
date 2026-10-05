<template>
  <div class="upload-panel">
    <label
      class="upload-box"
      :class="{ disabled, dragging }"
      @dragenter.prevent="dragging = true"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
    >
      <input
        ref="fileInput"
        class="file-input"
        type="file"
        accept=".pdf,.txt"
        :disabled="disabled"
        @change="onFileChange"
      >
      <span class="upload-icon">↑</span>
      <span class="upload-title">{{ dragging ? '松开即可上传' : '上传知识库文件' }}</span>
      <span class="upload-desc">支持 PDF、TXT，单个文件不超过 70MB；PDF 最多 100 页，解析文本最多 20 万字。</span>
      <span class="upload-action">{{ disabled ? '处理中...' : '选择文件' }}</span>
    </label>

    <div v-if="fileName" class="file-preview">
      <span>已选择：{{ fileName }}</span>
      <button class="clear-btn" type="button" :disabled="disabled" @click="clearFile">清空</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UploadPanel',
  props: {
    disabled: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      fileName: '',
      dragging: false
    }
  },
  methods: {
    onFileChange(event) {
      const file = event.target.files && event.target.files[0]
      this.selectFile(file)
    },
    onDrop(event) {
      this.dragging = false
      const file = event.dataTransfer.files && event.dataTransfer.files[0]
      this.selectFile(file)
    },
    selectFile(file) {
      this.fileName = file ? file.name : ''
      this.$emit('selected', file)
    },
    clearFile() {
      this.fileName = ''
      this.dragging = false
      if (this.$refs.fileInput) {
        this.$refs.fileInput.value = ''
      }
    }
  }
}
</script>

<style scoped>
.upload-panel {
  display: grid;
  gap: 12px;
}

.upload-box {
  display: grid;
  gap: 8px;
  padding: 22px;
  border: 1px dashed #fb7185;
  border-radius: 8px;
  background: #fff1f2;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.upload-box.dragging {
  border-style: solid;
  border-color: #ef343d;
  background: #ffffff;
  box-shadow: 0 0 0 4px rgba(239, 52, 61, 0.1);
}

.upload-box:hover:not(.disabled) {
  transform: translateY(-1px);
  border-color: #ef343d;
  background: #ffffff;
}

.upload-box.disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.file-input {
  display: none;
}

.upload-title {
  color: #991b1b;
  font-weight: 800;
}

.upload-icon {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, #ef343d, #c91f2b);
  font-size: 22px;
  font-weight: 800;
}

.upload-desc,
.file-preview {
  color: #6b7280;
}

.upload-action {
  width: fit-content;
  margin-top: 4px;
  padding: 8px 12px;
  border-radius: 8px;
  color: #ffffff;
  background: #111827;
  font-size: 14px;
}

.file-preview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 36px;
  font-size: 14px;
}

.clear-btn {
  border: 0;
  color: #b91c1c;
  background: transparent;
}
</style>
