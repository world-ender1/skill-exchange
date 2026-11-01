// Lưu vé JWT trong localStorage (D23): tải lại trang (F5) vẫn còn đăng nhập.
// localStorage có thể bị chặn (chế độ riêng tư, trình duyệt cấm lưu), nên mọi lần đọc/ghi đều bọc try/catch.

const TOKEN_KEY = 'skillexchange.accessToken'

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY)
  } catch {
    return null
  }
}

export function setToken(token: string): void {
  try {
    localStorage.setItem(TOKEN_KEY, token)
  } catch {
    // Không lưu được thì vé chỉ sống tới khi tải lại trang; không làm hỏng việc đăng nhập.
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(TOKEN_KEY)
  } catch {
    // Không có gì để xóa.
  }
}
