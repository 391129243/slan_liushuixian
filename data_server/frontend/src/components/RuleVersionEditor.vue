<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { serverApi } from '../server-api'
const props = defineProps<{ bundleId?: string; versionId?: string; disabled?: boolean }>()
const emit = defineEmits<{ 'update:versionId': [value: string]; dirty: [value: boolean] }>()
type Rule = Record<string, any>
type File = { fileName: string; rules: Rule[] | null; role?: string }
type Policy = { name: string; mode: string; ct: { fileName: string; ruleIndex: number }; peak: { fileName: string; ruleIndex: number } }
const files = ref<File[]>([]), policies = ref<Policy[]>([]), versions = ref<{ id: string; label: string; created_by: string }[]>([])
const busy = ref(false), dirty = ref(false), error = ref(''), label = ref('规则调整版本'), selected = ref('')
let generation = 0
const ctFields = ['CTvalue', 'CTvalueDown', 'CTvalueUP', 'CTvalueUP_Suspicious'], peakFields = ['PEAKvalue', 'PEAKvalueDown', 'PEAKvalueUP', 'PEAKvalueRM']
const fieldNames: Record<string, string> = { CTvalue: 'Ct 中心', CTvalueDown: '下偏移', CTvalueUP: '上偏移', CTvalueUP_Suspicious: '可疑扩展', PEAKvalue: 'Tm 中心', PEAKvalueDown: '下偏移', PEAKvalueUP: '上偏移', PEAKvalueRM: 'Rm 下限' }
const isCt = (file: File) => file.fileName.toLowerCase().endsWith('.ct')
function changed() { dirty.value = true; emit('dirty', true) }
const references = computed(() => {
  const ct: { key: string; text: string; ref: Policy['ct'] }[] = [], peak: typeof ct = []
  for (const f of files.value) {
    let index = 0
    for (const group of f.rules || []) for (const rule of isCt(f) ? [group] : group.PEAKSingleRuleEntityList || []) {
      const ref = { fileName: f.fileName, ruleIndex: index++ }
      ;(isCt(f) ? ct : peak).push({ ref, key: JSON.stringify(ref), text: `${f.fileName} / ${group.Channel} / ${rule.Category}` })
    }
  }
  return { ct, peak }
})
async function load() {
  const epoch = ++generation; busy.value = true; error.value = ''
  try {
    const query = props.bundleId ? '?bundleId=' + encodeURIComponent(props.bundleId) : ''
    const [list, draft] = await Promise.all([serverApi.json<typeof versions.value>('/pcr/rules' + query), serverApi.json<{ files: File[]; combinationRules: Policy[] }>('/pcr/rules/draft' + (props.versionId ? '?versionId=' + encodeURIComponent(props.versionId) : query))])
    if (epoch !== generation) return
    versions.value = list; files.value = draft.files; policies.value = draft.combinationRules || []; selected.value = props.versionId || ''; dirty.value = false; emit('dirty', false)
  } catch (e) { if (epoch === generation) error.value = (e as Error).message } finally { if (epoch === generation) busy.value = false }
}
watch(() => [props.bundleId, props.versionId], load, { immediate: true })
function choose() { emit('update:versionId', selected.value) }
function addFile(ct: boolean) {
  let name = ct ? 'edited.ct' : 'edited.peak', n = 1
  while (files.value.some(f => f.fileName === name)) name = `edited-${n++}.${ct ? 'ct' : 'peak'}`
  const file: File = { fileName: name, rules: [] }; files.value.push(file); addGroup(file)
}
function single(ct: boolean): Rule { return ct ? { Category: '新项目', CTvalue: 35, CTvalueDown: 35, CTvalueUP: 3, CTvalueUP_Suspicious: 2 } : { Category: '新项目', PEAKvalue: 80, PEAKvalueDown: 1, PEAKvalueUP: 1, PEAKvalueRM: 100 } }
function addGroup(file: File) { if (!Array.isArray(file.rules)) file.rules = []; file.rules.push(isCt(file) ? { Channel: 'FAM', ...single(true) } : { Channel: 'FAM', PEAKSingleRuleEntityList: [single(false)] }); changed() }
function addPolicy() {
  if (!references.value.ct[0] || !references.value.peak[0]) return
  policies.value.push({ name: `综合项目 ${policies.value.length + 1}`, mode: 'ALL', ct: { ...references.value.ct[0].ref }, peak: { ...references.value.peak[0].ref } }); changed()
}
async function save() {
  if (busy.value || !label.value.trim()) return
  busy.value = true; error.value = ''
  try {
    const result = await serverApi.json<{ id: string }>('/pcr/rules', { baseBundleId: props.bundleId || null, parentVersionId: props.versionId || null, label: label.value.trim(), files: files.value.map(f => ({ fileName: f.fileName, rules: f.rules })), combinationRules: policies.value })
    dirty.value = false; emit('dirty', false); emit('update:versionId', result.id)
  } catch (e) { error.value = (e as Error).message } finally { busy.value = false }
}
</script>
<template>
  <details class="rule-editor">
    <summary>编辑判读规则与 CT / PEAK 综合结论</summary>
    <p>选择已有版本或编辑后保存新版本。原始上传规则和既有分析保持不变；草稿须先保存，才能用于分析。</p>
    <div v-if="error" class="rule-error" role="alert">{{ error }}</div>
    <fieldset :disabled="disabled || busy">
      <div class="rule-actions"><label>使用规则版本<select v-model="selected" aria-label="使用规则版本" @change="choose"><option value="">原始上传规则</option><option v-for="v in versions" :key="v.id" :value="v.id">{{ v.label }} · {{ v.created_by }} · {{ v.id.slice(0,8) }}</option></select></label><button class="button" @click="addFile(true)">新增 CT 文件</button><button class="button" @click="addFile(false)">新增 PEAK 文件</button></div>
      <section v-for="(file,fi) in files" :key="fi" class="rule-file">
        <div class="rule-actions"><label>规则文件名<input v-model="file.fileName" :aria-label="`规则文件 ${fi + 1} 名称`" @input="changed" /></label><button class="text-button" @click="files.splice(fi,1); changed()">删除文件</button><button class="button" @click="addGroup(file)">新增通道规则</button></div>
        <p v-if="!Array.isArray(file.rules)" class="rule-error">原文件不是有效规则数组，请新增通道规则替换。</p>
        <div v-for="(group,gi) in file.rules || []" :key="gi" class="rule-group">
          <div class="rule-actions"><label>通道<input v-model="group.Channel" :aria-label="`文件 ${fi + 1} 通道 ${gi + 1}`" @input="changed" /></label><button class="text-button" @click="file.rules!.splice(gi,1); changed()">删除通道规则</button><button v-if="!isCt(file)" class="button" @click="(group.PEAKSingleRuleEntityList ||= []).push(single(false)); changed()">新增峰项目</button></div>
          <div v-for="(item,ri) in isCt(file) ? [group] : group.PEAKSingleRuleEntityList || []" :key="ri" class="rule-fields">
            <label>项目<input v-model="item.Category" :aria-label="`文件 ${fi + 1} 通道 ${gi + 1} 项目 ${ri + 1}`" @input="changed" /></label>
            <label v-for="key in isCt(file) ? ctFields : peakFields" :key="key">{{ fieldNames[key] }}<input v-model.number="item[key]" type="number" step="any" :aria-label="`文件 ${fi + 1} 通道 ${gi + 1} 项目 ${ri + 1} ${fieldNames[key]}`" @input="changed" /></label>
            <button v-if="!isCt(file)" class="text-button" @click="group.PEAKSingleRuleEntityList.splice(ri,1); changed()">删除峰项目</button>
          </div>
        </div>
      </section>
      <div class="rule-actions"><strong>综合判读策略</strong><button class="button" :disabled="!references.ct.length || !references.peak.length" @click="addPolicy">新增综合项目</button></div>
      <p>全部命中：两项均阳性才阳性；任一项阴性则阴性，其余为可疑。任一命中：一项阳性即阳性；无阳性但有可疑则可疑，两项均阴性才阴性。任一项数据缺失或未判定时，综合结果均为未判定。</p>
      <div v-for="(p,i) in policies" :key="i" class="policy-grid">
        <label>综合项目名<input v-model="p.name" :aria-label="`综合项目 ${i + 1} 名称`" @input="changed" /></label>
        <label>组合方式<select v-model="p.mode" :aria-label="`综合项目 ${i + 1} 方式`" @change="changed"><option value="ALL">CT 与 PEAK 全部命中</option><option value="ANY">CT 或 PEAK 任一命中</option></select></label>
        <label>CT 规则<select v-model="p.ct" :aria-label="`综合项目 ${i + 1} CT 规则`" @change="changed"><option v-for="r in references.ct" :key="r.key" :value="r.ref">{{ r.text }}</option></select></label>
        <label>PEAK 规则<select v-model="p.peak" :aria-label="`综合项目 ${i + 1} PEAK 规则`" @change="changed"><option v-for="r in references.peak" :key="r.key" :value="r.ref">{{ r.text }}</option></select></label>
        <button class="text-button" @click="policies.splice(i,1); changed()">删除综合项目</button>
      </div>
      <div class="rule-actions"><label>新版本名称<input v-model="label" maxlength="120" aria-label="新规则版本名称" /></label><button class="button primary" :disabled="!files.length || !label.trim()" @click="save">保存并使用新规则版本</button><span>{{ dirty ? '有未保存的规则草稿' : '规则已同步' }}</span></div>
    </fieldset>
  </details>
