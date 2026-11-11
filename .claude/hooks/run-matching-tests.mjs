// Hook C13: sau khi AI sửa file trong package matching hoặc evaluation, tự chạy test JUnit thuần của package đó
// (không cần DB, ~20 giây). Tuần 5 thêm evaluation (kế hoạch tuần 5, bước 1).
// Claude Code gửi thông tin lần sửa vào stdin dạng JSON; thoát 2 = báo test đỏ cho AI đọc.
// Sửa hook này xong thì thử bằng `node .claude/hooks/thu-hook.mjs ...`, không dùng echo (mất dấu \).
import { spawnSync } from 'node:child_process'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

// Mỗi package một bộ test. MatchesApiTest là test tích hợp (cần DB) nên loại khỏi bộ của matching.
const SUITES = [
  { folder: '/com/skillexchange/matching/', name: 'lõi matching', tests: 'com.skillexchange.matching.*Test,!MatchesApiTest' },
  { folder: '/com/skillexchange/evaluation/', name: 'thực nghiệm evaluation', tests: 'com.skillexchange.evaluation.*Test' },
]

const input = JSON.parse(readFileSync(0, 'utf8'))
const file = (input.tool_input?.file_path ?? '').replaceAll('\\', '/')
const suite = SUITES.find((s) => file.includes(s.folder))
if (!suite) process.exit(0) // không thuộc package nào ở trên: bỏ qua

const backend = join(process.env.CLAUDE_PROJECT_DIR ?? process.cwd(), 'backend')
// cmd.exe không tự tìm mvnw.cmd trong thư mục làm việc, nên gọi bằng đường dẫn tuyệt đối.
// Ngoặc kép quanh -Dtest: với cmd, dấu phẩy là dấu tách tham số.
const command = `"${join(backend, 'mvnw.cmd')}" -q -o test "-Dtest=${suite.tests}"`
const run = spawnSync(command, { cwd: backend, shell: true, encoding: 'utf8' })

if (run.status === 0) process.exit(0)

// Chỉ đưa cho AI các dòng đáng đọc, không đổ cả log Maven.
const all = `${run.stdout ?? ''}${run.stderr ?? ''}`.split(/\r?\n/)
const picked = all.filter((l) => /Tests run:.*(Failures: [1-9]|Errors: [1-9])|expected|ERROR\]\s{3}|COMPILATION/.test(l))
// Lọc không ra dòng nào (Maven không chạy được...) thì đưa phần cuối log, đừng báo đỏ mà không có lý do.
const shown = picked.length > 0 ? picked.slice(0, 30) : all.filter((l) => l.trim() !== '').slice(-20)
console.error(`Test ${suite.name} ĐỎ sau lần sửa ${file}:\n${shown.join('\n')}`)
process.exit(2)
