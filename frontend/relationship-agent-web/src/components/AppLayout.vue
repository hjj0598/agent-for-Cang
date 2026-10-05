<template>
  <div class="app-shell" :class="{ 'nav-open': mobileNavOpen }">
    <div v-if="mobileNavOpen" class="sidebar-backdrop" @click="closeMobileNav"></div>

    <aside class="sidebar" :class="{ open: mobileNavOpen }">
      <div class="brand">
        <img class="brand-logo" src="@/assets/brand-logo.png" alt="品牌标识">
        <div>
          <h1 class="brand-title">识忆承缘</h1>
          <p class="brand-subtitle">We will must pop on the street</p>
        </div>
        <button class="sidebar-close" type="button" aria-label="关闭导航菜单" @click="closeMobileNav">
          ×
        </button>
      </div>
      <nav class="sidebar-nav">
        <div class="nav-group">
          <span class="nav-group-label">工作台</span>
          <router-link class="nav-link" to="/dashboard" @click.native="closeMobileNav">
            <span class="nav-icon">⌂</span>
            <span>首页</span>
          </router-link>
          <router-link class="nav-link" to="/chat" @click.native="closeMobileNav">
            <span class="nav-icon">✦</span>
            <span>AI 聊天</span>
          </router-link>
          <router-link class="nav-link" to="/persons" @click.native="closeMobileNav">
            <span class="nav-icon">人</span>
            <span>人物关系</span>
          </router-link>
          <router-link class="nav-link" to="/knowledge" @click.native="closeMobileNav">
            <span class="nav-icon">知</span>
            <span>知识库</span>
          </router-link>
          <router-link class="nav-link" to="/images" @click.native="closeMobileNav">
            <span class="nav-icon">图</span>
            <span>图片分析</span>
          </router-link>
        </div>

        <div class="nav-group">
          <span class="nav-group-label">协作</span>
          <router-link class="nav-link" to="/feedback" @click.native="closeMobileNav">
            <span class="nav-icon">意</span>
            <span>意见收集箱</span>
          </router-link>
          <router-link class="nav-link" to="/roadmap" @click.native="closeMobileNav">
            <span class="nav-icon">迭</span>
            <span>迭代计划</span>
          </router-link>
        </div>

        <div class="nav-group">
          <span class="nav-group-label">系统</span>
          <router-link v-if="isAdmin" class="nav-link admin-nav" to="/admin" @click.native="closeMobileNav">
            <span class="nav-icon">管</span>
            <span>管理台</span>
          </router-link>
          <router-link class="nav-link" to="/profile" @click.native="closeMobileNav">
            <span class="nav-icon">我</span>
            <span>个人中心</span>
          </router-link>
          <button class="nav-link logout-link" type="button" @click="showLogoutConfirm = true">
            <span class="nav-icon">↪</span>
            <span>退出登录</span>
          </button>
        </div>
      </nav>

      <div v-if="currentUser" class="sidebar-account">
        <div class="account-avatar">{{ accountInitial }}</div>
        <div class="account-copy">
          <strong>{{ currentUser.nickname || currentUser.username }}</strong>
          <span>{{ isAdmin ? '管理员账号' : '个人账号' }}</span>
        </div>
        <span class="account-status"></span>
      </div>
    </aside>

    <main class="main">
      <div class="mobile-topbar">
        <button class="mobile-topbar-menu" type="button" aria-label="打开导航菜单" @click="mobileNavOpen = true">
          <span></span>
          <span></span>
          <span></span>
        </button>
        <div>
          <strong>识忆承缘</strong>
          <span>AI Agent Workspace</span>
        </div>
      </div>
      <slot />
    </main>

    <div v-if="showLogoutConfirm" class="modal-mask" @click.self="showLogoutConfirm = false">
      <section class="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="logout-title">
        <h3 id="logout-title">确认退出登录？</h3>
        <p>退出后需要重新登录，当前页面的未保存输入可能会丢失。</p>
        <div class="dialog-actions">
          <button class="dialog-btn secondary" type="button" @click="showLogoutConfirm = false">取消</button>
          <button class="dialog-btn danger" type="button" @click="logout">确认退出</button>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { clearToken } from '@/store/user'
import { getCurrentUser } from '@/api/user'

