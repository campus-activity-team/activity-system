import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, login as loginRequest, logout as logoutRequest, register as registerRequest } from '../api/auth'
import type { LoginPayload, RegisterPayload } from '../api/auth'
import type { UserProfile } from '../types/auth'

const tokenStorageKey = 'activity-system-token'
const userStorageKey = 'activity-system-user'

function readUser(): UserProfile | null {
  const stored = localStorage.getItem(userStorageKey)
  if (!stored) return null
  try {
    return JSON.parse(stored) as UserProfile
  } catch {
    localStorage.removeItem(userStorageKey)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(tokenStorageKey))
  const user = ref<UserProfile | null>(readUser())
  const isAuthenticated = computed(() => Boolean(token.value && user.value))

  function saveSession(nextToken: string, nextUser: UserProfile) {
    token.value = nextToken
    user.value = nextUser
    localStorage.setItem(tokenStorageKey, nextToken)
    localStorage.setItem(userStorageKey, JSON.stringify(nextUser))
  }

  function clearSession() {
    token.value = null
    user.value = null
    localStorage.removeItem(tokenStorageKey)
    localStorage.removeItem(userStorageKey)
  }

  async function login(payload: LoginPayload) {
    const response = await loginRequest(payload)
    saveSession(response.data.data.token, response.data.data.user)
  }

  async function register(payload: RegisterPayload) {
    const response = await registerRequest(payload)
    return response.data.data
  }

  async function loadCurrentUser() {
    if (!token.value) return
    try {
      const response = await getCurrentUser()
      user.value = response.data.data
      localStorage.setItem(userStorageKey, JSON.stringify(user.value))
    } catch {
      clearSession()
    }
  }

  async function logout() {
    try {
      if (token.value) await logoutRequest()
    } finally {
      clearSession()
    }
  }

  return { token, user, isAuthenticated, login, register, loadCurrentUser, logout, clearSession }
})
