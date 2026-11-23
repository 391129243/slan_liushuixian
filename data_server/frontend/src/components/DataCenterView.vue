<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CurveChart from './CurveChart.vue'
import Icon from './Icon.vue'
import SdkParameterEditor from './SdkParameterEditor.vue'
import RuleVersionEditor from './RuleVersionEditor.vue'
import { formatSize, type Parameters } from '../domain'
import { serverApi, ApiError, type FileRecord, type ReportRecord, type AnalysisRecord, type Page, type VendorExperiment, type Curve, type RuleContext, type ParameterSet, type AdvancedParameters } from '../server-api'

const username = ref('admin'), password = ref(''), connected = ref(serverApi.authenticated()), busy = ref(false)
const error = ref(''), message = ref(''), tab = ref<'reports' | 'files' | 'analyses'>('reports'), page = ref(0)
const summary = ref({ reports: 0, files: 0, devices: 0 }), files = ref<FileRecord[]>([])
const reports = ref<Page<ReportRecord>>({ total: 0, items: [], offset: 0, limit: 20 }), analyses = ref<Page<AnalysisRecord>>({ total: 0, items: [], offset: 0, limit: 20 })
const selectedReport = ref<ReportRecord>(), current = ref<VendorExperiment>(), selectedRun = ref<AnalysisRecord>()
const session = ref<{ experimentId: string; reportId: string | null; sdkVersion?: string }>(), selectedFileId = ref(''), candidateFile = ref('')
const input = ref<HTMLInputElement>(), filename = ref('analysis-result.pcr'), historyReportId = ref('')
const parameters = ref<Parameters>({ autoBaseline: true, baselineStart: 6, baselineEnd: 12, threshold: 0.24 })
const parameterSets = ref<ParameterSet[]>([]), advancedMode = ref(false), advancedValid = ref(false), advancedParameters = ref<AdvancedParameters>({ scope: 'SELECTED', projects: [], targets: [] })
const ruleVersionId = ref(''), ruleDraftDirty = ref(false)
function advancedChanged(value: AdvancedParameters, valid: boolean) { advancedParameters.value = value; advancedValid.value = valid }
const curveMode = ref<'amplification' | 'raw' | 'rawMelt' | 'peak'>('amplification'), channel = ref('')
let generation = 0
let attempt: { key: string; requestId: string } | undefined
const retryPending = ref(false)
const ruleContext = ref<RuleContext>()
const ruleEvaluation = computed(() => selectedRun.value?.result?.applicationRuleEvaluation)
const combined = computed(() => selectedRun.value?.result?.combinedConclusions)
const ruleStatus = (value: string) => ({ EVALUATED: '判读完成', PARTIAL: '部分判读，存在缺失数据或无效规则', INVALID_RULES: '规则格式无效', INVALID_DATA: '结果数据无效', MISSING_RULES: '未提供规则' }[value] || value)
const ruleReason = (value?: string) => ({ MISSING_CHANNEL: '未找到对应通道', AMBIGUOUS_CHANNEL: '同名通道不唯一', MISSING_OR_INVALID_CT: 'Ct 缺失或无效', MISSING_PEAK_DATA: '未提供熔解峰数据', INVALID_PEAK_DATA: '峰数据不完整', NON_AMPLIFYING_CT: 'SDK 无有限 Ct' }[value || ''] || value || '')
const conclusionStatus = (value: string) => ({ POSITIVE: '阳性', NEGATIVE: '阴性', SUSPICIOUS: '可疑样本', UNDETERMINED: '未判定' }[value] || value)
const date = (value: number) => new Date(value).toLocaleString('zh-CN', { hour12: false })
const status = (value: string) => ({ RUNNING: '分析中', SUCCEEDED: '分析成功', FAILED: '失败', NOT_SAVED: '未保存', SAVING: '保存中', SAVED: '已保存' }[value] || value)
const validParameters = computed(() => Number.isInteger(parameters.value.baselineStart) && Number.isInteger(parameters.value.baselineEnd) && parameters.value.baselineStart >= 1 && parameters.value.baselineStart < parameters.value.baselineEnd && parameters.value.baselineEnd <= 20 && Number.isFinite(parameters.value.threshold) && parameters.value.threshold > 0)
// C# SDK amplification curves are double[] (one measurement per cycle); Android can send X/Y objects.
// Numeric melt arrays cannot be placed on a temperature axis without explicit temperature coordinates.
const points = (value?: Curve) => (value || []).flatMap((p, index): [number, number][] => {
  if (typeof p === 'number') return !['rawMelt', 'peak'].includes(curveMode.value) && Number.isFinite(p) ? [[index + 1, p]] : []
  return p && Number.isFinite(p.X) && Number.isFinite(p.Y) ? [[p.X, p.Y]] : []
})
const targets = computed(() => (current.value?.WellResult || []).flatMap(w => (w.TargetResults || []).map(t => ({
  well: w.SampleWellNames?.join(', ') || String(w.ID ?? '未知孔位'), channel: t.Dye || t.Name || `通道 ${t.Channel}`,
  ct: typeof t.Ct === 'number' && Number.isFinite(t.Ct) ? t.Ct : null,
  points: points(curveMode.value === 'amplification' ? t.AMPCurve : curveMode.value === 'raw' ? t.RawCurve : curveMode.value === 'rawMelt' ? t.RawMeltCurve : t.PeakCurve)
}))))
const channels = computed(() => [...new Set(targets.value.map(t => t.channel))])
const visible = computed(() => targets.value.filter(t => !channel.value || t.channel === channel.value))
const total = computed(() => tab.value === 'reports' ? reports.value.total : tab.value === 'analyses' ? analyses.value.total : summary.value.files)
const hasNext = computed(() => tab.value === 'files' ? files.value.length === 20 : (page.value + 1) * 20 < total.value)
const originalFiles = computed(() => files.value.filter(f => f.upload_source !== 'SDK_ANALYSIS'))
function clearSelection() { selectedReport.value = undefined; current.value = undefined; selectedRun.value = undefined; session.value = undefined; selectedFileId.value = ''; attempt = undefined; retryPending.value = false; channel.value = ''; ruleContext.value = undefined; parameterSets.value = []; advancedMode.value = false; ruleVersionId.value = ''; ruleDraftDirty.value = false }
function logout() { generation++; serverApi.clear(); connected.value = false; password.value = ''; busy.value = false; clearSelection(); files.value = []; reports.value.items = []; analyses.value.items = []; message.value = ''; error.value = '' }
async function action(work: () => Promise<void>) {
  if (busy.value) return
  const epoch = generation; busy.value = true; error.value = ''; message.value = ''
  try { await work() } catch (e) {
    if (epoch === generation) {
      if (e instanceof ApiError && e.status === 401) logout()
      error.value = (e as Error).message
    }
  } finally { if (epoch === generation) busy.value = false }
}
async function load() {
  const offset = page.value * 20
  summary.value = await serverApi.json('/summary')
  if (tab.value === 'reports') reports.value = await serverApi.json(`/reports?offset=${offset}&limit=20`)
  else if (tab.value === 'files') files.value = await serverApi.json(`/files?offset=${offset}&limit=20`)
  else analyses.value = await serverApi.json(`/pcr/analyses?offset=${offset}&limit=20${historyReportId.value ? '&reportId=' + encodeURIComponent(historyReportId.value) : ''}`)
}
async function login() { await action(async () => { summary.value = await serverApi.login(username.value, password.value); password.value = ''; connected.value = true; page.value = 0; await load() }) }
function switchTab(next: typeof tab.value) { if (busy.value) return; tab.value = next; page.value = 0; void action(load) }
function turn(delta: number) { page.value += delta; void action(load) }
function showExperiment(experiment?: VendorExperiment) {
  current.value = experiment; channel.value = ''; curveMode.value = (experiment?.WellResult || []).some(w => w.TargetResults?.some(t => t.AMPCurve?.length)) ? 'amplification' : 'rawMelt'
}
async function upload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  await action(async () => { const result = await serverApi.upload(file); tab.value = 'files'; page.value = 0; await load(); selectedFileId.value = result.FileID; message.value = result.Duplicate ? '文件已存在，已复用原文件记录。' : '文件已上传并保存，可以读取分析。' })
  if (input.value) input.value.value = ''
}
async function openReport(row: ReportRecord) {
  await action(async () => { const detail = await serverApi.json<ReportRecord>(`/reports/${row.id}`); clearSelection(); selectedReport.value = detail; selectedFileId.value = detail.file_id || ''; showExperiment(detail.Experiment)
    if (detail.bundle_id) {
      const bundle = await serverApi.json<{ manifest: RuleContext['manifest'] }>(`/bundles/${detail.bundle_id}`)
      ruleContext.value = { bundleId: detail.bundle_id, manifest: bundle.manifest, evaluationStatus: 'ARCHIVED_NOT_EVALUATED' }
    }
    if (!detail.file_id) { files.value = await serverApi.json('/files?offset=0&limit=100'); candidateFile.value = originalFiles.value[0]?.id || '' }
  })
}
async function bindFile() {
  if (!selectedReport.value || !candidateFile.value) return
  await action(async () => { await serverApi.json(`/reports/${selectedReport.value!.id}/file/${candidateFile.value}`, undefined, 'PUT'); selectedReport.value!.file_id = candidateFile.value; selectedFileId.value = candidateFile.value; message.value = '原始文件已明确关联，可以读取分析。' })
}
async function read(fileId: string, context?: { reportId?: string | null; parentAnalysisId?: string }) {
  await action(async () => {
    const result = await serverApi.json<{ experimentId: string; experiment: VendorExperiment; reportId: string | null; sdkVersion?: string; ruleContext?: RuleContext | null; parameterSets?: ParameterSet[]; ruleVersionId?: string }>('/pcr/read', { fileId, ...context })
    parameterSets.value = result.parameterSets || []; advancedMode.value = false; ruleVersionId.value = result.ruleVersionId || ''; ruleDraftDirty.value = false
    ruleContext.value = result.ruleContext || undefined; session.value = result; selectedFileId.value = fileId; selectedRun.value = undefined; attempt = undefined; retryPending.value = false; showExperiment(result.experiment)
    const name = files.value.find(f => f.id === fileId)?.file_name || 'analysis-result.pcr'; filename.value = name.replace(/\.pcr$/i, '') + '-analysis.pcr'; message.value = '已读取 SDK 结果，调整参数后可创建新的分析版本。'
  })
}
async function analyze() {
  if (!session.value || !validParameters.value || ruleDraftDirty.value || (advancedMode.value && !advancedValid.value)) return
  await action(async () => {
    const key = JSON.stringify({ experimentId: session.value!.experimentId, parameters: { ...parameters.value, ...(advancedMode.value ? advancedParameters.value : {}) }, ruleVersionId: ruleVersionId.value || 'ORIGINAL' })
    if (!attempt || attempt.key !== key) attempt = { key, requestId: crypto.randomUUID() }
    retryPending.value = true
    try {
      const result = await serverApi.json<{ analysisRunId: string; experiment: VendorExperiment }>('/pcr/reanalyze', { ...JSON.parse(key), requestId: attempt.requestId })
      selectedRun.value = await serverApi.json(`/pcr/analyses/${result.analysisRunId}`); showExperiment(result.experiment)
      attempt = undefined; retryPending.value = false; message.value = '二次分析完成，参数、操作者和结果已保存到数据库。'
    } catch (e) { if (e instanceof ApiError && [400, 404, 422].includes(e.status)) { attempt = undefined; retryPending.value = false }; throw e }
  })
}
async function openRun(row: AnalysisRecord) {
  await action(async () => { const detail = await serverApi.json<AnalysisRecord>(`/pcr/analyses/${row.id}`); clearSelection(); selectedRun.value = detail; ruleContext.value = detail.ruleSnapshot?.bundleId ? detail.ruleSnapshot : undefined; ruleVersionId.value = detail.rule_version_id || ''; selectedFileId.value = detail.input_file_id; parameterSets.value = detail.result?.parameterSets || []; if (detail.parameters) { const p = detail.parameters; parameters.value = { autoBaseline: p.autoBaseline, baselineStart: p.baselineStart, baselineEnd: p.baselineEnd, threshold: p.threshold } }; showExperiment(detail.result?.experiment); message.value = '已从数据库读取历史版本。' })
}
async function save() {
  if (!selectedRun.value) return
  await action(async () => { const id = selectedRun.value!.id; await serverApi.json('/pcr/save', { analysisId: id, fileName: filename.value }); selectedRun.value = await serverApi.json(`/pcr/analyses/${id}`); message.value = '新的 .pcr 已保存并关联到当前分析版本。'; await load() })
}
function download(fileId: string, name = 'result.pcr') { void action(async () => { await serverApi.download(`/files/${fileId}/download`, name); message.value = '文件下载已开始。' }) }
function historyForReport() { historyReportId.value = selectedReport.value?.id || ''; switchTab('analyses') }
onMounted(() => { if (connected.value) void action(load) })
</script>

