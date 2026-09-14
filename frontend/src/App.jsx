import { Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout.jsx'
import StandingsPage from './pages/StandingsPage.jsx'
import CompetitorsPage from './pages/CompetitorsPage.jsx'
import TeamsPage from './pages/TeamsPage.jsx'
import RacesPage from './pages/RacesPage.jsx'
import RaceDetailPage from './pages/RaceDetailPage.jsx'

export default function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<Navigate to="/standings" replace />} />
        <Route path="/standings" element={<StandingsPage />} />
        <Route path="/competitors" element={<CompetitorsPage />} />
        <Route path="/teams" element={<TeamsPage />} />
        <Route path="/races" element={<RacesPage />} />
        <Route path="/races/:raceId" element={<RaceDetailPage />} />
        <Route path="*" element={<p>Esta página no existe.</p>} />
      </Routes>
    </Layout>
  )
}
