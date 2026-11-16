<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, MarkLineComponent, DataZoomComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { channels, type Result } from '../domain'
echarts.use([LineChart, GridComponent, TooltipComponent, MarkLineComponent, DataZoomComponent, CanvasRenderer])
const props = defineProps<{ results: Result[]; threshold: number }>()
const container = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined, observer: ResizeObserver | undefined
function render() {
  chart?.setOption({
    animationDuration: 350, color: channels.map(c => c.color),
    tooltip: { trigger: 'axis', renderMode: 'richText', confine: true },
    grid: { left: 58, right: 25, top: 36, bottom: 64 },
    xAxis: { type: 'value', min: 1, max: 40, name: '循环数', nameLocation: 'middle', nameGap: 32, axisLine: { lineStyle: { color: '#d9e1e8' } }, axisLabel: { color: '#82909d', fontSize: 11 }, splitLine: { show: false }, axisTick: { show: false } },
    yAxis: { type: 'value', name: '荧光信号 ΔRn', nameTextStyle: { color: '#82909d', padding: [0, 0, 8, 36] }, min: -50, max: 3000, splitLine: { lineStyle: { color: '#edf1f5', type: 'dashed' } }, axisLabel: { color: '#82909d', fontSize: 11 } },
    dataZoom: [{ type: 'inside', xAxisIndex: 0 }],
    series: props.results.map((r, index) => ({ name: `${r.well} · ${r.channel}`, type: 'line', showSymbol: false, smooth: 0.2,
      lineStyle: { width: 1.8, color: channels.find(c => c.name === r.channel)?.color, opacity: 0.85 }, data: r.points,
      markLine: index === 0 ? { symbol: 'none', silent: true, lineStyle: { color: '#a4b1be', type: 'dashed', width: 1 }, label: { formatter: `阈值 ${props.threshold}`, position: 'insideEndTop', color: '#82909d', fontSize: 10 }, data: [{ yAxis: props.threshold }] } : undefined }))
  }, { notMerge: true })
}
onMounted(() => { chart = echarts.init(container.value!); observer = new ResizeObserver(() => chart?.resize()); observer.observe(container.value!); render() })
watch(() => [props.results, props.threshold], render, { deep: true })
onBeforeUnmount(() => { observer?.disconnect(); chart?.dispose() })
</script>
<template><div ref="container" class="curve-chart" role="img" aria-label="模拟扩增曲线图，横轴为循环数，纵轴为荧光信号" /></template>
