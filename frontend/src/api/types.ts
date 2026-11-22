// Kiểu dữ liệu khớp với DTO của backend (D28, D29, D40).

export type Role = 'STUDENT' | 'ADMIN'
export type UserStatus = 'PENDING_VERIFICATION' | 'ACTIVE' | 'LOCKED'
export type LearningMode = 'ONLINE' | 'OFFLINE' | 'ANY'
export type Period = 'MORNING' | 'AFTERNOON' | 'EVENING'

export type MyProfile = {
  id: number
  email: string
  fullName: string
  role: Role
  status: UserStatus
  bio: string | null
  learningMode: LearningMode | null
  phone: string | null
  contactInfo: string | null
}

export type AvailabilitySlot = {
  dayOfWeek: number
  period: Period
}

export type Skill = {
  id: number
  name: string
  categoryId: number
  categoryName: string
}

export type TeachSkill = {
  skillId: number
  skillName: string
  categoryId: number
  categoryName: string
  isActive: boolean
  level: number
}

export type LearnSkill = {
  skillId: number
  skillName: string
  categoryId: number
  categoryName: string
  isActive: boolean
  targetLevel: number | null
}

// ---------- Tuần 4: gợi ý ghép cặp (TK 4.5), hồ sơ công khai và tìm kiếm (TK 4.3) ----------

export type MatchType = 'TWO_WAY' | 'ONE_WAY'
export type SkillRelation = 'EXACT' | 'RELATED'

/** D và 5 thành phần của một chiều, đã làm tròn 3 chữ số (HALF_UP, kế hoạch tuần 4 bước 8). */
export type ScoreBreakdown = {
  d: number
  skillFit: number
  levelFit: number
  scheduleFit: number
  modeFit: number
  reputation: number
}

/** Dòng "Bạn học": theirLevel là trình độ người kia khai cho kỹ năng họ dạy. */
export type YouLearnItem = {
  want: string
  teach: string
  relation: SkillRelation
  theirLevel: number
}

/** Dòng "Bạn dạy": yourLevel là trình độ của chính bạn. */
export type TheyLearnItem = {
  want: string
  teach: string
  relation: SkillRelation
  yourLevel: number
}

export type MatchItem = {
  userId: number
  fullName: string
  type: MatchType
  score: number
  forward: ScoreBreakdown
  // D49: kết quả ONE_WAY KHÔNG có hai key này (không phải null) → khai báo bằng "?".
  backward?: ScoreBreakdown
  learningMode: LearningMode | null
  ratingAverage: number | null
  reviewCount: number
  hasPendingRequest: boolean
  youLearn: YouLearnItem[]
  theyLearn?: TheyLearnItem[]
  commonSlots: AvailabilitySlot[]
}

export type MatchList = {
  type: MatchType
  needsLearnSkills: boolean
  total: number
  items: MatchItem[]
}

/** Kỹ năng dạy trong hồ sơ công khai và kết quả tìm kiếm (không có isActive, D46c). */
export type PublicTeachSkill = {
  skillId: number
  skillName: string
  categoryName: string
  level: number
}

export type PublicLearnSkill = {
  skillId: number
  skillName: string
  categoryName: string
  targetLevel: number | null
}

/** GET /api/users/{id}: không có email, phone, contactInfo (NFR-02). */
export type PublicProfile = {
  id: number
  fullName: string
  bio: string | null
  learningMode: LearningMode | null
  teachSkills: PublicTeachSkill[]
  learnSkills: PublicLearnSkill[]
  availability: AvailabilitySlot[]
}

/** Một phần tử của GET /api/users/search (D45d). */
export type UserSearchResult = {
  id: number
  fullName: string
  learningMode: LearningMode | null
  teachSkills: PublicTeachSkill[]
}

// ---------- Tuần 5: quản trị danh mục kỹ năng A01 (TK 4.9, D73) ----------

/** GET /api/skill-categories (D40); cũng là response của POST/PUT /api/admin/skill-categories. */
export type SkillCategory = {
  id: number
  name: string
}

/** Một phần tử của GET /api/admin/skills: có cả kỹ năng đã ẩn (isActive = false). */
export type AdminSkill = {
  id: number
  name: string
  categoryId: number
  categoryName: string
  isActive: boolean
}

/** Một cặp của GET /api/admin/skill-relations, luôn skillAId < skillBId. */
export type AdminSkillRelation = {
  skillAId: number
  skillAName: string
  skillBId: number
  skillBName: string
}

// ---------- Tuần 6: yêu cầu trao đổi (TK 4.6, D89–D91) ----------

