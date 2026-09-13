import { useEffect, useState } from 'react'
import { competitorsApi } from '../api.js'
import { hasRole } from '../auth.js'
import ErrorBanner from '../components/ErrorBanner.jsx'
import StatusTag from '../components/StatusTag.jsx'
import { competitorStatusLabels, competitorTypeLabels } from '../format.js'

const EMPTY_FORM = {
  name: '',
  nickname: '',
  competitorType: 'DWARF',
  age: '',
  height: '',
  weight: '',
  placeOfOrigin: '',
}

export default function CompetitorsPage() {
  const [competitors, setCompetitors] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(EMPTY_FORM)
  const [saving, setSaving] = useState(false)

  const canManage = hasRole('ADMINISTRATOR')

  function load() {
    setLoading(true)
    competitorsApi
      .list()
      .then(setCompetitors)
      .catch(setError)
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function handleCreate(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await competitorsApi.create({
        ...form,
        age: Number(form.age),
        height: Number(form.height),
        weight: Number(form.weight),
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

  async function handleRetire(id) {
    setError(null)
    try {
      await competitorsApi.changeStatus(id, 'RETIRED')
      load()
    } catch (err) {
      setError(err)
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Este competidor ya está retirado. ¿Eliminarlo definitivamente?')) return
    setError(null)
    try {
      await competitorsApi.remove(id)
      load()
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Competidores</h1>
          <p className="page-lede">Camellos, enanos y demás participantes registrados en la liga.</p>
        </div>
        {canManage && (
          <button type="button" className="btn" onClick={() => setShowForm((v) => !v)}>
            {showForm ? 'Cancelar' : 'Nuevo competidor'}
          </button>
        )}
      </div>

      <ErrorBanner error={error} />

      {showForm && (
        <form className="panel" onSubmit={handleCreate}>
          <h2>Registrar competidor</h2>
          <div className="field-row">
            <div className="field">
              <label htmlFor="name">Nombre</label>
              <input
                id="name"
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="nickname">Apodo</label>
              <input
                id="nickname"
                required
                value={form.nickname}
                onChange={(e) => setForm({ ...form, nickname: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="competitorType">Tipo</label>
              <select
                id="competitorType"
                value={form.competitorType}
                onChange={(e) => setForm({ ...form, competitorType: e.target.value })}
              >
                {Object.entries(competitorTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="field-row">
            <div className="field">
              <label htmlFor="age">Edad</label>
              <input
                id="age"
                type="number"
                min="0"
                required
                value={form.age}
                onChange={(e) => setForm({ ...form, age: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="height">Altura (cm)</label>
              <input
                id="height"
                type="number"
                min="0"
                required
                value={form.height}
                onChange={(e) => setForm({ ...form, height: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="weight">Peso (kg)</label>
              <input
                id="weight"
                type="number"
                min="0"
                required
                value={form.weight}
                onChange={(e) => setForm({ ...form, weight: e.target.value })}
              />
            </div>
            <div className="field">
              <label htmlFor="placeOfOrigin">Lugar de origen</label>
              <input
                id="placeOfOrigin"
                required
                value={form.placeOfOrigin}
                onChange={(e) => setForm({ ...form, placeOfOrigin: e.target.value })}
              />
            </div>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar competidor'}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="empty-state">Cargando competidores…</p>
      ) : competitors.length === 0 ? (
        <p className="empty-state">Todavía no hay competidores registrados.</p>
      ) : (
        <div className="table-wrap">
          <table className="sheet">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Apodo</th>
                <th>Tipo</th>
                <th className="num">Edad</th>
                <th>Origen</th>
                <th>Estado</th>
                {canManage && <th>Acciones</th>}
              </tr>
            </thead>
            <tbody>
              {competitors.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td>{c.nickname}</td>
                  <td>{competitorTypeLabels[c.competitorType]}</td>
                  <td className="num">{c.age}</td>
                  <td>{c.placeOfOrigin}</td>
                  <td>
                    <StatusTag status={c.competitorStatus} label={competitorStatusLabels[c.competitorStatus]} />
                  </td>
                  {canManage && (
                    <td>
                      {c.competitorStatus !== 'RETIRED' ? (
                        <button type="button" className="btn btn-outline btn-small" onClick={() => handleRetire(c.id)}>
                          Retirar
                        </button>
                      ) : (
                        <button type="button" className="btn btn-danger btn-small" onClick={() => handleDelete(c.id)}>
                          Eliminar
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
    </>
  )
}
