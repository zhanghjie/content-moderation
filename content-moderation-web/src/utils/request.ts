import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElLoading } from 'element-plus'
import type { LoadingInstance } from 'element-plus/es/components/loading/src/loading'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

let loadingInstance: LoadingInstance | null = null
let loadingRequestCount = 0

const showGlobalLoading = () => {
  if (loadingRequestCount === 0) {
    loadingInstance = ElLoading.service({
      lock: true,
      text: '加载中...',
      background: 'rgba(0, 0, 0, 0.2)'
    })
  }
  loadingRequestCount += 1
}

const hideGlobalLoading = () => {
  if (loadingRequestCount > 0) {
    loadingRequestCount -= 1
  }
  if (loadingRequestCount === 0 && loadingInstance) {
    loadingInstance.close()
    loadingInstance = null
  }
}

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    showGlobalLoading()
    return config
  },
  (error) => {
    hideGlobalLoading()
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse) => {
    hideGlobalLoading()
    const { code, message: msg } = response.data

    if (code !== 200) {
      return Promise.reject(new Error(msg || '请求失败'))
    }

    return response.data
  },
  (error) => {
    hideGlobalLoading()
    return Promise.reject(error)
  }
)

export default request

export const get = <T = any>(url: string, params?: any): Promise<T> => {
  return request.get(url, { params })
}

export const post = <T = any>(url: string, data?: any): Promise<T> => {
  return request.post(url, data)
}

export const put = <T = any>(url: string, data?: any): Promise<T> => {
  return request.put(url, data)
}

export const del = <T = any>(url: string, params?: any): Promise<T> => {
  return request.delete(url, { params })
}
