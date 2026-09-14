import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { racesApi } from '../api.js'
import { hasRole } from '../auth.js'
import ErrorBanner from '../components/ErrorBanner.jsx'
import StatusTag from '../components/StatusTag.jsx'
import { formatDateTime, raceStatusLabels, raceTypeLabels } from '../format.js'

const EMPTY_FORM = {
  name: '',
  description: '',
  scheduledAt: '',
  registrationDeadline: '',
  startLocation: '',
  endLocation: '',
  distanceMeters: '',
  maxParticipants: '',
  type: 'INDIVIDUAL',
}

export default function RacesPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState({ content: [], number: 0, totalPages: 1 })
  const [filters, setFilters] = useState({ status: '', type: '', name: '' })
  const [pageIndex, setPageIndex] = useState(0)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const canCreate = hasRole('ADMINISTRATOR', 'RACE_ORGANIZER')
  const canCancel = hasRole('ADMINISTRATOR')

  function load() {
    setLoading(true)
    racesApi
      .list({ ...filters, page: pageIndex, size: 10 })
      .then(setPage)
      .catch(setError)
      .finally(() => setLoading(false))
  }

  useEffect(load, [filters, pageIndex])

  async function handleCreate(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await racesApi.create({
        ...form,
        distanceMeters: Number(form.distanceMeters),
        maxParticipants: Number(form.maxParticipants),
        scheduledAt: new Date(form.scheduledAt).toISOString(),
        registrationDeadline: new Date(form.registrationDeadline).toISOString(),
      })
      setForm(EMPTY_FORM)
      setShowForm(false)
      load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function handleCancel(id) {
    if (!window.confirm('¿Cancelar esta carrera?')) return
    setError(null)
    try {
      await racesApi.cancel(id)
      load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Carreras</h1>
          <p className="page-lede">Calendario de carreras de la liga, en cualquier estado.</p>
        </div>
        {canCreate && (
          <button type="button" className="btn" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cancelar' : 'Nueva carrera'}
          </button>
        )}
      </div>

      <ErrorBanner error={error} />

      {showForm && (
        <form className="panel" onSubmit={handleCreate}>
          <h2>Programar carrera</h2>
          <div className="field-row">
            <div className="field">
              <label htmlFor="raceName">Nombre</label>
              <input
                id="raceName"
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="type">Tipo</label>
              <select id="type" value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                {Object.entries(raceTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="scheduledAt">Fecha y hora</label>
              <input
                id="scheduledAt"
                type="datetime-local"
                required
                value={form.scheduledAt}
                onChange={(e) => setForm({ ...form, scheduledAt: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="registrationDeadline">Cierre de inscripciones</label>
              <input
                id="registrationDeadline"
                type="datetime-local"
                required
                value={form.registrationDeadline}
                onChange={(e) => setForm({ ...form, registrationDeadline: e.target.value })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="startLocation">Punto de partida</label>
              <input
                id="startLocation"
                required
                value={form.startLocation}
                onChange={(e) => setForm({ ...form, startLocation: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="endLocation">Punto de llegada</label>
              <input
                id="endLocation"
                required
                value={form.endLocation}
                onChange={(e) => setForm({ ...form, endLocation: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="distanceMeters">Distancia (m)</label>
              <input
                id="distanceMeters"
                type="number"
                min="1"
                required
                value={form.distanceMeters}
                onChange={(e) => setForm({ ...form, distanceMeters: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="maxParticipants">Máx. participantes</label>
              <input
                id="maxParticipants"
                type="number"
                min="1"
                required
                value={form.maxParticipants}
                onChange={(e) => setForm({ ...form, maxParticipants: e.target.value })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="description">Descripción</label>
              <textarea
                id="description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar carrera'}
            </button>
          </div>
        </form>
      )}

      <div className="field-row">
        <div className="field">
          <label htmlFor="filterStatus">Estado</label>
          <select
            id="filterStatus"
            value={filters.status}
            onChange={(e) => {
              setPageIndex(0)
              setFilters({ ...filters, status: e.target.value })
            }}
          >
            <option value="">Todos</option>
            {Object.entries(raceStatusLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filterType">Tipo</label>
          <select
            id="filterType"
            value={filters.type}
            onChange={(e) => {
              setPageIndex(0)
              setFilters({ ...filters, type: e.target.value })
            }}
          >
            <option value="">Todos</option>
            {Object.entries(raceTypeLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </div>
        <div className="field">
          <label htmlFor="filterName">Buscar por nombre</label>
          <input
            id="filterName"
            value={filters.name}
            onChange={(e) => {
              setPageIndex(0)
              setFilters({ ...filters, name: e.target.value })
            }}
          />
        </div>
      </div>

      {loading ? (
        <p className="empty-state">Cargando carreras…</p>
      ) : page.content.length === 0 ? (
        <p className="empty-state">No hay carreras que coincidan con estos filtros.</p>
      ) : (
        <>
          <div className="table-wrap">
            <table className="sheet">
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Estado</th>
                  <th className="num">Cupos</th>
                  {canCancel && <th>Acciones</th>}
                </tr>
              </thead>
              <tbody>
                {page.content.map((race) => (
                  <tr key={race.id} className="clickable" onClick={() => navigate(`/races/${race.id}`)}>
                    <td>{race.name}</td>
                    <td>{formatDateTime(race.scheduledAt)}</td>
                    <td>{raceTypeLabels[race.type]}</td>
                    <td>
                      <StatusTag status={race.status} label={raceStatusLabels[race.status]} />
                    </td>
                    <td className="num">{race.maxParticipants}</td>
                    {canCancel && (
                      <td onClick={(e) => e.stopPropagation()}>
                        {race.status !== 'COMPLETED' && race.status !== 'CANCELLED' && (
                          <button type="button" className="btn btn-danger btn-small" onClick={() => handleCancel(race.id)}>
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
          <div className="pagination">
            <button
              type="button"
              className="btn btn-outline btn-small"
              disabled={pageIndex === 0}
              onClick={() => setPageIndex((p) => p - 1)}
            >
              Anterior
            </button>
            <span>
              Página {page.number + 1} de {Math.max(page.totalPages, 1)}
            </span>
            <button
              type="button"
              className="btn btn-outline btn-small"
              disabled={pageIndex + 1 >= page.totalPages}
              onClick={() => setPageIndex((p) => p + 1)}
            >
              Siguiente
            </button>
          </div>
        </>
      )}
    </>
  )
}
