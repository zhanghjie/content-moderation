<template>
  <div ref="shellRef" class="organic-radar-shell" :style="{ '--radar-height': `${heightPx}px` }">
    <svg
      v-if="pointCount >= 3"
      :viewBox="`0 0 ${width} ${height}`"
      class="organic-radar-svg"
      draggable="false"
      @dragstart.prevent
      @mousedown.prevent
      @mousemove="handleChartMouseMove"
      @mouseleave="handleChartMouseLeave"
    >
      <defs>
        <linearGradient id="radarFillGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="rgba(56, 189, 248, 0.52)" />
          <stop offset="50%" stop-color="rgba(14, 165, 233, 0.36)" />
          <stop offset="100%" stop-color="rgba(99, 102, 241, 0.18)" />
        </linearGradient>
        <linearGradient id="radarStrokeGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#67e8f9" />
          <stop offset="100%" stop-color="#38bdf8" />
        </linearGradient>
        <linearGradient id="scanStrokeGradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="rgba(56,189,248,0)" />
          <stop offset="55%" stop-color="rgba(56,189,248,0.7)" />
          <stop offset="100%" stop-color="rgba(99,102,241,0)" />
        </linearGradient>
        <filter id="radarGlowFilter" x="-50%" y="-50%" width="200%" height="200%">
          <feDropShadow dx="0" dy="0" stdDeviation="8" flood-color="rgba(56,189,248,0.45)" />
        </filter>
        <filter id="pointGlowFilter" x="-200%" y="-200%" width="400%" height="400%">
          <feDropShadow dx="0" dy="0" stdDeviation="4" flood-color="rgba(56,189,248,0.8)" />
        </filter>
      </defs>

      <g :transform="`translate(${centerX}, ${centerY})`">
        <polygon
          v-for="level in levels"
          :key="`grid-${level}`"
          :points="getGridPolygon(level)"
          class="grid-layer"
        />
        <line
          v-for="(axis, idx) in axes"
          :key="`axis-${idx}`"
          x1="0"
          y1="0"
          :x2="axis.x"
          :y2="axis.y"
          class="axis-line"
        />
        <g v-if="showScan" class="scan-group">
          <circle
            :r="chartRadius * 0.97"
            fill="none"
            stroke="url(#scanStrokeGradient)"
            :stroke-width="18"
            stroke-linecap="round"
            :stroke-dasharray="`${chartRadius * 1.9} ${chartRadius * 3.8}`"
            opacity="0.28"
          />
        </g>
        <path
          :d="organicAreaPath"
          fill="url(#radarFillGradient)"
          filter="url(#radarGlowFilter)"
          class="organic-area"
        />
        <path
          :d="organicAreaPath"
          fill="none"
          stroke="url(#radarStrokeGradient)"
          stroke-width="2.5"
          stroke-linejoin="round"
          stroke-linecap="round"
          class="organic-stroke"
        />
        <g v-for="(point, idx) in dataPoints" :key="`point-${idx}`" class="point-group">
          <circle
            :cx="point.x"
            :cy="point.y"
            r="4.6"
            class="data-point"
            :class="{ active: hoveredIndex === idx }"
            filter="url(#pointGlowFilter)"
          />
        </g>
      </g>

      <g :transform="`translate(${centerX}, ${centerY})`">
        <text
          v-for="(label, idx) in labels"
          :key="`label-${idx}`"
          :x="label.x"
          :y="label.y"
          :text-anchor="label.anchor"
          class="axis-label"
        >
          {{ label.name }}
        </text>
      </g>
    </svg>

    <div v-if="hoveredIndex >= 0 && hoveredPoint" class="radar-tooltip fixed">
      <div class="tooltip-title">{{ hoveredPoint.name }}</div>
      <div class="tooltip-score">{{ hoveredPoint.score }} 分</div>
    </div>

    <div v-if="pointCount < 3" class="radar-empty">
      维度数据不足，至少需要 3 个维度
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import type { ProfileDimension } from '@/types/profile'

type RadarPoint = { x: number; y: number; name: string; score: number }

