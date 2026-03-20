<template>
  <el-container class="app-container">
    <div v-if="isMobile && isSidebarOpen" class="sidebar-mask" @click="isSidebarOpen = false"></div>
    <el-aside :width="sidebarWidth" class="sidebar" :class="{ collapsed: isCollapse, mobile: isMobile, 'mobile-open': isSidebarOpen }">
      <div class="logo-section">
        <div class="logo-wrapper" @click="goRoute('/dashboard')">
          <div class="logo-icon">
            <svg viewBox="0 0 48 48" width="32" height="32">
              <defs>
                <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#0ea5e9;stop-opacity:1" />
                  <stop offset="100%" style="stop-color:#6366f1;stop-opacity:1" />
                </linearGradient>
              </defs>
              <path fill="url(#logoGradient)" d="M24 2L6 10v12c0 11 8 21 18 24 10-3 18-13 18-24V10L24 2zm0 4l14 6.3V22c0 8.8-6.2 17.4-14 20.3C16.2 39.4 10 30.8 10 22V12.3L24 6z"/>
              <path fill="url(#logoGradient)" d="M24 14c-4.4 0-8 3.6-8 8 0 4.4 3.6 8 8 8s8-3.6 8-8c0-4.4-3.6-8-8-8zm0 12c-2.2 0-4-1.8-4-4s1.8-4 4-4 4 1.8 4 4-1.8 4-4 4z"/>
            </svg>
          </div>
          <span v-show="!isCollapse" class="logo-text">内容治理系统</span>
        </div>
      </div>

      <button
        class="quick-search"
        :class="{ pulse: searchPulse, collapsed: isCollapse }"
          aria-keyshortcuts="Ctrl+K Meta+K"
        aria-label="快捷搜索"
        @click="searchPulse = true"
      >
        <el-icon><Search /></el-icon>
        <span v-show="!isCollapse">搜索功能 (Ctrl+K)</span>
      </button>

      <nav ref="navListRef" class="nav-list" aria-label="主导航">
        <div class="active-pill" :style="activePillStyle"></div>
        <div v-for="(group, groupIndex) in menuGroups" :key="group.title" class="nav-group">
          <div v-show="!isCollapse" class="group-title">{{ group.title }}</div>
          <button
            v-for="(item, itemIndex) in group.items"
            :key="item.route"
            :ref="(el) => setNavItemRef(item.route, el)"
            class="nav-item child-item"
            :class="{ active: isRouteActive(item.route), collapsed: isCollapse }"
            :style="{ animationDelay: `${(groupIndex * 3 + itemIndex) * 52}ms` }"
            :aria-current="isRouteActive(item.route) ? 'page' : undefined"
            :aria-label="item.label"
            @mousemove="handleMagneticMove($event)"
            @mouseleave="handleMagneticLeave($event)"
            @click="goRoute(item.route)"
          >
            <span class="nav-icon-wrap">
              <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
            </span>
            <span v-show="!isCollapse" class="menu-title">{{ item.label }}</span>
          </button>
        </div>
      </nav>

      <div class="profile-card">
        <button class="profile-trigger" @click="toggleProfileMenu">
          <el-avatar :size="34" class="user-avatar" :style="{ background: 'var(--gradient-primary)' }">管</el-avatar>
          <div v-show="!isCollapse" class="profile-texts">
            <span class="profile-name">管理员</span>
            <span class="profile-role">超级管理员</span>
          </div>
          <el-icon v-show="!isCollapse" class="profile-arrow" :class="{ open: profileMenuOpen }"><ArrowDown /></el-icon>
        </button>
        <transition name="profile-pop">
          <div v-if="profileMenuOpen && !isCollapse" class="profile-popover">
            <button class="pop-item">个人中心</button>
            <button class="pop-item">个人设置</button>
            <button class="pop-item danger">退出登录</button>
          </div>
        </transition>
      </div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <div class="collapse-btn" role="button" tabindex="0" aria-label="切换导航栏" @click="toggleSidebarState" @keydown.enter="toggleSidebarState" @keydown.space.prevent="toggleSidebarState">
            <el-icon :size="20">
              <component :is="isMobile ? Menu : (isCollapse ? Expand : Fold)" />
            </el-icon>
          </div>
          
          <div class="breadcrumb-divider"></div>

          <!-- 面包屑 -->
          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <!-- 通知 -->
          <div class="header-action">
            <el-badge :value="3" :max="99" class="action-badge">
              <el-button :icon="Bell" circle class="action-btn" />
            </el-badge>
          </div>
          
          <!-- 暗黑模式切换 -->
          <div class="header-action">
            <el-button :icon="isDark ? 'Sunny' : 'Moon'" circle class="action-btn" @click="toggleDarkMode" />
          </div>
          
          <!-- 用户菜单 -->
          <el-dropdown trigger="click">
            <div class="user-dropdown">
              <el-avatar :size="36" class="user-avatar" :style="{ background: 'var(--gradient-primary)' }">
                管
              </el-avatar>
              <div class="user-info">
                <span class="user-name">管理员</span>
                <span class="user-role">超级管理员</span>
              </div>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-menu">
                <el-dropdown-item>
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item>
                  <el-icon><Setting /></el-icon>
                  个人设置
                </el-dropdown-item>
                <el-dropdown-item divided>
                  <el-icon><SwitchButton /></el-icon>
                  <span class="logout-text">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区域 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DataAnalysis,
  VideoCamera,
  User,
  Setting,
  Files,
  Bell,
  Fold,
  Expand,
  Sunny,
  Moon,
  Search,
  Menu,
  Promotion,
  Warning,
  ArrowDown,
  SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const isCollapse = ref(false)
