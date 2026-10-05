<template>
  <div class="login-page">
    <section class="brand-stage">
      <img class="hero-logo animated-logo" src="@/assets/brand-logo.png" alt="品牌标识">
      <div class="stage-copy">
        <span class="eyebrow">AI Relationship Agent</span>
        <h1>识忆承缘</h1>
        <p class="slogan">We will must pop on the street</p>
        <p>把人物、记忆、知识库和聊天历史放在一个工作台里，让 AI 更懂你的关系上下文。</p>
      </div>
      <div class="feature-strip">
        <span>人物记忆</span>
        <span>知识库检索</span>
        <span>聊天留痕</span>
      </div>
      <BrandVideo compact />
    </section>

    <section class="login-card">
      <div class="card-title">
        <span class="tag">{{ resetMode ? 'Password Reset' : 'Secure Login' }}</span>
        <h2>{{ resetMode ? '找回密码' : '登录控制台' }}</h2>
        <p>{{ resetMode ? '验证码有效期 1 分钟，验证成功后即可设置新密码。' : '注册时昵称默认使用账号，登录后可以在个人中心修改。' }}</p>
      </div>

      <form v-if="!resetMode" class="login-form" @submit.prevent="handleLogin">
        <label>
          账号
          <input v-model="form.username" class="input" placeholder="请输入账号">
        </label>
        <label>
          密码
          <input v-model="form.password" class="input" type="password" placeholder="请输入密码">
        </label>

        <div v-if="message" :class="['message', messageType]">{{ message }}</div>

        <button class="btn primary-action" type="submit" :disabled="loading || loginLockSeconds > 0">
          {{ loginButtonText }}
        </button>
        <button class="btn secondary" type="button" :disabled="loading" @click="handleRegister">
          {{ loading ? '处理中...' : '注册新账号' }}
        </button>
        <button class="text-action" type="button" :disabled="loading" @click="openResetMode">
          忘记密码？去找回
        </button>
      </form>

      <form v-else class="login-form reset-form" @submit.prevent="handleResetPassword">
        <label>
          账号
          <div class="code-row">
            <input v-model="resetForm.username" class="input" placeholder="请输入要找回的账号">
            <button
              class="btn secondary code-btn"
              type="button"
              :disabled="loading || resetCooldown > 0"
              @click="handleForgotCode"
            >
              {{ codeButtonText }}
            </button>
          </div>
        </label>

        <div v-if="resetCode" class="code-card">
          <span>测试验证码</span>
          <strong>{{ resetCode }}</strong>
          <small>后端已存入 Redis，请手动输入验证码。{{ cooldownText }}</small>
        </div>

        <div v-else-if="resetCooldown > 0" class="cooldown-card">
          验证码已发送，{{ resetCooldown }} 秒后可以重新发送。
        </div>

        <label>
          验证码
          <input v-model="resetForm.code" class="input" maxlength="6" placeholder="请输入 6 位验证码">
        </label>
        <label>
          新密码
          <input v-model="resetForm.newPassword" class="input" type="password" placeholder="请输入新密码">
        </label>

        <div v-if="message" :class="['message', messageType]">{{ message }}</div>

        <button class="btn primary-action" type="submit" :disabled="loading">
          {{ loading ? '重置中...' : '确认重置密码' }}
        </button>
        <button class="btn secondary" type="button" :disabled="loading" @click="closeResetMode">
          返回登录
        </button>
      </form>

      <p class="demo-tip">{{ resetMode ? '学习版会直接显示验证码；企业版后续可改成短信或邮箱发送。' : '先注册账号，再登录进入聊天页面。' }}</p>
    </section>
  </div>
</template>

<script>
import BrandVideo from '@/components/BrandVideo.vue'
import { forgotCode, login, register, resetPassword } from '@/api/auth'
import {
  consumeAuthExpiredMessage,
  setToken
} from '@/store/user'
import { resetAuthRedirectState } from '@/utils/request'

