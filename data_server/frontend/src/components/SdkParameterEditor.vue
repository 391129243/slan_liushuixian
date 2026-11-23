<script setup lang="ts">
import { ref, watch } from 'vue'
type Settings = Record<string, number | boolean | undefined>
type ParameterSet = { ProjectName?: string; Name?: string; AMPAlgorithm?: number; Targets?: { Name?: string; Dye?: string; ChannelNo?: number; TubeNo?: number; Paramter?: Record<string, unknown> }[] }
const props = defineProps<{ sets?: ParameterSet[]; disabled?: boolean }>()
const emit = defineEmits<{ change: [value: { scope: string; projects: { projectIndex: number; ampAlgorithm: number }[]; targets: { projectIndex: number; targetIndex: number; settings: Settings }[] }, valid: boolean] }>()
const algorithms = ['相对荧光值法', '绝对荧光值法', '相对荧光值法（线性）']
const fields = [
  { key: 'optimizationMode', sdk: 'OptimizationMode', label: '基线优化', options: ['不优化', '自动优化', '手动优化'] },
  { key: 'baselineStart', sdk: 'BeginBaseline', label: '基线起点' }, { key: 'baselineEnd', sdk: 'EndBaseline', label: '基线终点' },
  { key: 'autoThreshold', sdk: 'AutoThreshold', label: '自动阈值', boolean: true }, { key: 'threshold', sdk: 'Threshold', label: '扩增荧光阈值' },
  { key: 'qualitativeAnalysis', sdk: 'QualitativeAnalysis', label: '定性分析', boolean: true },
  { key: 'digitalFilter', sdk: 'DigitalFilter', label: '数字滤波', options: ['不滤波', '轻度滤波', '重度滤波'] },
  { key: 'peakHeightThreshold', sdk: 'PeekHightThreshold', label: '熔解峰高阈值' },
  { key: 'minTemperature', sdk: 'MinTemperature', label: '最低温度（℃）' }, { key: 'maxTemperature', sdk: 'MaxTemperature', label: '最高温度（℃）' },
  { key: 'meltPeakAlgorithm', sdk: 'MeltPeakAlgorithm', label: '熔解峰算法', options: ['标准算法', '高级算法'] },
  { key: 'peakType', sdk: 'PeekType', label: '峰类型', options: ['正峰', '负峰', '正负峰'] },
]
type Target = { index: number; name: string; selected: boolean; original: Settings; settings: Settings }
const projects = ref<{ index: number; name: string; algorithm: number; originalAlgorithm: number; targets: Target[] }[]>([])
function publish() {
  const selectedProjects: { projectIndex: number; ampAlgorithm: number }[] = [], targets: { projectIndex: number; targetIndex: number; settings: Settings }[] = []
  let valid = true
  for (const p of projects.value) {
    if (p.algorithm !== p.originalAlgorithm) selectedProjects.push({ projectIndex: p.index, ampAlgorithm: p.algorithm })
    for (const t of p.targets.filter(t => t.selected)) {
      const patch: Settings = {}
      for (const [key, value] of Object.entries(t.settings)) {
        if (key === 'threshold' && t.settings.autoThreshold) continue
        if (value !== t.original[key]) patch[key] = value
      }
      if (Object.values(patch).some(v => typeof v !== 'boolean' && (typeof v !== 'number' || !Number.isFinite(v)))) valid = false
      if (patch.baselineStart !== undefined || patch.baselineEnd !== undefined || patch.optimizationMode === 2) {
        const start = Number(t.settings.baselineStart), end = Number(t.settings.baselineEnd)
        if (!Number.isInteger(start) || !Number.isInteger(end) || start < 1 || start >= end || end > 20) valid = false
      }
      if (!t.settings.autoThreshold && 'threshold' in t.settings && (!Number.isFinite(Number(t.settings.threshold)) || Number(t.settings.threshold) <= 0)) valid = false
      if (patch.peakHeightThreshold !== undefined && Number(patch.peakHeightThreshold) < 0) valid = false
      if ((patch.minTemperature !== undefined || patch.maxTemperature !== undefined) && Number(t.settings.minTemperature) >= Number(t.settings.maxTemperature)) valid = false
      targets.push({ projectIndex: p.index, targetIndex: t.index, settings: patch })
    }
  }
  emit('change', { scope: 'SELECTED', projects: selectedProjects, targets }, valid && (targets.length > 0 || selectedProjects.length > 0))
}
watch(() => props.sets, sets => {
  projects.value = (sets || []).map((p, index) => ({ index, name: p.ProjectName || p.Name || `项目 ${index + 1}`, algorithm: p.AMPAlgorithm ?? 0, originalAlgorithm: p.AMPAlgorithm ?? 0,
    targets: (p.Targets || []).map((t, i) => {
      const settings: Settings = {}; for (const f of fields) if (t.Paramter && f.sdk in t.Paramter) settings[f.key] = t.Paramter[f.sdk] as number | boolean
      return { index: i, name: `${t.Name || t.Dye || '靶标'} · 通道 ${t.ChannelNo ?? '?'} · 管 ${t.TubeNo ?? 1}`, selected: false, original: { ...settings }, settings }
    }) }))
  publish()
}, { immediate: true })
</script>
<template>
  <div class="sdk-editor">
    <p class="editor-note">从原文件读取 SDK 参数。勾选要重算的靶标，只提交修改的字段；未选靶标的参数保留。扩增算法属于项目级设置，修改后会作用于该项目的全部通道。熔解参数仅在该靶标支持时显示。</p>
    <p v-if="!projects.length" class="editor-note">请重新读取文件以获取可编辑参数。</p>
    <fieldset v-for="p in projects" :key="p.index" :disabled="disabled">
      <legend>{{ p.name }} · 项目 {{ p.index + 1 }}</legend>
      <label class="algorithm">扩增算法<select v-model="p.algorithm" :aria-label="`项目 ${p.index + 1} 扩增算法`" @change="publish"><option v-for="(name,i) in algorithms" :key="i" :value="i">{{ name }}</option></select></label>
      <details v-for="t in p.targets" :key="t.index" :open="t.selected" class="target-settings">
        <summary><label><input v-model="t.selected" type="checkbox" :aria-label="`选择项目 ${p.index + 1} 靶标 ${t.index + 1}`" @change="publish" @click.stop />{{ t.name }}</label></summary>
        <div class="parameter-grid">
          <label v-for="f in fields.filter(f=>f.key in t.settings)" :key="f.key">{{ f.label }}
            <input v-if="f.boolean" v-model="t.settings[f.key]" type="checkbox" :disabled="!t.selected" :aria-label="`项目 ${p.index + 1} 靶标 ${t.index + 1} ${f.label}`" @change="publish" />
            <select v-else-if="f.options" v-model="t.settings[f.key]" :disabled="!t.selected" :aria-label="`项目 ${p.index + 1} 靶标 ${t.index + 1} ${f.label}`" @change="publish"><option v-for="(name,i) in f.options" :key="i" :value="i">{{ name }}</option></select>
            <input v-else v-model.number="t.settings[f.key]" type="number" step="any" :disabled="!t.selected || (f.key==='threshold' && Boolean(t.settings.autoThreshold))" :aria-label="`项目 ${p.index + 1} 靶标 ${t.index + 1} ${f.label}`" @input="publish" />
          </label>
        </div>
      </details>
    </fieldset>
  </div>
</template>
<style scoped>
.sdk-editor{padding:0 24px 20px}.editor-note{font-size:12px;color:#738d9b;line-height:1.9;margin:12px 0}fieldset{border:1px solid #dce7eb;border-radius:6px;padding:16px;margin-top:15px;min-width:0}legend{font-size:13px;color:#365b6e;padding:0 8px}.algorithm{display:flex;align-items:center;gap:12px;font-size:12px}.target-settings{border-top:1px solid #e8eff2;margin-top:15px;padding-top:12px}summary{font-size:12px;cursor:pointer}summary label{display:inline-flex;gap:10px;align-items:center}.parameter-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(140px,1fr));gap:14px;padding:18px 0 5px}.parameter-grid label{font-size:11px;color:#627d8a;display:flex;flex-direction:column;gap:8px}input[type=number],select{padding:9px;border:1px solid #dbe5e9;background:white;border-radius:5px;min-width:0;width:100%;box-sizing:border-box;color:#355365}input[type=checkbox]{align-self:flex-start}.algorithm select{max-width:240px}input:disabled,select:disabled{opacity:.5}@media(max-width:600px){.sdk-editor{padding-inline:17px}.parameter-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
