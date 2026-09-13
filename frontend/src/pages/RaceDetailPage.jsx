import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { competitorsApi, racesApi, registrationsApi, resultsApi, teamsApi } from '../api.js'
import { hasRole } from '../auth.js'
import ErrorBanner from '../components/ErrorBanner.jsx'
import StatusTag from '../components/StatusTag.jsx'
import {
  formatDateTime,
  formatSeconds,
  nextRaceStatuses,
  raceStatusLabels,
  raceTypeLabels,
  registrationStatusLabels,
  resultStatusLabels,
} from '../format.js'

const EMPTY_REGISTRATION = { participantType: 'competitor', participantId: '', notes: '' }
const EMPTY_RESULT = {
  registrationId: '',
  status: 'FINISHED',
  startPosition: '',
  finalPosition: '',
  completionTimeSeconds: '',
  penaltyTimeSeconds: '',
  notes: '',
}

export default function RaceDetailPage() {
  const { raceId } = useParams()
  const [race, setRace] = useState(null)
  const [registrations, setRegistrations] = useState([])
  const [results, setResults] = useState([])
  const [competitors, setCompetitors] = useState([])
  const [teams, setTeams] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [showRegForm, setShowRegForm] = useState(false)
  const [regForm, setRegForm] = useState(EMPTY_REGISTRATION)
  const [showResultForm, setShowResultForm] = useState(false)
  const [resultForm, setResultForm] = useState(EMPTY_RESULT)
  const [nextStatus, setNextStatus] = useState('')
  const [rejectNotes, setRejectNotes] = useState({})

  const canManageRace = hasRole('ADMINISTRATOR', 'RACE_ORGANIZER')
  const canCancelRegistration = hasRole('ADMINISTRATOR')

  function load() {
    setLoading(true)
    Promise.all([
      racesApi.get(raceId),
      registrationsApi.listForRace(raceId, { size: 50 }),
      resultsApi.listForRace(raceId, { size: 50 }),
      competitorsApi.list(),
      teamsApi.list(),
    ])
      .then(([raceData, regPage, resultPage, competitorList, teamList]) => {
        setRace(raceData)
        setRegistrations(regPage.content)
        setResults(resultPage.content)
        setCompetitors(competitorList)
        setTeams(teamList)
        setNextStatus('')
      })
      .catch(setError)
      .finally(() => setLoading(false))
  }

  useEffect(load, [raceId])

  if (loading && !race) return <p className="empty-state">Cargando carrera…</p>
  if (!race) return <ErrorBanner error={error} />

  const approvedRegistrations = registrations.filter((r) => r.status === 'APPROVED')

  async function handleStatusChange() {
    if (!nextStatus) return
    setError(null)
    try {
      await racesApi.updateStatus(raceId, nextStatus)
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleCreateRegistration(event) {
    event.preventDefault()
    setError(null)
    try {
      const body = { notes: regForm.notes }
      if (regForm.participantType === 'competitor') body.competitorId = regForm.participantId
      else body.teamId = regForm.participantId
      await registrationsApi.create(raceId, body)
      setRegForm(EMPTY_REGISTRATION)
      setShowRegForm(false)
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleApprove(id) {
    setError(null)
    try {
      await registrationsApi.approve(id, {})
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleReject(id) {
    const notes = rejectNotes[id]
    if (!notes) {
      setError(new Error('Escribe un motivo antes de rechazar la inscripción.'))
      return
    }
    setError(null)
    try {
      await registrationsApi.reject(id, notes)
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleCancelRegistration(id) {
    if (!window.confirm('¿Cancelar esta inscripción?')) return
    setError(null)
    try {
      await registrationsApi.cancel(id)
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleCreateResult(event) {
    event.preventDefault()
    setError(null)
    const registration = registrations.find((r) => r.id === resultForm.registrationId)
    if (!registration) {
      setError(new Error('Elige un participante inscrito y aprobado.'))
      return
    }
    try {
      await resultsApi.create(raceId, {
        competitorId: registration.competitor?.id,
        teamId: registration.team?.id,
        status: resultForm.status,
        startPosition: resultForm.startPosition ? Number(resultForm.startPosition) : null,
        finalPosition: resultForm.finalPosition ? Number(resultForm.finalPosition) : null,
        completionTimeSeconds: resultForm.completionTimeSeconds ? Number(resultForm.completionTimeSeconds) : null,
        penaltyTimeSeconds: resultForm.penaltyTimeSeconds ? Number(resultForm.penaltyTimeSeconds) : 0,
        notes: resultForm.notes,
      })
      setResultForm(EMPTY_RESULT)
      setShowResultForm(false)
      load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <Link to="/races" className="back-link">
        ← Volver a carreras
      </Link>

      <div className="page-header">
        <div>
          <h1>{race.name}</h1>
          <p className="page-lede">
            {formatDateTime(race.scheduledAt)} · {race.startLocation} → {race.endLocation} · {race.distanceMeters} m
          </p>
        </div>
        <StatusTag status={race.status} label={raceStatusLabels[race.status]} />
      </div>

      <ErrorBanner error={error} />

      <div className="panel">
        <h2>Detalles</h2>
        <p style={{ margin: 0 }}>{race.description || 'Sin descripción.'}</p>
        <p style={{ color: 'var(--text-muted)', fontSize: 13, marginTop: 10 }}>
          Tipo: {raceTypeLabels[race.type]} · Cupo máximo: {race.maxParticipants} · Cierre de inscripciones:{' '}
          {formatDateTime(race.registrationDeadline)}
        </p>
        {canManageRace && nextRaceStatuses(race.status).length > 0 && (
          <div className="form-actions" style={{ marginTop: 14 }}>
            <select value={nextStatus} onChange={(e) => setNextStatus(e.target.value)}>
              <option value="">Cambiar estado a…</option>
              {nextRaceStatuses(race.status).map((status) => (
                <option key={status} value={status}>
                  {raceStatusLabels[status]}
                </option>
              ))}
            </select>
            <button type="button" className="btn btn-small" disabled={!nextStatus} onClick={handleStatusChange}>
              Aplicar
            </button>
          </div>
        )}
      </div>

      <hr className="rule" />

      <div className="page-header">
        <h2 style={{ fontSize: 20 }}>Inscripciones</h2>
        {canManageRace && (
          <button type="button" className="btn btn-small" onClick={() => setShowRegForm((v) => !v)}>
            {showRegForm ? 'Cancelar' : 'Inscribir'}
          </button>
        )}
      </div>

      {showRegForm && (
        <form className="panel" onSubmit={handleCreateRegistration}>
          <div className="field-row">
            <div className="field" style={{ maxWidth: 180 }}>
              <label htmlFor="participantType">Tipo de inscripción</label>
              <select
                id="participantType"
                value={regForm.participantType}
                onChange={(e) => setRegForm({ ...regForm, participantType: e.target.value, participantId: '' })}
              >
                <option value="competitor">Competidor individual</option>
                <option value="team">Equipo</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="participantId">Participante</label>
              <select
                id="participantId"
                required
                value={regForm.participantId}
                onChange={(e) => setRegForm({ ...regForm, participantId: e.target.value })}
              >
                <option value="">Elegir…</option>
                {(regForm.participantType === 'competitor' ? competitors : teams).map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nickname ?? p.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="regNotes">Notas</label>
              <input id="regNotes" value={regForm.notes} onChange={(e) => setRegForm({ ...regForm, notes: e.target.value })} />
            </div>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-small">
              Guardar inscripción
            </button>
          </div>
        </form>
      )}

      {registrations.length === 0 ? (
        <p className="empty-state">Todavía no hay inscripciones para esta carrera.</p>
      ) : (
        <div className="table-wrap">
          <table className="sheet">
            <thead>
              <tr>
                <th>Participante</th>
                <th>Estado</th>
                <th className="num">Carril</th>
                <th>Notas</th>
                {canManageRace && <th>Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {registrations.map((r) => (
                <tr key={r.id}>
                  <td>{r.competitor ? r.competitor.nickname : r.team.name}</td>
                  <td>
                    <StatusTag status={r.status} label={registrationStatusLabels[r.status]} />
                  </td>
                  <td className="num">{r.lane ?? '—'}</td>
                  <td>{r.notes || '—'}</td>
                  {canManageRace && (
                    <td>
                      {r.status === 'PENDING' && (
                        <div className="form-actions">
                          <button type="button" className="btn btn-small" onClick={() => handleApprove(r.id)}>
                            Aprobar
                          </button>
                          <input
                            placeholder="Motivo de rechazo"
                            style={{ border: '1px solid var(--paper-line)', borderRadius: 3, padding: '4px 8px', fontSize: 13 }}
                            value={rejectNotes[r.id] ?? ''}
                            onChange={(e) => setRejectNotes({ ...rejectNotes, [r.id]: e.target.value })}
                          />
                          <button type="button" className="btn btn-outline btn-small" onClick={() => handleReject(r.id)}>
                            Rechazar
                          </button>
                        </div>
                      )}
                      {canCancelRegistration && r.status === 'APPROVED' && (
                        <button type="button" className="btn btn-danger btn-small" onClick={() => handleCancelRegistration(r.id)}>
                          Cancelar
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <hr className="rule" />

      <div className="page-header">
        <h2 style={{ fontSize: 20 }}>Resultados</h2>
        {canManageRace && (
          <button type="button" className="btn btn-small" onClick={() => setShowResultForm((v) => !v)}>
            {showResultForm ? 'Cancelar' : 'Registrar resultado'}
          </button>
        )}
      </div>

      {showResultForm && (
        <form className="panel" onSubmit={handleCreateResult}>
          <div className="field-row">
            <div className="field">
              <label htmlFor="registrationId">Participante inscrito (aprobado)</label>
              <select
                id="registrationId"
                required
                value={resultForm.registrationId}
                onChange={(e) => setResultForm({ ...resultForm, registrationId: e.target.value })}
              >
                <option value="">Elegir…</option>
                {approvedRegistrations.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.competitor ? r.competitor.nickname : r.team.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="resultStatus">Estado</label>
              <select
                id="resultStatus"
                value={resultForm.status}
                onChange={(e) => setResultForm({ ...resultForm, status: e.target.value })}
              >
                {Object.entries(resultStatusLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="startPosition">Posición de salida</label>
              <input
                id="startPosition"
                type="number"
                min="1"
                value={resultForm.startPosition}
                onChange={(e) => setResultForm({ ...resultForm, startPosition: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="finalPosition">Posición final</label>
              <input
                id="finalPosition"
                type="number"
                min="1"
                value={resultForm.finalPosition}
                onChange={(e) => setResultForm({ ...resultForm, finalPosition: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="completionTimeSeconds">Tiempo (segundos)</label>
              <input
                id="completionTimeSeconds"
                type="number"
                min="0"
                step="0.01"
                value={resultForm.completionTimeSeconds}
                onChange={(e) => setResultForm({ ...resultForm, completionTimeSeconds: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="penaltyTimeSeconds">Penalización (segundos)</label>
              <input
                id="penaltyTimeSeconds"
                type="number"
                min="0"
                step="0.01"
                value={resultForm.penaltyTimeSeconds}
                onChange={(e) => setResultForm({ ...resultForm, penaltyTimeSeconds: e.target.value })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="resultNotes">Notas</label>
              <input
                id="resultNotes"
                value={resultForm.notes}
                onChange={(e) => setResultForm({ ...resultForm, notes: e.target.value })}
              />
            </div>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-small">
              Guardar resultado
            </button>
          </div>
        </form>
      )}

      {results.length === 0 ? (
        <p className="empty-state">Todavía no hay resultados registrados para esta carrera.</p>
      ) : (
        <div className="table-wrap">
          <table className="sheet">
            <thead>
              <tr>
                <th>Participante</th>
                <th>Estado</th>
                <th className="num">Puesto</th>
                <th className="num">Tiempo total</th>
                <th className="num">Puntos</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => (
                <tr key={r.id}>
                  <td>{r.competitor ? r.competitor.nickname : r.team.name}</td>
                  <td>
                    <StatusTag status={r.status} label={resultStatusLabels[r.status]} />
                  </td>
                  <td className="num">{r.finalPosition ?? '—'}</td>
                  <td className="num">{formatSeconds(r.totalTimeSeconds)}</td>
                  <td className="num">{r.points}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
