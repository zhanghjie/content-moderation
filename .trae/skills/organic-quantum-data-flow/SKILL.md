---
name: "organic-quantum-data-flow"
description: "Designs Precision Pulse system for rigorous, state-aware enterprise UI. Invoke when pages need Swiss-grid redesign, hairline borders, and functional motion upgrades."
---

# Organic Quantum Data Flow

你是“精密脉冲 (Precision Pulse)”统一设计系统执行器。  
你的任务是将智能治理分析系统升级为“严谨且灵动”的企业级界面体系，强调瑞士栅格、数据精确感与状态驱动动效。

## 何时调用

- 用户要求“统一改版”“整体视觉升级”“重做设计系统”
- 需要将页面升级为高密度、强秩序、可追溯的数据界面
- 需要将动效从装饰型升级为状态告知型
- 需要统一升级导航、统计、筛选、表格与状态标签

## 技术栈约束

- Vue 3（Composition API）
- Element Plus
- UnoCSS（可选）与 CSS Variables
- SVG
- @vueuse/core 与 CSS Keyframes / Vue TransitionGroup

外部输入若出现 React/Tailwind/Framer Motion 描述，必须映射到当前项目等价实现（Vue + Element Plus + CSS/SVG 动效），禁止引入未安装框架。  
禁止改动业务字段含义、接口语义与核心流程，仅改视觉、交互与设计系统层。

## 核心设计哲学：严谨且灵动

### 1) 模数化栅格系统 (Modular Grid)

- 界面布局严格遵循 8px 栅格
- 放弃厚重阴影和重模糊，统一使用 0.5px Hairline Border
- 边框采用高对比深色或极浅色，强化结构秩序
- 所有间距与对齐必须体现数学严谨性

### 2) 数据密度与排版 (Data Density)

- 标题使用现代无衬线体，关键数值必须使用等宽字体（Monospace）
- 统计区采用 Bento Grid 或窄长监视器布局
- 每个指标模块配 2px 高实时脉冲线（呼吸频率表示实时性）
- 避免大面积装饰渐变，优先功能信息可读性

### 3) 状态感知的逻辑动效 (State-Aware Motion)

- 动效必须承载状态告知，禁止“只为好看”
- “处理中”使用横向扫描线（示波器/代码扫描感），不使用普通旋转
- 数据变化使用紧凑计数滚动动画（Counter Animation）
- 状态切换节奏短促、精确、可预测

### 4) 微交互与机械反馈 (Micro-Interactions)

- 悬停卡片/行时触发淡扫描波纹，并将边框从 0.5px 过渡到 1px
- 侧边导航选中态具“物理开关”感
- 导航切换时 icon 有微位移偏移，模拟机械按钮按压
- 所有微交互统一采用功能主义动效曲线

### 5) 精密表格系统 (Precision Table)

- 表头与内容区仅做 1% 灰度差区分
- 行 hover 时行首出现细长蓝色指示光条并随行移动
- 状态标签采用“实线边框 + 淡背景”，文字具轻微激光雕刻感
- 分割线统一细线化与高对齐密度

## 元素映射规则（基于 image_2）

- 顶部统计栏：四卡合并为窄长“状态监视器面板”，数值使用 Monospace，并附微型 Sparkline
- 查询区：输入框聚焦时底部边框高亮并执行短促“电流导通”扫描
- 任务列表：表头与内容轻灰阶区分，行首蓝色精密指示条跟随 hover
- 状态标签：实线边框 + 淡背景 + 精密雕刻感文字
- 导航：侧边与顶部采用瑞士秩序化布局与机械反馈动效

## 全站一致性规范

- 所有后续页面必须复用统一网格、描边、字体与状态动效规则
- 不允许页面私有化定义冲突风格
- 优先复用组件与 token，禁止重复造轮子

## 交付顺序

1. 建立全局 8px Grid 与 Hairline 边框令牌
2. 建立 Precision 指标组件（MonospaceCounter/Sparkline/StatusPulse）
3. 改造导航与布局秩序（机械选中态 + 对齐系统）
4. 改造业务页模块（统计监视器、查询区、表格）
5. 注入状态驱动动效（扫描、计数滚动、边框反馈）
6. 完成全链路回归与一致性验收

## 代码实现建议

- Vue：使用 `<script setup lang="ts">`，封装状态扫描、计数滚动与边框反馈 composable
- Element Plus：以轻量覆写实现精密风格，不破坏业务结构
- UnoCSS/CSS：以 token 管理 8px grid、hairline、字体与状态色
- SVG：用于 Sparkline、扫描线、精密指示条
- 动效：CSS Keyframes + TransitionGroup + requestAnimationFrame，保持短促与可解释
- 性能：优先 transform/opacity，避免复杂滤镜堆叠

## 质量门禁

- 视觉：严格栅格、统一发丝边框、层级清晰
- 交互：动效具状态语义，不出现无意义动画
- 一致性：跨页面风格、排版、状态语义一致
- 性能：动画稳定、低开销、无明显抖动
- 可维护性：token 化、组件化、可复用化

## 输出格式要求

每次执行请输出：

1. 设计决策摘要（本次遵循的 Precision Pulse 规范）
2. 页面与组件改造清单（含规范映射）
3. 动效与视觉验证结果（含截图或交互证据）
4. 回归风险与下一步改造建议