const props = withDefaults(
  defineProps<{
    dimensions: ProfileDimension[]
    maxValue?: number
    height?: number
    subtleMotion?: boolean
    showScan?: boolean
  }>(),
  { maxValue: 100, height: 440, subtleMotion: false, showScan: false }
)

const shellRef = ref<HTMLElement | null>(null)
const width = ref(560)
const height = ref(380)
const hoveredIndex = ref(-1)
const animValues = ref<number[]>([])
const velocities = ref<number[]>([])
const frameId = ref<number>()
let resizeObserver: ResizeObserver | null = null
const heightPx = computed(() => props.height)
const showScan = computed(() => props.showScan)

const levels = 5
const pointCount = computed(() => props.dimensions.length)
const centerX = computed(() => width.value / 2)
const centerY = computed(() => height.value / 2)
const chartRadius = computed(() => Math.max(112, Math.min(width.value, height.value) * 0.38))

const normalizedTargets = computed(() =>
  props.dimensions.map(item => Math.min(1, Math.max(0, item.score / props.maxValue)))
)

const axes = computed(() =>
  props.dimensions.map((_, idx) => {
    const angle = getAngle(idx, pointCount.value)
    return {
      x: Math.cos(angle) * chartRadius.value,
      y: Math.sin(angle) * chartRadius.value
    }
  })
)

const labels = computed(() =>
  props.dimensions.map((item, idx) => {
    const angle = getAngle(idx, pointCount.value)
    const radius = chartRadius.value + 28
    const x = Math.cos(angle) * radius
    const y = Math.sin(angle) * radius + 5
    const anchor = x > 12 ? 'start' : x < -12 ? 'end' : 'middle'
    return { name: item.name, x, y, anchor }
  })
)

const dataPoints = computed<RadarPoint[]>(() =>
  props.dimensions.map((item, idx) => {
    const value = animValues.value[idx] ?? 0
    const angle = getAngle(idx, pointCount.value)
    const radius = value * chartRadius.value
    return {
      x: Math.cos(angle) * radius,
      y: Math.sin(angle) * radius,
      name: item.name,
      score: item.score
    }
  })
)

const organicAreaPath = computed(() => {
  const points = dataPoints.value.map((p, idx) => {
    const angle = getAngle(idx, pointCount.value)
    const wobble = props.subtleMotion ? (Math.sin(Date.now() / 1100 + idx * 1.1) * 0.004) : 0
    const ratio = Math.min(1, Math.max(0.03, (animValues.value[idx] ?? 0) + wobble))
    const radius = ratio * chartRadius.value
    return {
      x: Math.cos(angle) * radius,
      y: Math.sin(angle) * radius
    }
  })
  return pointsToClosedPath(points)
})

const hoveredPoint = computed(() => {
  if (hoveredIndex.value < 0) return null
  return dataPoints.value[hoveredIndex.value] || null
})

function getAngle(index: number, total: number) {
  return -Math.PI / 2 + (Math.PI * 2 * index) / total
}

function getGridPolygon(level: number) {
  const ratio = level / levels
  const points = props.dimensions.map((_, idx) => {
    const angle = getAngle(idx, pointCount.value)
    const x = Math.cos(angle) * chartRadius.value * ratio
    const y = Math.sin(angle) * chartRadius.value * ratio
    return `${x},${y}`
  })
  return points.join(' ')
}

function pointsToClosedPath(points: Array<{ x: number; y: number }>) {
  if (points.length < 3) return ''
  let path = `M ${points[0].x} ${points[0].y}`
  for (let i = 1; i < points.length; i++) {
    path += ` L ${points[i].x} ${points[i].y}`
  }
  return `${path} Z`
}

function updateSize() {
  if (!shellRef.value) return
  const rect = shellRef.value.getBoundingClientRect()
  if (rect.width > 0 && rect.height > 0) {
    width.value = rect.width
    height.value = rect.height
  }
}

