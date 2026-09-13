import { keycloak } from './auth.js'

async function request(path, { method = 'GET', body, params } = {}) {
  const url = new URL(path, window.location.origin)
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, value)
      }
    })
  }

  const res = await fetch(url.pathname + url.search, {
    method,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${keycloak.token}`,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (res.status === 204) return null

  const isJson = res.headers.get('content-type')?.includes('application/json')
  const data = isJson ? await res.json() : null

  if (!res.ok) {
    const error = new Error(data?.message || `Error ${res.status}`)
    error.status = res.status
    error.validationErrors = data?.validationErrors ?? null
    throw error
  }

  return data
}

export const competitorsApi = {
  list: () => request('/api/competitors'),
  create: (data) => request('/api/competitors', { method: 'POST', body: data }),
  changeStatus: (id, status) => request(`/api/competitors/${id}/status`, { method: 'PATCH', body: status }),
  remove: (id) => request(`/api/competitors/${id}`, { method: 'DELETE' }),
}

export const teamsApi = {
  list: () => request('/api/teams'),
  create: (data) => request('/api/teams', { method: 'POST', body: data }),
  deactivate: (id) => request(`/api/teams/${id}`, { method: 'DELETE' }),
  addMember: (teamId, competitorId) =>
    request(`/api/teams/${teamId}/members/${competitorId}`, { method: 'POST' }),
  removeMember: (teamId, competitorId) =>
    request(`/api/teams/${teamId}/members/${competitorId}`, { method: 'DELETE' }),
}

export const racesApi = {
  list: (params) => request('/api/races', { params }),
  get: (id) => request(`/api/races/${id}`),
  create: (data) => request('/api/races', { method: 'POST', body: data }),
  updateStatus: (id, status) => request(`/api/races/${id}/status`, { method: 'PATCH', body: { status } }),
  cancel: (id) => request(`/api/races/${id}`, { method: 'DELETE' }),
}

export const registrationsApi = {
  listForRace: (raceId, params) => request(`/api/races/${raceId}/registrations`, { params }),
  create: (raceId, data) => request(`/api/races/${raceId}/registrations`, { method: 'POST', body: data }),
  approve: (id, data) => request(`/api/registrations/${id}/approve`, { method: 'PATCH', body: data }),
  reject: (id, notes) => request(`/api/registrations/${id}/reject`, { method: 'PATCH', body: { notes } }),
  cancel: (id) => request(`/api/registrations/${id}`, { method: 'DELETE' }),
}

export const resultsApi = {
  listForRace: (raceId, params) => request(`/api/races/${raceId}/results`, { params }),
  create: (raceId, data) => request(`/api/races/${raceId}/results`, { method: 'POST', body: data }),
}

export const standingsApi = {
  global: (params) => request('/api/standings', { params }),
  competitors: (params) => request('/api/standings/competitors', { params }),
  teams: (params) => request('/api/standings/teams', { params }),
}