export default {
  name: 'LoginView',
  components: {
    BrandVideo
  },
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      resetMode: false,
      resetCode: '',
      resetForm: {
        username: '',
        code: '',
        newPassword: ''
      },
      loading: false,
      message: '',
      messageType: 'error',
      loginLockSeconds: 0,
      loginLockTimer: null,
      resetCooldown: 0,
      resetTimer: null
    }
  },
  created() {
    const authExpiredMessage = consumeAuthExpiredMessage()
    if (authExpiredMessage) {
      this.showMessage(authExpiredMessage)
    }
  },
  computed: {
    loginButtonText() {
      if (this.loading) return '登录中...'
      if (this.loginLockSeconds > 0) return `${this.loginLockSeconds}秒后可登录`
      return '登录'
    },
    codeButtonText() {
      if (this.loading) return '获取中...'
      if (this.resetCooldown > 0) return `${this.resetCooldown}秒后重发`
      return '获取验证码'
    },
    cooldownText() {
      if (this.resetCooldown > 0) {
        return `${this.resetCooldown} 秒后可重新发送。`
      }
      return '现在可以重新获取验证码。'
    }
  },
  beforeDestroy() {
    this.clearLoginLockTimer()
    this.clearResetTimer()
  },
  methods: {
    validateForm() {
      if (!this.form.username.trim()) {
        this.showMessage('请输入账号')
        return false
      }
      if (!this.form.password.trim()) {
        this.showMessage('请输入密码')
        return false
      }
      return true
    },
    showMessage(message, type = 'error') {
      this.message = message
      this.messageType = type
    },
    openResetMode() {
      this.resetMode = true
      this.message = ''
      this.resetCode = ''
      this.resetForm.username = this.form.username
      this.resetForm.code = ''
      this.resetForm.newPassword = ''
    },
    closeResetMode() {
      this.resetMode = false
      this.message = ''
      this.resetCode = ''
      this.clearResetTimer()
    },
    async handleLogin() {
      if (!this.validateForm()) return

      this.loading = true
      try {
        const result = await login(this.form)
        if (result.code !== 1) {
          const lockSeconds = this.parseLoginLockSeconds(result.msg)
          if (lockSeconds > 0) {
            this.startLoginLock(lockSeconds)
          }
          this.showMessage(result.msg || '登录失败')
          return
        }

        this.clearLoginLockTimer()
        resetAuthRedirectState()
        setToken(result.data.token)
        this.$router.push(this.getLoginRedirect())
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    startLoginLock(seconds) {
      this.clearLoginLockTimer()
      this.loginLockSeconds = Math.max(0, Number(seconds) || 0)

      if (this.loginLockSeconds <= 0) return

      this.loginLockTimer = window.setInterval(() => {
        if (this.loginLockSeconds <= 1) {
          this.clearLoginLockTimer()
          this.showMessage('账号锁定已结束，可以重新登录', 'success')
          return
        }

        this.loginLockSeconds -= 1
      }, 1000)
    },
    clearLoginLockTimer() {
      if (this.loginLockTimer) {
        window.clearInterval(this.loginLockTimer)
        this.loginLockTimer = null
      }
      this.loginLockSeconds = 0
    },
    parseLoginLockSeconds(message) {
      if (!message) return 0
      const matched = String(message).match(/请(\d+)秒后再试/)
      return matched ? Number(matched[1]) : 0
    },
    getLoginRedirect() {
      const redirect = this.$route.query.redirect
      if (
        typeof redirect === 'string' &&
        redirect.startsWith('/') &&
        !redirect.startsWith('//') &&
        redirect !== '/login'
      ) {
        return redirect
      }

      return '/chat'
    },
    async handleRegister() {
      if (!this.validateForm()) return

      this.loading = true
      this.showMessage(`正在注册，昵称将默认使用 ${this.form.username}`, 'success')
      try {
        const result = await register(this.form)
        if (result.code !== 1) {
          this.showMessage(result.msg || '注册失败')
          return
        }

        this.showMessage(`注册成功，昵称默认是 ${this.form.username}，现在可以登录了`, 'success')
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async handleForgotCode() {
      const username = this.resetForm.username.trim()
      if (!username) {
        this.showMessage('请先输入账号')
        return
      }

      this.loading = true
      this.resetCode = ''
      try {
        const result = await forgotCode({ username })
        if (result.code !== 1) {
          const remainSeconds = this.parseCooldownSeconds(result.msg)
          if (remainSeconds > 0) {
            this.startResetCooldown(remainSeconds)
          }
          this.showMessage(result.msg || '获取验证码失败')
          return
        }

        this.resetCode = result.data
        this.resetForm.code = ''
        this.startResetCooldown(60)
        this.showMessage('验证码已生成，请手动输入，并在 1 分钟内完成重置', 'success')
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    async handleResetPassword() {
      if (!this.resetForm.username.trim()) {
        this.showMessage('请先输入账号')
        return
      }
      if (!this.resetForm.code.trim()) {
        this.showMessage('请先输入验证码')
        return
      }
      if (!this.resetForm.newPassword.trim()) {
        this.showMessage('请先输入新密码')
        return
      }

      this.loading = true
      try {
        const result = await resetPassword(this.resetForm)
        if (result.code !== 1) {
          this.showMessage(result.msg || '重置密码失败')
          return
        }

        this.form.username = this.resetForm.username
        this.form.password = ''
        this.closeResetMode()
        this.showMessage('密码重置成功，请使用新密码登录', 'success')
      } catch (error) {
        this.showMessage('请求失败，请确认后端服务已经启动')
      } finally {
        this.loading = false
      }
    },
    startResetCooldown(seconds) {
      this.clearResetTimer()
      this.resetCooldown = Math.max(0, Number(seconds) || 0)

      if (this.resetCooldown <= 0) return

      this.resetTimer = window.setInterval(() => {
        if (this.resetCooldown <= 1) {
          this.clearResetTimer()
          return
        }

        this.resetCooldown -= 1
      }, 1000)
    },
    clearResetTimer() {
      if (this.resetTimer) {
        window.clearInterval(this.resetTimer)
        this.resetTimer = null
      }
      this.resetCooldown = 0
    },
    parseCooldownSeconds(message) {
      if (!message) return 0
      const matched = String(message).match(/请(\d+)秒后再试/)
      return matched ? Number(matched[1]) : 0
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 480px;
  gap: 28px;
  align-items: center;
  padding: 42px;
  background:
    linear-gradient(135deg, rgba(17, 21, 29, 0.94), rgba(31, 36, 46, 0.96)),
    #11151d;
}

.brand-stage {
  min-height: 620px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 24px;
  padding: 52px;
  color: #ffffff;
}

.hero-logo {
  width: min(300px, 52vw);
  height: auto;
  object-fit: contain;
  filter: drop-shadow(0 30px 70px rgba(0, 0, 0, 0.38));
}

.stage-copy {
  max-width: 620px;
}

.eyebrow {
  color: #fca5a5;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.stage-copy h1 {
  margin: 12px 0 0;
  font-size: clamp(36px, 7vw, 70px);
  line-height: 1.05;
  letter-spacing: 0;
}

.slogan {
  margin: 12px 0 0;
  color: #ffffff;
  font-size: clamp(22px, 3vw, 34px);
  font-weight: 800;
  line-height: 1.2;
}

.stage-copy p:not(.slogan) {
  max-width: 560px;
  margin: 18px 0 0;
  color: #d1d5db;
  font-size: 18px;
  line-height: 1.8;
}

.feature-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feature-strip span {
  padding: 9px 12px;
  border: 1px solid rgba(255, 255, 255, 0.13);
  border-radius: 8px;
  color: #e5e7eb;
  background: rgba(255, 255, 255, 0.06);
}

.login-card {
  display: grid;
  gap: 22px;
  padding: 34px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 30px 90px rgba(0, 0, 0, 0.32);
}

.card-title h2 {
  margin: 12px 0 0;
  font-size: 28px;
}

.card-title p,
.demo-tip {
  color: #6b7280;
  line-height: 1.6;
}

.login-form {
  display: grid;
  gap: 14px;
}

label {
  display: grid;
  gap: 7px;
  color: #374151;
  font-weight: 700;
}

.code-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.code-btn {
  white-space: nowrap;
}

.code-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border: 1px solid #fecdd3;
  border-radius: 8px;
  background: #fff1f2;
}

.code-card span {
  color: #991b1b;
  font-size: 12px;
  font-weight: 800;
}

.code-card strong {
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.code-card small {
  color: #6b7280;
}

.cooldown-card {
  padding: 12px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  color: #9a3412;
  background: #fff7ed;
  font-size: 14px;
}

.message {
  min-height: 22px;
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

.primary-action {
  margin-top: 4px;
}

.btn.secondary {
  background: #ffffff;
  color: #111827;
  border: 1px solid #d1d5db;
}

.text-action {
  width: fit-content;
  border: 0;
  padding: 0;
  color: #b91c1c;
  background: transparent;
  font-weight: 800;
}

.text-action:hover {
  text-decoration: underline;
}

@media (max-width: 1000px) {
  .login-page {
    grid-template-columns: 1fr;
    padding: 22px;
  }

  .brand-stage {
    min-height: auto;
    padding: 18px 4px;
  }

  .hero-logo {
    width: 180px;
  }
}

@media (max-width: 560px) {
  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