const isDark = ref(false)
const isMobile = ref(false)
const isSidebarOpen = ref(true)
const profileMenuOpen = ref(false)
const searchPulse = ref(false)
const navListRef = ref<HTMLElement | null>(null)
const navItemRefs = ref<Record<string, HTMLElement>>({})
const activePillStyle = ref<Record<string, string>>({ opacity: '0' })
const isSidebarTransitioning = ref(false)
let sidebarSyncTimer: number | null = null
const SIDEBAR_TRANSITION_MS = 460

type MenuItem = {
  label: string
  icon: any
  route: string
}

const menuGroups: Array<{ title: string; items: MenuItem[] }> = [
  {
    title: '总览',
    items: [{ label: '数据概览', icon: DataAnalysis, route: '/dashboard' }]
  },
  {
    title: '任务管理',
    items: [
      { label: '分析任务', icon: VideoCamera, route: '/video/tasks' },
      { label: '发起分析', icon: Promotion, route: '/video/new' },
      { label: '违规事件', icon: Warning, route: '/violations' }
    ]
  },
  {
    title: '数据仓库',
    items: [
      { label: '服务画像', icon: User, route: '/profile' },
      { label: '治理标签', icon: Files, route: '/tags' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { label: '系统设置', icon: Setting, route: '/settings/api' },
      { label: 'Prompt管理', icon: Files, route: '/settings/prompts' }
    ]
  }
]
const menuItems = computed(() => menuGroups.flatMap(group => group.items))

const sidebarWidth = computed(() => {
  if (isMobile.value) return '260px'
  return isCollapse.value ? '84px' : '286px'
})

function isProfilePath(path: string) {
  return /^\/profile(\/|$)/.test(path)
}

// 当前激活菜单
const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/video/')) return '/video/tasks'
  if (isProfilePath(path)) return '/profile'
  if (path.startsWith('/settings/prompts')) return '/settings/prompts'
  if (path.startsWith('/settings/')) return '/settings/api'
  return path
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    path: item.path,
    title: item.meta.title as string
  }))
})

// 切换暗黑模式
const toggleDarkMode = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
}

function isRouteActive(itemRoute: string) {
  if (itemRoute === '/video/tasks') return route.path === '/video/tasks' || /^\/video\/[^/]+$/.test(route.path) && route.path !== '/video/new'
  if (itemRoute === '/video/new') return route.path === '/video/new'
  if (itemRoute === '/profile') return isProfilePath(route.path)
  if (itemRoute === '/settings/prompts') return route.path.startsWith('/settings/prompts')
  if (itemRoute === '/settings/api') return route.path.startsWith('/settings/api')
  return route.path === itemRoute
}

