<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'

/**
 * ECharts 通用封装：传入 option 即渲染，自动响应容器尺寸变化
 */
const props = defineProps<{ option: Record<string, unknown>; height?: string }>()

const el = ref<HTMLElement>()
let chart: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

function render() {
  if (!el.value) return
  if (!chart) {
    chart = echarts.init(el.value)
  }
  chart.setOption(props.option, true)
}

onMounted(() => {
  render()
  resizeObserver = new ResizeObserver(() => chart?.resize())
  if (el.value) resizeObserver.observe(el.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div ref="el" class="stats-chart" :style="{ height: height || '320px' }" />
</template>

<style scoped>
.stats-chart {
  width: 100%;
}
</style>
