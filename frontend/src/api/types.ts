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
