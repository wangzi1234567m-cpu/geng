import { readFile, readdir } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = fileURLToPath(new URL('../', import.meta.url));
const read = async name => (await readFile(path.join(root, name), 'utf8')).replace(/^\uFEFF/, '');
const spec = JSON.parse(await read('contracts/openapi.draft.json'));
if (spec.openapi !== '3.1.1' || spec['x-status'] !== 'review-draft') throw new Error('Unexpected contract status/version');
const operations = Object.values(spec.paths).flatMap(item => Object.entries(item).filter(([verb]) => /^(get|post|put|patch|delete)$/.test(verb)).map(([, value]) => value));
const ids = operations.map(op => op.operationId);
if (new Set(ids).size !== ids.length || ids.some(id => !id)) throw new Error('Missing or duplicated operation ID');
const required = ['type', 'title', 'status', 'code', 'requestId'];
if (!required.every(field => spec.components.schemas.Problem.required.includes(field))) throw new Error('Problem contract incomplete');
if (!spec['x-pending-decisions']?.length) throw new Error('Undecided product constraints must remain explicit');

const migrationRoot = 'backend/infrastructure/src/main/resources/db/migration';
const manifest = JSON.parse(await read('contracts/migrations.sha256.json'));
const migrations = (await readdir(path.join(root, migrationRoot))).filter(name => name.endsWith('.sql')).sort();
if (migrations.length !== manifest.length) throw new Error('New migration must be included in the reviewed checksum manifest');
for (const item of manifest) {
  const bytes = await readFile(path.join(root, migrationRoot, item.name));
  // SQL source may have CRLF on Windows; Git stores canonical LF.
  const canonical = bytes.toString('utf8').replace(/\r\n/g, '\n');
  const actual = createHash('sha256').update(canonical).digest('hex');
  if (actual !== item.sha256) throw new Error(`Migration changed: ${item.name}; append a migration instead`);
}
const files = execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'], { cwd: root }).toString('utf8').split('\0').filter(Boolean);
const forbidden = /(^|\/)(node_modules|\.dev|\.tools|target|build)\/|(^|\/)(dev\.local\.json|local\.properties|\.env|local\.env)$|\.(p12|pfx|keystore|jks|apk|aab)$/i;
if (files.some(name => forbidden.test(name))) throw new Error('Private config or generated artifacts are candidates for Git');
console.log(`Source checks passed: ${operations.length} operations, ${manifest.length} immutable migrations, ${files.length} candidate files`);
