import { reference, analyze, type Experiment, type Analysis, type Parameters } from './domain'

export interface PcrAnalysisService {
  read(file: File): Promise<Experiment>
  reanalyze(experiment: Experiment, parameters: Parameters): Promise<Analysis>
  savePcr(analysis: Analysis): Promise<never>
}
const delay = () => new Promise(resolve => setTimeout(resolve, 250))
export const mockService: PcrAnalysisService = {
  async read(file) {
    if (!file.name.toLowerCase().endsWith('.pcr')) throw new Error('请选择 .pcr 文件。')
    if (file.size === 0 || file.size > 32 * 1024 * 1024) throw new Error('文件不能为空，且不能超过 32 MiB。')
    await delay()
    return { id: crypto.randomUUID(), name: file.name.replace(/\.pcr$/i, ''), fileName: file.name,
      size: file.size, importedAt: new Date().toISOString(), reference: false, mock: true }
  },
  async reanalyze(experiment, parameters) {
    await delay()
    return { id: crypto.randomUUID(), experimentId: experiment.id, experimentName: experiment.name,
      createdAt: new Date().toISOString(), parameters: structuredClone(parameters), results: analyze(parameters), mock: true }
  },
  async savePcr() { throw new Error('真实 .pcr 保存等待 C# SDK 接入。演示结果可以导出为 JSON，不能作为 .pcr 使用。') }
}

// Reserved HTTP contract for Java -> C# SDK. Mock UI never calls these endpoints.
export const pcrApi = {
  capabilities: '/api/admin/pcr/capabilities',
  read: '/api/admin/pcr/read', reanalyze: '/api/admin/pcr/reanalyze', save: '/api/admin/pcr/save'
}
type State = { experiments: Experiment[]; history: Analysis[] }
const key = 'pcr-studio-demo-v1'
export function loadState(): State {
  try {
    const saved = JSON.parse(localStorage.getItem(key) || 'null') as State | null
    if (saved && Array.isArray(saved.experiments) && Array.isArray(saved.history)
        && saved.experiments.every(e => e.mock === true && typeof e.id === 'string' && typeof e.name === 'string' && typeof e.size === 'number'))
      return { experiments: saved.experiments, history: saved.history.filter(h => h.mock === true && Array.isArray(h.results) && h.parameters).slice(0, 50) }
  } catch { /* fall back to the bundled reference metadata */ }
  return { experiments: [reference], history: [] }
}
export function saveState(state: State): boolean {
  try { localStorage.setItem(key, JSON.stringify({ ...state, history: state.history.slice(0, 50) })); return true } catch { return false }
}
export function exportSnapshot(analysis: Analysis) {
  const blob = new Blob([JSON.stringify({ format: 'pcr-studio-mock-analysis-v1', warning: '模拟数据，非真实 PCR 分析结果', ...analysis }, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob), link = document.createElement('a')
  link.href = url; link.download = `模拟分析_${analysis.id.slice(0, 8)}.analysis.json`; link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
