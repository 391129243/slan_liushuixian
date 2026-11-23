import { test, expect, type Page } from '@playwright/test'
import { readFile } from 'node:fs/promises'

// Browser behavior fixture only. Actual Java/C#/SDK behavior is verified by the backend and offline SDK tests.
async function fixture(page: Page, failFirstAnalysis = false, withRules = false) {
  const inputId = '11111111-1111-4111-8111-111111111111', outputId = '22222222-2222-4222-8222-222222222222', runId = '33333333-3333-4333-8333-333333333333'
  const experiment = (ct: number) => ({ Name: '接口实验', WellResult: [{ ID: 'A1', TargetResults: [{ Name: 'FAM', Ct: ct, AMPCurve: [0.01, 0.3] }, { Name: 'HEX', Ct: 'Infinity', AMPCurve: [{ X: 1, Y: 0 }, { X: 42, Y: 0.02 }] }] }] })
  const files: object[] = [], requests: string[] = []
  const parameterSets = [{ ProjectName: '项目一', AMPAlgorithm: 0, Targets: [{ Name: 'FAM', ChannelNo: 1, Paramter: { BeginBaseline: 3, EndBaseline: 12, Threshold: 0.12, AutoThreshold: false, QualitativeAnalysis: true, OptimizationMode: 1, DigitalFilter: 2, PeekHightThreshold: 100, MinTemperature: 40, MaxTemperature: 95, MeltPeakAlgorithm: 0, PeekType: 0 } }, { Name: 'HEX', ChannelNo: 2, Paramter: { BeginBaseline: 3, EndBaseline: 12, Threshold: 0.12, AutoThreshold: false } }] }]
  const payloads: any[] = [], versions: any[] = []
  const draft = { files: withRules ? [{ fileName: 'original.ct', rules: [{ Channel: 'FAM', Category: '示例项目', CTvalue: 35, CTvalueDown: 35, CTvalueUP: 3, CTvalueUP_Suspicious: 2 }] }] : [], combinationRules: [] }
  const ruleContext = withRules ? { bundleId: '44444444-4444-4444-8444-444444444444', evaluationStatus: 'ARCHIVED_NOT_EVALUATED', manifest: { version: 1, files: [{ fileName: 'original.ct', role: 'CT_RULE', size: 11, sha256: 'd'.repeat(64) }, { fileName: 'judgeConclusion.result', role: 'DEVICE_CONCLUSION', size: 2, sha256: 'e'.repeat(64) }] } } : null
  let run: Record<string, any> | undefined, failed = false
  await page.route('**/api/admin/**', async route => {
    const request = route.request(), path = new URL(request.url()).pathname.replace('/api/admin', ''), body = request.postDataJSON.bind(request)
    const send = (value: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(value) })
    if (request.headers().authorization !== 'Basic ' + Buffer.from('admin:12345678').toString('base64')) return send({ Message: 'unauthorized' }, 401)
    if (path === '/summary') return send({ reports: 0, files: files.length, devices: 0 })
    if (path === '/reports') return send({ total: 0, items: [], offset: 0, limit: 20 })
    if (path === '/files' && request.method() === 'POST') {
      expect(request.postDataBuffer()?.toString()).toContain('sample.pcr')
      if (!files.length) files.push({ id: inputId, file_name: 'sample.pcr', file_size: 512, file_sha256: 'a'.repeat(64), received_at: Date.now(), uploaded_by_username: 'admin', upload_source: 'ADMIN_UPLOAD' })
      return send({ Result: '1', FileID: inputId, Duplicate: false })
    }
    if (path === '/files') return send(files)
    if (path === '/pcr/rules/draft') return send(versions.find(v=>v.id===new URL(request.url()).searchParams.get('versionId'))?.snapshot || draft)
    if (path === '/pcr/rules' && request.method() === 'GET') return send(versions)
    if (path === '/pcr/rules' && request.method() === 'POST') { const payload = body(); const version = { id: '55555555-5555-4555-8555-555555555555', label: payload.label, created_by: 'admin', snapshot: payload }; versions.push(version); return send(version) }
    if (path === '/pcr/read') return send({ experimentId: 'b'.repeat(32), experiment: experiment(37.865534207440206), reportId: null, sdkVersion: 'fixture', ruleContext, parameterSets })
    if (path === '/pcr/reanalyze') {
      const payload = body(); expect(payload.parameters.threshold).toBe(0.24); expect(payload.operator_username).toBeUndefined(); requests.push(payload.requestId)
      payloads.push(payload)
      if (failFirstAnalysis && !failed) { failed = true; return send({ Message: '连接中断，请确认历史记录。' }, 503) }
      run = { id: runId, input_file_id: inputId, output_file_id: null, report_id: null, parent_analysis_id: null, operator_username: 'admin', status: 'SUCCEEDED', file_save_status: 'NOT_SAVED', created_at: Date.now(), sdk_version: 'fixture', parameters: payload.parameters, result: { experiment: experiment(39.053621906411) } }
      run.ruleSnapshot = ruleContext
      run.rule_version_id = payload.ruleVersionId === 'ORIGINAL' ? null : payload.ruleVersionId
      run.result.parameterSets = parameterSets
      if (run.rule_version_id) run.result.combinedConclusions = { status: 'EVALUATED', rows: [{ well: 'A1', name: '综合项目 1', mode: 'ALL', ctStatus: 'POSITIVE', peakStatus: 'POSITIVE', conclusion: '阳性' }] }
      if (withRules) run.result.applicationRuleEvaluation = { engineVersion: 'android-slan-rules-v1', status: 'EVALUATED', applicationRulesApplied: true, errors: [], rows: [{ fileName: 'original.ct', type: 'CT', well: 'A1', channel: 'FAM', category: '示例项目', lower: 0, upper: 38, suspiciousUpper: 40, actualCt: 39.053621906411, status: 'SUSPICIOUS', conclusion: '可疑样本' }] }
      return send({ analysisRunId: runId, analysisId: 'c'.repeat(32), experiment: run.result.experiment })
    }
    if (path === '/pcr/analyses') return send({ total: run ? 1 : 0, items: run ? [run] : [], offset: 0, limit: 20 })
    if (path === '/pcr/analyses/' + runId) return send(run)
    if (path === '/pcr/analyses/' + runId + '/result') return send(run?.result)
    if (path === '/pcr/save') { expect(body().analysisId).toBe(runId); run!.output_file_id = outputId; run!.file_save_status = 'SAVED'; run!.saved_by_username = 'admin'; return send({ Result: '1', FileID: outputId }) }
    if (path === '/files/' + outputId + '/download') return route.fulfill({ contentType: 'application/octet-stream', headers: { 'Content-Disposition': "attachment; filename*=UTF-8''analysis.pcr" }, body: Buffer.from('BROWSER-FIXTURE-PCR') })
    if (ruleContext && path === '/bundles/' + ruleContext.bundleId + '/attachments/original.ct') return route.fulfill({ contentType: 'application/octet-stream', body: Buffer.from('frozen rule') })
    return send({ Message: 'unexpected fixture endpoint: ' + path }, 500)
  })
  return { requests, payloads, versions }
}
async function login(page: Page) {
  await page.goto('/?mode=server')
  await page.getByRole('textbox', { name: '数据中心账号' }).fill('admin')
  await page.getByLabel('数据中心密码', { exact: true }).fill('12345678')
  await page.getByRole('button', { name: '登录数据中心', exact: true }).click()
  await expect(page.getByText('已连接数据服务')).toBeVisible()
}
async function uploadAndRead(page: Page) {
  await page.getByLabel('上传服务器 PCR 文件', { exact: true }).setInputFiles({ name: 'sample.pcr', mimeType: 'application/octet-stream', buffer: Buffer.from('browser-upload-fixture') })
  await expect(page.getByRole('status')).toContainText('文件已上传')
  await page.getByRole('button', { name: '读取分析', exact: true }).click()
  await expect(page.getByRole('heading', { name: '接口实验', exact: true })).toBeVisible()
  await expect(page.getByRole('img', { name: '真实扩增实验曲线，横轴为循环数，纵轴为 SDK 信号' })).toBeVisible()
}

