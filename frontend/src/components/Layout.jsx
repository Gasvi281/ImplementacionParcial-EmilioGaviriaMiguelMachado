import { NavLink } from 'react-router-dom'
import { getRoles, getUsername, logout } from '../auth.js'

const NAV_ITEMS = [
  { to: '/standings', label: 'Posiciones' },
  { to: '/competitors', label: 'Competidores' },
  { to: '/teams', label: 'Equipos' },
  { to: '/races', label: 'Carreras' },
]

const ROLE_LABELS = {
  ADMINISTRATOR: 'Administrador',
  RACE_ORGANIZER: 'Organizador',
  VIEWER: 'Espectador',
}

export default function Layout({ children }) {
  const role = getRoles()[0]

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          EIA Racing
          <span>Camellos vs. enanos</span>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div className="sidebar-user">{getUsername()}</div>
          <div className="sidebar-role">{ROLE_LABELS[role] ?? 'Sin rol asignado'}</div>
          <button type="button" className="link-button" onClick={logout}>
            Cerrar sesión
          </button>
        </div>
      </aside>
      <main className="content">{children}</main>
    </div>
  )
}
