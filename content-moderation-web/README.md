# Content Moderation Web

内容风控系统前端管理界面 - 基于 Vue 3 + Naive UI + TypeScript

## 🚀 技术栈

- **框架**: Vue 3.4+ (Composition API)
- **UI 组件库**: Naive UI 2.x
- **构建工具**: Vite 5.x
- **语言**: TypeScript 5.x
- **状态管理**: Pinia
- **路由**: Vue Router 4.x
- **HTTP 客户端**: Axios
- **图表**: ECharts 5.x
- **CSS 方案**: UnoCSS

## 📦 安装

```bash
cd content-moderation-web
npm install
```

## 🛠️ 开发

```bash
# 启动开发服务器（自动代理到后端 API）
npm run dev

# 访问地址：http://localhost:3000
```

## 📁 项目结构

```
src/
├── api/                    # API 接口封装
│   ├── video.ts           # 视频分析 API
│   ├── violation.ts       # 违规事件 API
│   ├── healthScore.ts     # 健康分 API
│   └── statistics.ts      # 统计 API
│
├── components/             # 公共组件
│   ├── PageHeader/        # 页面头部
│   ├── StatisticCard/     # 统计卡片
│   └── SearchForm/        # 搜索表单
│
├── composables/            # Composition API
├── layouts/                # 布局组件
│   └── BasicLayout.vue    # 基础布局
│
├── router/                 # 路由配置
├── stores/                 # Pinia 状态管理
│   └── app.ts             # 应用状态
│
├── types/                  # TypeScript 类型定义
│   ├── common.ts          # 通用类型
│   ├── video.ts           # 视频分析类型
│   ├── violation.ts       # 违规事件类型
│   └── healthScore.ts     # 健康分类型
│
├── utils/                  # 工具函数
│   └── request.ts         # Axios 封装
│
└── views/                  # 页面组件
    ├── dashboard/         # 仪表盘
    ├── video/             # 视频分析
    ├── violations/        # 违规事件
    ├── health-scores/     # 健康分管理
    ├── moderation/        # 审核记录
    └── settings/          # 系统设置
```

## 📄 功能模块

### 1. 仪表盘 (Dashboard)
- 今日数据统计
- 违规趋势图表
- 健康分分布
- 实时告警

### 2. 视频分析管理
- 任务列表（分页、搜索、筛选）
- 发起视频分析
- 任务详情查看
- 重新分析功能

### 3. 违规事件管理
- 事件列表
- 事件详情
- 批量处理
- 违规统计图表

### 4. 健康分管理
- 创作者列表
- 健康分详情
- 扣分记录
- 分数趋势图

### 5. 审核记录
- 审核历史查询
- 审核结果筛选

### 6. 系统设置
- API 配置
- 系统信息查看

## 🔌 API 配置

开发环境下，API 请求会自动代理到后端服务：

```typescript
// vite.config.ts
server: {
  proxy: {
    '/api': {
      target: 'http://47.99.69.21:8080',
      changeOrigin: true
    }
  }
}
```

## 🏗️ 构建

```bash
# 生产环境构建
npm run build

# 预览构建结果
npm run preview
```

## 📝 注意事项

1. **后端 API 依赖**: 本前端项目需要配合后端 `content-moderation-server` 使用
2. **跨域配置**: 开发环境使用 Vite 代理，生产环境需要配置 CORS 或使用 Nginx
3. **权限控制**: 当前版本暂未实现权限控制功能

## 🤝 开发规范

- 使用 ESLint + Prettier 保持代码风格一致
- 组件采用 Composition API + TypeScript
- 使用 Naive UI 组件库保持设计一致性
- 图表统一使用 ECharts

## 📅 开发日志

- 2026-03-15: 项目初始化，完成所有核心功能页面开发

## 📄 License

MIT
