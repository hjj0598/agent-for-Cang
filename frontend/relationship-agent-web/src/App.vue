<template>
  <div id="app">
    <transition name="page-fade" mode="out-in">
      <router-view :key="$route.fullPath" />
    </transition>
  </div>
</template>

<script>
export default {
  name: 'App',
  mounted() {
    window.addEventListener('auth-expired', this.handleAuthExpired)
  },
  beforeDestroy() {
    window.removeEventListener('auth-expired', this.handleAuthExpired)
  },
  methods: {
    handleAuthExpired(event) {
      if (this.$route.path === '/login') return

      const redirect = event && event.detail && event.detail.redirect
      this.$router.replace({
        path: '/login',
        query: redirect && redirect !== '/login'
          ? { redirect }
          : undefined
      }).catch(() => {})
    }
  }
}
</script>
