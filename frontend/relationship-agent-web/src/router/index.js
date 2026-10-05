import Vue from 'vue'
import VueRouter from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import ChatView from '@/views/ChatView.vue'
import PersonView from '@/views/PersonView.vue'
import PersonDetailView from '@/views/PersonDetailView.vue'
import KnowledgeView from '@/views/KnowledgeView.vue'
import ImageAnalyzeView from '@/views/ImageAnalyzeView.vue'
import ProfileView from '@/views/ProfileView.vue'
import DashboardView from '@/views/DashboardView.vue'
import RoadmapView from '@/views/RoadmapView.vue'
import FeedbackView from '@/views/FeedbackView.vue'
import AdminView from '@/views/AdminView.vue'
import { getToken } from '@/store/user'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: LoginView
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: DashboardView
  },
  {
    path: '/chat',
    name: 'Chat',
    component: ChatView
  },
  {
    path: '/persons',
    name: 'Persons',
    component: PersonView
  },
  {
    path: '/persons/:id',
    name: 'PersonDetail',
    component: PersonDetailView
  },
  {
    path: '/knowledge',
    name: 'Knowledge',
    component: KnowledgeView
  },
  {
    path: '/images',
    name: 'ImageAnalyze',
    component: ImageAnalyzeView
  },
  {
    path: '/profile',
    name: 'Profile',
    component: ProfileView
  },
  {
    path: '/roadmap',
    name: 'Roadmap',
    component: RoadmapView
  },
  {
    path: '/feedback',
    name: 'Feedback',
    component: FeedbackView
  },
  {
    path: '/admin',
    name: 'Admin',
    component: AdminView
  }
]

const router = new VueRouter({
  mode: 'history',
  routes
})

router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next()
    return
  }

  if (!getToken()) {
    next('/login')
    return
  }

  next()
})

export default router
