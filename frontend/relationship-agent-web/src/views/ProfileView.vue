<template>
  <AppLayout>
    <div class="page-header">
      <div>
        <h2 class="page-title">个人中心</h2>
        <p class="page-desc">查看当前账号信息，修改昵称和登录密码。</p>
      </div>
      <button class="btn secondary" type="button" :disabled="loading" @click="loadUserInfo">
        {{ loading ? '刷新中...' : '刷新资料' }}
      </button>
    </div>

    <div class="grid two">
      <section class="panel profile-card">
        <div class="profile-avatar">
          <img src="@/assets/brand-logo.png" alt="品牌标识">
        </div>
        <h3>账号信息</h3>
        <div v-if="loading && !userInfo" class="empty-text">正在加载用户信息...</div>
        <div v-else class="info-list">
          <div class="info-row">
            <span>用户ID</span>
            <strong>{{ userInfo ? userInfo.id : '-' }}</strong>
          </div>
          <div class="info-row">
            <span>账号</span>
            <strong>{{ userInfo ? userInfo.username : '-' }}</strong>
          </div>
          <div class="info-row">
            <span>昵称</span>
            <strong>{{ userInfo ? userInfo.nickname : '-' }}</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <h3>修改昵称</h3>
        <form class="profile-form" @submit.prevent="submitNickname">
          <label>
            新昵称
            <input v-model="nicknameForm.nickname" class="input" placeholder="请输入新的昵称">
          </label>
          <button class="btn" type="submit" :disabled="loading">
            {{ loading ? '保存中...' : '保存昵称' }}
          </button>
        </form>
      </section>
    </div>

    <section class="panel password-panel">
      <h3>修改密码</h3>
      <form class="profile-form password-form" @submit.prevent="submitPassword">
        <label>
          原密码
          <input v-model="passwordForm.oldPassword" class="input" type="password" placeholder="请输入当前密码">
        </label>
        <label>
          新密码
          <input v-model="passwordForm.newPassword" class="input" type="password" placeholder="请输入新密码">
        </label>
        <button class="btn" type="submit" :disabled="loading">
          {{ loading ? '修改中...' : '修改密码' }}
        </button>
      </form>
    </section>

    <div v-if="message" :class="['toast', messageType]">{{ message }}</div>
  </AppLayout>
</template>

<script>
import AppLayout from '@/components/AppLayout.vue'
import { getCurrentUser, updateNickname, updatePassword } from '@/api/user'

export default {
  name: 'ProfileView',
  components: {
    AppLayout
  },
  data() {
    return {
      loading: false,
      message: '',
      messageType: 'error',
      messageTimer: null,
      userInfo: null,
      nicknameForm: {
        nickname: ''
      },
      passwordForm: {
        oldPassword: '',
        newPassword: ''
      }
    }
  },
  created() {
    this.loadUserInfo()
  },
  beforeDestroy() {
    window.clearTimeout(this.messageTimer)
  },
  methods: {
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
      window.clearTimeout(this.messageTimer)
      this.messageTimer = window.setTimeout(() => {
        this.message = ''
      }, 2600)
    },
    async loadUserInfo() {
      this.loading = true
      try {
        const result = await getCurrentUser()
        if (result.code !== 1) {
          this.showMessage(result.msg || '加载用户信息失败')
          return
        }

        this.userInfo = result.data
        this.nicknameForm.nickname = result.data.nickname || ''
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async submitNickname() {
      if (!this.nicknameForm.nickname.trim()) {
        this.showMessage('请先填写昵称')
        return
      }

      this.loading = true
      try {
        const result = await updateNickname(this.nicknameForm)
        if (result.code !== 1) {
          this.showMessage(result.msg || '修改昵称失败')
          return
        }

        this.showMessage('昵称修改成功', 'success')
        await this.loadUserInfo()
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async submitPassword() {
      if (!this.passwordForm.oldPassword.trim()) {
        this.showMessage('请先填写原密码')
        return
      }

      if (!this.passwordForm.newPassword.trim()) {
        this.showMessage('请先填写新密码')
        return
      }

      this.loading = true
      try {
        const result = await updatePassword(this.passwordForm)
        if (result.code !== 1) {
          this.showMessage(result.msg || '修改密码失败')
          return
        }

        this.showMessage('密码修改成功，请记住新密码', 'success')
        this.passwordForm = {
          oldPassword: '',
          newPassword: ''
        }
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
h3 {
  margin: 0 0 16px;
}

.profile-card {
  position: relative;
  overflow: hidden;
}

.profile-card::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 88px;
  background: linear-gradient(135deg, #111827, #ef343d);
}

.profile-avatar {
  position: relative;
  width: 82px;
  height: 82px;
  display: grid;
  place-items: center;
  margin-bottom: 16px;
  border: 4px solid #ffffff;
  border-radius: 8px;
  background: #0d1118;
  box-shadow: 0 14px 32px rgba(17, 24, 39, 0.18);
}

.profile-avatar img {
  width: 66px;
  height: 66px;
  object-fit: contain;
}

.info-list {
  display: grid;
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.info-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.info-row span,
.empty-text {
  color: #6b7280;
}

.profile-form {
  display: grid;
  gap: 14px;
}

.profile-form label {
  display: grid;
  gap: 7px;
  color: #374151;
  font-weight: 700;
}

.password-panel {
  margin-top: 16px;
}

.password-form {
  max-width: 520px;
}

.toast {
  position: fixed;
  top: 24px;
  right: 24px;
  z-index: 20;
  max-width: min(360px, calc(100vw - 48px));
  padding: 11px 13px;
  border-radius: 8px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.14);
  font-size: 14px;
}

.toast.error {
  color: #991b1b;
  background: #fee2e2;
}

.toast.success {
  color: #047857;
  background: #d1fae5;
}
</style>
