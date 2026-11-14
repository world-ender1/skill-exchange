// Hook D72: chặn AI sửa, xóa, di chuyển kết quả thực nghiệm trong thuc_nghiem/ket_qua/
// (TCKQ §1.2 nguyên tắc 5; phạm vi chặn ở kế hoạch tuần 5, bước 8). Chạy TRƯỚC Write, Edit, Bash, PowerShell.
// Claude Code gửi lời gọi công cụ vào stdin dạng JSON; thoát 2 = chặn (lý do ở stderr được đưa cho AI), thoát 0 = cho qua.
// Giới hạn: hook so chuỗi lệnh, không hiểu shell đầy đủ. Nó chặn các cách xóa, ghi đè thường gặp, không chặn được mọi cách
// (script để trong file riêng tự mở file để ghi; xóa thư mục cha như cả repo). Lời dặn trong CLAUDE.md vẫn áp dụng.
// Sửa hook này xong thì chạy `node .claude/hooks/thu-protect-results.mjs` (bảng ca thử), không thử bằng echo (mất dấu \).
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

// Tên lệnh xóa, di chuyển, chép hoặc ghi file của Git Bash, PowerShell (cả bí danh) và cmd. So bằng chữ thường.
const WRITING_COMMANDS = new Set([
  'rm', 'rmdir', 'mv', 'cp', 'touch', 'unlink', 'shred', 'truncate', 'tee', 'dd', 'rsync',
  'remove-item', 'ri', 'del', 'erase', 'rd', 'move-item', 'mi', 'move', 'rename-item', 'rni', 'ren', 'rename',
  'copy-item', 'cpi', 'copy', 'xcopy', 'robocopy', 'new-item', 'ni', 'clear-item', 'cli',
  'set-content', 'sc', 'add-content', 'ac', 'clear-content', 'clc', 'out-file', 'tee-object',
  // Ghi file qua tùy chọn đầu ra hoặc giải nén (review tuần 5). Như cp, bị chặn cả khi chỉ đọc từ thuc_nghiem
  // (ví dụ tar -c thuc_nghiem): chặn nhầm một lệnh hiếm còn hơn để lọt một lệnh ghi đè.
  'export-csv', 'epcsv', 'expand-archive', 'unzip', 'tar', 'curl', 'wget', 'invoke-webrequest', 'iwr',
])

// Dấu hiệu ghi hoặc xóa không nằm ở tên lệnh.
const WRITING_PATTERNS = [
  />>?\s*['"]?[^\s'"|;&<>]*thuc_nghiem/i, // chuyển hướng > hoặc >> vào thuc_nghiem
  /\b(sed|perl)\b[^|;&]*\s(-[a-z]*i|--in-place)/i, // sửa tại chỗ: sed -i, perl -pi
  /\s-delete\b/i, // find ... -delete
  // Hàm xóa, ghi gọi thẳng từ python -c, node -e, PowerShell
  /rmtree|\bos\.(remove|unlink|rename|replace)\b|\bshutil\.(move|copy\w*)\b/i,
  /\bopen\([^)]*,\s*['"][wax]/i,
  /\.write_(text|bytes)\s*\(/i,
  /\b(rm|rmdir|unlink|rename|writeFile|appendFile|copyFile|cp)(Sync)?\s*\(/i,
  /\[(System\.)?IO\.(File|Directory)\]::(Delete|Move|Copy|Write|Append)/i, // [IO.File]:: cũng là System.IO.File
]

// "git [tùy chọn chung như -C <thư mục>] clean|stash": tùy chọn chung đứng giữa git và lệnh con.
const GIT_PREFIX = String.raw`\bgit(?:\s+-[cC]\s+\S+|\s+--?[\w-]+(?:=\S+)?)*\s+`
const GIT_CLEAN = new RegExp(GIT_PREFIX + String.raw`clean\b([^;|&\n]*)`)
const GIT_STASH = new RegExp(GIT_PREFIX + String.raw`stash\b([^;|&\n]*)`)

const input = JSON.parse(readFileSync(0, 'utf8'))
const reason = whyBlocked(input.tool_name, input.tool_input ?? {})
if (reason) {
  console.error(
    `Hook protect-results chặn ${input.tool_name}: ${reason}. ` +
      'File trong thuc_nghiem/ket_qua/ chỉ do EvaluationMain tạo, không sửa, xóa hay thêm bằng tay (D72). ' +
      'Muốn chạy lại thì tạo lượt mới với --ten khác; bị chặn nhầm thì báo người dùng, không tìm cách lách.',
  )
  process.exit(2)
}
process.exit(0)

function whyBlocked(tool, toolInput) {
  if (tool === 'Write' || tool === 'Edit') {
    const file = toolInput.file_path ?? ''
    return isInResults(file) ? `${file} nằm trong thuc_nghiem/ket_qua/` : null
  }
  if (tool === 'Bash' || tool === 'PowerShell') {
    return whyCommandBlocked(toolInput.command ?? '')
  }
  return null // Read, Grep... chỉ đọc
}

// Kể cả file chưa có: tạo file mới trong thư mục kết quả cũng là sửa kết quả.
function isInResults(file) {
  if (file === '') return false
  // resolve rút gọn ".."; so chữ thường vì đường dẫn Windows không phân biệt hoa thường.
  const normalized = resolve(file).replaceAll('\\', '/').toLowerCase()
  return normalized.includes('/thuc_nghiem/ket_qua/')
}

function whyCommandBlocked(command) {
  // git clean và git stash -u xóa file chưa theo dõi, gồm kết quả chưa commit, dù lệnh không nhắc tên thư mục.
  const clean = command.match(GIT_CLEAN)
  if (clean && !hasFlag(clean[1], /^(-[a-z]*n[a-z]*|--dry-run)$/i)) {
    return 'git clean xóa file chưa theo dõi, gồm kết quả chưa commit (chỉ cho phép git clean -n)'
  }
  const stash = command.match(GIT_STASH)
  if (stash && hasFlag(stash[1], /^(-[a-z]*[ua][a-z]*|--include-untracked|--all)$/i)) {
    return 'git stash -u/-a cất đi file chưa theo dõi, gồm kết quả chưa commit'
  }

  if (!/thuc_nghiem/i.test(command)) return null
  const word = commandWords(command).find((w) => WRITING_COMMANDS.has(w))
  if (word) return `lệnh "${word}" trong một lệnh nhắc tới thuc_nghiem`
  const pattern = WRITING_PATTERNS.find((p) => p.test(command))
  if (pattern) return `lệnh ghi hoặc xóa trên thuc_nghiem (khớp ${pattern})`
  return null
}

function hasFlag(args, flagPattern) {
  return args.trim().split(/\s+/).some((token) => flagPattern.test(token))
}

// Các từ của lệnh, tách theo khoảng trắng và dấu của shell; "/bin/rm" và "rm.exe" thành "rm".
function commandWords(command) {
  return command
    .split(/[\s;|&(){}`'"<>,=]+/)
    .map((w) => w.toLowerCase().replaceAll('\\', '/').split('/').pop().replace(/\.exe$/, ''))
    .filter((w) => w !== '')
}
