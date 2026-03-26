export interface ProjectItem {
  projectId: string
  name: string
  description: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
}

const STORAGE_KEY = 'content-moderation.projects'

const seedProjects: ProjectItem[] = [
  {
    projectId: 'video_moderation',
    name: '视频治理项目',
    description: '用于视频内容安全与治理分析',
    status: 'ACTIVE',
    createdAt: new Date('2026-01-01T00:00:00.000Z').toISOString()
  },
  {
    projectId: 'live_risk',
    name: '直播风控项目',
    description: '用于直播场景的风险分析与审核',
    status: 'ACTIVE',
    createdAt: new Date('2026-01-08T00:00:00.000Z').toISOString()
  }
]

const clone = (projects: ProjectItem[]) => projects.map(item => ({ ...item }))

const hasLocalStorage = () => typeof window !== 'undefined' && !!window.localStorage

const normalizeProject = (project: Partial<ProjectItem>): ProjectItem => ({
  projectId: String(project.projectId || '').trim(),
  name: String(project.name || '').trim(),
  description: String(project.description || '').trim(),
  status: project.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE',
  createdAt: project.createdAt || new Date().toISOString()
})

export const getProjectSeeds = () => clone(seedProjects)

export const loadProjects = (): ProjectItem[] => {
  if (!hasLocalStorage()) {
    return clone(seedProjects)
  }

  const raw = window.localStorage.getItem(STORAGE_KEY)
  if (!raw) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(seedProjects))
    return clone(seedProjects)
  }

  try {
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) {
      return clone(seedProjects)
    }
    return parsed
      .map((item: Partial<ProjectItem>) => normalizeProject(item))
      .filter(item => item.projectId)
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
  } catch {
    return clone(seedProjects)
  }
}

export const saveProjects = (projects: ProjectItem[]) => {
  const normalized = projects
    .map(item => normalizeProject(item))
    .filter(item => item.projectId)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))

  if (hasLocalStorage()) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized))
  }

  return clone(normalized)
}

export const addProject = (project: Partial<ProjectItem>) => {
  const normalized = normalizeProject(project)
  const current = loadProjects()
  const next = [normalized, ...current.filter(item => item.projectId !== normalized.projectId)]
  return saveProjects(next)
}

export const updateProject = (projectId: string, patch: Partial<ProjectItem>) => {
  const current = loadProjects()
  const targetId = String(projectId || '').trim()
  const next = current.map(item => {
    if (item.projectId !== targetId) return item
    return normalizeProject({
      ...item,
      ...patch,
      projectId: targetId
    })
  })
  return saveProjects(next)
}

export const deleteProject = (projectId: string) => {
  const current = loadProjects()
  const next = current.filter(item => item.projectId !== String(projectId || '').trim())
  return saveProjects(next)
}

export const getProjectById = (projectId: string) => {
  return loadProjects().find(item => item.projectId === String(projectId || '').trim()) || null
}
