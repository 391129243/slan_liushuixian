import type { Parameters } from './domain'

export type FileRecord = { id: string; file_name: string; file_sha256: string; file_size: number; received_at: number; uploaded_by_username: string | null; upload_source: string }
export type ReportRecord = { id: string; device_id: string; experiment_name: string; received_at: number; operator_name: string | null; file_id: string | null; analysis_status: string; bundle_id?: string | null; Experiment?: VendorExperiment }
export type VendorExperiment = { Name?: string; WellResult?: { ID?: string | number; SampleWellNames?: string[]; TargetResults?: { Name?: string; Dye?: string; Channel?: number; Ct?: unknown; RawCurve?: Curve; AMPCurve?: Curve; RawMeltCurve?: Curve; PeakCurve?: Curve }[] }[] }
export type Point = { X: number; Y: number }
export type Curve = Array<Point | number>
export type RuleContext = { bundleId: string; evaluationStatus: string; manifest: { version: number; files: { fileName: string; role: string; size: number; sha256: string }[] } }
export type RuleEvaluation = { engineVersion: string; status: string; applicationRulesApplied: boolean; errors: { fileName?: string; message: string }[]; rows: { fileName: string; type: string; well: string; sampleWellNames?: string[]; channel: string; category: string; lower: number; upper: number; suspiciousUpper?: number; minimumRm?: number; actualCt?: number | null; observedPeaks?: { tm: number; rm: number }[]; matchedPeaks?: { tm: number; rm: number }[]; status: string; conclusion: string; reason?: string }[] }
export type ParameterSet = { ProjectName?: string; Name?: string; AMPAlgorithm?: number; Targets?: { Name?: string; Dye?: string; ChannelNo?: number; TubeNo?: number; Paramter?: Record<string, unknown> }[] }
export type AdvancedParameters = { scope?: string; projects?: { projectIndex: number; ampAlgorithm: number }[]; targets?: { projectIndex: number; targetIndex: number; settings: Record<string, number | boolean | undefined> }[] }
export type CombinedConclusions = { status: string; rows: { well: string; sampleWellNames?: string[]; name: string; mode: string; ctStatus: string; peakStatus: string; conclusion: string }[] }
export type AnalysisRecord = { id: string; bridge_analysis_id: string | null; input_file_id: string; output_file_id: string | null; report_id: string | null; parent_analysis_id: string | null; bundle_id?: string | null; rule_version_id?: string | null; ruleSnapshot?: RuleContext | null; operator_username: string; saved_by_username: string | null; status: string; file_save_status: string; created_at: number; sdk_version: string | null; error_message: string | null; save_error_message: string | null; parameters?: Parameters & AdvancedParameters; result?: { experiment?: VendorExperiment; parameterSets?: ParameterSet[]; applicationRuleEvaluation?: RuleEvaluation; combinedConclusions?: CombinedConclusions } | null }
export type Page<T> = { total: number; items: T[]; offset: number; limit: number }
export class ApiError extends Error { constructor(public status: number, message: string) { super(message) } }

// Credentials live only in this page's module memory; never written to local/session storage or URLs.
let authorization = ''
export const serverApi = {
  authenticated: () => !!authorization,
  clear: () => { authorization = '' },
  async login(username: string, password: string) {
    if (!username || !password) throw new ApiError(401, '请输入账号和密码。')
    const bytes = new TextEncoder().encode(`${username}:${password}`)
    authorization = 'Basic ' + btoa(Array.from(bytes, byte => String.fromCharCode(byte)).join(''))
    try { return await this.json<{ reports: number; files: number; devices: number }>('/summary') }
    catch (error) { authorization = ''; throw error }
  },
  async request(path: string, init: RequestInit = {}) {
    if (!authorization) throw new ApiError(401, '请先登录数据中心。')
    const headers = new Headers(init.headers); headers.set('Authorization', authorization)
    let response: Response
    try { response = await fetch('/api/admin' + path, { ...init, headers, credentials: 'omit', cache: 'no-store', signal: AbortSignal.timeout(60000) }) }
    catch { throw new ApiError(0, '服务暂时不可达或请求超时。分析请求可能仍在处理，请先刷新历史，再重试本次请求。') }
    if (!response.ok) {
      if (response.status === 401) { authorization = ''; throw new ApiError(401, '账号或密码不正确，或登录身份已失效。') }
      const error = await response.json().catch(() => null)
      throw new ApiError(response.status, error?.Message || error?.message || `服务请求失败（${response.status}），请确认后端已启动。`)
    }
    return response
  },
  async json<T>(path: string, body?: unknown, method?: string): Promise<T> {
    const response = await this.request(path, { method: method || (body === undefined ? 'GET' : 'POST'),
      headers: body === undefined ? undefined : { 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body) })
    return response.status === 204 ? undefined as T : response.json()
  },
  async upload(file: File) {
    if (!file.name.toLowerCase().endsWith('.pcr') || !file.size || file.size > 32 * 1024 * 1024) throw new ApiError(400, '请选择非空的 .pcr 文件，最大 32 MiB。')
    const form = new FormData(); form.append('file', file)
    return (await this.request('/files', { method: 'POST', body: form })).json() as Promise<{ FileID: string; Duplicate: boolean }>
  },
  async download(path: string, fallback: string) {
    const response = await this.request(path), blob = await response.blob()
    let name = fallback
    const encoded = response.headers.get('Content-Disposition')?.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
    if (encoded) { try { name = decodeURIComponent(encoded) } catch { /* retain safe fallback */ } }
    const url = URL.createObjectURL(blob), link = document.createElement('a')
    link.href = url; link.download = name; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
}