function startLoop() {
  stopLoop()
  let stableFrames = 0
  const loop = () => {
    const target = normalizedTargets.value
    let maxMove = 0
    for (let i = 0; i < target.length; i++) {
      const current = animValues.value[i] ?? 0
      const velocity = velocities.value[i] ?? 0
      const nextVelocity = (velocity + (target[i] - current) * 0.13) * 0.82
      velocities.value[i] = nextVelocity
      animValues.value[i] = current + nextVelocity
      maxMove = Math.max(maxMove, Math.abs(nextVelocity))
    }
    if (maxMove < 0.0008) {
      stableFrames += 1
    } else {
      stableFrames = 0
    }
    if (props.subtleMotion || stableFrames < 18) {
      frameId.value = requestAnimationFrame(loop)
    } else {
      stopLoop()
    }
  }
  frameId.value = requestAnimationFrame(loop)
}

function stopLoop() {
  if (frameId.value) cancelAnimationFrame(frameId.value)
}

function resetAnimationState() {
  animValues.value = props.dimensions.map(() => 0)
  velocities.value = props.dimensions.map(() => 0)
}

function handleChartMouseLeave() {
  hoveredIndex.value = -1
}

function handleChartMouseMove(event: MouseEvent) {
  if (!shellRef.value || dataPoints.value.length === 0) return
  const rect = shellRef.value.getBoundingClientRect()
  const pointerX = event.clientX - rect.left
  const pointerY = event.clientY - rect.top
  let nearest = -1
  let minDist = Number.POSITIVE_INFINITY
  dataPoints.value.forEach((point, idx) => {
    const px = centerX.value + point.x
    const py = centerY.value + point.y
    const dx = pointerX - px
    const dy = pointerY - py
    const dist = dx * dx + dy * dy
    if (dist < minDist) {
      minDist = dist
      nearest = idx
    }
  })
  hoveredIndex.value = nearest
}

watch(
  () => props.dimensions,
  () => {
    resetAnimationState()
    startLoop()
  },
  { deep: true, immediate: true }
)

onMounted(() => {
  updateSize()
  resizeObserver = new ResizeObserver(updateSize)
  if (shellRef.value) resizeObserver.observe(shellRef.value)
  startLoop()
})

onUnmounted(() => {
  stopLoop()
  resizeObserver?.disconnect()
})
</script>

<style scoped>
.organic-radar-shell {
  position: relative;
  width: 100%;
  height: var(--radar-height);
  border-radius: 20px;
  padding: 14px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.88), rgba(232, 242, 255, 0.55));
  border: 1px solid rgba(100, 116, 139, 0.22);
  backdrop-filter: blur(10px) saturate(140%);
  overflow: hidden;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.45), 0 10px 24px rgba(15, 23, 42, 0.08);
  user-select: none;
  -webkit-user-select: none;
}

.organic-radar-svg {
  width: 100%;
  height: 100%;
  -webkit-user-drag: none;
  touch-action: none;
}

.grid-layer {
  fill: rgba(148, 163, 184, 0.03);
  stroke: rgba(148, 163, 184, 0.28);
  stroke-width: 0.9;
}

.axis-line {
  stroke: rgba(100, 116, 139, 0.32);
  stroke-width: 0.9;
}

.scan-group {
  transform-origin: center;
  animation: radar-scan 5.6s linear infinite;
}

.organic-area {
  opacity: 0.94;
}

.organic-stroke {
  opacity: 0.98;
}

.data-point {
  fill: #f0f9ff;
  stroke: #38bdf8;
  stroke-width: 2.2;
  cursor: default;
  transition: transform 180ms ease;
}

.data-point.active {
  transform: scale(1.08);
}

.axis-label {
  fill: var(--text-secondary);
  font-size: 12px;
  font-weight: 500;
}

.radar-tooltip {
  position: absolute;
  padding: 8px 10px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: rgba(16, 24, 40, 0.84);
  color: #f8fafc;
  pointer-events: none;
  white-space: nowrap;
  box-shadow: 0 8px 20px rgba(2, 6, 23, 0.24);
  backdrop-filter: blur(8px);
}

.radar-tooltip.fixed {
  top: 12px;
  right: 12px;
}

.tooltip-title {
  font-size: 12px;
  opacity: 0.86;
}

.tooltip-score {
  font-size: 14px;
  font-weight: 600;
  margin-top: 2px;
}

.radar-empty {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  font-size: 13px;
}

@keyframes radar-scan {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 768px) {
  .organic-radar-shell {
    height: calc(var(--radar-height) - 80px);
  }
}
</style>
