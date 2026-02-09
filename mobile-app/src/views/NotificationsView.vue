<template>
  <ion-page>
    <ion-header>
      <ion-toolbar class="bg-gradient-to-r from-blue-600 to-blue-700">
        <ion-title class="text-white font-bold">Notifications</ion-title>
        <ion-buttons slot="end" v-if="notificationService.unreadCount.value > 0">
          <ion-button @click="markAllAsRead" class="text-white">
            Tout marquer comme lu
          </ion-button>
        </ion-buttons>
      </ion-toolbar>
    </ion-header>

    <ion-content class="bg-slate-50">
      <!-- Filtres rapides -->
      <div class="px-4 py-2 flex gap-2 overflow-x-auto no-scrollbar bg-white border-b border-slate-100">
        <ion-chip @click="filterType = 'all'" :outline="filterType !== 'all'" color="primary" class="m-0">Tous</ion-chip>
        <ion-chip @click="filterType = 'unread'" :outline="filterType !== 'unread'" color="primary" class="m-0">Non lus</ion-chip>
      </div>

      <!-- État vide -->
      <div v-if="filteredNotifications.length === 0" 
           class="flex flex-col items-center justify-center h-full px-6 text-center">
        <div class="bg-slate-100 p-6 rounded-full mb-4">
          <ion-icon :icon="notificationsOffOutline" class="text-6xl text-slate-400"></ion-icon>
        </div>
        <h2 class="text-xl font-bold text-slate-800 mb-2">Aucune notification</h2>
        <p class="text-slate-500 max-w-[250px]">
          {{ filterType === 'unread' ? 'Vous n\'avez aucun message non lu.' : 'Vous serez notifié ici lorsque le statut de vos signalements changera.' }}
        </p>
      </div>

      <!-- Liste des notifications -->
      <ion-list v-else class="bg-transparent py-0">
        <ion-item-sliding v-for="notif in filteredNotifications" :key="notif.id">
          <ion-item 
            @click="handleNotificationClick(notif)"
            :class="['notification-item', { 'unread': !notif.lu }]"
            lines="none"
            button>
            <div class="w-full py-3 flex gap-4 items-start">
              <!-- Icône de gauche avec conteneur stylisé -->
              <div :class="['p-3 rounded-2xl flex items-center justify-center shrink-0 shadow-sm', getStatusBgColor(notif.newStatus)]">
                <ion-icon 
                  :icon="getStatusIcon(notif.newStatus)" 
                  class="text-2xl text-white">
                </ion-icon>
              </div>

              <div class="flex-1 min-w-0">
                <!-- En-tête -->
                <div class="flex items-center justify-between gap-2 mb-1">
                  <h3 :class="['font-bold truncate', !notif.lu ? 'text-slate-900' : 'text-slate-600']">
                    {{ notif.titre }}
                  </h3>
                  <span class="text-[10px] font-medium text-slate-400 whitespace-nowrap">
                    {{ formatDate(notif.dateCreation) }}
                  </span>
                </div>
                
                <!-- Corps -->
                <p :class="['text-sm leading-snug line-clamp-2 mb-2', !notif.lu ? 'text-slate-700 font-medium' : 'text-slate-500']">
                  {{ notif.message }}
                </p>
                
                <!-- Tags et Infos -->
                <div class="flex items-center gap-2">
                  <ion-badge v-if="notif.typeSignalement" class="rounded-lg bg-slate-100 text-slate-600 font-bold px-2 py-1 text-[10px]">
                    {{ notif.typeSignalement }}
                  </ion-badge>
                  <div class="flex items-center gap-1">
                    <div :class="['w-2 h-2 rounded-full', getStatusCircleColor(notif.newStatus)]"></div>
                    <span class="text-[10px] font-bold text-slate-400 uppercase tracking-wider">{{ notif.newStatus }}</span>
                  </div>
                </div>
              </div>

              <!-- Indicateur de non-lu -->
              <div v-if="!notif.lu" class="w-2.5 h-2.5 bg-blue-600 rounded-full mt-2 shrink-0"></div>
            </div>
          </ion-item>

          <!-- Actions de glissement -->
          <ion-item-options side="end">
            <ion-item-option 
              color="primary" 
              @click="markAsRead(notif.id)"
              v-if="!notif.lu"
              class="rounded-l-2xl">
              <div class="flex flex-col items-center gap-1">
                <ion-icon slot="icon-only" :icon="checkmarkOutline" class="text-2xl"></ion-icon>
                <span class="text-[10px]">Lu</span>
              </div>
            </ion-item-option>
          </ion-item-options>
        </ion-item-sliding>
      </ion-list>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage, IonHeader, IonToolbar, IonTitle, IonContent,
  IonList, IonItem, IonIcon, IonBadge, IonChip,
  IonButtons, IonButton, IonItemSliding, IonItemOptions, IonItemOption
} from '@ionic/vue';
import {
  notificationsOffOutline,
  checkmarkCircleOutline,
  timeOutline,
  closeCircleOutline,
  alertCircleOutline,
  checkmarkOutline
} from 'ionicons/icons';
import { notificationService, Notification } from '../services/notificationService';

