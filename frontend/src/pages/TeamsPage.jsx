import { useEffect, useState } from 'react'
import { competitorsApi, teamsApi } from '../api.js'
import { hasRole } from '../auth.js'
import ErrorBanner from '../components/ErrorBanner.jsx'
import StatusTag from '../components/StatusTag.jsx'
import { teamStatusLabels } from '../format.js'

const EMPTY_FORM = { name: '', description: '', coach: '', maxMembers: '' }

export default function TeamsPage() {
  const [teams, setTeams] = useState([])
  const [competitors, setCompetitors] = useState([])
  const [selectedTeamId, setSelectedTeamId] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)
  const [memberToAdd, setMemberToAdd] = useState('')

  const canManage = hasRole('ADMINISTRATOR')

  function load() {
    setLoading(true)
    Promise.all([teamsApi.list(), competitorsApi.list()])
      .then(([teamList, competitorList]) => {
        setTeams(teamList)
        setCompetitors(competitorList)
      })
      .catch(setError)
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  const selectedTeam = teams.find((t) => t.id === selectedTeamId)
  const availableCompetitors = competitors.filter(
    (c) => c.competitorStatus === 'ACTIVE' && (c.memberships?.length ?? 0) === 0,
  )
  const competitorsById = Object.fromEntries(competitors.map((c) => [c.id, c]))

  async function handleCreate(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await teamsApi.create({ ...form, maxMembers: Number(form.maxMembers) })
      setForm(EMPTY_FORM)
      setShowForm(false)
      load()
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function handleDeactivate(id) {
    if (!window.confirm('¿Disolver este equipo?')) return
    setError(null)
    try {
      await teamsApi.deactivate(id)
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleAddMember(event) {
    event.preventDefault()
    if (!memberToAdd || !selectedTeamId) return
    setError(null)
    try {
      await teamsApi.addMember(selectedTeamId, memberToAdd)
      setMemberToAdd('')
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleRemoveMember(competitorId) {
    setError(null)
    try {
      await teamsApi.removeMember(selectedTeamId, competitorId)
      load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Equipos</h1>
          <p className="page-lede">Escuadras que compiten en carreras por equipos o mixtas.</p>
        </div>
        {canManage && (
          <button type="button" className="btn" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cancelar' : 'Nuevo equipo'}
          </button>
        )}
      </div>

      <ErrorBanner error={error} />

      {showForm && (
        <form className="panel" onSubmit={handleCreate}>
          <h2>Registrar equipo</h2>
          <div className="field-row">
            <div className="field">
              <label htmlFor="teamName">Nombre</label>
              <input
                id="teamName"
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="coach">Entrenador</label>
              <input
                id="coach"
                required
                value={form.coach}
                onChange={(e) => setForm({ ...form, coach: e.target.value })}
              />
            </div>
            <div className="field" style={{ maxWidth: 140 }}>
              <label htmlFor="maxMembers">Máx. miembros</label>
              <input
                id="maxMembers"
                type="number"
                min="1"
                required
                value={form.maxMembers}
                onChange={(e) => setForm({ ...form, maxMembers: e.target.value })}
              />
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="description">Descripción</label>
              <textarea
                id="description"
                required
                minLength={30}
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar equipo'}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="empty-state">Cargando equipos…</p>
      ) : teams.length === 0 ? (
        <p className="empty-state">Todavía no hay equipos registrados.</p>
      ) : (
        <div className="table-wrap">
          <table className="sheet">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Entrenador</th>
                <th className="num">Miembros</th>
                <th>Estado</th>
                {canManage && <th>Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {teams.map((t) => (
                <tr
                  key={t.id}
                  className="clickable"
                  onClick={() => setSelectedTeamId(t.id === selectedTeamId ? null : t.id)}
                >
                  <td>{t.name}</td>
                  <td>{t.coach}</td>
                  <td className="num">
                    {t.teamMembers.length} / {t.maxMembers}
                  </td>
                  <td>
                    <StatusTag status={t.status} label={teamStatusLabels[t.status]} />
                  </td>
                  {canManage && (
                    <td onClick={(e) => e.stopPropagation()}>
                      {t.status !== 'DISBANDED' && (
                        <button type="button" className="btn btn-danger btn-small" onClick={() => handleDeactivate(t.id)}>
                          Disolver
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

      {selectedTeam && (
        <div className="panel">
          <h2>Miembros de {selectedTeam.name}</h2>
          {selectedTeam.teamMembers.length === 0 ? (
            <p className="empty-state">Este equipo todavía no tiene miembros.</p>
          ) : (
            <div className="table-wrap">
              <table className="sheet">
                <thead>
                  <tr>
                    <th>Apodo</th>
                    <th>Tipo</th>
                    {canManage && <th>Acciones</th>}
                  </tr>
                </thead>
                <tbody>
                  {selectedTeam.teamMembers.map((m) => {
                    const competitor = competitorsById[m.competitor_id]
                    return (
                      <tr key={m.id}>
                        <td>{competitor?.nickname ?? m.competitor_id}</td>
                        <td>{competitor?.competitorType ?? '—'}</td>
                        {canManage && (
                          <td>
                            <button
                              type="button"
                              className="btn btn-outline btn-small"
                              onClick={() => handleRemoveMember(m.competitor_id)}
                            >
                              Quitar
                            </button>
                          </td>
                        )}
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}

          {canManage && (
            <form className="form-actions" style={{ marginTop: 16 }} onSubmit={handleAddMember}>
              <select value={memberToAdd} onChange={(e) => setMemberToAdd(e.target.value)}>
                <option value="">Elegir competidor activo sin equipo…</option>
                {availableCompetitors.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.nickname}
                  </option>
                ))}
              </select>
              <button type="submit" className="btn btn-small" disabled={!memberToAdd}>
                Agregar al equipo
              </button>
            </form>
          )}
        </div>
      )}
    </>
  )
}