function goRoute(path: string) {
  router.push(path)
  profileMenuOpen.value = false
  if (isMobile.value) isSidebarOpen.value = false
}

function setNavItemRef(path: string, el: unknown) {
  if (el instanceof HTMLElement) {
    navItemRefs.value[path] = el
  }
}

function updateActivePill() {
  if (isSidebarTransitioning.value) {
    activePillStyle.value = { opacity: '0' }
    return
  }
  const activeItem = menuItems.value.find(item => isRouteActive(item.route))
  if (!activeItem || !navListRef.value) {
    activePillStyle.value = { opacity: '0' }
    return
  }
  const el = navItemRefs.value[activeItem.route]
  if (!el) {
    activePillStyle.value = { opacity: '0' }
    return
  }
  activePillStyle.value = {
    transform: `translate(${el.offsetLeft}px, ${el.offsetTop}px)`,
    width: `${el.offsetWidth}px`,
    height: `${el.offsetHeight}px`,
    opacity: '1'
  }
}

function scheduleSidebarSync() {
  if (sidebarSyncTimer !== null) {
    window.clearTimeout(sidebarSyncTimer)
  }
  sidebarSyncTimer = window.setTimeout(() => {
    isSidebarTransitioning.value = false
    updateActivePill()
    sidebarSyncTimer = null
  }, SIDEBAR_TRANSITION_MS)
}

function handleMagneticMove(event: MouseEvent) {
  const target = event.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width - 0.5) * 8
  const y = ((event.clientY - rect.top) / rect.height - 0.5) * 8
  target.style.setProperty('--mx', `${x.toFixed(2)}px`)
  target.style.setProperty('--my', `${y.toFixed(2)}px`)
}

function handleMagneticLeave(event: MouseEvent) {
  const target = event.currentTarget as HTMLElement
  target.style.setProperty('--mx', '0px')
  target.style.setProperty('--my', '0px')
}

function toggleSidebarState() {
  if (isMobile.value) {
    isSidebarOpen.value = !isSidebarOpen.value
  } else {
    isSidebarTransitioning.value = true
    activePillStyle.value = { opacity: '0' }
    isCollapse.value = !isCollapse.value
    scheduleSidebarSync()
  }
}

function toggleProfileMenu() {
  profileMenuOpen.value = !profileMenuOpen.value
}

function handleResize() {
  const mobile = window.innerWidth < 992
  isMobile.value = mobile
  isSidebarOpen.value = !mobile
}

function handleGlobalKeydown(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    searchPulse.value = true
    window.setTimeout(() => {
      searchPulse.value = false
    }, 520)
  }
}

watch(
  () => route.path,
  async () => {
    await nextTick()
    updateActivePill()
  },
  { immediate: true }
)

watch(
  () => isCollapse.value,
  async () => {
    await nextTick()
    if (!isSidebarTransitioning.value) {
      updateActivePill()
    }
  }
)

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  window.addEventListener('keydown', handleGlobalKeydown)
  nextTick(updateActivePill)
})

onUnmounted(() => {
  if (sidebarSyncTimer !== null) {
    window.clearTimeout(sidebarSyncTimer)
    sidebarSyncTimer = null
  }
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('keydown', handleGlobalKeydown)
})
</script>

<style scoped>
/* ========== 整体布局 ========== */
.app-container {
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

/* ========== 侧边栏（悬浮毛玻璃） ========== */
.sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 110;
  background: rgba(15, 23, 42, 0.36);
  backdrop-filter: blur(2px);
}

.sidebar {
  position: relative;
  margin: 12px;
  height: calc(100vh - 24px);
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(18, 40, 69, 0.86) 0%, rgba(18, 40, 69, 0.72) 100%);
  border: 1px solid rgba(148, 163, 184, 0.26);
  backdrop-filter: blur(22px) saturate(160%);
  overflow: hidden;
  box-shadow: 0 14px 30px rgba(2, 6, 23, 0.32), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  z-index: 120;
  transition: width 420ms cubic-bezier(0.24, 1.1, 0.24, 1), transform 360ms cubic-bezier(0.24, 1.1, 0.24, 1);
  display: flex;
  flex-direction: column;
  padding: 8px 10px 14px;
}

