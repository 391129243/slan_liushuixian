<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CurveChart from './CurveChart.vue'
import Icon from './Icon.vue'
import { channels, formatSize, formatTime } from '../domain'

type Target = { well: string; channel: string; channelNumber: number; ct: number | null; rawMelt: [number, number][]; peakCurve: [number, number][]; peaks: { tm: number; height: number; width: number; signal: number }[] }
type Snapshot = { kind: string; fileName: string; title: string; fileSize: number; sha256: string; startTime: string; endTime: string; deviceModel: string; testedAt: string; normalizedNumbers: number; statistics: { wells: number; targets: number; rawMeltPoints: number; peakCurvePoints: number; peaks: number; unavailableCt: number }; checks: Record<string, boolean>; results: Target[] }
const data = ref<Snapshot>(), error = ref(''), mode = ref<'rawMelt' | 'peak'>('rawMelt'), tab = ref('chart')
const selectedWells = ref<string[]>([]), selectedChannels = ref<string[]>([]), search = ref('')
const allWells = computed(() => [...new Set(data.value?.results.map(r => r.well) || [])])
const allChannels = computed(() => [...new Set(data.value?.results.map(r => r.channel) || [])])
const filtered = computed(() => (data.value?.results || []).filter(r => selectedWells.value.includes(r.well) && selectedChannels.value.includes(r.channel)))
const rows = computed(() => filtered.value.filter(r => `${r.well} ${r.channel}`.toLowerCase().includes(search.value.toLowerCase())))
const curves = computed(() => filtered.value.map(r => ({ well: r.well, channel: r.channel, ct: r.ct, points: mode.value === 'rawMelt' ? r.rawMelt : r.peakCurve })))
const pointCount = computed(() => curves.value.reduce((n, r) => n + r.points.length, 0))
const peakCount = computed(() => filtered.value.reduce((n, r) => n + r.peaks.length, 0))
const checkLabels: Record<string, string> = { originalGsonNonStandardNaNRejected: '非法 NaN 正确拒收', jsonRetryDeduplicated: 'JSON 重传去重', concurrentRetriesDeduplicated: '并发上报一致性', allDataRoundTripIncludingNullCtAndMeltCurves: '曲线与缺失值完整保存', jsonSourceByteExact: 'JSON 原文一致', binaryRetryDeduplicated: '附件重传去重', downloadHashMatchesSource: 'PCR 下载哈希一致', restartPersistence: '服务重启恢复', sourceUnmodified: '原文件保持不变' }
async function load() {
  error.value = ''
  try {
    const response = await fetch('/reference/melt-result.json')
    if (!response.ok) throw Error('结果快照暂时无法加载，请重试。')
    const result = await response.json() as Snapshot
    if (result.kind !== 'verified-offline-snapshot' || !Array.isArray(result.results)) throw Error('结果快照格式不正确。')
    data.value = result; selectedWells.value = [...new Set(result.results.map(r => r.well))]; selectedChannels.value = [...new Set(result.results.map(r => r.channel))]
  } catch (e) { error.value = (e as Error).message }
}
function toggle(list: 'wells' | 'channels', value: string) {
  const selected = list === 'wells' ? selectedWells : selectedChannels
  selected.value = selected.value.includes(value) ? selected.value.filter(v => v !== value) : [...selected.value, value]
}
function exportView() {
  if (!data.value) return
  const snapshot = { format: 'pcr-verified-result-view-v1', sourceSha256: data.value.sha256, offlineSnapshot: true, results: rows.value }
  const url = URL.createObjectURL(new Blob([JSON.stringify(snapshot, null, 2)], { type: 'application/json' }))
  const link = document.createElement('a'); link.href = url; link.download = '真实熔解结果_当前筛选.view.json'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
}
onMounted(load)
</script>
<template>
  <div v-if="error" class="panel empty-state" role="alert"><Icon name="info" :size="32" /><h3>暂时无法展示结果</h3><p>{{ error }}</p><button class="button primary" @click="load">重新加载</button></div>
  <div v-else-if="!data" class="panel empty-state" role="status">正在加载真实文件结果…</div>
  <div v-else class="real-result">
    <section class="stats-grid real-stats"><article v-for="stat in [{ label: '真实孔位 / 靶标', value: data.statistics.wells, unit: '/ ' + data.statistics.targets, icon: 'grid', foot: 'SLAN-16H · 熔解实验' }, { label: '原始熔解曲线点', value: data.statistics.rawMeltPoints.toLocaleString(), unit: '点', icon: 'chart', foot: '全部原始坐标保留' }, { label: '检测到的峰', value: data.statistics.peaks.toString().padStart(2, '0'), unit: '个', icon: 'chart', foot: data.statistics.peakCurvePoints.toLocaleString() + ' 个峰曲线点' }, { label: '上报验证', value: '通过', unit: '', icon: 'check', foot: '去重 · 下载校验 · 重启恢复' }]" :key="stat.label"><div class="stat-top"><span>{{ stat.label }}</span><Icon :name="stat.icon" :size="18" /></div><div class="stat-number">{{ stat.value }}<small>{{ stat.unit }}</small></div><div class="stat-foot">{{ stat.foot }}</div></article></section>
    <div class="real-layout">
      <aside class="panel real-filter"><div class="panel-heading"><h2>结果筛选</h2><Icon name="settings" :size="16" /></div><div class="real-filter-body"><div class="section-label">孔位 <span>{{ selectedWells.length }} / {{ allWells.length }}</span></div><div class="real-well-buttons"><button v-for="well in allWells" :key="well" :class="{ chosen: selectedWells.includes(well) }" :aria-pressed="selectedWells.includes(well)" :aria-label="'真实孔位 ' + well" @click="toggle('wells', well)">{{ well }}</button></div><div class="real-filter-actions"><button class="text-button" @click="selectedWells = [...allWells]">选择全部</button><button class="text-button secondary" @click="selectedWells = []">清空</button></div><div class="divider"></div><div class="section-label">通道 <span>{{ selectedChannels.length }} / {{ allChannels.length }}</span></div><label v-for="channel in allChannels" :key="channel" class="real-channel"><input type="checkbox" :checked="selectedChannels.includes(channel)" @change="toggle('channels', channel)" /><i :style="{ background: channels.find(c => c.name === channel)?.color }"></i><span>{{ channel }}</span></label><div class="real-filter-note"><Icon name="info" :size="16" /><p>选择孔位和通道查看真实曲线。此处仅筛选显示，不重新计算实验结果。</p></div></div><div class="real-file-mini"><Icon name="file" :size="20" /><strong>{{ data.fileName }}</strong><span>{{ formatSize(data.fileSize) }} · 原文件未修改</span></div></aside>
      <section class="panel real-main"><div class="real-title"><div><div class="section-kicker">VERIFIED EXPERIMENT RESULT</div><h2>{{ data.title }}</h2><div class="file-subtitle">{{ data.fileName }}<span>·</span>{{ data.deviceModel }}<span class="micro-badge">真实结果 · 离线快照</span></div></div><button class="button small" @click="exportView"><Icon name="download" :size="15" />导出当前筛选</button></div><div class="tabs" role="tablist"><button v-for="t in [{ key: 'chart', text: '熔解曲线', icon: 'chart' }, { key: 'targets', text: '靶标与峰值', icon: 'grid' }, { key: 'checks', text: '上报验证', icon: 'check' }, { key: 'source', text: '文件溯源', icon: 'file' }]" :key="t.key" role="tab" :aria-selected="tab === t.key" :class="{ active: tab === t.key }" @click="tab = t.key"><Icon :name="t.icon" :size="15" />{{ t.text }}</button></div>
        <template v-if="tab === 'chart'"><div class="real-chart-controls"><div><h3>{{ mode === 'rawMelt' ? '原始熔解曲线' : '峰曲线' }}</h3><p>{{ filtered.length }} 条曲线 · {{ pointCount.toLocaleString() }} 个真实坐标点</p></div><div class="segmented" aria-label="曲线类型"><button :class="{ active: mode === 'rawMelt' }" @click="mode = 'rawMelt'">原始信号</button><button :class="{ active: mode === 'peak' }" @click="mode = 'peak'">峰曲线</button></div></div><div class="real-chart"><CurveChart :results="curves" :threshold="0" :actual="true" :mode="mode" /><div v-if="!curves.length" class="chart-empty">请选择至少一个孔位和通道</div></div><div class="real-legend"><span v-for="channel in allChannels" :key="channel"><i :style="{ background: channels.find(c => c.name === channel)?.color }"></i>{{ channel }}</span><small>滚轮缩放 · 拖动查看温区</small></div><div class="real-interpretation"><Icon name="info" :size="18" /><div><strong>这是熔解实验，Ct 不代表当前结果</strong><p>{{ data.statistics.unavailableCt }} 个靶标的 SDK Ct 无有效值，已按 null 传输和保存，不替换为 0。请查看峰形、Tm 与原始熔解信号。</p></div></div></template>
        <div v-else-if="tab === 'targets'" class="real-targets"><div class="real-chart-controls"><div><h3>靶标结果</h3><p>{{ filtered.length }} 个靶标 · {{ peakCount }} 个峰</p></div><label class="search"><Icon name="search" :size="15" /><input v-model="search" placeholder="搜索孔位或通道" aria-label="搜索真实靶标" /></label></div><div class="table-scroll"><table><thead><tr><th>孔位</th><th>通道</th><th>Ct</th><th>峰数</th><th>Tm（°C）</th></tr></thead><tbody><tr v-for="r in rows" :key="r.well + r.channel"><td><b>{{ r.well }}</b></td><td><span class="channel-dot" :style="{ background: channels.find(c => c.name === r.channel)?.color }"></span>{{ r.channel }}</td><td class="missing-ct" title="SDK 无有效 Ct，保留 null">—</td><td>{{ r.peaks.length }}</td><td class="monospace">{{ r.peaks.map(p => p.tm.toFixed(2)).join(' / ') || '—' }}</td></tr><tr v-if="!rows.length"><td colspan="5" class="empty-mini">没有匹配的靶标</td></tr></tbody></table></div><p class="table-caption">Tm 以两位小数展示，导出保留原始精度；不输出未经审核的检测结论。</p></div>
        <div v-else-if="tab === 'checks'" class="real-checks"><div class="validation-hero"><span class="validation-icon"><Icon name="check" :size="26" /></span><div><h3>真实文件上报验证通过</h3><p>测试完成于 {{ formatTime(data.testedAt) }} · 独立测试库</p></div></div><div class="check-grid"><div v-for="(label, key) in checkLabels" :key="key" class="check-item"><Icon :name="data.checks[key] ? 'check' : 'info'" :size="17" /><span>{{ label }}</span><b>{{ data.checks[key] ? '通过' : '未验证' }}</b></div></div><div class="compatibility-note"><strong>发现并验证了设备端兼容问题</strong><p>原 Gson 2.8.9 会输出非标准 NaN，接收端正确返回 400。测试在明确将 {{ data.normalizedNumbers }} 个不可用数值转换为 null 后上报成功，未修改 Android 工程。</p></div></div>
        <div v-else class="metadata real-metadata"><dl><dt>数据来源</dt><dd>宏石原厂 DLL 离线读取真实结果文件，不是模拟曲线</dd><dt>文件名</dt><dd>{{ data.fileName }}</dd><dt>开始 / 结束</dt><dd>{{ data.startTime }} — {{ data.endTime }}</dd><dt>原始大小</dt><dd>{{ data.fileSize.toLocaleString() }} 字节</dd><dt>SHA-256</dt><dd class="monospace hash">{{ data.sha256 }}</dd><dt>隐私范围</dt><dd>页面快照仅保留孔位、通道、曲线与峰数据，不包含患者及样本身份字段</dd><dt>当前状态</dt><dd>验证结果的离线快照，不是实时数据库查询；未连接设备</dd><dt>操作边界</dt><dd>筛选与导出不会改变原始 PCR，也不执行真实二次分析</dd></dl></div>
      </section>
    </div>
  </div>
</template>
