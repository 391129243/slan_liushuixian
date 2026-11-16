export type Parameters = { baselineStart: number; baselineEnd: number; threshold: number; autoBaseline: boolean }
export type Experiment = { id: string; name: string; fileName: string; size: number; importedAt: string; reference: boolean; mock: true }
export type Analysis = { id: string; experimentId: string; experimentName: string; createdAt: string; parameters: Parameters; results: Result[]; mock: true }
export type Result = { well: string; channel: string; ct: number | null; points: [number, number][] }
export const channels = [
  { name: 'FAM', color: '#00a38d' }, { name: 'HEX', color: '#d8a335' },
  { name: 'ROX', color: '#e17968' }, { name: 'CY5', color: '#7995dd' },
  { name: 'CY5.5', color: '#aa82cc' }, { name: 'ATTO', color: '#67a8ae' }
]
export const wells = Array.from({ length: 8 }, (_, i) => `A${i + 1}`)
export const defaults: Parameters = { baselineStart: 3, baselineEnd: 15, threshold: 300, autoBaseline: true }
export const reference: Experiment = {
  id: 'reference-20261116', name: '07-自研试剂 ab 管多重测试', fileName: '2026-11-16-13-04-40.pcr', size: 5736758,
  importedAt: '2026-11-16T13:04:40+08:00', reference: true, mock: true
}
export const referenceFolder = 'AIO_AAA_2026-11-16-11-01-01_07-自研试剂ab管多重测试_20261020'
export const referenceSha256 = '577afff986549e59d29f611358a776b5e1c0439bc34d4504cbda7889724031e2'

// Synthetic sigmoid curves for UI demonstration only; no proprietary PCR decoding.
export function analyze(parameters: Parameters): Result[] {
  if (!Number.isInteger(parameters.baselineStart) || !Number.isInteger(parameters.baselineEnd)
    || parameters.baselineStart < 1 || parameters.baselineStart >= parameters.baselineEnd || parameters.baselineEnd > 20
    || !Number.isFinite(parameters.threshold) || parameters.threshold < 10 || parameters.threshold > 3000) {
    throw new Error('基线需满足 1 ≤ 起点 < 终点 ≤ 20；阈值范围为 10–3000。')
  }
  const start = parameters.autoBaseline ? 3 : parameters.baselineStart
  const end = parameters.autoBaseline ? 15 : parameters.baselineEnd
  return wells.flatMap((well, w) => channels.map((channel, c) => {
    const flat = w === 7 || (w + c) % 7 === 5
    const raw = Array.from({ length: 40 }, (_, n) => {
      const x = n + 1
      const signal = flat ? 0 : (1800 + c * 170 + w * 55) / (1 + Math.exp(-(x - (24 + w * 0.8 + c * 0.6)) / 1.65))
      return 95 + signal + Math.sin(x * 0.9 + w + c) * 4 + x * 0.12
    })
    const baseline = raw.slice(start - 1, end).reduce((sum, n) => sum + n, 0) / (end - start + 1)
    const points: [number, number][] = raw.map((y, i) => [i + 1, +(y - baseline).toFixed(2)])
    let ct: number | null = null
    for (let i = 1; i < points.length; i++) {
      const previous = points[i - 1]!, current = points[i]!
      if (previous[1] < parameters.threshold && current[1] >= parameters.threshold) {
        ct = +(previous[0] + (parameters.threshold - previous[1]) / (current[1] - previous[1])).toFixed(2); break
      }
    }
    return { well, channel: channel.name, ct, points }
  }))
}
export function formatSize(bytes: number) { return `${(bytes / 1024 / 1024).toFixed(2)} MiB` }
export function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
