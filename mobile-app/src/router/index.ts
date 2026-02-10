import { createRouter, createWebHistory } from '@ionic/vue-router';
import { RouteRecordRaw } from 'vue-router';
import TabsPage from '../views/TabsPage.vue'
import MapView from '../views/MapView.vue'
import ListView from '../views/ListView.vue'
import NotificationsView from '../views/NotificationsView.vue'
import LoginView from '../views/LoginView.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/tabs/map'
  },
  {
    path: '/login',
    component: LoginView
  },
  {
    path: '/tabs',
    component: TabsPage,
    children: [
      {
        path: '',
        redirect: '/tabs/map'
      },
      {
        path: 'map',
        component: MapView
      },
      {
        path: 'list',
        component: ListView
      },
      {
        path: 'notifications',
        component: NotificationsView
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
