export function formatDateTime(value) {
  if (!value) return '—'
  return new Date(value).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' })
}

export function formatSeconds(value) {
  if (value === null || value === undefined) return '—'
  const minutes = Math.floor(value / 60)
  const seconds = (value % 60).toFixed(2)
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`
}

export const competitorTypeLabels = {
  DWARF: 'Enano',
  CAMEL: 'Camello',
  MEDIUM: 'Mediano',
  OTHER: 'Otro',
}

export const competitorStatusLabels = {
  ACTIVE: 'Activo',
  INJURED: 'Lesionado',
  SUSPENDED: 'Suspendido',
  RETIRED: 'Retirado',
}

export const teamStatusLabels = {
  ACTIVE: 'Activo',
  SUSPENDED: 'Suspendido',
  DISBANDED: 'Disuelto',
}

export const raceStatusLabels = {
  DRAFT: 'Borrador',
  OPEN_FOR_REGISTRATION: 'Inscripciones abiertas',
  CLOSED_FOR_REGISTRATION: 'Inscripciones cerradas',
  IN_PROGRESS: 'En curso',
  COMPLETED: 'Finalizada',
  CANCELLED: 'Cancelada',
}

export const raceTypeLabels = {
  INDIVIDUAL: 'Individual',
  TEAM: 'Por equipos',
  MIXED: 'Mixta',
}

export const registrationStatusLabels = {
  PENDING: 'Pendiente',
  APPROVED: 'Aprobada',
  REJECTED: 'Rechazada',
  CANCELLED: 'Cancelada',
}

export const resultStatusLabels = {
  FINISHED: 'Finalizó',
  DISQUALIFIED: 'Descalificado',
  DID_NOT_FINISH: 'No terminó',
  DID_NOT_START: 'No se presentó',
}

// Progresión de un solo sentido de RaceStatus (.claude/rules/races.md) — CANCELLED es alcanzable
// desde cualquier estado no terminal, así que se agrega aparte a cada lista de "siguientes".
const RACE_STATUS_ORDER = [
  'DRAFT',
  'OPEN_FOR_REGISTRATION',
  'CLOSED_FOR_REGISTRATION',
  'IN_PROGRESS',
  'COMPLETED',
]

export function nextRaceStatuses(current) {
  const index = RACE_STATUS_ORDER.indexOf(current)
  if (index === -1 || index === RACE_STATUS_ORDER.length - 1) return []
  return RACE_STATUS_ORDER.slice(index + 1)
}
