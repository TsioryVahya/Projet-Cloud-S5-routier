<template>
  <ion-page>
    <ion-tabs>
      <ion-router-outlet></ion-router-outlet>
      <ion-tab-bar slot="bottom" class="border-t-0 h-20 bg-transparent px-4 pb-4">
        <div class="flex w-full bg-white/90 backdrop-blur-xl rounded-[28px] shadow-2xl shadow-black/5 border border-white/50 overflow-hidden h-full items-center px-2">
          <ion-tab-button tab="map" href="/tabs/map" class="bg-transparent">
            <ion-icon :icon="mapOutline" />
            <span class="text-[10px] font-black uppercase tracking-tighter mt-1">Carte</span>
          </ion-tab-button>

          <ion-tab-button tab="list" href="/tabs/list" class="bg-transparent">
            <div class="relative">
              <ion-icon :icon="listOutline" />
              <ion-badge v-if="store.signalements.length > 0" color="primary" class="custom-badge-new">
                {{ store.signalements.length }}
              </ion-badge>
            </div>
            <span class="text-[10px] font-black uppercase tracking-tighter mt-1">Travaux</span>
          </ion-tab-button>

          <ion-tab-button tab="notifications" href="/tabs/notifications" class="bg-transparent">
            <div class="relative">
              <ion-icon :icon="notificationsOutline" />
              <ion-badge v-if="notificationService.unreadCount.value > 0" color="danger" class="custom-badge-new">
                {{ notificationService.unreadCount.value }}
              </ion-badge>
            </div>
            <span class="text-[10px] font-black uppercase tracking-tighter mt-1">Notifications</span>
          </ion-tab-button>
        </div>
      </ion-tab-bar>
    </ion-tabs>
  </ion-page>
</template>

<script setup lang="ts">
import { IonPage, IonTabs, IonRouterOutlet, IonTabBar, IonTabButton, IonIcon, IonLabel, IonBadge } from '@ionic/vue';
import { mapOutline, listOutline, notificationsOutline } from 'ionicons/icons';
import { store } from '../store';
import { notificationService } from '../services/notificationService';
</script>


<style scoped>
ion-tab-bar {
  --background: transparent;
  --border: none;
}

ion-tab-button {
  --color: #94a3b8; /* slate-400 */
  --color-selected: #2563eb; /* blue-600 */
  --background: transparent;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

ion-icon {
  font-size: 22px;
  margin-bottom: 2px;
}

.custom-badge-new {
  position: absolute;
  top: -8px;
  right: -12px;
  --padding-start: 4px;
  --padding-end: 4px;
  min-width: 16px;
  height: 16px;
  font-size: 9px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid white;
  border-radius: 50%;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}
</style>
