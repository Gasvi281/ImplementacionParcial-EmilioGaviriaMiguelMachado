import { useEffect, useState } from 'react'
import { standingsApi } from '../api.js'
import ErrorBanner from '../components/ErrorBanner.jsx'
import { competitorTypeLabels, formatSeconds } from '../format.js'

const TABS = [
  { key: 'global', label: 'General', fetcher: standingsApi.global },
  { key: 'competitors', label: 'Competidores', fetcher: standingsApi.competitors },
  { key: 'teams', label: 'Equipos', fetcher: standingsApi.teams },
]

export default function StandingsPage() {
  const [tab, setTab] = useState('global')
  const [rows, setRows] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const active = TABS.find((t) => t.key === tab)
    setLoading(true)
    setError(null)
    active
      .fetcher({ size: 50 })
      .then((page) => setRows(page.content))
      .catch(setError)
      .finally(() => setLoading(false))
  }, [tab])

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Posiciones</h1>
          <p className="page-lede">Calculadas a partir de los resultados oficiales registrados.</p>
        </div>
      </div>

      <div className="tabs">
        {TABS.map((t) => (
          <button key={t.key} type="button" className={tab === t.key ? 'active' : ''} onClick={() => setTab(t.key)}>
            {t.label}
          </button>
        ))}
      </div>

      <ErrorBanner error={error} />

      {loading ? (
        <p className="empty-state">Cargando posiciones…</p>
      ) : rows.length === 0 ? (
        <p className="empty-state">Todavía no hay resultados registrados para calcular posiciones.</p>
      ) : (
        <div className="table-wrap">
          <table className="sheet">
            <thead>
              <tr>
                <th className="num">#</th>
                <th>Nombre</th>
                <th>Tipo</th>
                <th className="num">Carreras</th>
                <th className="num">Victorias</th>
                <th className="num">Podios</th>
                <th className="num">Puntos</th>
                <th className="num">Mejor puesto</th>
                <th className="num">Tiempo total</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row) => {
                const participant = row.competitor ?? row.team
                return (
                  <tr key={`${row.rank}-${participant?.id}`}>
                    <td className="num">{row.rank}</td>
                    <td>{row.competitor ? row.competitor.nickname : row.team.name}</td>
                    <td>{row.competitor ? competitorTypeLabels[row.competitor.type] : 'Equipo'}</td>
                    <td className="num">{row.racesParticipated}</td>
                    <td className="num">{row.wins}</td>
                    <td className="num">{row.podiums}</td>
                    <td className="num">{row.totalPoints}</td>
                    <td className="num">{row.bestPosition ?? '—'}</td>
                    <td className="num">{formatSeconds(row.totalTimeSeconds)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
