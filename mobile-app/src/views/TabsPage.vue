<template>
  <ion-page>
    <ion-tabs>
      <ion-router-outlet></ion-router-outlet>
      <ion-tab-bar slot="bottom" class="border-t border-slate-100 h-16  backdrop-blur-lg shadow-lg">
        <ion-tab-button tab="map" href="/tabs/map" class="text-slate-400 hover:text-slate-600 transition-colors">
          <ion-icon :icon="mapOutline" class="text-2xl" />
        </ion-tab-button>

        <ion-tab-button tab="list" href="/tabs/list" class="text-slate-400 hover:text-slate-600 transition-colors relative">
          <div class="relative">
            <ion-icon :icon="listOutline" class="text-2xl" />
            <div v-if="store.signalements.length > 0" class="absolute -top-1 -right-2 bg-blue-600 text-white text-[9px] font-bold w-4 h-4 rounded-full flex items-center justify-center border border-white shadow-sm">
              {{ store.signalements.length }}
            </div>
          </div>
        </ion-tab-button>
      </ion-tab-bar>
    </ion-tabs>
  </ion-page>
</template>

<script setup lang="ts">
import { IonPage, IonTabs, IonRouterOutlet, IonTabBar, IonTabButton, IonIcon } from '@ionic/vue';
import { mapOutline, listOutline } from 'ionicons/icons';
import { store } from '../store';
</script>

<style scoped>
ion-tab-button.tab-selected {
  color: #2563eb; /* text-blue-600 */
}
ion-tab-button.tab-selected ion-icon {
  transform: scale(1.1);
  transition: transform 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  filter: drop-shadow(0 4px 6px rgba(37, 99, 235, 0.2));
}
</style>
