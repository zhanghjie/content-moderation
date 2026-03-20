// API 响应基础类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页参数
export interface PageParams {
  current?: number
  pageSize?: number
}

// 分页响应
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pageSize: number
}
