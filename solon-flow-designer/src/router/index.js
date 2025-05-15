import { createRouter, createWebHistory,createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/design',
    name: 'Design',
    component: () => import('@/views/designer/Index.vue')
  },
  {
    path: '/import',
    name: 'Import',
    component: () => import('@/views/designer/import/Index.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router