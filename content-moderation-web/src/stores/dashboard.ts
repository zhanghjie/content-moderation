import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface DashboardStatistics {
  totalUsers: number
  highRiskUsers: number
  mediumRiskUsers: number
  lowRiskUsers: number
  trends: {
    totalUsers: number
    highRiskUsers: number
    mediumRiskUsers: number
    lowRiskUsers: number
  }
}

export interface RiskDistribution {
  high: number
  medium: number
  low: number
}

export interface ViolationItem {
  violationName: string
  confidence: number
  createdAt: string
}

export interface HighRiskUser {
  userId: number
  trustScore: number
  totalViolations: number
}

export const useDashboardStore = defineStore('dashboard', () => {
  // State
  const statistics = ref<DashboardStatistics>({
    totalUsers: 0,
    highRiskUsers: 0,
    mediumRiskUsers: 0,
    lowRiskUsers: 0,
    trends: {
      totalUsers: 0,
      highRiskUsers: 0,
      mediumRiskUsers: 0,
      lowRiskUsers: 0
    }
  })

  const highRiskUsers = ref<HighRiskUser[]>([])
  const recentViolations = ref<ViolationItem[]>([])
  const loading = ref(false)
  const lastUpdated = ref<Date | null>(null)

  // Computed
  const totalRiskUsers = computed(() => {
    return statistics.value.highRiskUsers + statistics.value.mediumRiskUsers
  })

  const riskRate = computed(() => {
    if (statistics.value.totalUsers === 0) return 0
    return ((totalRiskUsers.value / statistics.value.totalUsers) * 100).toFixed(1)
  })

  // Actions
  async function fetchStatistics() {
    loading.value = true
    try {
      // TODO: Replace with actual API call
      // const response = await dashboardApi.getStatistics()
      // statistics.value = response
      
      // Mock data for demo
      statistics.value = {
        totalUsers: 12847,
        highRiskUsers: 256,
        mediumRiskUsers: 892,
        lowRiskUsers: 11699,
        trends: {
          totalUsers: 2.5,
          highRiskUsers: -12.3,
          mediumRiskUsers: -5.8,
          lowRiskUsers: 3.2
        }
      }
      
      lastUpdated.value = new Date()
    } catch (error) {
      console.error('Failed to fetch statistics:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchHighRiskUsers(limit = 5) {
    try {
      // TODO: Replace with actual API call
      // highRiskUsers.value = await dashboardApi.getHighRiskUsers(limit)
      
      // Mock data for demo
      highRiskUsers.value = [
        { userId: 10234, trustScore: 32, totalViolations: 8 },
        { userId: 10567, trustScore: 28, totalViolations: 12 },
        { userId: 10892, trustScore: 35, totalViolations: 6 },
        { userId: 11023, trustScore: 30, totalViolations: 9 },
        { userId: 11156, trustScore: 25, totalViolations: 15 }
      ]
    } catch (error) {
      console.error('Failed to fetch high risk users:', error)
      throw error
    }
  }

  async function fetchRecentViolations(limit = 5) {
    try {
      // TODO: Replace with actual API call
      // recentViolations.value = await dashboardApi.getRecentViolations(limit)
      
      // Mock data for demo
      recentViolations.value = [
        { violationName: '敏感言论', confidence: 0.95, createdAt: '2026-03-16T10:30:00Z' },
        { violationName: '广告推广', confidence: 0.88, createdAt: '2026-03-16T10:25:00Z' },
        { violationName: '不当内容', confidence: 0.92, createdAt: '2026-03-16T10:20:00Z' },
        { violationName: '敏感言论', confidence: 0.87, createdAt: '2026-03-16T10:15:00Z' },
        { violationName: '虚假信息', confidence: 0.91, createdAt: '2026-03-16T10:10:00Z' }
      ]
    } catch (error) {
      console.error('Failed to fetch recent violations:', error)
      throw error
    }
  }

  async function refreshAllData() {
    loading.value = true
    try {
      await Promise.all([
        fetchStatistics(),
        fetchHighRiskUsers(),
        fetchRecentViolations()
      ])
    } finally {
      loading.value = false
    }
  }

  return {
    // State
    statistics,
    highRiskUsers,
    recentViolations,
    loading,
    lastUpdated,
    
    // Computed
    totalRiskUsers,
    riskRate,
    
    // Actions
    fetchStatistics,
    fetchHighRiskUsers,
    fetchRecentViolations,
    refreshAllData
  }
})
