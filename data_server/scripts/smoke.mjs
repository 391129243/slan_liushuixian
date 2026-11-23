// Packaged application, real HTTP, file-backed H2, and process restart verification.
import { spawn } from 'node:child_process';
import { mkdirSync, openSync, closeSync, readFileSync } from 'node:fs';
import { dirname, resolve, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { homedir } from 'node:os';
import { randomBytes } from 'node:crypto';
import { createServer } from 'node:net';
import { once } from 'node:events';
import assert from 'node:assert/strict';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const cwd = join(root, 'target', 'smoke-' + Date.now());
mkdirSync(cwd, { recursive: true });
const jdk = process.env.JAVA_HOME || join(homedir(), '.jdks', 'jbr-21.0.11');
const reservation = createServer();
reservation.listen(0, '127.0.0.1');
await once(reservation, 'listening');
const port = reservation.address().port;
await new Promise(resolve => reservation.close(resolve));
const base = `http://127.0.0.1:${port}`;
const password = randomBytes(24).toString('hex');
const auth = 'Basic ' + Buffer.from(`admin:${password}`).toString('base64');
let child;
async function start(legacyEnabled = true) {
  const fd = openSync(join(cwd, 'server.log'), 'a');
  child = spawn(join(jdk, 'bin', process.platform === 'win32' ? 'java.exe' : 'java'),
    ['-jar', join(root, 'target', 'dataserver-0.1.0.jar')], {
      cwd, windowsHide: true, stdio: ['ignore', fd, fd],
      env: { ...process.env, DATASERVER_ADMIN_PASSWORD: password, DATASERVER_PORT: String(port), DATASERVER_MANAGED_ENABLED: 'false',
        DATASERVER_BIND: '127.0.0.1', DATASERVER_LEGACY_UPLOAD_ENABLED: String(legacyEnabled),
        DATASERVER_STORAGE: join(cwd, 'files'), SPRING_PROFILES_ACTIVE: 'default',
        SPRING_DATASOURCE_URL: 'jdbc:h2:file:' + join(cwd, 'database', 'smoke').replaceAll('\\', '/')
          + ';MODE=MySQL;DATABASE_TO_LOWER=TRUE;WRITE_DELAY=0',
        SPRING_DATASOURCE_USERNAME: 'sa', SPRING_DATASOURCE_PASSWORD: '' }
    });
  closeSync(fd);
  for (let i = 0; i < 120; i++) {
    if (child.exitCode !== null) throw new Error('Server exited during startup');
    try { if ((await fetch(base + '/actuator/health')).ok) return; } catch {}
    await new Promise(resolve => setTimeout(resolve, 250));
  }
  throw new Error('Server did not become healthy');
}
async function stop() {
  if (child && child.exitCode === null) {
    const exited = once(child, 'exit'); child.kill(); await exited;
  }
}
const raw = readFileSync(join(root, 'samples', 'experiment.json'));
async function upload() {
  const response = await fetch(base + '/upload', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: raw });
  assert.equal(response.status, 200); return response.json();
}
try {
  await start();
  const first = await upload();
  assert.equal(first.Result, '1');
  const file = Buffer.from('smoke-pcr-storage-only');
  const form = new FormData(); form.append('file', new Blob([file]), 'smoke.pcr');
  const stored = await fetch(base + '/upload', { method: 'POST', body: form });
  assert.equal(stored.status, 200);
  const attachment = await stored.json();
  const link = await fetch(`${base}/api/admin/reports/${first.ReportID}/file/${attachment.FileID}`,
    { method: 'PUT', headers: { Authorization: auth } });
  assert.equal(link.status, 204);
  await stop();
  await start();
  const retry = await upload();
  assert.equal(retry.ReportID, first.ReportID); assert.equal(retry.Duplicate, true);
  const detail = await fetch(`${base}/api/admin/reports/${first.ReportID}`, { headers: { Authorization: auth } });
  assert.equal(detail.status, 200);
  const report = await detail.json();
  assert.deepEqual(report.Experiment, JSON.parse(raw.toString('utf8')));
  assert.equal(report.file_id, attachment.FileID);
  const download = await fetch(`${base}/api/admin/files/${attachment.FileID}/download`, { headers: { Authorization: auth } });
  assert.equal(download.status, 200);
  assert.deepEqual(Buffer.from(await download.arrayBuffer()), file);
  await stop();
  await start(false);
  const closed = await fetch(base + '/upload', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: raw });
  assert.equal(closed.status, 401);
  const adminStillWorks = await fetch(base + '/api/admin/summary', { headers: { Authorization: auth } });
  assert.equal(adminStillWorks.status, 200);
  console.log('PASS: executable JAR, HTTP JSON/multipart, H2 restart recovery, report and file persistence, duplicate retry, legacy endpoint disabled.');
} finally { await stop(); }