export default {
  name: 'AppLayout',
  data() {
    return {
      showLogoutConfirm: false,
      currentUser: null,
      mobileNavOpen: false
    }
  },
  computed: {
    isAdmin() {
      return this.currentUser && this.currentUser.role === 'ADMIN'
    },
    accountInitial() {
      const name = this.currentUser && (this.currentUser.nickname || this.currentUser.username)
      return name ? String(name).slice(0, 1).toUpperCase() : '我'
    }
  },
  watch: {
    '$route.fullPath'() {
      this.closeMobileNav()
    }
  },
  created() {
    this.loadCurrentUser()
  },
  methods: {
    async loadCurrentUser() {
      try {
        const result = await getCurrentUser()
        if (result.code === 1) {
          this.currentUser = result.data || null
        }
      } catch (error) {
        this.currentUser = null
      }
    },
    logout() {
      clearToken()
      this.closeMobileNav()
      this.$router.push('/login')
    },
    closeMobileNav() {
      this.mobileNavOpen = false
    }
  }
}
</script>

<style scoped>
.logout-link {
  width: 100%;
  border: 0;
  text-align: left;
  background: transparent;
}

.sidebar-close,
.mobile-topbar {
  display: none;
}

.nav-group {
  margin-bottom: 20px;
}

.nav-group-label {
  display: block;
  margin: 0 10px 8px;
  color: #6b7280;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.nav-icon {
  display: grid;
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 7px;
  color: #d7dde7;
  background: rgba(255, 255, 255, 0.05);
  font-size: 12px;
  font-weight: 800;
}

.nav-link.router-link-active .nav-icon,
.nav-link:hover .nav-icon {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.14);
}

.sidebar-account {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
}

.account-avatar {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  color: #ffffff;
  background: linear-gradient(135deg, #ef343d, #c91f2b);
  font-size: 14px;
  font-weight: 800;
}

.account-copy {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.account-copy strong,
.account-copy span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-copy strong {
  color: #ffffff;
  font-size: 13px;
}

.account-copy span {
  color: #9ca3af;
  font-size: 11px;
}

.account-status {
  width: 7px;
  height: 7px;
  margin-left: auto;
  border-radius: 999px;
  background: #34d399;
  box-shadow: 0 0 0 4px rgba(52, 211, 153, 0.12);
}

.admin-nav {
  border: 1px solid rgba(239, 52, 61, 0.36);
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(17, 24, 39, 0.48);
}

.confirm-dialog {
  width: min(420px, 100%);
  padding: 22px;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 80px rgba(15, 23, 42, 0.28);
  animation: dialog-in 0.16s ease-out;
}

.confirm-dialog h3 {
  margin: 0;
  color: #111827;
  font-size: 20px;
}

.confirm-dialog p {
  margin: 10px 0 0;
  color: #6b7280;
  line-height: 1.6;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.dialog-btn {
  border: 0;
  border-radius: 8px;
  padding: 9px 14px;
  color: #ffffff;
  background: #2563eb;
}

.dialog-btn.secondary {
  color: #374151;
  background: #e5e7eb;
}

.dialog-btn.danger {
  background: #dc2626;
}

@keyframes dialog-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (max-width: 900px) {
  .sidebar-close,
  .mobile-topbar {
    display: grid;
  }

  .sidebar-close {
    flex: 0 0 auto;
    width: 30px;
    height: 30px;
    place-items: center;
    margin-left: auto;
    border: 1px solid rgba(255, 255, 255, 0.12);
    border-radius: 7px;
    color: #ffffff;
    background: rgba(255, 255, 255, 0.06);
    font-size: 20px;
    line-height: 1;
  }

  .sidebar {
    position: fixed;
    z-index: 70;
    top: 0;
    bottom: 0;
    left: 0;
    width: min(310px, 86vw);
    height: 100vh;
    overflow-y: auto;
    transform: translateX(-102%);
    transition: transform 0.22s ease;
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .sidebar-backdrop {
    position: fixed;
    z-index: 60;
    inset: 0;
    background: rgba(15, 23, 42, 0.48);
    backdrop-filter: blur(3px);
  }

  .mobile-topbar-menu span {
    display: block;
    width: 18px;
    height: 2px;
    border-radius: 999px;
    background: #111827;
  }

  .mobile-topbar {
    display: flex;
    align-items: center;
    gap: 12px;
    min-height: 54px;
    margin: -4px -4px 18px;
    padding: 0 0 0 54px;
  }

  .mobile-topbar-menu {
    display: grid;
    flex: 0 0 42px;
    width: 42px;
    height: 42px;
    place-items: center;
    gap: 4px;
    padding: 10px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #ffffff;
  }

  .mobile-topbar strong,
  .mobile-topbar span {
    display: block;
  }

  .mobile-topbar strong {
    color: #111827;
    font-size: 14px;
  }

  .mobile-topbar span {
    margin-top: 2px;
    color: #9ca3af;
    font-size: 11px;
  }
}
</style>
