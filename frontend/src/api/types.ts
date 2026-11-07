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
