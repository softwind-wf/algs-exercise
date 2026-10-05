// Inspect npm tarball file list for @a9i5k4/dsh-auto-memory latest.
const { execFileSync } = require('node:child_process');
const url = process.argv[2];
const out = execFileSync('tar', ['-tzf', '-'], { input: require('node:fs').readFileSync(url), maxBuffer: 1 << 28 });
console.log(out.toString('utf8'));
