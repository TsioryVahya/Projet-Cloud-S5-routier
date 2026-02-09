import { createRouter, createWebHistory } from '@ionic/vue-router';
import { RouteRecordRaw } from 'vue-router';
import TabsPage from '../views/TabsPage.vue'
import MapView from '../views/MapView.vue'
import ListView from '../views/ListView.vue'
import MyReportsView from '../views/MyReportsView.vue'
import NotificationsView from '../views/NotificationsView.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/tabs/map'
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
        path: 'my-reports',
        component: MyReportsView
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
