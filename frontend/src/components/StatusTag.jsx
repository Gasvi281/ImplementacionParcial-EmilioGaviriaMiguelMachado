const TONE_BY_STATUS = {
  ACTIVE: 'good',
  APPROVED: 'good',
  FINISHED: 'good',
  COMPLETED: 'good',
  OPEN_FOR_REGISTRATION: 'good',
  PENDING: 'pending',
  DRAFT: 'pending',
  CLOSED_FOR_REGISTRATION: 'pending',
  IN_PROGRESS: 'pending',
  INJURED: 'pending',
  SUSPENDED: 'pending',
  RETIRED: 'bad',
  REJECTED: 'bad',
  CANCELLED: 'bad',
  DISBANDED: 'bad',
  DISQUALIFIED: 'bad',
  DID_NOT_FINISH: 'bad',
  DID_NOT_START: 'bad',
}

export default function StatusTag({ status, label }) {
  const tone = TONE_BY_STATUS[status] ?? 'neutral'
  return <span className={`tag tag-${tone}`}>{label ?? status}</span>
}
