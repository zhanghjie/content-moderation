import request from '@/utils/request'

export interface ProjectItem {
  projectId: string
  name: string
  description: string
  status: 'ACTIVE' | 'INACTIVE'
  createdAt: string
  updatedAt?: string
}

export const projectApi = {
  listProjects: async () => {
    const res = await request.get<{ projects: ProjectItem[] }>('/projects')
    return res.data
  },
  createProject: async (payload: { projectId: string; name: string; description?: string; status?: 'ACTIVE' | 'INACTIVE' }) => {
    const res = await request.post<{ projects: ProjectItem[] }>('/projects', payload)
    return res.data
  },
  updateProject: async (
    projectId: string,
    payload: { name?: string; description?: string; status?: 'ACTIVE' | 'INACTIVE' }
  ) => {
    const res = await request.put<{ projects: ProjectItem[] }>(`/projects/${projectId}`, payload)
    return res.data
  },
  deleteProject: async (projectId: string) => {
    const res = await request.delete<{ projects: ProjectItem[] }>(`/projects/${projectId}`)
    return res.data
  }
}
