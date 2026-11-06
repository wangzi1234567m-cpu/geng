import { execFileSync } from 'node:child_process';
import { mkdir, copyFile, lstat } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const root = fileURLToPath(new URL('../', import.meta.url));
const parent = path.join(root, '.dev', 'review-snapshots');
const destination = path.resolve(process.argv[2] || '');
if (!destination.startsWith(parent + path.sep)) throw new Error('Snapshot must stay in the private review directory');
const files = execFileSync('git', ['ls-files','--cached','--others','--exclude-standard','-z'], {cwd:root}).toString('utf8').split('\0').filter(Boolean);
for (const name of new Set(files)) {
  const source = path.join(root, name);
  if (!(await lstat(source)).isFile()) throw new Error(`Snapshot refuses symlinks: ${name}`);
  const target = path.join(destination, name);
  await mkdir(path.dirname(target), {recursive:true});
  await copyFile(source, target);
}
console.log(`Review snapshot created: ${files.length} candidate files; ignored private files excluded`);