const router = useRouter();
const filterType = ref('all');

const filteredNotifications = computed(() => {
  if (filterType.value === 'unread') {
    return notificationService.notifications.value.filter(n => !n.lu);
  }
  return notificationService.notifications.value;
});

onMounted(async () => {
  await notificationService.loadNotifications();
});

onUnmounted(() => {
  // Garder le listener actif pour recevoir les notifications
});

const handleNotificationClick = async (notif: Notification) => {
  // Marquer comme lue
  if (!notif.lu) {
    await notificationService.markAsRead(notif.id);
  }
  
  // Naviguer vers la liste des signalements
  router.push('/tabs/list');
};

const markAsRead = async (notificationId: string) => {
  await notificationService.markAsRead(notificationId);
};

const markAllAsRead = async () => {
  await notificationService.markAllAsRead();
};

const getStatusIcon = (status: string) => {
  if (!status) return alertCircleOutline;
  switch (status.toLowerCase()) {
    case 'validé':
    case 'résolu':
      return checkmarkCircleOutline;
    case 'en cours':
      return timeOutline;
    case 'nouveau':
      return alertCircleOutline;
    case 'rejeté':
      return closeCircleOutline;
    default:
      return alertCircleOutline;
  }
};

const getStatusBgColor = (status: string) => {
  if (!status) return 'bg-yellow-500';
  switch (status.toLowerCase()) {
    case 'validé':
    case 'résolu':
      return 'bg-green-500';
    case 'en cours':
      return 'bg-blue-500';
    case 'rejeté':
      return 'bg-red-500';
    default:
      return 'bg-yellow-500';
  }
};

const getStatusCircleColor = (status: string) => {
  if (!status) return 'bg-yellow-500';
  switch (status.toLowerCase()) {
    case 'validé':
    case 'résolu':
      return 'bg-green-500';
    case 'en cours':
      return 'bg-blue-500';
    case 'rejeté':
      return 'bg-red-500';
    default:
      return 'bg-yellow-500';
  }
};

const formatDate = (date: any) => {
  if (!date) return '';
  
  let dateObj: Date;
  if (date.toDate) {
    dateObj = date.toDate();
  } else if (date instanceof Date) {
    dateObj = date;
  } else {
    dateObj = new Date(date);
  }
  
  const now = new Date();
  const diff = now.getTime() - dateObj.getTime();
  
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  
  if (minutes < 1) return 'À l\'instant';
  if (minutes < 60) return `Il y a ${minutes} min`;
  if (hours < 24) return `Il y a ${hours}h`;
  if (days < 7) return `Il y a ${days}j`;
  
  return dateObj.toLocaleDateString('fr-FR', {
    day: 'numeric',
    month: 'short',
    year: dateObj.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
  });
};
</script>

<style scoped>
.notification-item {
  --background: white;
  margin-bottom: 8px;
  border-radius: 16px;
  overflow: hidden;
}

.notification-item.unread {
  --background: #f8faff;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

ion-list {
  padding: 16px;
}

ion-item-sliding {
  margin-bottom: 12px;
  background: transparent;
}
</style>