</template>
<style scoped>
.rule-editor{margin:18px 24px;border:1px solid #dce7eb;border-radius:6px;padding:16px;min-width:0}summary{font-size:13px;font-weight:550;cursor:pointer;color:#365b6e}p{font-size:11px;color:#738d9b;line-height:1.9;margin:14px 0}fieldset{border:0;padding:0;min-width:0}.rule-actions{display:flex;gap:12px;align-items:flex-end;flex-wrap:wrap;margin:14px 0}.rule-actions strong,.rule-actions span{font-size:12px;color:#627d8a}label{display:flex;flex-direction:column;gap:7px;font-size:11px;color:#627d8a;min-width:0}input,select{padding:9px;border:1px solid #dbe5e9;border-radius:5px;min-width:0;background:#fff;color:#355365;box-sizing:border-box;width:100%}.rule-actions label{min-width:150px;max-width:100%}.rule-file{background:#f7fafb;border:1px solid #e4ecef;border-radius:5px;padding:0 14px 14px;margin-top:14px}.rule-group{border-top:1px solid #e0eaee;margin-top:12px}.rule-fields,.policy-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(110px,1fr));gap:12px;align-items:end;margin:13px 0}.policy-grid{padding-bottom:14px;border-bottom:1px solid #e4ecef}.rule-error{font-size:12px;color:#b14d42;line-height:1.8;overflow-wrap:anywhere}button:disabled{opacity:.5}@media(max-width:600px){.rule-editor{margin:15px 17px;padding:12px}.rule-fields,.policy-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.rule-actions label{width:100%;min-width:0}}
</style>
