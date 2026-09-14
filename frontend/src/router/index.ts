import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/activities', name: 'activities', component: () => import('../views/activity/ActivityListView.vue') },
    { path: '/activity/:id', name: 'activity-detail', component: () => import('../views/activity/ActivityDetailView.vue') },
    { path: '/my-registrations', name: 'my-registrations', component: () => import('../views/registration/MyRegistrationsView.vue'), meta: { roles: ['USER', 'ORGANIZER', 'ADMIN'] } },
    { path: '/organizer/activities', name: 'organizer-activities', component: () => import('../views/organizer/OrganizerActivitiesView.vue'), meta: { roles: ['ORGANIZER', 'ADMIN'] } },
    { path: '/admin/review', name: 'admin-review', component: () => import('../views/admin/AdminReviewView.vue'), meta: { roles: ['ADMIN'] } },
    { path: '/login', name: 'login', component: () => import('../views/auth/LoginView.vue'), meta: { guestOnly: true } },
    { path: '/register', name: 'register', component: () => import('../views/auth/RegisterView.vue'), meta: { guestOnly: true } },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.guestOnly && auth.isAuthenticated) return { name: 'home' }
  const roles = to.meta.roles as string[] | undefined
  if (roles && (!auth.isAuthenticated || !auth.user || !roles.includes(auth.user.role))) {
    return auth.isAuthenticated ? { name: 'home' } : { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
