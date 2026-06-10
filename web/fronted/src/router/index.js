import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '实时监控' }
  },
  {
    path: '/sensor-history',
    name: 'SensorHistory',
    component: () => import('@/views/SensorHistory.vue'),
    meta: { title: '传感器历史' }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/Statistics.vue'),
    meta: { title: '统计分析' }
  },
  {
    path: '/alerts',
    name: 'AlertCenter',
    component: () => import('@/views/AlertCenter.vue'),
    meta: { title: '告警中心' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '番茄监测'} - 番茄长势实时监测系统`
  next()
})

export default router