<template>
  <div class="data-center">
    <section v-if="!connected" class="panel dc-login">
      <div class="dc-login-mark"><Icon name="folder" :size="28" /></div><h2>登录数据中心</h2><p>管理服务器上的实验文件、真实结果与分析版本。</p>
      <form @submit.prevent="login"><label>账号<input v-model="username" autocomplete="username" required aria-label="数据中心账号" /></label><label>密码<input v-model="password" type="password" autocomplete="current-password" required aria-label="数据中心密码" /></label><button class="button primary" :disabled="busy">{{ busy ? '正在连接…' : '登录数据中心' }}</button></form>
      <small>后端默认地址 127.0.0.1:8088；账号由服务端验证。</small>
    </section>
    <div v-if="error" class="dc-alert" role="alert"><Icon name="info" :size="17" />{{ error }}</div>
    <div v-if="message" class="dc-message" role="status"><Icon name="check" :size="17" />{{ message }}</div>
    <template v-if="connected">
      <section class="dc-summary"><article v-for="item in [{label:'实验报告',value:summary.reports},{label:'存储文件',value:summary.files},{label:'登记设备',value:summary.devices}]" :key="item.label" class="panel"><small>{{ item.label }}</small><strong>{{ item.value }}</strong></article><article class="panel dc-account"><span>已连接数据服务</span><button class="text-button" :disabled="busy" @click="logout">退出登录</button></article></section>
      <section class="panel dc-library">
        <div class="dc-tools"><div class="tabs" role="tablist" aria-label="数据中心列表"><button v-for="item in [{key:'reports',text:'服务器报告'},{key:'files',text:'服务器文件'},{key:'analyses',text:'分析版本'}] as const" :key="item.key" role="tab" :aria-selected="tab===item.key" :class="{active:tab===item.key}" :disabled="busy" @click="switchTab(item.key)">{{ item.text }}</button></div><div class="dc-actions"><button class="button" :disabled="busy" @click="action(load)">刷新列表</button><button class="button primary" :disabled="busy" @click="input?.click()"><Icon name="upload" :size="15" />上传 .pcr</button><input ref="input" type="file" accept=".pcr" class="sr-only" aria-label="上传服务器 PCR 文件" @change="upload" /></div></div>
        <div v-if="tab==='analyses' && historyReportId" class="dc-filter">仅显示当前报告版本<button class="text-button" :disabled="busy" @click="historyReportId=''; page=0; action(load)">显示全部</button></div>
        <div class="table-scroll">
          <table v-if="tab==='reports'"><thead><tr><th>实验名称 / 设备</th><th>实验操作者</th><th>接收时间</th><th>原始文件</th><th>操作</th></tr></thead><tbody><tr v-for="r in reports.items" :key="r.id"><td><strong>{{ r.experiment_name }}</strong><small class="cell-subtitle">{{ r.device_id }}</small></td><td>{{ r.operator_name || '未提供' }}</td><td>{{ date(r.received_at) }}</td><td>{{ r.file_id ? '已关联' : '待关联' }}</td><td><button class="text-button" :disabled="busy" @click="openReport(r)">查看报告</button></td></tr><tr v-if="!reports.items.length"><td colspan="5" class="empty-mini">暂无上报报告，可以先上传 PCR 文件进行分析。</td></tr></tbody></table>
          <table v-else-if="tab==='files'"><thead><tr><th>文件名</th><th>大小 / 上传账号</th><th>来源</th><th>接收时间</th><th>操作</th></tr></thead><tbody><tr v-for="f in files" :key="f.id"><td :title="f.file_sha256"><strong>{{ f.file_name }}</strong><small class="cell-subtitle monospace">SHA256 {{ f.file_sha256.slice(0,16) }}…</small></td><td>{{ formatSize(f.file_size) }}<small class="cell-subtitle">{{ f.uploaded_by_username || '设备 / 历史上传' }}</small></td><td>{{ f.upload_source==='SDK_ANALYSIS' ? '分析输出' : f.upload_source==='ADMIN_UPLOAD' ? '人员上传' : '设备 / 历史文件' }}</td><td>{{ date(f.received_at) }}</td><td class="dc-row-actions"><button class="text-button" :disabled="busy" @click="read(f.id)">读取分析</button><button class="text-button" :disabled="busy" @click="download(f.id,f.file_name)">下载</button></td></tr><tr v-if="!files.length"><td colspan="5" class="empty-mini">暂无服务器文件，上传一个 .pcr 开始。</td></tr></tbody></table>
          <table v-else><thead><tr><th>版本编号 / 归属</th><th>分析账号</th><th>分析状态</th><th>文件状态</th><th>创建时间</th><th>操作</th></tr></thead><tbody><tr v-for="r in analyses.items" :key="r.id"><td><strong class="monospace">{{ r.id.slice(0,8) }}</strong><small class="cell-subtitle">{{ r.report_id ? '关联报告' : '独立文件分析' }}</small></td><td>{{ r.operator_username }}</td><td>{{ status(r.status) }}</td><td>{{ status(r.file_save_status) }}</td><td>{{ date(r.created_at) }}</td><td><button class="text-button" :disabled="busy" @click="openRun(r)">查看版本</button></td></tr><tr v-if="!analyses.items.length"><td colspan="6" class="empty-mini">暂无分析版本。读取文件并执行二次分析后，结果会自动入库。</td></tr></tbody></table>
        </div>
        <div class="dc-pagination"><span>第 {{ page+1 }} 页<span v-if="tab!=='files'"> · 共 {{ total }} 条</span></span><button class="button" :disabled="busy||page===0" @click="turn(-1)">上一页</button><button class="button" :disabled="busy||!hasNext" @click="turn(1)">下一页</button></div>
      </section>
      <section v-if="current || selectedReport || selectedRun" class="panel dc-workspace">
        <div class="dc-workspace-heading"><div><div class="section-kicker">SERVER EXPERIMENT</div><h2>{{ current?.Name || selectedReport?.experiment_name || '分析版本详情' }}</h2><p>{{ selectedRun ? `版本 ${selectedRun.id}` : session ? 'SDK 文件读取结果' : '设备上报原始结果' }}</p></div><div class="dc-actions"><button v-if="selectedReport" class="button" :disabled="busy" @click="historyForReport">该报告的分析历史</button><button v-if="selectedFileId && !session" class="button" :disabled="busy" @click="read(selectedFileId,{reportId:selectedRun?.report_id || selectedReport?.id,parentAnalysisId:selectedRun?.parent_analysis_id || undefined})">读取输入文件</button></div></div>
        <div v-if="selectedReport && !selectedReport.file_id" class="dc-binding"><span>该报告尚无原始文件，请明确关联后再分析。</span><select v-model="candidateFile" aria-label="待关联原始文件"><option value="">选择已上传原始文件</option><option v-for="f in originalFiles" :key="f.id" :value="f.id">{{ f.file_name }}</option></select><button class="button" :disabled="busy||!candidateFile" @click="bindFile">关联原始文件</button></div>
        <div v-if="selectedRun" class="dc-version"><span>分析：{{ status(selectedRun.status) }}</span><span>文件：{{ status(selectedRun.file_save_status) }}</span><span>分析账号：{{ selectedRun.operator_username }}</span><span v-if="selectedRun.saved_by_username">保存账号：{{ selectedRun.saved_by_username }}</span><span>SDK：{{ selectedRun.sdk_version || '未提供版本号' }}</span><strong v-if="selectedRun.error_message || selectedRun.save_error_message">{{ selectedRun.error_message || selectedRun.save_error_message }}</strong></div>
        <section v-if="ruleContext" class="dc-rules">
          <div class="dc-chart-tools"><strong>规则与结论快照 · {{ ruleContext.manifest.files.filter(f=>f.role!=='PCR').length }} 个附件</strong><button class="button" :disabled="busy" @click="action(()=>serverApi.download('/bundles/'+ruleContext!.bundleId+'/download','original-result-bundle.zip'))">下载原始结果包</button></div>
          <p class="dc-note">保留实验当时的规则及原始结论。SDK 重算 Ct 与熔解峰后，服务器按 Android 的 .ct/.peak 区间规则重新判读；原始结论单独归档。</p>
          <div class="table-scroll"><table aria-label="规则与结论附件"><thead><tr><th>文件名</th><th>类型</th><th>大小</th><th>SHA-256</th><th>操作</th></tr></thead><tbody><tr v-for="f in ruleContext.manifest.files.filter(f=>f.role!=='PCR')" :key="f.fileName"><td>{{ f.fileName }}</td><td>{{ f.role==='CT_RULE'?'Ct 规则':f.role==='PEAK_RULE'?'峰值规则':'原始结论附件' }}</td><td>{{ formatSize(f.size) }}</td><td class="monospace" :title="f.sha256">{{ f.sha256.slice(0,16) }}…</td><td><button class="text-button" :disabled="busy" @click="action(()=>serverApi.download('/bundles/'+ruleContext!.bundleId+'/attachments/'+encodeURIComponent(f.fileName),f.fileName))">下载附件</button></td></tr></tbody></table></div>
        </section>
        <RuleVersionEditor v-if="current" :key="[session?.experimentId,selectedRun?.id,ruleContext?.bundleId].join(':')" :bundle-id="ruleContext?.bundleId" :version-id="ruleVersionId || undefined" :disabled="busy" @update:version-id="ruleVersionId=$event" @dirty="ruleDraftDirty=$event" />
        <div class="dc-chart-tools"><label><input v-model="advancedMode" type="checkbox" :disabled="busy || !parameterSets.length" aria-label="按项目和通道配置高级参数" />按项目和通道配置高级参数</label><span v-if="advancedMode">只修改选中的靶标及已调整的项目算法</span></div>
        <SdkParameterEditor v-if="advancedMode" :sets="parameterSets" :disabled="busy" @change="advancedChanged" />
        <div class="dc-parameters"><label><input v-model="parameters.autoBaseline" type="checkbox" :disabled="busy || advancedMode" />自动基线</label><label>基线起点<input v-model.number="parameters.baselineStart" type="number" min="1" max="19" step="1" aria-label="服务器基线起点" :disabled="busy || advancedMode" /></label><label>基线终点<input v-model.number="parameters.baselineEnd" type="number" min="2" max="20" step="1" aria-label="服务器基线终点" :disabled="busy || advancedMode" /></label><label>荧光阈值<input v-model.number="parameters.threshold" type="number" min="0" step="any" aria-label="服务器荧光阈值" :disabled="busy || advancedMode" /></label><button class="button primary" :disabled="busy||!session||!validParameters||ruleDraftDirty||(advancedMode&&!advancedValid)" @click="analyze">{{ retryPending ? '重试本次请求' : '执行服务器二次分析' }}</button></div>
        <p class="dc-note">{{ advancedMode ? '高级模式使用上方逐项目、逐通道配置；下方全局参数不参与覆盖。' : '全局模式将基线与手动荧光阈值应用于全部靶标。' }} 每次成功分析保存新版本。{{ ruleDraftDirty ? '请先保存规则草稿。' : '' }}</p>
        <div v-if="selectedRun?.status==='SUCCEEDED'" class="dc-save"><template v-if="!selectedRun.output_file_id"><label>输出文件名<input v-model="filename" maxlength="255" aria-label="分析输出文件名" :disabled="busy" /></label><button class="button primary" :disabled="busy||!filename.toLowerCase().endsWith('.pcr')" @click="save">保存新 .pcr</button></template><template v-else><button class="button primary" :disabled="busy" @click="download(selectedRun.output_file_id!)">下载结果 .pcr</button><button class="button" :disabled="busy" @click="read(selectedRun.output_file_id!,{parentAnalysisId:selectedRun.id})">基于此结果再分析</button><button class="button" :disabled="busy" @click="action(()=>serverApi.download('/pcr/analyses/'+selectedRun!.id+'/bundle','analysis-result-bundle.zip'))">下载分析结果包</button></template><button class="button" :disabled="busy" @click="action(()=>serverApi.download('/pcr/analyses/'+selectedRun!.id+'/result','analysis-result.json'))">导出版本 JSON</button></div>
        <template v-if="current"><div class="dc-chart-tools"><strong>{{ visible.length }} 条通道结果 · {{ visible.filter(t=>t.ct!==null).length }} 个有效 Ct</strong><label>曲线类型<select v-model="curveMode" aria-label="服务器曲线类型"><option value="amplification">扩增曲线</option><option value="raw">原始扩增曲线</option><option value="rawMelt">熔解曲线</option><option value="peak">峰曲线</option></select></label><label>通道<select v-model="channel" aria-label="服务器通道筛选"><option value="">全部通道</option><option v-for="c in channels" :key="c">{{ c }}</option></select></label></div>
          <CurveChart v-if="visible.some(t=>t.points.length)" :results="visible" :threshold="selectedRun?.parameters?.threshold || 0" :mode="curveMode" actual /><div v-else class="empty-mini">当前结果没有此类型的曲线数据。</div>
          <div class="table-scroll"><table aria-label="服务器靶标结果"><thead><tr><th>孔位</th><th>通道</th><th>Ct</th><th>当前曲线点数</th></tr></thead><tbody><tr v-for="(r,i) in visible" :key="`${r.well}-${r.channel}-${i}`"><td>{{ r.well }}</td><td>{{ r.channel }}</td><td :title="r.ct===null?'SDK 未提供有限 Ct':String(r.ct)">{{ r.ct===null?'—':r.ct.toFixed(6) }}</td><td>{{ r.points.length }}</td></tr></tbody></table></div>
          <section v-if="combined && combined.status!=='NOT_CONFIGURED'" class="dc-rules">
            <div class="dc-chart-tools"><strong>CT / PEAK 综合判读 · {{ ruleStatus(combined.status) }}</strong><span>由本版本保存的组合策略生成</span></div>
            <div class="table-scroll"><table aria-label="综合判读结果"><thead><tr><th>孔位</th><th>综合项目</th><th>组合方式</th><th>CT / PEAK 状态</th><th>综合结论</th></tr></thead><tbody><tr v-for="(r,i) in combined.rows" :key="i"><td>{{ r.sampleWellNames?.join(', ') || r.well }}</td><td>{{ r.name }}</td><td>{{ r.mode==='ALL'?'全部命中':'任一命中' }}</td><td>{{ conclusionStatus(r.ctStatus) }} / {{ conclusionStatus(r.peakStatus) }}</td><td>{{ r.conclusion }}</td></tr></tbody></table></div>
          </section>
          <section v-if="ruleEvaluation" class="dc-rules">
            <div class="dc-chart-tools"><strong>本版本规则判读 · {{ ruleStatus(ruleEvaluation.status) }}</strong><span>{{ ruleEvaluation.engineVersion }}</span></div>
            <p class="dc-note">使用本版本绑定的规则快照。缺少通道、Ct 或峰数据时显示未判定；原始结论附件不参与重新判读。</p>
            <p v-for="(e,i) in ruleEvaluation.errors" :key="i" class="dc-note">{{ e.fileName || '数据' }}：{{ e.message }}</p>
            <div v-if="ruleEvaluation.rows.length" class="table-scroll"><table aria-label="本版本规则判读"><thead><tr><th>孔位 / 通道</th><th>项目 / 规则来源</th><th>规则范围</th><th>本次测量</th><th>判读</th></tr></thead><tbody><tr v-for="(r,i) in ruleEvaluation.rows" :key="i"><td>{{ r.sampleWellNames?.join(', ') || r.well }} / {{ r.channel }}</td><td>{{ r.category }}<small class="cell-subtitle">{{ r.fileName }}</small></td><td>{{ r.type==='CT'?'Ct':'Tm' }} ∈ [{{ r.lower }}, {{ r.upper }}]<small v-if="r.type==='CT'" class="cell-subtitle">可疑上限 {{ r.suspiciousUpper }}</small><small v-else class="cell-subtitle">Rm ≥ {{ r.minimumRm }}</small></td><td>{{ r.type==='CT' ? (r.actualCt ?? '—') : r.observedPeaks?.map(p=>`Tm ${p.tm} / Rm ${p.rm}`).join('；') || '—' }}</td><td>{{ r.conclusion }}<small v-if="r.reason" class="cell-subtitle">{{ ruleReason(r.reason) }}</small></td></tr></tbody></table></div>
          </section>
        </template>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dc-library,.dc-workspace{min-width:0}.data-center{display:grid;gap:20px;min-width:0}.dc-login{max-width:440px;width:100%;padding:34px;margin:15px auto;box-sizing:border-box}.dc-login-mark{color:#008d7b;margin-bottom:20px}.dc-login h2{font-size:21px}.dc-login p{font-size:12px;color:#7a929f;line-height:1.9;margin:12px 0 22px}.dc-login form{display:grid;gap:18px}.dc-login small{display:block;font-size:11px;color:#8ba0ad;line-height:1.8;margin-top:20px}.dc-login label,.dc-parameters label,.dc-save label{display:flex;flex-direction:column;gap:8px;font-size:12px;color:#627d8a}.dc-login input,.dc-parameters input[type=number],.dc-save input,select{border:1px solid #dbe5e9;border-radius:5px;padding:10px;background:#fff;color:#355365;min-width:0;box-sizing:border-box}.dc-alert,.dc-message{display:flex;align-items:flex-start;gap:9px;border-radius:6px;padding:15px 18px;font-size:12px;line-height:1.8;overflow-wrap:anywhere}.dc-alert{color:#a44840;background:#fff0ed;border:1px solid #f3d6ce}.dc-message{color:#22775f;background:#edf8f2;border:1px solid #d5ebdf}.dc-summary{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:17px}.dc-summary article{padding:22px}.dc-summary small{font-size:12px;color:#8297a3}.dc-summary strong{display:block;font-size:29px;margin-top:10px;font-weight:550}.dc-account{display:flex;flex-direction:column;justify-content:center;gap:12px;font-size:12px;color:#008d7b}.dc-tools{display:flex;justify-content:space-between;align-items:center;gap:14px;border-bottom:1px solid #e8eef2;padding:0 20px}.dc-tools .tabs{border:0;padding:0;gap:24px}.dc-tools .tabs button{font-size:12px}.dc-actions,.dc-row-actions{display:flex;gap:10px;align-items:center;flex-wrap:wrap}.dc-library table,.dc-workspace table{font-size:12px}.dc-library td,.dc-library th,.dc-workspace td,.dc-workspace th{padding:16px 20px}.dc-library td:first-child strong{display:block;max-width:360px;overflow-wrap:anywhere}.dc-pagination{display:flex;align-items:center;justify-content:flex-end;gap:12px;padding:16px 20px;border-top:1px solid #edf2f5}.dc-pagination>span{margin-right:auto;color:#8198a4;font-size:11px}.dc-workspace-heading{padding:24px;display:flex;justify-content:space-between;align-items:flex-start;gap:16px}.dc-workspace h2{font-size:18px;overflow-wrap:anywhere;margin-top:8px}.dc-workspace-heading p{font-size:10px;color:#8aa0ad;margin-top:10px;overflow-wrap:anywhere}.dc-binding,.dc-version,.dc-filter{display:flex;gap:16px;align-items:center;flex-wrap:wrap;padding:15px 24px;background:#f4f8fa;font-size:12px;color:#6a8392}.dc-binding select{max-width:320px}.dc-version strong{width:100%;font-size:12px;color:#b65f51;font-weight:500;overflow-wrap:anywhere}.dc-parameters{padding:22px 24px 12px;display:flex;gap:17px;align-items:flex-end;flex-wrap:wrap}.dc-parameters label:first-child{flex-direction:row;align-items:center;align-self:center}.dc-parameters input[type=number]{width:105px}.dc-parameters label:last-of-type input{width:130px}.dc-note{padding:0 24px 18px;color:#8298a5;font-size:11px;line-height:1.9}.dc-save{padding:18px 24px;background:#f5faf8;border-block:1px solid #e4eee9;display:flex;align-items:flex-end;gap:12px;flex-wrap:wrap}.dc-save label{flex:1;min-width:200px}.dc-chart-tools{display:flex;gap:16px;align-items:center;flex-wrap:wrap;padding:24px;font-size:12px}.dc-chart-tools strong{margin-right:auto;color:#4d6c7c;font-weight:500}.dc-chart-tools label{display:flex;gap:8px;align-items:center;color:#8097a4}.dc-chart-tools select{font-size:11px;padding:8px}.dc-workspace .curve-chart{height:350px}.dc-library button:disabled,.dc-workspace button:disabled{opacity:.5;cursor:not-allowed}
@media(max-width:850px){.dc-summary{grid-template-columns:repeat(2,minmax(0,1fr))}.dc-tools{flex-wrap:wrap;padding:14px}.dc-tools .tabs{width:100%;justify-content:space-between}.dc-workspace-heading{flex-wrap:wrap}.dc-library .table-scroll{max-width:100%}.dc-save{align-items:stretch}.dc-parameters{gap:12px}.dc-parameters input[type=number]{width:90px}}
@media(max-width:600px){.data-center{gap:14px}.dc-login{padding:24px;margin:0}.dc-summary{gap:10px}.dc-summary article{padding:17px}.dc-summary strong{font-size:25px}.dc-library table{min-width:650px}.dc-workspace-heading,.dc-chart-tools{padding:19px}.dc-workspace h2{font-size:16px}.dc-parameters,.dc-save{padding:17px}.dc-note{padding-inline:17px}.dc-parameters label:first-child{width:100%}.dc-parameters button{width:100%}.dc-binding{padding:17px}.dc-binding select{max-width:100%;width:100%}.dc-workspace .curve-chart{height:300px}.dc-pagination{gap:8px;padding:14px}.dc-chart-tools label{width:100%;justify-content:space-between}.dc-chart-tools select{width:65%}.dc-save label{min-width:100%}}
</style>
