<script setup lang="ts">
import { computed, reactive, ref, watch, defineAsyncComponent } from 'vue'
import Icon from './components/Icon.vue'
import CurveChart from './components/CurveChart.vue'
import { channels, wells, defaults, referenceFolder, referenceSha256, analyze, formatSize, formatTime, type Analysis, type Experiment } from './domain'
import { loadState, saveState, mockService, exportSnapshot, pcrApi } from './services'
const RealResultView = defineAsyncComponent(() => import('./components/RealResultView.vue'))
const DataCenterView = defineAsyncComponent(() => import('./components/DataCenterView.vue'))

const saved = loadState()
const experiments = ref(saved.experiments), history = ref(saved.history)
const selectedId = ref(experiments.value[0]?.id || '')
const mode = new URLSearchParams(location.search).get('mode')
const view = ref(mode === 'server' ? 'server' : mode === 'mock' ? 'workspace' : 'real'), tab = ref('curves'), query = ref(''), busy = ref(false), compare = ref(false)
const selectedWells = ref(['A1', 'A2', 'A3', 'A4']), selectedChannels = ref(['FAM', 'HEX', 'ROX'])
const parameters = reactive({ ...defaults })
const modal = ref(''), toast = ref(''), fileInput = ref<HTMLInputElement>(), current = ref<Analysis>()
let toastTimer: ReturnType<typeof setTimeout>
const active = computed(() => experiments.value.find(e => e.id === selectedId.value))
const filteredExperiments = computed(() => experiments.value.filter(e => `${e.name} ${e.fileName}`.toLowerCase().includes(query.value.toLowerCase())))
function initial(experiment: Experiment): Analysis { return { id: 'initial', experimentId: experiment.id, experimentName: experiment.name,
  createdAt: experiment.importedAt, parameters: { ...defaults }, results: analyze(defaults), mock: true } }
