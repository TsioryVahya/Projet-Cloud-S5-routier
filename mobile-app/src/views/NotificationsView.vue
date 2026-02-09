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
      <!-- État vide -->
      <div v-if="notificationService.notifications.value.length === 0" 
           class="flex flex-col items-center justify-center h-full px-6 text-center">
        <ion-icon :icon="notificationsOffOutline" class="text-8xl text-slate-300 mb-4"></ion-icon>
        <h2 class="text-xl font-semibold text-slate-700 mb-2">Aucune notification</h2>
        <p class="text-slate-500">
          Vous serez notifié ici lorsque le statut de vos signalements changera
        </p>
      </div>

      <!-- Liste des notifications -->
      <ion-list v-else class="bg-transparent">
        <ion-item-sliding v-for="notif in notificationService.notifications.value" :key="notif.id">
          <ion-item 
            @click="handleNotificationClick(notif)"
            :class="['notification-item', { 'unread': !notif.lu }]"
            button>
            <div class="w-full py-2">
              <!-- En-tête de la notification -->
              <div class="flex items-start justify-between mb-1">
                <div class="flex items-center gap-2 flex-1">
                  <ion-icon 
                    :icon="getStatusIcon(notif.newStatus)" 
                    :class="['text-2xl', getStatusColor(notif.newStatus)]">
                  </ion-icon>
                  <div class="flex-1">
                    <h3 class="font-semibold text-slate-800 text-sm">{{ notif.titre }}</h3>
                  </div>
                  <ion-badge v-if="!notif.lu" color="primary" class="notification-badge">
                    Nouveau
                  </ion-badge>
                </div>
              </div>
              
              <!-- Corps de la notification -->
              <p class="text-sm text-slate-600 ml-8 mb-2">{{ notif.message }}</p>
              
              <!-- Pied de page -->
              <div class="flex items-center justify-between ml-8">
                <span class="text-xs text-slate-400">
                  {{ formatDate(notif.dateCreation) }}
                </span>
                <ion-chip 
                  outline 
                  :color="getStatusColorName(notif.newStatus)"
                  class="text-xs">
                  {{ notif.newStatus }}
                </ion-chip>
              </div>
            </div>
          </ion-item>

          <!-- Actions de glissement -->
          <ion-item-options side="end">
            <ion-item-option 
              color="primary" 
              @click="markAsRead(notif.id)"
              v-if="!notif.lu">
              <ion-icon slot="icon-only" :icon="checkmarkOutline"></ion-icon>
            </ion-item-option>
          </ion-item-options>
        </ion-item-sliding>
      </ion-list>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue';
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
    case 'en cours':
    case 'résolu':
      return checkmarkCircleOutline;
    case 'nouveau':
      return alertCircleOutline;
    case 'rejeté':
      return closeCircleOutline;
    default:
      return alertCircleOutline;
  }
};

const getStatusColor = (status: string) => {
  if (!status) return 'text-yellow-500';
  switch (status.toLowerCase()) {
    case 'validé':
    case 'résolu':
      return 'text-green-500';
    case 'en cours':
      return 'text-blue-500';
    case 'rejeté':
      return 'text-red-500';
    default:
      return 'text-yellow-500';
  }
};

const getStatusColorName = (status: string) => {
  if (!status) return 'warning';
  switch (status.toLowerCase()) {
    case 'validé':
    case 'résolu':
      return 'success';
    case 'en cours':
      return 'primary';
    case 'rejeté':
      return 'danger';
    default:
      return 'warning';
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
  --border-width: 0 0 1px 0;
  --border-color: #e2e8f0;
  margin-bottom: 0;
}

.notification-item.unread {
  --background: #eff6ff;
  border-left: 4px solid #2563eb;
}

.notification-badge {
  font-size: 10px;
  padding: 2px 8px;
  height: 20px;
}

ion-list {
  padding: 0;
}

ion-item-sliding {
  margin-bottom: 0;
}
</style>