.sidebar.collapsed {
  padding-inline: 8px;
}

.sidebar.mobile {
  position: fixed;
  top: 0;
  left: 0;
  margin: 0;
  height: 100vh;
  border-radius: 0 20px 20px 0;
  transform: translateX(-102%);
}

.sidebar.mobile.mobile-open {
  transform: translateX(0);
}

.logo-section {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 10px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  cursor: pointer;
}

.logo-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.08);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.08);
}

.logo-text {
  color: rgba(248, 250, 252, 0.94);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.2px;
}

.quick-search {
  margin: 8px 6px 10px;
  height: 40px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(241, 245, 249, 0.9);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 10px;
  padding: 0 12px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.quick-search.collapsed {
  justify-content: center;
  padding: 0;
}

.quick-search.pulse::after {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 2px solid rgba(56, 189, 248, 0.55);
  animation: search-ripple 520ms ease-out;
}

.nav-list {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 6px;
}

.nav-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.group-title {
  font-size: 12px;
  color: rgba(241, 245, 249, 0.66);
  font-weight: 600;
  padding: 6px 12px 2px;
}

.active-pill {
  position: absolute;
  top: 0;
  left: 0;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.95), rgba(99, 102, 241, 0.88));
  box-shadow: 0 8px 22px rgba(14, 165, 233, 0.32);
  transition: transform 420ms cubic-bezier(0.22, 1, 0.36, 1), width 420ms cubic-bezier(0.22, 1, 0.36, 1), height 420ms cubic-bezier(0.22, 1, 0.36, 1), opacity 220ms ease;
}

.nav-item {
  position: relative;
  z-index: 1;
  height: 46px;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: rgba(241, 245, 249, 0.74);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px;
  cursor: pointer;
  transition: color 180ms ease, background-color 180ms ease;
  animation: nav-in 380ms cubic-bezier(0.16, 1, 0.3, 1) both;
  transform: translateY(0);
}

.child-item:not(.collapsed) {
  margin-left: 10px;
}

.nav-item.collapsed {
  justify-content: center;
  padding: 0;
}

.nav-item:hover {
  color: rgba(255, 255, 255, 0.95);
  background-color: rgba(255, 255, 255, 0.06);
}

.nav-item.active {
  color: #fff;
}

.nav-icon-wrap {
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transform: translate3d(var(--mx, 0px), var(--my, 0px), 0);
  transition: transform 160ms ease-out;
}

.menu-icon {
  font-size: 18px;
  filter: drop-shadow(0 0 0 rgba(56, 189, 248, 0));
  transition: filter 220ms ease, color 220ms ease;
}

.nav-item:hover .menu-icon,
.nav-item.active .menu-icon {
  filter: drop-shadow(0 0 8px rgba(56, 189, 248, 0.35));
}

.menu-title {
  flex: 1;
  text-align: left;
  font-size: 14px;
  font-weight: 520;
  white-space: nowrap;
}

.profile-card {
  position: relative;
  margin-top: 8px;
  padding: 6px;
}

.profile-trigger {
  width: 100%;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  color: rgba(241, 245, 249, 0.92);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  cursor: pointer;
}

.profile-texts {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 1px;
  flex: 1;
}

.profile-name {
  font-size: 13px;
  font-weight: 600;
}

.profile-role {
  font-size: 11px;
  color: rgba(241, 245, 249, 0.64);
}

.profile-arrow {
  color: rgba(241, 245, 249, 0.7);
  transition: transform 180ms ease;
}

.profile-arrow.open {
  transform: rotate(180deg);
}

