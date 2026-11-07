import { useState } from 'react'
import { Link } from 'react-router'

import type { MatchItem, ScoreBreakdown, TheyLearnItem, YouLearnItem } from '../api/types'
import { formatMode, formatPercent, formatSlot } from '../format'

/**
 * Một ứng viên trong S05 (wireframe TK 6.3). Mọi lý do hiển thị lấy thẳng từ response (NFR-09):
 * giao diện không tự tính lại điểm.
 * Chưa có nút "Gửi yêu cầu" (D31, tuần 6 mới làm).
 */
export function MatchCard({ match }: { match: MatchItem }) {
  const [showDetail, setShowDetail] = useState(false)

  return (
    <article className="card match-card">
      <header className="match-head">
        <h2 className="match-name">{match.fullName}</h2>
        <span className="match-score">Mức phù hợp: <strong>{formatPercent(match.score)}</strong></span>
        <span className="badge">
          {match.reviewCount === 0 ? 'Mới' : `★ ${match.ratingAverage} (${match.reviewCount})`}
        </span>
        {match.hasPendingRequest && <span className="badge">Đã gửi yêu cầu</span>}
      </header>

      {match.youLearn.map((item) => (
        <p key={`learn-${item.want}`} className="match-line">Bạn học: {describeYouLearn(item)}</p>
      ))}
      {/* theyLearn chỉ có ở TWO_WAY (D49): vắng key thì không vẽ dòng nào. */}
      {match.theyLearn?.map((item) => (
        <p key={`teach-${item.want}`} className="match-line">Bạn dạy: {describeTheyLearn(item)}</p>
      ))}
      <p className="match-line">
        Lịch trùng: {match.commonSlots.length === 0 ? 'chưa có' : match.commonSlots.map(formatSlot).join(', ')}
        <span className="match-sep" aria-hidden="true">·</span>
        Hình thức: {formatMode(match.learningMode)}
      </p>

      {showDetail && (
        <div className="score-detail">
          <ScoreTable title={match.backward ? 'Bạn học từ họ' : 'Điểm'} score={match.forward} />
          {match.backward && <ScoreTable title="Họ học từ bạn" score={match.backward} />}
        </div>
      )}

      <div className="actions">
        <button type="button" onClick={() => setShowDetail(!showDetail)} aria-expanded={showDetail}>
          {showDetail ? 'Ẩn chi tiết điểm' : 'Xem chi tiết điểm'}
        </button>
        <Link to={`/users/${match.userId}`}>Xem hồ sơ</Link>
      </div>
    </article>
  )
}

/** Mũi tên đi từ kỹ năng được dạy tới kỹ năng muốn học: "Java ← Kotlin" (kế hoạch bước 10). */
function describeYouLearn(item: YouLearnItem): string {
  return item.relation === 'EXACT'
    ? `${item.want} (trùng, trình độ ${item.theirLevel})`
    : `${item.want} ← ${item.teach} (kỹ năng liên quan, trình độ ${item.theirLevel})`
}

function describeTheyLearn(item: TheyLearnItem): string {
  return item.relation === 'EXACT'
    ? `${item.teach} (trùng, trình độ ${item.yourLevel})`
    : `${item.teach} → ${item.want} (kỹ năng liên quan, trình độ ${item.yourLevel})`
}

const COMPONENTS: { key: keyof ScoreBreakdown; label: string }[] = [
  { key: 'skillFit', label: 'Khớp kỹ năng' },
  { key: 'levelFit', label: 'Khớp trình độ' },
  { key: 'scheduleFit', label: 'Khớp lịch' },
  { key: 'modeFit', label: 'Khớp hình thức' },
  { key: 'reputation', label: 'Uy tín' },
]

/** D và 5 thành phần, in nguyên số API trả để so với bảng tính tay. */
function ScoreTable({ title, score }: { title: string; score: ScoreBreakdown }) {
  return (
    <table className="score-table">
      <caption>{title}: D = {score.d}</caption>
      <tbody>
        {COMPONENTS.map(({ key, label }) => (
          <tr key={key}>
            <th scope="row">{label}</th>
            <td>{score[key]}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
