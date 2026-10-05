<template>
  <section class="brand-video-card" :class="{ compact, 'sound-on': !muted }">
    <video
      ref="video"
      class="brand-video"
      src="/media/company-video.mp4"
      autoplay
      loop
      playsinline
      preload="metadata"
      :controls="!muted"
      :muted="muted"
      @canplay="playMuted"
      @play="playing = true"
      @pause="playing = false"
    ></video>

    <div class="video-shade"></div>

    <div class="brand-mark">
      <img src="@/assets/brand-logo.png" alt="品牌标识">
    </div>

    <div class="video-content">
      <span class="video-label">Company Spirit</span>
      <strong>We will must pop on the street</strong>
      <p>{{ muted ? '当前为静音展示，点击按钮即可开启原视频声音。' : '声音已开启，再次点击可切回静音。' }}</p>
    </div>

    <div class="video-actions">
      <button class="sound-toggle primary" type="button" @click.stop="enableSound">
        {{ muted ? '播放并开启声音' : '声音播放中' }}
      </button>
      <button class="sound-toggle" type="button" @click.stop="muteVideo">
        静音播放
      </button>
    </div>
  </section>
</template>

<script>
export default {
  name: 'BrandVideo',
  props: {
    compact: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      muted: true,
      playing: false
    }
  },
  mounted() {
    this.playMuted()
  },
  methods: {
    playMuted() {
      const video = this.$refs.video
      if (!video) return

      video.muted = true
      video.volume = 0
      video.play().catch(() => {})
    },
    enableSound() {
      const video = this.$refs.video
      if (!video) return

      this.muted = false
      video.removeAttribute('muted')
      video.defaultMuted = false
      video.muted = false
      video.volume = 1
      if (video.paused) {
        video.currentTime = video.currentTime || 0
      }
      video.play().catch(() => {
        this.muted = true
        video.muted = true
      })
    },
    muteVideo() {
      const video = this.$refs.video
      if (!video) return

      this.muted = true
      video.setAttribute('muted', 'muted')
      video.defaultMuted = true
      video.muted = true
      video.volume = 0
      video.play().catch(() => {})
    }
  }
}
</script>

<style scoped>
.brand-video-card {
  position: relative;
  min-height: 300px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: #0d1118;
  box-shadow: 0 22px 60px rgba(0, 0, 0, 0.24);
}

.brand-video-card.compact {
  min-height: 230px;
}

.brand-video-card.sound-on {
  border-color: rgba(239, 52, 61, 0.55);
  box-shadow: 0 24px 70px rgba(239, 52, 61, 0.18);
}

.brand-video {
  width: 100%;
  height: 100%;
  min-height: inherit;
  display: block;
  object-fit: cover;
  transform: scale(1.02);
}

.video-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(13, 17, 24, 0.92), rgba(13, 17, 24, 0.22)),
    linear-gradient(0deg, rgba(0, 0, 0, 0.42), transparent 58%);
}

.brand-mark {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(5, 7, 10, 0.76);
  backdrop-filter: blur(10px);
}

.brand-mark img {
  width: 44px;
  height: 44px;
  object-fit: contain;
  animation: brand-pulse 3.2s ease-in-out infinite;
}

.video-content {
  position: absolute;
  left: 20px;
  bottom: 72px;
  right: 20px;
  display: grid;
  gap: 8px;
  color: #ffffff;
}

.video-label {
  width: fit-content;
  padding: 5px 8px;
  border-radius: 999px;
  color: #fecdd3;
  background: rgba(239, 52, 61, 0.24);
  font-size: 12px;
  font-weight: 800;
}

.video-content strong {
  max-width: 760px;
  font-size: clamp(22px, 4vw, 34px);
  line-height: 1.15;
  letter-spacing: 0;
  text-shadow: 0 12px 28px rgba(0, 0, 0, 0.35);
}

.video-content p {
  max-width: 520px;
  margin: 0;
  color: #d1d5db;
  line-height: 1.5;
  font-size: 13px;
}

.video-actions {
  position: absolute;
  left: 20px;
  right: 20px;
  bottom: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sound-toggle {
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  padding: 8px 11px;
  color: #ffffff;
  background: rgba(17, 24, 39, 0.72);
  backdrop-filter: blur(10px);
}

.sound-toggle.primary {
  background: rgba(239, 52, 61, 0.88);
}

.sound-toggle:hover {
  background: rgba(239, 52, 61, 0.96);
}

@keyframes brand-pulse {
  0%,
  100% {
    transform: scale(1);
    filter: drop-shadow(0 0 0 rgba(239, 52, 61, 0));
  }

  50% {
    transform: scale(1.06);
    filter: drop-shadow(0 0 14px rgba(239, 52, 61, 0.42));
  }
}

@media (max-width: 720px) {
  .video-content {
    bottom: 94px;
  }
}
</style>