function notify(text: string) { toast.value = text; clearTimeout(toastTimer); toastTimer = setTimeout(() => toast.value = '', 5000) }
function selectExperiment(experiment: Experiment) {
  selectedId.value = experiment.id; current.value = initial(experiment); Object.assign(parameters, defaults); tab.value = 'curves'; compare.value = false
}
if (active.value) current.value = initial(active.value)
watch([experiments, history], () => { if (!saveState({ experiments: experiments.value, history: history.value })) notify('浏览器存储空间不足，本次修改仅保留在当前页面。') }, { deep: true })
const visibleResults = computed(() => (current.value?.results || []).filter(r => selectedWells.value.includes(r.well) && selectedChannels.value.includes(r.channel)))
const originalResults = computed(() => analyze(defaults).filter(r => selectedWells.value.includes(r.well) && selectedChannels.value.includes(r.channel)))
const dirty = computed(() => JSON.stringify(parameters) !== JSON.stringify(current.value?.parameters))
async function readFile(file?: File) {
  if (!file || busy.value) return
  busy.value = true
  try { const result = await mockService.read(file); experiments.value.unshift(result); selectExperiment(result); view.value = 'workspace'; notify('已读取文件信息并载入模拟数据；未解码文件内容。') }
  catch (error) { notify((error as Error).message) } finally { busy.value = false; if (fileInput.value) fileInput.value.value = '' }
}
function onFileChange(event: Event) { void readFile((event.target as HTMLInputElement).files?.[0]) }
function drop(event: DragEvent) { void readFile(event.dataTransfer?.files[0]) }
async function reanalyze() {
  if (!active.value || busy.value) return
  busy.value = true
  try { const result = await mockService.reanalyze(active.value, { ...parameters }); current.value = result; history.value.unshift(result); history.value = history.value.slice(0, 50); notify('模拟二次分析完成，已保存独立分析记录。') }
  catch (error) { notify((error as Error).message) } finally { busy.value = false }
}
function toggleWell(well: string) { selectedWells.value = selectedWells.value.includes(well) ? selectedWells.value.filter(w => w !== well) : [...selectedWells.value, well] }
function toggleChannel(channel: string) { selectedChannels.value = selectedChannels.value.includes(channel) ? selectedChannels.value.filter(c => c !== channel) : [...selectedChannels.value, channel] }
function openHistory(analysis: Analysis) {
  const experiment = experiments.value.find(e => e.id === analysis.experimentId)
  if (!experiment) return notify('对应的模拟文件信息不存在。')
  selectedId.value = experiment.id; current.value = analysis; Object.assign(parameters, analysis.parameters); view.value = 'workspace'; tab.value = 'results'
}
function exportCurrent() { if (current.value) { exportSnapshot(current.value); notify('已导出模拟分析 JSON，未生成 .pcr 文件。') } }
const pageTitle = computed(() => ({ server: '数据中心', real: '真实结果验证', workspace: '实验分析工作台', files: '实验文件库', history: '分析记录', settings: '接入设置' })[view.value])
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <a class="brand" href="#" @click.prevent="view = 'workspace'"><span class="brand-symbol"><i></i><i></i><i></i></span><span>PCR<span class="brand-light"> Studio</span><small>实验数据分析平台</small></span></a>
      <div class="nav-caption">工作空间</div>
      <nav aria-label="主导航">
        <button v-for="item in [{ key: 'server', icon: 'folder', text: '数据中心' }, { key: 'real', icon: 'check', text: '真实结果验证' }, { key: 'workspace', icon: 'chart', text: '模拟工作台' }, { key: 'files', icon: 'folder', text: '文件库' }, { key: 'history', icon: 'clock', text: '分析记录' }, { key: 'settings', icon: 'settings', text: '接入设置' }]" :key="item.key" :class="{ active: view === item.key }" @click="view = item.key"><Icon :name="item.icon" /><span>{{ item.text }}</span><span v-if="item.key === 'files'" class="nav-count">{{ experiments.length }}</span></button>
      </nav>
      <div class="sidebar-note"><span class="tiny-dot"></span> {{ view === 'server' ? '服务器数据管理' : view === 'real' ? '真实文件离线快照' : '独立演示环境' }}<p>无需连接设备<br>{{ view === 'server' ? '文件与版本保存在服务端' : view === 'real' ? '已验证上报与存储' : '模拟数据保存在浏览器' }}</p></div>
      <div class="sidebar-user"><span class="avatar">研</span><div>研发工作空间<small>{{ view === 'server' ? '在线分析 · v0.3' : view === 'real' ? '结果验证 · v0.3' : '本地模拟 · v0.3' }}</small></div></div>
    </aside>

    <main>
      <header class="topbar"><div class="breadcrumb">工作空间 <Icon name="chevron" :size="13" /> <span>{{ pageTitle }}</span></div><div class="topbar-right"><span class="demo-label"><span class="tiny-dot"></span> {{ view === 'server' ? '服务器数据' : view === 'real' ? '真实数据 · 离线' : '模拟演示' }}</span><span class="top-date">{{ new Date().toLocaleDateString('zh-CN', { timeZone: 'Asia/Shanghai' }) }}</span><span class="avatar light">研</span></div></header>
      <div class="content">
        <section class="page-heading"><div><div class="eyebrow">EXPERIMENT WORKSPACE</div><h1>{{ pageTitle }}</h1><p>{{ view === 'server' ? '上传实验文件、查看实际结果，保留每次分析的参数与版本。' : view === 'real' ? '查看真实熔解曲线、峰值及 HTTP 上报验证结果。' : view === 'workspace' ? '从实验文件到分析结果，让每一次数据复核更清晰。' : view === 'files' ? '统一管理实验文件信息，保留原始数据与分析记录。' : view === 'history' ? '每次参数调整独立留存，随时回看与导出。' : '预留 Java 与 C# SDK 服务之间的分析接口。' }}</p></div><button v-if="view !== 'real' && view !== 'server'" class="button primary" :disabled="busy" @click="fileInput?.click()"><Icon name="upload" :size="17" />读取文件</button><button v-else-if="view === 'real'" class="button" @click="view = 'workspace'"><Icon name="play" :size="16" />进入模拟分析</button></section>
        <input ref="fileInput" type="file" accept=".pcr" class="sr-only" aria-label="选择 PCR 文件" @change="onFileChange" />
        <div v-if="view !== 'server'" class="demo-banner"><Icon name="info" :size="17" /><span>{{ view === 'real' ? '当前展示原厂 SDK 离线读取的真实文件快照；不是实时查询，页面未连接设备。' : '当前为模拟数据。模拟曲线与 Ct 不代表真实文件的检测结果。' }}</span><button @click="view = 'settings'">查看接入说明 <Icon name="arrow" :size="14" /></button></div>

        <DataCenterView v-if="view === 'server'" />
        <RealResultView v-else-if="view === 'real'" />
        <template v-else-if="view === 'workspace'">
          <section class="stats-grid"><article v-for="stat in [{ label: '实验文件', value: experiments.length.toString().padStart(2, '0'), unit: '份', icon: 'folder', foot: '当前工作空间' }, { label: '模拟孔位', value: '08', unit: '孔', icon: 'grid', foot: 'A1–A8 · 演示布局' }, { label: '显示通道', value: selectedChannels.length.toString().padStart(2, '0'), unit: '/ 6', icon: 'chart', foot: '支持多通道叠加' }, { label: '分析记录', value: history.length.toString().padStart(2, '0'), unit: '次', icon: 'clock', foot: '参数版本独立留存' }]" :key="stat.label"><div class="stat-top"><span>{{ stat.label }}</span><Icon :name="stat.icon" :size="18" /></div><div class="stat-number">{{ stat.value }}<small>{{ stat.unit }}</small></div><div class="stat-foot">{{ stat.foot }}</div></article></section>

          <section class="workspace-grid">
            <aside class="panel file-panel"><div class="panel-heading"><h2>实验文件</h2><span class="subtle-count">{{ experiments.length }}</span></div><label class="search"><Icon name="search" :size="16" /><input v-model="query" placeholder="搜索实验名称" aria-label="搜索实验名称" /></label><div class="experiment-list"><button v-for="e in filteredExperiments" :key="e.id" class="experiment-card" :class="{ selected: e.id === selectedId }" @click="selectExperiment(e)"><div class="file-card-top"><span class="file-icon"><Icon name="file" :size="19" /></span><span class="micro-badge">{{ e.reference ? '参考文件' : '模拟导入' }}</span></div><strong>{{ e.name }}</strong><small>{{ e.fileName }}</small><div class="file-card-bottom"><span>{{ formatSize(e.size) }}</span><span>{{ new Date(e.importedAt).toLocaleDateString('zh-CN') }}</span></div></button><div v-if="!filteredExperiments.length" class="empty-mini">没有匹配的实验</div></div><button class="dropzone" @click="fileInput?.click()" @dragover.prevent @drop.prevent="drop"><Icon name="upload" :size="22" /><strong>添加实验文件</strong><span>点击选择或拖入 .pcr</span><small>仅模拟读取 · 最大 32 MiB</small></button><div class="storage-note"><Icon name="check" :size="14" />原始文件不会被修改</div></aside>

            <section class="panel analysis-panel" v-if="active && current"><div class="analysis-heading"><div><div class="section-kicker">CURRENT EXPERIMENT</div><h2 :title="active.name">{{ active.name }}</h2><div class="file-subtitle">{{ active.fileName }}<span>·</span>{{ formatSize(active.size) }}<span class="micro-badge amber">模拟数据</span></div></div><button class="icon-button" title="查看文件信息" aria-label="查看文件信息" @click="tab = 'metadata'"><Icon name="info" :size="19" /></button></div>
              <div class="tabs" role="tablist"><button v-for="t in [{ key: 'curves', text: '扩增曲线', icon: 'chart' }, { key: 'results', text: '结果数据', icon: 'grid' }, { key: 'metadata', text: '文件信息', icon: 'file' }]" :key="t.key" role="tab" :aria-selected="tab === t.key" :class="{ active: tab === t.key }" @click="tab = t.key"><Icon :name="t.icon" :size="16" />{{ t.text }}</button></div>

              <template v-if="tab === 'curves'"><div class="chart-toolbar"><div><strong>扩增曲线</strong><small>{{ visibleResults.length }} 条曲线 · {{ selectedWells.length }} 个孔位</small></div><label class="check-label"><input v-model="compare" type="checkbox" /> 对比初始演示</label></div><div class="chart-wrap"><CurveChart :results="visibleResults" :threshold="current.parameters.threshold" /><div v-if="!visibleResults.length" class="chart-empty">选择至少一个孔位和通道以显示曲线</div><span class="chart-watermark">SIMULATED DATA</span></div><div class="channel-legend"><button v-for="channel in channels" :key="channel.name" :class="{ muted: !selectedChannels.includes(channel.name) }" :aria-pressed="selectedChannels.includes(channel.name)" @click="toggleChannel(channel.name)"><span :style="{ background: channel.color }"></span>{{ channel.name }}</button></div><div v-if="compare" class="comparison"><div class="section-kicker">初始演示 · 阈值 {{ defaults.threshold }}</div><CurveChart :results="originalResults" :threshold="defaults.threshold" /></div></template>

              <div v-else-if="tab === 'results'" class="results-panel"><div class="chart-toolbar"><div><strong>孔位分析结果</strong><small>显示已选择的孔位与通道</small></div><button class="button small" @click="exportCurrent"><Icon name="download" :size="14" />导出 JSON</button></div><div class="table-scroll result-table"><table><thead><tr><th>孔位</th><th>通道</th><th>Ct（模拟）</th><th>曲线表现</th></tr></thead><tbody><tr v-for="result in visibleResults" :key="result.well + result.channel"><td><b>{{ result.well }}</b></td><td><span class="channel-dot" :style="{ background: channels.find(c => c.name === result.channel)?.color }"></span>{{ result.channel }}</td><td class="monospace">{{ result.ct?.toFixed(2) ?? '—' }}</td><td><span :class="['status-badge', result.ct === null ? 'neutral' : '']">{{ result.ct === null ? '无扩增' : '有扩增' }}</span></td></tr><tr v-if="!visibleResults.length"><td colspan="4" class="empty-mini">请先选择孔位与通道</td></tr></tbody></table></div><p class="table-caption">演示结果仅用于验证交互，不代表真实检测结论。</p></div>
              <div v-else class="metadata"><dl><dt>文件名</dt><dd>{{ active.fileName }}</dd><dt>大小</dt><dd>{{ formatSize(active.size) }}（{{ active.size.toLocaleString() }} 字节）</dd><dt>加入时间</dt><dd>{{ formatTime(active.importedAt) }}</dd><dt>读取状态</dt><dd>已载入模拟数据，未解码真实文件</dd><template v-if="active.reference"><dt>参考目录</dt><dd>{{ referenceFolder }}</dd><dt>SHA-256</dt><dd class="monospace hash">{{ referenceSha256 }}</dd></template><dt>数据来源</dt><dd>确定性生成的演示曲线；不是 SDK 解析结果</dd><dt>原文件状态</dt><dd>保持不变；不在浏览器存储二进制内容</dd></dl></div>

              <div class="plate-section"><div class="plate-heading"><h3>孔位选择 <span>{{ selectedWells.length }} / 8</span></h3><div><button class="text-button" @click="selectedWells = [...wells]">全选</button><button class="text-button secondary" @click="selectedWells = []">清空</button></div></div><div class="plate-grid"><span></span><span v-for="n in 12" :key="n" class="plate-index">{{ n }}</span><template v-for="row in ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']" :key="row"><span class="plate-index">{{ row }}</span><button v-for="n in 12" :key="row + n" :disabled="!wells.includes(row + n)" :class="{ filled: wells.includes(row + n), chosen: selectedWells.includes(row + n) }" :aria-label="'孔位 ' + row + n" :aria-pressed="selectedWells.includes(row + n)" @click="toggleWell(row + n)">{{ wells.includes(row + n) ? row + n : '' }}</button></template></div><div class="plate-legend"><span><i class="selected-key"></i>已选择</span><span><i class="available-key"></i>可选择</span><span><i></i>无演示数据</span></div></div>
            </section><div v-else class="panel empty-mini">请先添加实验文件。</div>

            <aside class="panel parameters-panel"><div class="panel-heading"><h2>分析参数</h2><button class="icon-button" title="重置参数" aria-label="重置参数" @click="Object.assign(parameters, defaults)"><Icon name="reset" :size="16" /></button></div><div class="parameter-content"><div class="section-label">基线设置<span>BASELINE</span></div><label class="switch-row"><span>自动基线</span><input v-model="parameters.autoBaseline" class="switch" type="checkbox" /></label><div class="number-pair"><label>起始循环<input v-model.number="parameters.baselineStart" type="number" min="1" max="19" :disabled="parameters.autoBaseline" aria-label="基线起始循环" /></label><label>结束循环<input v-model.number="parameters.baselineEnd" type="number" min="2" max="20" :disabled="parameters.autoBaseline" aria-label="基线结束循环" /></label></div><p class="field-help">自动模式使用演示基线 3–15。</p><div class="divider"></div><div class="section-label">阈值设置<span>THRESHOLD</span></div><label class="threshold-field">荧光阈值<div><input v-model.number="parameters.threshold" type="number" min="10" max="3000" aria-label="荧光阈值" /><span>ΔRn</span></div></label><p class="field-help">修改参数后执行二次分析，更新曲线与模拟 Ct。</p><div v-if="dirty" class="dirty-note">参数已修改，尚未应用</div><div class="divider"></div><div class="analysis-status"><Icon name="check" :size="17" /><div>{{ current?.id === 'initial' ? '初始演示已载入' : '模拟分析已完成' }}<small>{{ current?.id === 'initial' ? '尚未创建分析版本' : '本次结果已加入分析记录' }}</small></div></div><button class="button primary full" :disabled="busy || !active" @click="reanalyze"><Icon name="play" :size="16" />{{ busy ? '处理中…' : '执行二次分析' }}</button><button class="button full" :disabled="!current" @click="modal = 'save'"><Icon name="save" :size="16" />保存 .pcr</button><button class="text-button export-link" :disabled="!current" @click="exportCurrent"><Icon name="download" :size="15" />导出模拟分析 JSON</button><div class="parameter-note"><Icon name="info" :size="15" /><span>真实读取、二次分析与保存待 C# SDK 接入。</span></div></div></aside>
          </section>
        </template>

        <section v-else-if="view === 'files'" class="panel library-panel"><div class="panel-heading"><div><h2>全部实验文件</h2><p>文件名与分析数据分别管理，原文件保持不变。</p></div><label class="search"><Icon name="search" :size="16" /><input v-model="query" placeholder="搜索文件" aria-label="搜索文件" /></label></div><div class="table-scroll"><table><thead><tr><th>实验名称</th><th>文件名</th><th>大小</th><th>数据状态</th><th>操作</th></tr></thead><tbody><tr v-for="e in filteredExperiments" :key="e.id"><td><strong>{{ e.name }}</strong><small class="cell-subtitle">{{ formatTime(e.importedAt) }}</small></td><td>{{ e.fileName }}</td><td>{{ formatSize(e.size) }}</td><td><span class="micro-badge amber">{{ e.reference ? '参考文件 · 模拟数据' : '模拟读取' }}</span></td><td><button class="text-button" @click="selectExperiment(e); view = 'workspace'">进入分析 <Icon name="arrow" :size="14" /></button></td></tr><tr v-if="!filteredExperiments.length"><td colspan="5" class="empty-mini">没有匹配的文件</td></tr></tbody></table></div></section>

        <section v-else-if="view === 'history'" class="panel library-panel"><div class="panel-heading"><div><h2>模拟分析版本</h2><p>保存最近 50 次分析记录，可回看参数、结果或导出快照。</p></div><span class="micro-badge">{{ history.length }} 个版本</span></div><div v-if="!history.length" class="empty-state"><Icon name="clock" :size="36" /><h3>还没有分析记录</h3><p>在实验工作台调整参数并执行二次分析后，记录将显示在这里。</p><button class="button primary" @click="view = 'workspace'">前往工作台</button></div><div v-else class="table-scroll"><table><thead><tr><th>实验 / 版本</th><th>分析时间</th><th>基线</th><th>阈值</th><th>操作</th></tr></thead><tbody><tr v-for="h in history" :key="h.id"><td><strong>{{ h.experimentName }}</strong><small class="cell-subtitle monospace">{{ h.id.slice(0, 8) }} · 模拟版本</small></td><td>{{ formatTime(h.createdAt) }}</td><td>{{ h.parameters.autoBaseline ? '自动 3–15' : `${h.parameters.baselineStart}–${h.parameters.baselineEnd}` }}</td><td>{{ h.parameters.threshold }}</td><td class="actions"><button class="text-button" @click="openHistory(h)">查看</button><button class="text-button secondary" @click="exportSnapshot(h)">导出 JSON</button></td></tr></tbody></table></div></section>

        <section v-else class="settings-grid"><article class="panel settings-card"><div class="settings-icon"><Icon name="settings" :size="24" /></div><h2>C# SDK 分析服务</h2><p>Java 保留业务与文件管理，独立的 C# 服务负责调用厂家 SDK。</p><div class="connection-state"><span class="tiny-dot"></span>真实 SDK 离线分析已验证</div><dl><dt>页面运行方式</dt><dd>数据中心连接后端，模拟入口独立</dd><dt>模拟数据存储</dt><dd>当前浏览器，保留最近 50 个版本</dd><dt>真实 .pcr 读取</dt><dd>数据中心已接入真实离线分析</dd><dt>真实二次分析</dt><dd>数据中心已接入真实离线分析</dd><dt>真实 .pcr 保存</dt><dd>数据中心保存新文件并关联版本</dd></dl></article><article class="panel settings-card"><h2>服务接口约定</h2><p>页面 → Java HTTP API → C# HTTP 服务 → 厂家 SDK。演示按钮使用独立模拟服务。</p><div class="endpoint" v-for="(path, key) in pcrApi" :key="key"><span>{{ key === 'capabilities' ? 'GET' : 'POST' }}</span><code>{{ path }}</code></div><div class="integration-note"><Icon name="info" :size="18" /><p>数据中心通过 Java 调用 C# 分析服务；离线快照和模拟工作台不发送分析请求。</p></div></article></section>
        <footer><span>PCR Studio <span class="footer-dot">·</span> 实验数据分析工作空间</span><span>{{ view === 'server' ? '服务器工作空间' : view === 'real' ? '真实离线快照' : '模拟模式' }} <span class="footer-dot">·</span> 原文件保持不变</span></footer>
      </div>
    </main>
    <Transition name="toast"><div v-if="toast" class="toast-message" role="status"><Icon name="info" :size="18" />{{ toast }}<button class="icon-button" aria-label="关闭提示" @click="toast = ''"><Icon name="close" :size="15" /></button></div></Transition>
    <div v-if="modal" class="modal-backdrop" @click.self="modal = ''"><section class="modal" role="dialog" aria-modal="true" aria-labelledby="save-title"><div class="modal-icon"><Icon name="save" :size="26" /></div><h2 id="save-title">保存真实 .pcr 文件</h2><p>真实 PCR 保存请进入数据中心。当前演示不会修改原文件，也不会把模拟 JSON 伪装成 .pcr。</p><div class="modal-info">你可以导出演示快照，保存本次模拟参数与曲线，文件格式为 <b>.analysis.json</b>。</div><div class="modal-actions"><button class="button" @click="modal = ''">关闭</button><button class="button primary" @click="exportCurrent(); modal = ''">导出演示快照</button></div></section></div>
  </div>
</template>
