import { clearToken, getToken } from '../auth/token'

/**
 * Lỗi từ backend, đã đọc từ Problem Details (RFC 9457, D24).
 * - detail: câu tiếng Việt để hiện chung cho cả form.
 * - fieldErrors: lỗi theo từng ô, ví dụ { email: "Chỉ nhận email trường ..." }.
 */
export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, detail: string, fieldErrors: Record<string, string> = {}) {
    super(detail)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

type ProblemDetail = {
  detail?: string
  errors?: { field: string; message: string }[]
}

type RequestOptions = {
  method?: 'GET' | 'POST' | 'PUT'
  body?: unknown
}

/**
 * Hàm gọi API dùng chung cho mọi màn hình:
 * - tự gắn "Authorization: Bearer <vé>" (trừ /api/auth/*, là API công khai);
 * - thành công: trả JSON (hoặc undefined với 204);
 * - lỗi: ném ApiError có detail và lỗi theo ô;
 * - 401 ở API cần đăng nhập: vé hết hạn hoặc không còn hợp lệ → xóa vé, về trang đăng nhập.
 *
 * Frontend chỉ giúp người dùng thấy lỗi sớm; mọi quyền vẫn do backend kiểm (spec 6.3).
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const isPublic = path.startsWith('/api/auth/')
  const headers: Record<string, string> = {}
  const token = getToken()
  if (token && !isPublic) {
    headers.Authorization = `Bearer ${token}`
  }
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(path, {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    })
  } catch {
    // fetch chỉ ném lỗi khi không gửi được request (backend tắt, mất mạng).
    throw new ApiError(0, 'Không kết nối được máy chủ. Kiểm tra backend đã chạy chưa rồi thử lại.')
  }

  if (response.status === 401 && !isPublic) {
    clearToken()
    window.location.assign('/login')
    throw new ApiError(401, 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.')
  }

  if (!response.ok) {
    throw await toApiError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  let problem: ProblemDetail = {}
  try {
    problem = (await response.json()) as ProblemDetail
  } catch {
    // Body không phải JSON (ví dụ proxy của Vite báo lỗi khi backend tắt).
  }

  if (response.status >= 500 && !problem.detail) {
    return new ApiError(response.status, 'Không kết nối được máy chủ. Kiểm tra backend đã chạy chưa rồi thử lại.')
  }

  const fieldErrors: Record<string, string> = {}
  for (const error of problem.errors ?? []) {
    // Giữ lỗi đầu tiên của mỗi ô: một ô chỉ cần hiện một câu.
    fieldErrors[error.field] ??= error.message
  }
  return new ApiError(response.status, problem.detail ?? `Lỗi ${response.status}`, fieldErrors)
}

/** Đổi lỗi bất kỳ thành ApiError để màn hình hiện được. */
export function toError(error: unknown): ApiError {
  return error instanceof ApiError ? error : new ApiError(0, 'Đã xảy ra lỗi không mong đợi.')
}