/** Cùng hai giá trị với MatchType: tab của S05 chọn sẵn loại yêu cầu (D95). */
export type RequestType = MatchType
/** Hình thức của một buổi: không có ANY như LearningMode (D89). */
export type MeetingMode = 'ONLINE' | 'OFFLINE'
export type RequestStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'
export type RequestRole = 'SENDER' | 'RECEIVER'

/** Body của POST /api/requests (D89). Không có senderId: người gửi luôn là chủ của vé. */
export type CreateRequestBody = {
  receiverId: number
  type: RequestType
  senderLearnSkillId: number | null
  // ONE_WAY phải để null; TWO_WAY bắt buộc có (D89 bước 2).
  receiverLearnSkillId: number | null
  // Chuỗi ISO-8601 có múi giờ (D88), ví dụ "2026-11-24T12:00:00.000Z".
  proposedStartAt: string | null
  durationMinutes: number | null
  mode: MeetingMode
  locationOrLink: string
  message: string
}

export type PersonRef = { id: number; fullName: string }
export type SkillRef = { id: number; name: string }

/**
 * Một yêu cầu như người xem thấy (RequestItem, bảng 2.2). Mọi khóa luôn có, không có giá trị thì null.
 * locationOrLink là null khi người nhận xem yêu cầu chưa ACCEPTED (Q1, D91).
 */
export type RequestItem = {
  id: number
  type: RequestType
  status: RequestStatus
  myRole: RequestRole
  sender: PersonRef
  receiver: PersonRef
  senderLearnSkill: SkillRef
  receiverLearnSkill: SkillRef | null
  proposedStartAt: string
  durationMinutes: number
  mode: MeetingMode
  locationOrLink: string | null
  message: string | null
  responseNote: string | null
  createdAt: string
  respondedAt: string | null
  sessionId: number | null
}

/** Response của POST /api/requests/{id}/accept (D90): chỉ hai mã, đủ để mở phiên vừa tạo. */
export type AcceptResponse = {
  requestId: number
  sessionId: number
}

// ---------- Tuần 6: phiên trao đổi (TK 4.7, D92–D94) ----------

export type SessionStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'WAITING_CONFIRMATION' | 'COMPLETED' | 'CANCELLED'

/**
 * Một phiên trong GET /api/sessions (SessionItem, bảng 2.2). Không có địa điểm/link và liên hệ: chỉ chi tiết
 * phiên mới có (D92, NFR-02).
 */
export type SessionItem = {
  id: number
  requestId: number
  type: RequestType
  status: SessionStatus
  myRole: RequestRole
  partner: PersonRef
  senderLearnSkill: SkillRef
  receiverLearnSkill: SkillRef | null
  startAt: string
  durationMinutes: number
  mode: MeetingMode
  createdAt: string
}

/** Bên kia của phiên kèm liên hệ hiện tại trong hồ sơ của họ; chưa khai thì null (D92). */
export type SessionPartner = PersonRef & {
  phone: string | null
  contactInfo: string | null
}

/** Ai hủy phiên, nhìn từ phía người xem; backend đã đổi id người hủy thành ba giá trị này (D92). */
export type CancelledBy = 'ME' | 'PARTNER' | 'ADMIN'

/** Báo cáo OPEN của phiên: không có lý do, vì lý do chỉ dành cho admin (D92). */
export type OpenReport = {
  id: number
  reportedByMe: boolean
  createdAt: string
}

export type ReportResolution = 'SESSION_COMPLETED' | 'SESSION_CANCELLED'

/** Kết quả admin chốt báo cáo (tuần 7 mới có dữ liệu thật). */
export type SessionResolution = {
  result: ReportResolution
  adminNote: string | null
  resolvedAt: string
}

/**
 * GET /api/sessions/{id}, cũng là response của start, complete, cancel (SessionDetail, bảng 2.2; D92, D93).
 * = các khóa của SessionItem (partner có thêm liên hệ) + các khóa riêng của chi tiết.
 */
export type SessionDetail = Omit<SessionItem, 'partner'> & {
  partner: SessionPartner
  // Cột DB cho phép NULL (dữ liệu chèn tay của test cũ); phiên tạo qua API luôn có (Q3).
  locationOrLink: string | null
  startedAt: string | null
  completedAt: string | null
  myCompleted: boolean
  cancelledBy: CancelledBy | null
  cancelReason: string | null
  cancelledAt: string | null
  myReviewed: boolean
  openReport: OpenReport | null
  resolution: SessionResolution | null
}

/** Response 201 của POST /api/sessions/{id}/reports (D94): không trả lại lý do. */
export type ReportResponse = {
  id: number
  sessionId: number
  status: 'OPEN'
  createdAt: string
}
