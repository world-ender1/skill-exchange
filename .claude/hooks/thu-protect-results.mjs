// Thử hook protect-results.mjs bằng bảng ca: mỗi ca là (công cụ, đầu vào) → chặn hay cho qua (kế hoạch tuần 5, bước 8).
// Đường dẫn Windows dựng bằng String.fromCharCode(92), vì echo/printf/node -e trong Git Bash làm mất dấu \ (docs/harness-log.md).
// Cách dùng, từ C:\DACN:
//   node .claude/hooks/thu-protect-results.mjs
// Mong đợi: mọi dòng "đúng", exit=0. Có dòng "SAI" thì exit=1.
import { spawnSync } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const hook = join(dirname(fileURLToPath(import.meta.url)), 'protect-results.mjs')
const SLASH = String.fromCharCode(92)
const abs = (...parts) => ['C:', 'DACN', ...parts].join(SLASH) // C:\DACN\...
const rel = (...parts) => parts.join(SLASH) // thuc_nghiem\ket_qua\...
const CHAN = 'chặn'
const QUA = 'cho qua'

const RUN = '2026-11-14_chon-cau-hinh'
const CASES = [
  // Write, Edit: mọi file trong thuc_nghiem/ket_qua/, kể cả file chưa có
  ['Edit', { file_path: abs('thuc_nghiem', 'ket_qua', RUN, 'README.md') }, CHAN],
  ['Write', { file_path: abs('thuc_nghiem', 'ket_qua', '2026-11-14_moi', 'metrics.csv') }, CHAN],
  ['Write', { file_path: abs('docs', '..', 'Thuc_Nghiem', 'Ket_Qua', 'x', 'a.md') }, CHAN],
  ['Write', { file_path: abs('thuc_nghiem', 'khac', '..', 'ket_qua', 'x', 'a.md') }, CHAN],
  ['Write', { file_path: 'C:/DACN/thuc_nghiem/ket_qua/x/log.txt' }, CHAN],
  ['Write', { file_path: abs('docs', 'ket_qua.md') }, QUA],
  ['Edit', { file_path: abs('docs', 'tuan_5', 'Quyet_dinh_tuan_5.md') }, QUA],
  ['Edit', { file_path: abs('thuc_nghiem', 'ghi_chu.md') }, QUA],

  // Bash: xóa, di chuyển, chép, ghi
  ['Bash', { command: `rm -rf thuc_nghiem/ket_qua/${RUN}` }, CHAN],
  ['Bash', { command: 'cd /c/DACN/thuc_nghiem/ket_qua && rm -r 2026-11-14_bao-cao' }, CHAN],
  ['Bash', { command: 'mv thuc_nghiem/ket_qua/a thuc_nghiem/ket_qua/b' }, CHAN],
  ['Bash', { command: 'cp /tmp/metrics.csv thuc_nghiem/ket_qua/a/metrics.csv' }, CHAN],
  ['Bash', { command: 'touch thuc_nghiem/ket_qua/a/README.md' }, CHAN],
  ['Bash', { command: 'echo 0.99 > thuc_nghiem/ket_qua/a/metrics.csv' }, CHAN],
  ['Bash', { command: 'printf x >>thuc_nghiem/ket_qua/a/log.txt' }, CHAN],
  ['Bash', { command: "sed -i 's/0.9/0.95/' thuc_nghiem/ket_qua/a/metrics.csv" }, CHAN],
  ['Bash', { command: "find thuc_nghiem -name '*.csv' -delete" }, CHAN],
  ['Bash', { command: `find thuc_nghiem -name x -exec rm {} ${SLASH};` }, CHAN],
  ['Bash', { command: 'git rm -r thuc_nghiem/ket_qua/a' }, CHAN],
  ['Bash', { command: `python -c "import shutil; shutil.rmtree('thuc_nghiem/ket_qua/a')"` }, CHAN],
  ['Bash', { command: `python -c "open('thuc_nghiem/ket_qua/a/metrics.csv', 'w').write('x')"` }, CHAN],
  ['Bash', { command: `node -e "require('fs').rmSync('thuc_nghiem/ket_qua/a', { recursive: true })"` }, CHAN],
  ['Bash', { command: 'powershell -Command "Remove-Item -Recurse thuc_nghiem/ket_qua/a"' }, CHAN],
  // Bash: git xóa file chưa theo dõi, dù không nhắc tên thư mục
  ['Bash', { command: 'git clean -fd' }, CHAN],
  ['Bash', { command: 'git -C /c/DACN clean -fdx' }, CHAN],
  ['Bash', { command: 'git stash -u' }, CHAN],
  ['Bash', { command: 'git stash push --include-untracked' }, CHAN],
  // Bash: đọc, git add/commit, chạy thực nghiệm
  ['Bash', { command: 'ls -la thuc_nghiem/ket_qua' }, QUA],
  ['Bash', { command: 'cat thuc_nghiem/ket_qua/a/metrics.csv | head -5' }, QUA],
  ['Bash', { command: `sed -n 1,20p thuc_nghiem/ket_qua/${RUN}/README.md` }, QUA],
  ['Bash', { command: 'wc -l thuc_nghiem/ket_qua/a/per_user.csv 2>/dev/null' }, QUA],
  ['Bash', { command: 'git add thuc_nghiem/ket_qua/a docs/ket_qua.md && git commit -m "docs: add experiment results"' }, QUA],
  ['Bash', { command: 'git commit -m "chore: clean up hook"' }, QUA],
  ['Bash', { command: 'cd backend && java -cp target/classes com.skillexchange.evaluation.EvaluationMain --giai-doan chon --ten chon-cau-hinh' }, QUA],
  ['Bash', { command: 'git clean -n' }, QUA],
  ['Bash', { command: 'git clean -nd' }, QUA],
  ['Bash', { command: 'git stash list' }, QUA],
  ['Bash', { command: 'rm -rf backend/target' }, QUA],

  // PowerShell
  ['PowerShell', { command: `Remove-Item -Recurse -Force ${abs('thuc_nghiem', 'ket_qua', RUN)}` }, CHAN],
  ['PowerShell', { command: `Get-ChildItem ${rel('thuc_nghiem', 'ket_qua')} | Remove-Item -Recurse` }, CHAN],
  ['PowerShell', { command: `ri -r ${rel('thuc_nghiem')}` }, CHAN],
  ['PowerShell', { command: `Set-Content ${rel('thuc_nghiem', 'ket_qua', 'a', 'README.md')} 'x'` }, CHAN],
  ['PowerShell', { command: `Move-Item ${rel('thuc_nghiem', 'ket_qua', 'a')} C:${SLASH}tam` }, CHAN],
  ['PowerShell', { command: `'x' | Out-File ${rel('thuc_nghiem', 'ket_qua', 'a', 'log.txt')}` }, CHAN],
  ['PowerShell', { command: `Get-Content ${rel('thuc_nghiem', 'ket_qua', 'a', 'metrics.csv')} -TotalCount 5` }, QUA],
  ['PowerShell', { command: `Import-Csv ${rel('thuc_nghiem', 'ket_qua', 'a', 'metrics.csv')} | Where-Object chi_so -eq 'coverage'` }, QUA],

  // Lọt qua bản đầu của hook, tìm ra khi review độc lập tuần 5 (README kiểm thử tuần 5)
  ['PowerShell', { command: `Import-Csv ${rel('thuc_nghiem', 'ket_qua', 'a', 'metrics.csv')} | Export-Csv -Path ${rel('thuc_nghiem', 'ket_qua', 'a', 'metrics.csv')}` }, CHAN],
  ['PowerShell', { command: `[IO.File]::WriteAllText('thuc_nghiem/ket_qua/a/README.md', 'x')` }, CHAN],
  ['PowerShell', { command: `Expand-Archive x.zip -DestinationPath ${rel('thuc_nghiem', 'ket_qua', 'a')} -Force` }, CHAN],
  ['PowerShell', { command: `Invoke-WebRequest http://example.invalid/x -OutFile ${rel('thuc_nghiem', 'ket_qua', 'a', 'metrics.csv')}` }, CHAN],
  ['Bash', { command: 'curl -o thuc_nghiem/ket_qua/a/metrics.csv http://example.invalid/x' }, CHAN],
  ['Bash', { command: 'wget -O thuc_nghiem/ket_qua/a/metrics.csv http://example.invalid/x' }, CHAN],
  ['Bash', { command: 'unzip -o x.zip -d thuc_nghiem/ket_qua/a' }, CHAN],
  ['Bash', { command: 'tar -xf x.tar -C thuc_nghiem/ket_qua/a' }, CHAN],

  // Công cụ khác không xét
  ['Read', { file_path: abs('thuc_nghiem', 'ket_qua', RUN, 'README.md') }, QUA],
]

let wrong = 0
for (const [tool, toolInput, expected] of CASES) {
  const run = spawnSync('node', [hook], {
    input: JSON.stringify({ tool_name: tool, tool_input: toolInput }),
    encoding: 'utf8',
  })
  const actual = run.status === 2 ? CHAN : run.status === 0 ? QUA : `lỗi (exit=${run.status})`
  const ok = actual === expected
  if (!ok) wrong++
  const shown = toolInput.command ?? toolInput.file_path
  console.log(`${ok ? 'đúng' : 'SAI '} | mong ${expected.padEnd(7)} | được ${actual.padEnd(7)} | ${tool}: ${shown}`)
  if (!ok && run.stderr.trim()) console.log(`       ${run.stderr.trim().split(/\r?\n/)[0]}`)
}
console.log(`${CASES.length - wrong}/${CASES.length} ca đúng`)
process.exit(wrong === 0 ? 0 : 1)
