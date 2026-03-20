import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 侧边栏状态
  const collapsed = ref(false)
  
  // 主题模式
  const darkMode = ref(false)

  // 切换侧边栏
  const toggleCollapsed = () => {
    collapsed.value = !collapsed.value
  }

  // 切换主题
  const toggleDarkMode = () => {
    darkMode.value = !darkMode.value
    if (darkMode.value) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  return {
    collapsed,
    darkMode,
    toggleCollapsed,
    toggleDarkMode
  }
})
