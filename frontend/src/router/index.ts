import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },
    { path: '/activities', name: 'activities', component: () => import('../views/activity/ActivityListView.vue') },
    { path: '/activity/:id', name: 'activity-detail', component: () => import('../views/activity/ActivityDetailView.vue') },
    { path: '/checkin', name: 'checkin', component: () => import('../views/registration/CheckinView.vue') },
    { path: '/notifications', name: 'notifications', component: () => import('../views/notification/NotificationCenterView.vue'), meta: { roles: ['USER', 'ORGANIZER', 'ADMIN'] } },
    { path: '/my-registrations', name: 'my-registrations', component: () => import('../views/registration/MyRegistrationsView.vue'), meta: { roles: ['USER', 'ORGANIZER', 'ADMIN'] } },
    { path: '/organizer-application', name: 'organizer-application', component: () => import('../views/organizer/OrganizerApplicationView.vue'), meta: { roles: ['USER', 'ORGANIZER', 'ADMIN'] } },
    { path: '/organizer/activities', name: 'organizer-activities', component: () => import('../views/organizer/OrganizerActivitiesView.vue'), meta: { roles: ['ORGANIZER', 'ADMIN'] } },
    { path: '/admin/review', name: 'admin-review', component: () => import('../views/admin/AdminReviewView.vue'), meta: { roles: ['ADMIN'] } },
    { path: '/admin/users', name: 'admin-users', component: () => import('../views/admin/AdminUsersView.vue'), meta: { roles: ['ADMIN'] } },
    { path: '/admin/operation-logs', name: 'admin-operation-logs', component: () => import('../views/admin/AdminOperationLogsView.vue'), meta: { roles: ['ADMIN'] } },
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
