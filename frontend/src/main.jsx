import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App.jsx'
import { keycloak, initKeycloak } from './auth.js'
import './styles.css'

const root = createRoot(document.getElementById('root'))

function renderBootError(message) {
  root.render(
    <div className="boot-error">
      <h1>No se pudo cargar el sistema</h1>
      <p>{message}</p>
    </div>,
  )
}

initKeycloak()
  .then((authenticated) => {
    if (!authenticated) {
      renderBootError('Keycloak no autenticó la sesión. Recarga la página para reintentar.')
      return
    }
    setInterval(() => {
      keycloak.updateToken(60).catch(() => keycloak.login())
    }, 30000)
    root.render(
      <BrowserRouter>
        <App />
      </BrowserRouter>,
    )
  })
  .catch(() => {
    renderBootError(
      `No se pudo conectar con Keycloak en ${keycloak.authServerUrl}. Verifica que el contenedor esté corriendo (docker compose up -d).`,
    )
  })
