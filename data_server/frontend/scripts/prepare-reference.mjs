// Generate a privacy-limited UI snapshot from the verified real-file upload test.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import assert from 'node:assert/strict'
const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const source = JSON.parse(readFileSync(resolve(root, 'target/real-pcr-upload-20261121/upload-experiment.json'), 'utf8'))
const report = JSON.parse(readFileSync(resolve(root, 'test-reports/real-pcr-upload-2026-11-21.json'), 'utf8'))
assert.equal(report.status, 'PASS')
const points = values => (values || []).map(point => {
  assert.ok(Number.isFinite(point.X) && Number.isFinite(point.Y), 'Invalid source point')
  return [point.X, point.Y]
})
const results = source.WellResult.flatMap(well => (well.TargetResults || []).map(target => ({
  well: well.ID, channel: target.Dye || target.Name, channelNumber: target.Channel,
  ct: target.Ct, rawMelt: points(target.RawMeltCurve), peakCurve: points(target.PeakCurve),
  peaks: (target.PeakValues || []).map(peak => ({ tm: peak.Tm, height: peak.Rm, width: peak.Width, signal: peak.YValue }))
})))
const snapshot = {
  kind: 'verified-offline-snapshot', fileName: '2026-11-16-13-04-40.pcr',
  title: '自研试剂 ab 管多重测试', fileSize: report.sourceBytes, sha256: report.sourceSha256,
  startTime: source.StartTime, endTime: source.EndTime, deviceModel: source.DeviceModel,
  experimentType: 'melt', testedAt: report.finishedAt, statistics: report.statistics,
  normalizedNumbers: report.normalizedNumbers, checks: report.checks, results
}
assert.equal(results.length, report.statistics.targets)
assert.equal(results.reduce((n, r) => n + r.rawMelt.length, 0), report.statistics.rawMeltPoints)
assert.equal(results.reduce((n, r) => n + r.peakCurve.length, 0), report.statistics.peakCurvePoints)
const output = resolve(root, 'frontend/public/reference')
mkdirSync(output, { recursive: true })
writeFileSync(resolve(output, 'melt-result.json'), JSON.stringify(snapshot))
console.log('Prepared verified UI snapshot; all curve points retained, patient/sample identity fields excluded.')
