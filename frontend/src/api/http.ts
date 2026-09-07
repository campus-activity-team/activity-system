import axios from 'axios'

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 10_000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('activity-system-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export interface HealthResponse {
  code: number
  message: string
  data: {
    status: string
    service: string
  }
}

export function getHealth() {
  return http.get<HealthResponse>('/health')
}
