<template>
  <form class="memory-form" @submit.prevent="$emit('submit')">
    <div class="grid two">
      <label>
        人物姓名
        <input class="input" :value="value.personName" disabled>
      </label>
      <label>
        记忆类型
        <select class="select" :value="value.memoryType" @change="update('memoryType', $event.target.value)">
          <option value="HOBBY">HOBBY 爱好</option>
          <option value="QUOTE">QUOTE 说过的话</option>
          <option value="TRAIT">TRAIT 性格特点</option>
          <option value="DISLIKE">DISLIKE 不喜欢</option>
          <option value="DATE">DATE 重要日期</option>
          <option value="NOTE">NOTE 备注</option>
        </select>
      </label>
    </div>
    <label>
      记忆内容
      <textarea
        class="textarea"
        :value="value.content"
        placeholder="例如：喜欢吃甜食，喜欢川西和南昌"
        @input="update('content', $event.target.value)"
      />
    </label>
    <label>
      来源
      <input
        class="input"
        :value="value.source"
        placeholder="例如：手动记录、聊天提取、文件导入"
        @input="update('source', $event.target.value)"
      >
    </label>
    <div class="form-actions">
      <button class="btn" type="submit" :disabled="submitting">
        {{ submitting ? '保存中...' : '保存记忆' }}
      </button>
      <button class="btn secondary" type="button" @click="$emit('reset')">清空</button>
    </div>
  </form>
</template>

<script>
export default {
  name: 'MemoryForm',
  props: {
    value: {
      type: Object,
      required: true
    },
    submitting: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    update(field, val) {
      this.$emit('input', {
        ...this.value,
        [field]: val
      })
    }
  }
}
</script>

<style scoped>
.memory-form {
  display: grid;
  gap: 14px;
}

label {
  display: grid;
  gap: 7px;
  color: #374151;
  font-size: 14px;
  font-weight: 700;
}

.form-actions {
  display: flex;
  gap: 10px;
}
</style>
