<template>
  <ion-page>
    <ion-tabs>
      <ion-router-outlet></ion-router-outlet>
      <ion-tab-bar slot="bottom" class="border-t border-slate-100 h-16 backdrop-blur-lg shadow-lg">
        <ion-tab-button tab="map" href="/tabs/map">
          <ion-icon :icon="mapOutline" />
          <ion-label class="hidden-label">Carte</ion-label>
        </ion-tab-button>

        <ion-tab-button tab="list" href="/tabs/list">
          <ion-icon :icon="listOutline" />
          <ion-badge v-if="store.signalements.length > 0" color="primary" class="custom-badge">
            {{ store.signalements.length }}
          </ion-badge>
          <ion-label class="hidden-label">Liste</ion-label>
        </ion-tab-button>

        <ion-tab-button tab="notifications" href="/tabs/notifications">
          <ion-icon :icon="notificationsOutline" />
          <ion-badge v-if="unreadCount > 0" color="danger" class="custom-badge">
            {{ unreadCount }}
          </ion-badge>
          <ion-label class="hidden-label">Notifications</ion-label>
        </ion-tab-button>

        <ion-tab-button tab="my-reports" href="/tabs/my-reports">
          <ion-icon :icon="personOutline" />
          <ion-label class="hidden-label">Moi</ion-label>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  </ion-page>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { IonPage, IonTabs, IonRouterOutlet, IonTabBar, IonTabButton, IonIcon, IonLabel, IonBadge } from '@ionic/vue';
import { mapOutline, listOutline, personOutline, notificationsOutline } from 'ionicons/icons';
import { store } from '../store';
import { notificationService } from '../services/notificationService';

// Utiliser computed pour éviter les erreurs de réactivité
const unreadCount = computed(() => notificationService.unreadCount.value);
</script>


<style scoped>
ion-tab-bar {
  --background: rgba(255, 255, 255, 0.8);
}

ion-tab-button {
  --color: #94a3b8; /* slate-400 */
  --color-selected: #2563eb; /* blue-600 */
  transition: all 0.2s ease;
}

ion-icon {
  font-size: 24px;
}

.hidden-label {
  display: none;
}

.custom-badge {
  position: absolute;
  top: 8px;
  right: calc(50% - 18px);
  --padding-start: 4px;
  --padding-end: 4px;
  min-width: 16px;
  height: 16px;
  font-size: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
  border-radius: 50%;
}
</style>