test('authenticated upload, real response display, persistent version, save/download and logout', async ({ page }) => {
  await fixture(page)
  await login(page); await uploadAndRead(page)
  const results = page.getByRole('table', { name: '服务器靶标结果' })
  await expect(results.getByRole('row')).toHaveCount(3)
  await expect(results.getByText('37.865534', { exact: true })).toBeVisible()
  await expect(results.getByText('—', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '执行服务器二次分析', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('结果已保存到数据库')
  await expect(results.getByText('39.053622', { exact: true })).toBeVisible()
  await page.getByLabel('分析输出文件名', { exact: true }).fill('analysis.pcr')
  await page.getByRole('button', { name: '保存新 .pcr', exact: true }).click()
  await expect(page.getByText('保存账号：admin')).toBeVisible()
  const downloading = page.waitForEvent('download')
  await page.getByRole('button', { name: '下载结果 .pcr', exact: true }).click()
  const download = await downloading
  expect(download.suggestedFilename()).toBe('analysis.pcr'); expect(await readFile((await download.path())!, 'utf8')).toBe('BROWSER-FIXTURE-PCR')
  await page.getByRole('tab', { name: '分析版本', exact: true }).click()
  await page.getByRole('button', { name: '查看版本', exact: true }).click()
  await expect(results.getByText('39.053622', { exact: true })).toBeVisible()
  await page.screenshot({ path: 'test-results/data-center-desktop.png', fullPage: true })
  const storage = await page.evaluate(() => JSON.stringify({ local: { ...localStorage }, session: { ...sessionStorage } }))
  expect(storage).not.toContain('12345678'); expect(storage).not.toContain('Basic ')
  await page.getByRole('button', { name: '退出登录', exact: true }).click()
  await expect(page.getByRole('heading', { name: '登录数据中心' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '接口实验' })).toHaveCount(0)
})

test('rule attachments stay visible on the stored version and remain distinct from newly computed conclusions', async ({ page }) => {
  await fixture(page, false, true); await login(page); await uploadAndRead(page)
  const table = page.getByRole('table', { name: '规则与结论附件' })
  await expect(table.getByRole('row')).toHaveCount(3)
  await expect(page.getByText('服务器按 Android 的 .ct/.peak 区间规则重新判读', { exact: false })).toBeVisible()
  const downloading = page.waitForEvent('download')
  await table.getByRole('row').filter({ hasText: 'original.ct' }).getByRole('button', { name: '下载附件' }).click()
  expect(await readFile((await (await downloading).path())!, 'utf8')).toBe('frozen rule')
  await page.getByRole('button', { name: '执行服务器二次分析', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('结果已保存到数据库')
  await expect(page.getByRole('table', { name: '本版本规则判读', exact: true }).getByText('可疑样本', { exact: true })).toBeVisible()
  await page.getByRole('tab', { name: '分析版本', exact: true }).click(); await page.getByRole('button', { name: '查看版本' }).click()
  await expect(table.getByText('original.ct', { exact: true })).toBeVisible()
  await expect(page.getByRole('table', { name: '本版本规则判读', exact: true }).getByText('可疑样本', { exact: true })).toBeVisible()
})

test('uncertain analysis response retries the same requestId without fabricating success', async ({ page }) => {
  const { requests } = await fixture(page, true)
  await login(page); await uploadAndRead(page)
  await page.getByRole('button', { name: '执行服务器二次分析', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('连接中断')
  await expect(page.getByRole('button', { name: '保存新 .pcr', exact: true })).toHaveCount(0)
  await page.getByRole('button', { name: '重试本次请求', exact: true }).click()
  await expect(page.getByRole('status')).toContainText('结果已保存到数据库')
  expect(requests).toHaveLength(2); expect(requests[0]).toBe(requests[1])
})

test('advanced per-target parameters, editable rule versions and explicit CT/PEAK combinations', async ({ page }) => {
  const { payloads, versions } = await fixture(page, false, true)
  await login(page); await uploadAndRead(page)
  await page.getByText('编辑判读规则与 CT / PEAK 综合结论', { exact: true }).click()
  await page.getByLabel('文件 1 通道 1 项目 1 上偏移', { exact: true }).fill('6')
  await expect(page.getByRole('button', { name: '执行服务器二次分析', exact: true })).toBeDisabled()
  await page.getByRole('button', { name: '新增 PEAK 文件', exact: true }).click()
  await page.getByRole('button', { name: '新增综合项目', exact: true }).click()
  await page.getByLabel('新规则版本名称', { exact: true }).fill('页面编辑规则 v1')
  await page.getByRole('button', { name: '保存并使用新规则版本', exact: true }).click()
  await expect(page.getByLabel('使用规则版本', { exact: true })).toHaveValue('55555555-5555-4555-8555-555555555555')
  expect(versions[0].snapshot.files).toHaveLength(2); expect(versions[0].snapshot.combinationRules[0].mode).toBe('ALL')
  await page.getByLabel('按项目和通道配置高级参数', { exact: true }).check()
  await page.getByLabel('项目 1 扩增算法', { exact: true }).selectOption('2')
  await page.getByLabel('选择项目 1 靶标 1', { exact: true }).check()
  await page.getByLabel('项目 1 靶标 1 基线起点', { exact: true }).fill('6')
  await page.getByLabel('项目 1 靶标 1 自动阈值', { exact: true }).check()
  await page.getByLabel('项目 1 靶标 1 定性分析', { exact: true }).uncheck()
  await page.getByLabel('项目 1 靶标 1 数字滤波', { exact: true }).selectOption('1')
  await page.getByLabel('项目 1 靶标 1 熔解峰高阈值', { exact: true }).fill('200')
  await page.getByLabel('项目 1 靶标 1 最低温度（℃）', { exact: true }).fill('50')
  await page.getByLabel('项目 1 靶标 1 熔解峰算法', { exact: true }).selectOption('1')
  await page.getByLabel('项目 1 靶标 1 峰类型', { exact: true }).selectOption('2')
  await page.getByRole('button', { name: '执行服务器二次分析', exact: true }).click()
  await expect(page.getByRole('table', { name: '综合判读结果', exact: true }).getByText('阳性', { exact: true })).toBeVisible()
  const params = payloads[0].parameters
  expect(params.scope).toBe('SELECTED'); expect(params.projects).toEqual([{ projectIndex: 0, ampAlgorithm: 2 }]); expect(params.targets).toHaveLength(1)
  expect(params.targets[0].settings).toMatchObject({ baselineStart: 6, autoThreshold: true, qualitativeAnalysis: false, digitalFilter: 1, peakHeightThreshold: 200, minTemperature: 50, meltPeakAlgorithm: 1, peakType: 2 })
  expect(params.targets[0].settings.threshold).toBeUndefined()
  await page.getByRole('tab', { name: '分析版本', exact: true }).click(); await page.getByRole('button', { name: '查看版本', exact: true }).click()
  await expect(page.getByRole('table', { name: '综合判读结果', exact: true }).getByText('阳性', { exact: true })).toBeVisible()
  await page.setViewportSize({ width: 390, height: 844 })
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)
})

test('bad password remains logged out and mobile tables scroll within the viewport', async ({ page }) => {
  await fixture(page)
  await page.setViewportSize({ width: 390, height: 844 }); await page.goto('/?mode=server')
  await page.getByLabel('数据中心密码', { exact: true }).fill('bad-password')
  await page.getByRole('button', { name: '登录数据中心', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('账号或密码不正确')
  await page.getByLabel('数据中心密码', { exact: true }).fill('12345678'); await page.getByRole('button', { name: '登录数据中心', exact: true }).click()
  await uploadAndRead(page)
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390)
  await page.screenshot({ path: 'test-results/data-center-mobile.png', fullPage: true })
})
