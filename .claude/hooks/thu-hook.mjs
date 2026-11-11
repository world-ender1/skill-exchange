// Thử hook run-matching-tests.mjs bằng tay, với đường dẫn Windows thật (dấu \) như Claude Code gửi.
// Đường dẫn dựng bằng String.fromCharCode(92), vì echo/printf/node -e trong Git Bash làm mất dấu \ (docs/harness-log.md).
// Cách dùng, từ C:\DACN:
//   node .claude/hooks/thu-hook.mjs DACN backend src main java com skillexchange evaluation SimUser.java
// Mong đợi: code đúng thì exit=0; test đỏ thì exit=2 kèm dòng lỗi; file ngoài matching/, evaluation/ thì exit=0 ngay.
import { spawnSync } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const parts = process.argv.slice(2)
if (parts.length === 0) {
  console.error('Thiếu đường dẫn. Ví dụ: node .claude/hooks/thu-hook.mjs DACN backend src main java com skillexchange evaluation SimUser.java')
  process.exit(1)
}

const hooksDir = dirname(fileURLToPath(import.meta.url))
const projectDir = join(hooksDir, '..', '..')
const filePath = ['C:', ...parts].join(String.fromCharCode(92))
const run = spawnSync('node', [join(hooksDir, 'run-matching-tests.mjs')], {
  input: JSON.stringify({ tool_input: { file_path: filePath } }),
  encoding: 'utf8',
  env: { ...process.env, CLAUDE_PROJECT_DIR: projectDir },
})

console.log(`Đã gửi: ${filePath}`)
if (run.stderr.trim()) console.log(run.stderr.trim())
console.log(`exit=${run.status}`)