.profile-popover {
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 64px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(15, 23, 42, 0.92);
  box-shadow: 0 12px 28px rgba(2, 6, 23, 0.34);
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pop-item {
  border: none;
  height: 34px;
  border-radius: 10px;
  text-align: left;
  padding: 0 10px;
  background: transparent;
  color: rgba(241, 245, 249, 0.88);
  cursor: pointer;
}

.pop-item:hover {
  background: rgba(255, 255, 255, 0.08);
}

.pop-item.danger {
  color: #fca5a5;
}

.profile-pop-enter-active,
.profile-pop-leave-active {
  transition: all 220ms ease;
}

.profile-pop-enter-from,
.profile-pop-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}

/* ========== 主内容区 ========== */
.main-container {
  display: flex;
  flex-direction: column;
  background-color: var(--bg-body);
  overflow: hidden;
}

/* ========== 顶部导航栏 ========== */
.header {
  background: var(--bg-header);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--spacing-2xl);
  box-shadow: var(--shadow-xs);
  height: var(--header-height);
  flex-shrink: 0;
  border-bottom: 1px solid var(--border-light);
  z-index: 50;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
}

.collapse-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.collapse-btn:hover {
  background-color: var(--bg-hover);
  color: var(--primary-600);
}

.breadcrumb-divider {
  width: 1px;
  height: 24px;
  background: var(--border-light);
}

:deep(.el-breadcrumb) {
  margin-left: var(--spacing-md);
}

:deep(.el-breadcrumb__item) {
  font-size: var(--text-sm);
}

:deep(.el-breadcrumb__inner) {
  color: var(--text-secondary);
  transition: color var(--transition-fast);
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--text-primary);
  font-weight: var(--font-medium);
}

:deep(.el-breadcrumb__separator) {
  color: var(--border-heavy);
}

/* ========== 右侧操作区 ========== */
.header-right {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.header-action {
  display: flex;
  align-items: center;
}

.action-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background-color: var(--bg-hover);
  color: var(--primary-600);
}

.action-badge {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-badge__content.is-fixed) {
  top: 4px;
  right: 4px;
  height: 18px;
  min-width: 18px;
  padding: 0 4px;
  font-size: 10px;
  border: 2px solid var(--bg-header);
}

/* ========== 用户下拉菜单 ========== */
.user-dropdown {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-xs) var(--spacing-md);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-dropdown:hover {
  background-color: var(--bg-hover);
}

.user-avatar {
  flex-shrink: 0;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: #fff;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-name {
  color: var(--text-primary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
}

.user-role {
  color: var(--text-secondary);
  font-size: 12px;
}

.dropdown-icon {
  color: var(--text-secondary);
  font-size: 14px;
  transition: transform var(--transition-fast);
}

.user-dropdown:hover .dropdown-icon {
  transform: rotate(180deg);
}

/* 用户菜单 */
.user-menu {
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  padding: var(--spacing-sm) 0;
  min-width: 180px;
}

:deep(.el-dropdown-menu__item) {
  padding: var(--spacing-md) var(--spacing-xl);
  font-size: var(--text-sm);
  color: var(--text-regular);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: var(--bg-hover);
  color: var(--primary-600);
}

:deep(.el-dropdown-menu__item .el-icon) {
  font-size: 16px;
  color: var(--text-secondary);
}

:deep(.el-dropdown-menu__item:hover .el-icon) {
  color: var(--primary-600);
}

.logout-text {
  color: var(--danger);
}

/* ========== 主内容区 ========== */
.main-content {
  padding: var(--spacing-xl);
  overflow-y: auto;
  flex: 1;
}

/* ========== 页面过渡动画 ========== */
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--transition-slow), transform var(--transition-slow);
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

@keyframes nav-in {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes search-ripple {
  from {
    transform: scale(0.5);
    opacity: 0.75;
  }
  to {
    transform: scale(12);
    opacity: 0;
  }
}

.nav-item:focus-visible,
.quick-search:focus-visible,
.profile-trigger:focus-visible,
.pop-item:focus-visible {
  outline: 2px solid rgba(125, 211, 252, 0.9);
  outline-offset: 2px;
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .header {
    padding: 0 var(--spacing-lg);
  }
  
  .user-info {
    display: none;
  }
  
  .breadcrumb-divider {
    display: none;
  }
  
  :deep(.el-breadcrumb) {
    display: none;
  }

  .sidebar {
    width: 260px !important;
  }
}
</style>
