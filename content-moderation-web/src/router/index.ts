import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据概览' }
      },
      {
        path: 'video/tasks',
        name: 'VideoTasks',
        component: () => import('@/views/video/TaskList.vue'),
        meta: { title: '分析任务' }
      },
      {
        path: 'video/new',
        name: 'VideoNew',
        component: () => import('@/views/video/TaskNew.vue'),
        meta: { title: '发起分析' }
      },
      {
        path: 'video/:callId',
        name: 'VideoDetail',
        component: () => import('@/views/video/TaskDetail.vue'),
        meta: { title: '分析详情', hidden: true }
      },
      {
        path: 'violations',
        name: 'Violations',
        component: () => import('@/views/violations/EventList.vue'),
        meta: { title: '违规事件' }
      },
      {
        path: 'violations/:eventId',
        name: 'ViolationDetail',
        component: () => import('@/views/violations/EventDetail.vue'),
        meta: { title: '事件详情', hidden: true }
      },
      {
        path: 'profile',
        name: 'ProfileList',
        component: () => import('@/views/profile/ProfileList.vue'),
        meta: { title: '服务质量画像' }
      },
      {
        path: 'profile/:userId',
        name: 'ProfileDetail',
        component: () => import('@/views/profile/ProfileDetail.vue'),
        meta: { title: '画像详情', hidden: true }
      },
      {
        path: 'tags',
        name: 'TagRepository',
        component: () => import('@/views/profile/TagRepository.vue'),
        meta: { title: '治理级标签' }
      },
      {
        path: 'settings/api',
        name: 'SettingsApi',
        component: () => import('@/views/settings/ApiConfig.vue'),
        meta: { title: 'API 配置' }
      },
      {
        path: 'settings/system',
        name: 'SettingsSystem',
        component: () => import('@/views/settings/SystemInfo.vue'),
        meta: { title: '系统信息' }
      },
      {
        path: 'settings/skills',
        name: 'SkillFactory',
        component: () => import('@/views/settings/SkillFactory.vue'),
        meta: { title: 'Skill 注册工厂' }
      },
      {
        path: 'settings/policies',
        name: 'PolicyOverview',
        component: () => import('@/views/settings/PolicyOverview.vue'),
        meta: { title: 'Policy 配置中心' }
      },
      {
        path: 'settings/policies/editor',
        name: 'PolicyCenter',
        component: () => import('@/views/settings/PolicyCenter.vue'),
        meta: { title: 'Policy 编排编辑', hidden: true }
      },
      {
        path: 'settings/prompts',
        name: 'SettingsPrompts',
        redirect: '/settings/api',
        meta: { title: '系统设置', hidden: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
