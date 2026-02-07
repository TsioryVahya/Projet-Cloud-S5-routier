<template>
  <ion-page>
    <!-- Overlay de Connexion (Modale) -->
    <div v-if="showLoginModal" class="absolute inset-0 z-[100] bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6">
      <div class="w-full max-w-md bg-white rounded-3xl shadow-2xl p-8 border border-slate-100 relative animate-in zoom-in duration-200">
        <button @click="showLoginModal = false" class="absolute top-4 right-4 w-8 h-8 flex items-center justify-center bg-slate-100 rounded-full text-slate-400 hover:text-slate-600">
          ✕
        </button>

        <div class="text-center mb-8">
          <div class="w-20 h-20 bg-blue-600 rounded-2xl shadow-xl shadow-blue-200 flex items-center justify-center mx-auto mb-4">
            <ion-icon :icon="trailSignOutline" class="text-4xl text-white" />
          </div>
          <h2 class="text-2xl font-bold text-slate-800">Se connecter</h2>
          <p class="text-slate-500 text-sm mt-1">Accès réservé aux agents</p>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5 ml-1">Email</label>
            <input v-model="loginEmail" type="email" placeholder="votre@email.com" class="w-full px-5 py-4 bg-slate-50 border border-slate-100 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all text-slate-700">
          </div>
          <div>
            <label class="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5 ml-1">Mot de passe</label>
            <input v-model="loginPassword" type="password" placeholder="••••••••" class="w-full px-5 py-4 bg-slate-50 border border-slate-100 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all text-slate-700">
          </div>
          
          <p v-if="authError" class="text-red-500 text-xs font-medium bg-red-50 p-3 rounded-xl border border-red-100">
            {{ authError }}
          </p>

          <button @click="handleLogin" :disabled="isAuthLoading" class="w-full py-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white rounded-2xl font-bold shadow-lg shadow-blue-500/30 transition-all transform active:scale-[0.98] flex items-center justify-center">
            <span v-if="isAuthLoading" class="animate-spin mr-2">
              <ion-icon :icon="constructOutline" />
            </span>
            {{ isAuthLoading ? 'Connexion...' : 'Se connecter' }}
          </button>
        </div>
        
        <p class="text-center text-[10px] text-slate-400 mt-8 leading-relaxed uppercase tracking-widest font-bold">
          L'inscription se fait uniquement via le Manager sur l'application Web
        </p>
      </div>
    </div>

    <ion-content :fullscreen="true" class="ion-no-padding">
      <!-- Carte en plein écran -->
      <div id="map" class="absolute inset-0 z-0 h-full w-full"></div>

      <!-- Overlay Gradient pour la lisibilité -->
      <div class="absolute inset-0 pointer-events-none bg-gradient-to-b from-black/10 via-transparent to-black/30 z-10"></div>

      <!-- Header Flottant Moderne -->
      <div class="absolute top-0 left-0 right-0 p-4 z-20 flex flex-col gap-3">
        <div class="bg-white/20 backdrop-blur-md rounded-2xl shadow-xl border border-white/20 p-4 flex items-center justify-between">
          <div>
            <h1 class="text-xl font-bold text-slate-800 tracking-tight">Signalement Routier</h1>
            <p v-if="store.user" class="text-[10px] font-bold text-blue-600 uppercase tracking-widest">{{ store.user.email }}</p>
            <p v-else class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Mode Visiteur</p>
          </div>
          <div class="flex gap-2">
            <button v-if="!store.user" @click="showLoginModal = true" class="bg-blue-600 px-4 py-2 rounded-xl text-white text-xs font-bold shadow-lg shadow-blue-500/20 active:scale-95 transition-all">
              Se connecter
            </button>
            <button v-else @click="handleLogout" class="bg-red-50 p-2.5 rounded-xl text-red-500 hover:bg-red-100 transition-colors active:scale-95 border border-red-100 shadow-sm">
              <ion-icon :icon="logOutOutline" class="text-xl" />
            </button>
          </div>
        </div>
        
        <!-- Filtres et Actions -->
        <div class="flex gap-2 overflow-x-auto no-scrollbar pb-2">
          <button 
            v-if="store.user"
            @click="filterMine = !filterMine"
            :class="filterMine ? 'bg-blue-600 text-white shadow-blue-500/30' : 'bg-white/30 text-slate-700 border-white/20'"
            class="backdrop-blur-md px-4 py-2 rounded-full shadow-lg border whitespace-nowrap flex items-center gap-2 font-bold text-sm transition-all"
          >
            <ion-icon :icon="personOutline" /> Mes signalements
          </button>
          <div class="bg-white/30 backdrop-blur-md px-4 py-2 rounded-full shadow-lg border border-white/20 whitespace-nowrap flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></span>
            <span class="text-sm font-semibold text-slate-700">{{ filteredSignalements.length }} Signalements</span>
          </div>
        </div>
      </div>


      <!-- Bouton GPS -->
      <div class="absolute bottom-24 right-4 z-20">
        <button 
          @click="locateUser" 
          :class="isLocating ? 'bg-blue-600 text-white' : 'bg-white text-blue-600'"
          class="w-11 h-11 rounded-xl shadow-2xl flex items-center justify-center active:scale-95 transition-all border border-slate-100"
        >
          <ion-icon :icon="locateOutline" class="text-xl" />
        </button>
      </div>

      <!-- Modal de Signalement Rapide -->
      <div v-if="newSignalementPoint" class="absolute inset-0 z-50 flex items-center justify-center p-6 bg-black/40 backdrop-blur-sm">
        <div class="w-full max-w-sm bg-white rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in duration-200">
          <div class="bg-blue-600 p-6 text-white text-center">
            <div class="text-4xl mb-2 flex justify-center">
              <ion-icon :icon="constructOutline" />
            </div>
            <h3 class="text-xl font-bold">Signaler un problème</h3>
            <p class="text-blue-100 text-xs">Aidez-nous à améliorer les routes</p>
          </div>
          <div class="p-6 space-y-4">
            <div>
              <label class="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-2 ml-1">Description</label>
              <textarea v-model="reportDescription" rows="3" placeholder="Décrivez le problème..." class="w-full px-5 py-4 bg-slate-50 border border-slate-100 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all text-slate-700 resize-none"></textarea>
            </div>

            <!-- Sélection du type de signalement -->
            <div>
              <label class="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 ml-1">Type de problème</label>
              <div class="relative group">
                <!-- Boutons de navigation -->
                <button 
                  @click="scrollCarousel('left')"
                  class="absolute left-0 top-1/2 -translate-y-1/2 z-10 bg-white/80 backdrop-blur-sm p-1 rounded-full shadow-md text-slate-600 opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <ion-icon :icon="chevronBackOutline" />
                </button>
                
                <div 
                  ref="carouselRef"
                  class="flex gap-3 overflow-x-auto no-scrollbar scroll-smooth pb-2 px-1"
                >
                  <div 
                    v-for="type in typesSignalement" 
                    :key="type.id"
                    @click="selectedTypeId = type.id"
                    :class="[
                      'flex-shrink-0 w-28 h-28 rounded-2xl p-3 flex flex-col items-center justify-between cursor-pointer transition-all border-2',
                      selectedTypeId === type.id ? 'border-blue-500 scale-95 shadow-inner' : 'border-transparent bg-slate-50'
                    ]"
                  >
                    <div 
                      class="w-12 h-12 rounded-xl flex items-center justify-center shadow-sm"
                      :style="{ backgroundColor: type.couleur + '20' }"
                    >
                      <svg viewBox="0 0 24 24" class="w-7 h-7" :style="{ fill: type.couleur }">
                        <path :d="type.icone_path" />
                      </svg>
                    </div>
                    <span class="text-[10px] font-bold text-center leading-tight text-slate-700 uppercase tracking-tighter line-clamp-2">
                      {{ type.nom }}
                    </span>
                  </div>
                </div>

                <button 
                  @click="scrollCarousel('right')"
                  class="absolute right-0 top-1/2 -translate-y-1/2 z-10 bg-white/80 backdrop-blur-sm p-1 rounded-full shadow-md text-slate-600 opacity-0 group-hover:opacity-100 transition-opacity"
                >
                  <ion-icon :icon="chevronForwardOutline" />
                </button>
              </div>
            </div>

            <div>
              <label class="block text-xs font-bold text-slate-400 uppercase mb-2 ml-1">Photo</label>
              <div v-if="!reportPhotoUrl" @click="takePhoto" class="w-full h-32 bg-slate-50 border-2 border-dashed border-slate-200 rounded-xl flex flex-col items-center justify-center text-slate-400 gap-2 cursor-pointer hover:bg-slate-100 hover:border-blue-200 transition-colors">
                <ion-icon :icon="cameraOutline" class="text-3xl" />
                <span class="text-xs font-bold">Prendre une photo</span>
              </div>
              <div v-else class="relative w-full h-48 rounded-xl overflow-hidden group">
                <img :src="reportPhotoUrl" class="w-full h-full object-cover">
                <div class="absolute inset-0 bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                   <button @click="removePhoto" class="bg-red-500 text-white p-3 rounded-full shadow-lg transform hover:scale-110 transition-transform">
                     <ion-icon :icon="trashOutline" class="text-xl" />
                   </button>
                </div>
              </div>
            </div>
            <div class="flex gap-3">
              <button @click="newSignalementPoint = null" class="flex-1 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl active:scale-95 transition-all">Annuler</button>
              <button @click="submitReport" :disabled="isSubmitting" class="flex-1 py-3 bg-blue-600 text-white font-bold rounded-xl shadow-lg shadow-blue-500/20 active:scale-95 transition-all flex items-center justify-center">
                <span v-if="isSubmitting" class="animate-spin mr-2">
                  <ion-icon :icon="sendOutline" />
                </span>
                {{ isSubmitting ? 'Envoi...' : 'Envoyer' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, onUnmounted } from 'vue';
import { IonPage, IonContent, IonIcon } from '@ionic/vue';

interface TypeSignalement {
  id: number;
  nom: string;
  description: string;
  icone_path: string;
  couleur: string;
}

import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';
import { 
  logOutOutline, 
  personOutline, 
  constructOutline, 
  closeOutline, 
  sendOutline, 
  trailSignOutline,
  eyeOutline,
  locationOutline,
  locateOutline,
  chevronBackOutline,
  chevronForwardOutline,
  cameraOutline,
  trashOutline,
  notificationsOutline, 
  checkmarkCircleOutline,
  alertCircleOutline
} from 'ionicons/icons';
import * as L from 'leaflet';
import { 
  collection, 
  addDoc, 
  serverTimestamp, 
  query, 
  where, 
  getDocs,
  doc,
  getDoc,
  updateDoc,
  orderBy,
  onSnapshot
} from 'firebase/firestore';
import { getToken, getMessaging, onMessage } from 'firebase/messaging';
import { PushNotifications } from '@capacitor/push-notifications';
import { Haptics } from '@capacitor/haptics';
import { Capacitor } from '@capacitor/core';
import { db } from '../firebase/config';
import { store, setUser } from '../store';
import { toastController } from '@ionic/vue';

// UI State
const showLoginModal = ref(false);
const filterMine = ref(false);

// Auth State (Local for login form)
const loginEmail = ref('');
const loginPassword = ref('');
const authError = ref('');
const isAuthLoading = ref(false);

// Listener en temps réel sur les notifications
let unsubscribeNotifications: (() => void) | null = null;

const showModernToast = async (title: string, body: string, type: 'info' | 'success' | 'warning' = 'info') => {
  const icon = type === 'success' ? checkmarkCircleOutline : (type === 'warning' ? alertCircleOutline : notificationsOutline);
  const color = type === 'success' ? 'success' : (type === 'warning' ? 'warning' : 'primary');

  const toast = await toastController.create({
    header: title,
    message: body,
    duration: 5000,
    position: 'top',
    animated: true,
    icon: icon,
    color: color,
    cssClass: 'modern-notification-toast',
    buttons: [
      {
        text: 'OK',
        role: 'cancel'
      }
    ]
  });
  await toast.present();
};

// FCM Logic
const requestFcmToken = async (userDocRef: any) => {
  console.log('Début requestFcmToken...');
  
  // Vérifier si on est sur mobile (Android/iOS) ou Web
  const isNative = Capacitor.isNativePlatform();
  console.log('Plateforme native:', isNative);

  if (isNative) {
    try {
      // 1. Demander la permission (Native)
      let permStatus = await PushNotifications.checkPermissions();
      if (permStatus.receive === 'prompt') {
        permStatus = await PushNotifications.requestPermissions();
      }

      if (permStatus.receive !== 'granted') {
        console.warn('Permission push native refusée');
        await updateDoc(userDocRef, {
          fcmTokenStatus: 'native_permission_denied',
          fcmTokenDate: new Date().toISOString()
        });
        return;
      }

      // 2. S'enregistrer pour recevoir les notifications
      await PushNotifications.register();

      // 3. Écouter le succès de l'enregistrement pour obtenir le token
      PushNotifications.addListener('registration', async (token) => {
        console.log('Native FCM Token reçu:', token.value);
        await updateDoc(userDocRef, {
          fcmToken: token.value,
          fcmTokenStatus: 'success_native',
          fcmTokenDate: new Date().toISOString(),
          fcmTokenError: null
        });
        
        if (store.user) {
          store.user.fcmToken = token.value;
          localStorage.setItem('app_user', JSON.stringify(store.user));
        }
      });

      // 4. Écouter les erreurs
      PushNotifications.addListener('registrationError', async (error) => {
        console.error('Erreur registration native:', error);
        await updateDoc(userDocRef, {
          fcmTokenStatus: 'error_native',
          fcmTokenError: error.error,
          fcmTokenDate: new Date().toISOString()
        });
      });

      // 5. Écouter les notifications reçues (Optionnel ici car géré par le plugin)
      PushNotifications.addListener('pushNotificationReceived', (notification) => {
        console.log('Push reçue (native):', notification);
        
        if (!store.user) return;

        showModernToast(
          notification.title || 'Mise à jour',
          notification.body || 'Un changement a été détecté',
          'info'
        );
      });

      // 6. Écouter les clics sur les notifications
      PushNotifications.addListener('pushNotificationActionPerformed', (notification) => {
        console.log('📱 Notification cliquée:', notification);
        // Vous pouvez naviguer vers une page spécifique ici
      });

    } catch (err: any) {
      console.error('Erreur lors de la config push native:', err);
    }
    return; // Fin pour le mode natif
  }

  // LOGIQUE WEB (Fallback si pas natif)
  try {
    // 1. Obtenir l'instance de messaging (qui s'assure que le SW est prêt)
    // IMPORTANT: On utilise directement getMessaging de firebase/messaging ici
    // car on a besoin d'une instance fraîche
    const messaging = await getMessaging();
    
    if (!messaging) {
      console.error('Messaging non disponible');
      await updateDoc(userDocRef, {
        fcmTokenStatus: 'messaging_unavailable',
        fcmTokenDate: new Date().toISOString()
      });
      return;
    }
    
    // 2. Vérifier la permission
    const permission = await Notification.requestPermission();
    console.log('Permission notification:', permission);
    
    if (permission !== 'granted') {
      await updateDoc(userDocRef, {
        fcmTokenStatus: 'permission_denied',
        fcmTokenDate: new Date().toISOString()
      });
      return;
    }

    // 3. S'assurer que le Service Worker est prêt
    console.log('Attente du Service Worker...');
    const registration = await navigator.serviceWorker.ready;

    // 4. Récupérer le token avec une seule méthode propre
    console.log('Récupération du token FCM...');
    const currentToken = await getToken(messaging, {
      vapidKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI',
      serviceWorkerRegistration: registration
    });
    
    if (currentToken) {
      console.log('FCM Token reçu:', currentToken);
      await updateDoc(userDocRef, {
        fcmToken: currentToken,
        fcmTokenStatus: 'success',
        fcmTokenDate: new Date().toISOString(),
        fcmTokenError: null
      });
      
      if (store.user) {
        store.user.fcmToken = currentToken;
        localStorage.setItem('app_user', JSON.stringify(store.user));
      }
    } else {
      throw new Error('Aucun token renvoyé par Firebase');
    }
  } catch (err: any) {
    console.error('Erreur lors de la récupération du token FCM:', err);
    await updateDoc(userDocRef, {
      fcmTokenStatus: 'error', 
      fcmTokenError: err.message,
      fcmTokenDate: new Date().toISOString()
    });
    
    // Si c'est une erreur de service de push, c'est souvent le navigateur ou le réseau
    if (err.message.includes('push service error')) {
      console.warn("Le service de push du navigateur a échoué. Essayez de redémarrer le navigateur ou vérifiez votre connexion internet.");
    }
  }
};

// Listener en temps réel sur les notifications
const setupNotificationListener = async (userEmail: string) => {
  try {
    console.log('🔔 Configuration du listener temps réel pour:', userEmail);
    
    // Désactiver l'ancien listener s'il existe
    if (unsubscribeNotifications) {
      unsubscribeNotifications();
    }
    
    const notificationsRef = collection(db, 'notifications');
    const q = query(
      notificationsRef,
      where('userEmail', '==', userEmail),
      where('lue', '==', false)
      // orderBy('dateCreation', 'desc') -- Temporairement désactivé en attendant l'index
    );
    
    // Écouter les nouvelles notifications en temps réel
    unsubscribeNotifications = onSnapshot(q, async (snapshot) => {
      console.log('📬 Snapshot reçu:', snapshot.size, 'notification(s) non lue(s)', '| User connecté:', !!store.user);
      
      // Vérifier d'abord que l'utilisateur est toujours connecté
      if (!store.user) {
        console.log('⚠️ Snapshot reçu mais utilisateur déconnecté - Ne rien faire');
        return;
      }
      
      // Ne traiter que les nouvelles notifications (ajouts)
      snapshot.docChanges().forEach(async (change) => {
        if (change.type === 'added') {
          const notif = change.doc.data();
          const notifId = change.doc.id;
          
          // Vérifier si c'est une notification vraiment nouvelle (créée il y a moins de 10 secondes)
          const dateCreation = new Date(notif.dateCreation);
          const now = new Date();
          const diffSeconds = (now.getTime() - dateCreation.getTime()) / 1000;
          
          // Ne traiter que les notifications récentes pour éviter d'afficher les anciennes au chargement
          if (diffSeconds > 10) {
            console.log('⏭️ Notification trop ancienne, ignorée:', diffSeconds, 'secondes');
            return;
          }
          
          console.log('📩 Nouvelle notification détectée:', notif);
          
          // Vérifier que l'utilisateur est toujours connecté
          if (!store.user) {
            console.log('⏭️ Utilisateur déconnecté, notification ignorée');
            return;
          }
          
          // Afficher immédiatement dans un toast
          try {
            console.log('🎨 Création du toast en temps réel pour:', notif.titre);
            
            // Déterminer l'icône et la couleur selon le statut
            let icon = '🔔';
            let color = 'primary';
            const message = notif.message.toLowerCase();
            
            if (message.includes('approuvé')) {
              icon = '✅';
              color = 'success';
            } else if (message.includes('rejeté')) {
              icon = '❌';
              color = 'danger';
            } else if (message.includes('en cours')) {
              icon = '🔧';
              color = 'warning';
            } else if (message.includes('résolu')) {
              icon = '🎉';
              color = 'success';
            }
            
            const toast = await toastController.create({
              header: notif.titre,
              message: `${icon} ${notif.message}`,
              duration: 7000,
              position: 'top',
              color: color,
              cssClass: 'notification-toast',
              buttons: [
                {
                  text: 'Voir',
                  role: 'info',
                  handler: () => {
                    console.log('👉 Navigation vers signalement:', notif.signalementId);
                    // TODO: Naviguer vers le détail du signalement
                  }
                },
                {
                  text: 'Fermer',
                  role: 'cancel'
                }
              ]
            });
            
            // Vibration sur mobile pour attirer l'attention
            if (Capacitor.isNativePlatform()) {
              try {
                await Haptics.vibrate({ duration: 200 });
              } catch (e) {
                console.log('⚠️ Vibration non disponible');
              }
            }
            
            console.log('🎨 Toast créé, présentation...');
            await toast.present();
            console.log('✅ Toast affiché en temps réel');
          } catch (err) {
            console.error('❌ Erreur affichage toast temps réel:', err);
          }
          
          // Marquer comme lue après un court délai
          setTimeout(async () => {
            try {
              await updateDoc(doc(db, 'notifications', notifId), {
                lue: true,
                dateLecture: new Date().toISOString()
              });
              console.log('✅ Notification marquée comme lue:', notifId);
            } catch (err) {
              console.error('❌ Erreur marquage notification:', err);
            }
          }, 1000);
        }
      });
    }, (error) => {
      console.error('❌ Erreur listener notifications:', error);
    });
    
    console.log('✅ Listener notifications activé');
  } catch (error) {
    console.error('❌ Erreur setup listener:', error);
  }
};

// Vérifier les notifications non lues au moment de la connexion
const checkUnreadNotifications = async (userEmail: string) => {
  try {
    console.log('🔔 Vérification des notifications non lues pour:', userEmail);
    
    // Récupérer les notifications non lues de l'utilisateur
    const notificationsRef = collection(db, 'notifications');
    const q = query(
      notificationsRef,
      where('userEmail', '==', userEmail),
      where('lue', '==', false)
      // orderBy('dateCreation', 'desc') -- Temporairement désactivé en attendant l'index
    );
    
    const snapshot = await getDocs(q);
    console.log(`📬 ${snapshot.size} notification(s) non lue(s) trouvée(s)`);
    
    if (snapshot.empty) {
      return;
    }
    
    // Trier les notifications par date (plus récente en premier) côté client
    const notifications = snapshot.docs
      .map(doc => ({ id: doc.id, ...doc.data() }))
      .sort((a: any, b: any) => {
        const dateA = new Date(a.dateCreation).getTime();
        const dateB = new Date(b.dateCreation).getTime();
        return dateB - dateA; // Plus récente en premier
      });
    
    // Afficher chaque notification et la marquer comme lue
    let delayMs = 0;
    for (const notif of notifications) {
      const notifData = notif as any; // Cast pour TypeScript
      console.log('📩 Affichage notification:', notifData);
      
      // Afficher un toast pour chaque notification avec un délai progressif
      setTimeout(async () => {
        try {
          console.log('🎨 Création du toast pour:', notifData.titre);
          
          // Déterminer l'icône et la couleur selon le statut
          let icon = '🔔';
          let color = 'primary';
          const message = notifData.message.toLowerCase();
          
          if (message.includes('approuvé')) {
            icon = '✅';
            color = 'success';
          } else if (message.includes('rejeté')) {
            icon = '❌';
            color = 'danger';
          } else if (message.includes('en cours')) {
            icon = '🔧';
            color = 'warning';
          } else if (message.includes('résolu')) {
            icon = '🎉';
            color = 'success';
          }
          
          const toast = await toastController.create({
            header: notifData.titre,
            message: `${icon} ${notifData.message}`,
            duration: 5000,
            position: 'top',
            color: color,
            cssClass: 'notification-toast',
            buttons: [
              {
                text: 'Voir',
                role: 'info',
                handler: () => {
                  console.log('👉 Navigation vers signalement:', notifData.signalementId);
                  // TODO: Naviguer vers le détail du signalement
                }
              },
              {
                text: 'OK',
                role: 'cancel'
              }
            ]
          });
          
          console.log('🎨 Toast créé, présentation...');
          await toast.present();
          console.log('✅ Toast affiché');
        } catch (err) {
          console.error('❌ Erreur affichage toast:', err);
        }
      }, delayMs);
      
      // Incrémenter le délai pour éviter le chevauchement
      delayMs += 800;
      
      // Marquer comme lue
      await updateDoc(doc(db, 'notifications', notifData.id), {
        lue: true,
        dateLecture: new Date().toISOString()
      });
      
      // Petit délai entre chaque notification pour ne pas toutes les afficher en même temps
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    
    console.log('✅ Toutes les notifications ont été affichées et marquées comme lues');
  } catch (error) {
    console.error('❌ Erreur lors de la vérification des notifications:', error);
  }
};

// Signalement State
const newSignalementPoint = ref<{lat: number, lng: number} | null>(null);
const reportDescription = ref('');
const reportPhotoUrl = ref('');
const selectedTypeId = ref<number | null>(null);
const typesSignalement = ref<TypeSignalement[]>([]);
const isSubmitting = ref(false);
const isLocating = ref(false);

let map: L.Map | null = null;
const markersMap = new Map<string, L.Marker>();
let userLocationMarker: L.LayerGroup | null = null;

const filteredSignalements = computed(() => {
  if (filterMine.value && store.user) {
    return store.signalements.filter(s => s.email === store.user?.email);
  }
  return store.signalements;
});

// Auth Methods
const takePhoto = async () => {
  console.log('Tentative de prise de photo...');
  try {
    const photo = await Camera.getPhoto({
      quality: 70,
      allowEditing: false,
      resultType: CameraResultType.Base64,
      source: CameraSource.Prompt
    });
    
    console.log('Photo reçue:', photo.format);
    if (photo.base64String) {
      reportPhotoUrl.value = `data:image/jpeg;base64,${photo.base64String}`;
    }
  } catch (e) {
    console.error('Erreur Camera:', e);
  }
};

const removePhoto = () => {
  reportPhotoUrl.value = '';
};

const handleLogin = async () => {
  if (!loginEmail.value || !loginPassword.value) {
    authError.value = "Veuillez remplir tous les champs";
    return;
  }
  isAuthLoading.value = true;
  authError.value = "";
  try {
    const email = loginEmail.value.trim();
    const password = loginPassword.value.trim();

    // 1. Récupérer la configuration de la limite (max_tentatives_connexion)
    const configDoc = await getDoc(doc(db, 'configurations', 'max_tentatives_connexion'));
    const maxTentatives = configDoc.exists() ? parseInt(configDoc.data().valeur) : 3;

    // 2. Chercher l'utilisateur dans Firestore
    // On cherche par email dans tous les documents car l'ID du document est maintenant l'UUID Postgres
    const usersRef = collection(db, 'utilisateurs');
    const q = query(usersRef, where('email', '==', email));
    const querySnapshot = await getDocs(q);

    if (querySnapshot.empty) {
      authError.value = "Email ou mot de passe incorrect";
      return;
    }

    const userDoc = querySnapshot.docs[0];
    const userDocRef = userDoc.ref;
    const userData = userDoc.data();

    console.log("Utilisateur trouvé dans Firestore:", userData.email);
    console.log("MDP Firestore:", userData.motDePasse, "| MDP saisi:", password);

    // 2.5 Récupérer la durée de session (duree_session_heures)
    const sessionConfigDoc = await getDoc(doc(db, 'configurations', 'duree_session_heures'));
    const dureeHeures = sessionConfigDoc.exists() ? parseFloat(sessionConfigDoc.data().valeur) : 24;

    // 3. Vérifier si le compte est bloqué
    if (userData.statut === 'BLOQUE') {
      authError.value = "Votre compte est bloqué. Contactez un administrateur.";
      return;
    }

    const appUser = {
      email: userData.email,
      role: userData.role,
      statut: userData.statut,
      postgresId: userData.postgresId
    };
    // 4. Vérifier le mot de passe
    if (userData.motDePasse === password) {
      // Succès : réinitialiser les tentatives
      await updateDoc(userDocRef, {
        tentatives_connexion: 0,
        derniereConnexion: new Date().toISOString()
      });

      const expiresAt = new Date(Date.now() + dureeHeures * 3600 * 1000).toISOString();

      const finalUser = {
        ...appUser,
        expiresAt: expiresAt
      };

      setUser(finalUser);
      localStorage.setItem('app_user', JSON.stringify(finalUser));
      
      // Récupérer le token FCM après connexion réussie
      await requestFcmToken(userDocRef);
      
      // Vérifier les notifications non lues après connexion
      await checkUnreadNotifications(appUser.email);
      
      // Activer le listener temps réel sur les notifications
      await setupNotificationListener(appUser.email);
      
      showLoginModal.value = false;
      loginEmail.value = '';
      loginPassword.value = '';
    } else {
      // Échec : incrémenter les tentatives
      const nouvellesTentatives = (userData.tentatives_connexion || 0) + 1;
      
      const updates: any = {
        tentatives_connexion: nouvellesTentatives
      };

      if (nouvellesTentatives >= maxTentatives) {
        updates.statut = 'BLOQUE';
        authError.value = `Compte bloqué après ${nouvellesTentatives} tentatives infructueuses.`;
      } else {
        authError.value = `Email ou mot de passe incorrect (${nouvellesTentatives}/${maxTentatives} tentatives)`;
      }

      await updateDoc(userDocRef, updates);
    }
  } catch (e: any) {
    console.error('Erreur Auth Firestore:', e);
    authError.value = "Une erreur est survenue lors de l'authentification";
  } finally {
    isAuthLoading.value = false;
  }
};

const handleLogout = async () => {
  console.log('🚪 Déconnexion en cours...');
  
  // 1. Supprimer le token FCM côté Firestore pour bloquer TOUTES les notifications
  if (store.user && store.user.postgresId) {
    try {
      const userDocRef = doc(db, 'utilisateurs', store.user.postgresId);
      await updateDoc(userDocRef, {
        fcmToken: null,
        fcmTokenStatus: 'logged_out',
        fcmTokenDate: new Date().toISOString()
      });
      console.log('✅ Token FCM supprimé - Notifications backend bloquées');
    } catch (err) {
      console.error('❌ Erreur suppression token FCM:', err);
    }
  }
  
  // 2. Désactiver le listener Firestore
  if (unsubscribeNotifications) {
    console.log('🔕 Désactivation du listener Firestore');
    unsubscribeNotifications();
    unsubscribeNotifications = null;
    console.log('✅ Listener Firestore désactivé');
  } else {
    console.log('ℹ️ Aucun listener Firestore à désactiver');
  }
  
  // 3. Déconnexion locale
  setUser(null);
  localStorage.removeItem('app_user');
  filterMine.value = false;
  
  console.log('✅ Déconnexion terminée - Mode visiteur (aucune notification)');
};

// Map & Signalement Methods
const initMap = () => {
  if (map) return;
  
  const tana = { lat: -18.8792, lng: 47.5079 };
  map = L.map('map', {
    zoomControl: false,
    attributionControl: false,
    tap: false, // Désactiver tap pour éviter les conflits touchmove sur mobile
    touchZoom: true
  }).setView([tana.lat, tana.lng], 13);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
  }).addTo(map);

  map.on('click', (e: L.LeafletMouseEvent) => {
    if (store.user) {
      newSignalementPoint.value = { lat: e.latlng.lat, lng: e.latlng.lng };
    } else {
      showLoginModal.value = true;
    }
  });

  const iconDefault = L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41]
  });
  L.Marker.prototype.options.icon = iconDefault;

  setTimeout(() => map?.invalidateSize(), 500);
};

const submitReport = async () => {
  if (!newSignalementPoint.value || !store.user) return;
  if (!selectedTypeId.value) {
    alert("Veuillez sélectionner un type de signalement");
    return;
  }
  
  isSubmitting.value = true;
  try {
    const reportData = {
      latitude: newSignalementPoint.value.lat,
      longitude: newSignalementPoint.value.lng,
      description: reportDescription.value,
      photo_url: reportPhotoUrl.value,
      utilisateur_id: store.user.postgresId,
      email: store.user.email,
      id_type_signalement: selectedTypeId.value,
      statut: 'nouveau',
      entreprise: null,
      dateSignalement: new Date().toISOString(),
      createdAt: serverTimestamp()
    };

    const docRef = await addDoc(collection(db, 'signalements'), reportData);

    newSignalementPoint.value = null;
    reportDescription.value = '';
    reportPhotoUrl.value = '';
    selectedTypeId.value = null;
  } catch (err) {
    console.error(err);
  } finally {
    isSubmitting.value = false;
  }
};

const locateUser = () => {
  if (!map) return;
  
  if (isLocating.value) {
    // Dézoomer en état normal (Tana par défaut)
    const tana = { lat: -18.8792, lng: 47.5079 };
    map.setView([tana.lat, tana.lng], 13);
    
    // Nettoyer le marqueur
    if (userLocationMarker) {
      map.removeLayer(userLocationMarker);
      userLocationMarker = null;
    }
    isLocating.value = false;
    return;
  }
  
  if ("geolocation" in navigator) {
    isLocating.value = true;
    navigator.geolocation.getCurrentPosition((position) => {
      const { latitude, longitude } = position.coords;
      
      // Zoomer sur la position
      map?.setView([latitude, longitude], 16);
      
      // Nettoyer l'ancien marqueur s'il existe
      if (userLocationMarker) {
        map?.removeLayer(userLocationMarker);
      }
      
      // Créer un nouveau groupe pour le marqueur de position
      userLocationMarker = L.layerGroup().addTo(map!);
      
      // Ajouter un cercle d'incertitude
      L.circle([latitude, longitude], {
        color: '#3b82f6',
        fillColor: '#3b82f6',
        fillOpacity: 0.15,
        radius: 100,
        weight: 1
      }).addTo(userLocationMarker);
      
      // Ajouter le point bleu
      L.circleMarker([latitude, longitude], {
        radius: 8,
        fillColor: "#3b82f6",
        color: "#fff",
        weight: 3,
        opacity: 1,
        fillOpacity: 1
      }).addTo(userLocationMarker);
      
    }, (error) => {
      isLocating.value = false;
      console.error("Erreur de géolocalisation:", error);
      let message = "Impossible de vous localiser";
      if (error.code === error.PERMISSION_DENIED) {
        message = "Permission de géolocalisation refusée";
      }
      alert(message);
    }, {
      enableHighAccuracy: true,
      timeout: 5000,
      maximumAge: 0
    });
  } else {
    alert("La géolocalisation n'est pas supportée par votre appareil");
  }
};

const getStatusTextColor = (status: string) => {
  switch(status?.toLowerCase()) {
    case 'nouveau': return '#2563EB'; // text-blue-700
    case 'en cours':
    case 'en_cours': return '#B45309'; // text-amber-700
    case 'terminé':
    case 'termine': return '#047857'; // text-emerald-700
    default: return '#334155'; // text-slate-700
  }
};

const formatDate = (dateString: string) => {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(date);
  } catch (e) { return ''; }
};

const updateMarkers = () => {
  if (!map) return;
  
  markersMap.forEach((marker) => marker.remove());
  markersMap.clear();

  filteredSignalements.value.forEach(s => {
    if (s.latitude && s.longitude) {
      const m = L.marker([s.latitude, s.longitude])
        .addTo(map!)
        .bindPopup(`
          <div class="p-2 min-w-[160px]">
            <div class="text-[10px] font-bold uppercase mb-1" style="color: ${getStatusTextColor(s.statut)}">${s.statut}</div>
            ${s.photo_url ? `<div class="w-full h-24 rounded-lg overflow-hidden mb-2 bg-slate-100"><img src="${s.photo_url}" class="w-full h-full object-cover"></div>` : ''}
            <div class="font-bold text-slate-800 text-sm mb-1">${s.description || 'Signalement'}</div>
            ${s.entreprise ? `<div class="text-[9px] text-blue-600 font-bold italic mb-1">Entreprise: ${s.entreprise}</div>` : ''}
            <div class="text-[10px] text-slate-400 mt-2 pt-2 border-t border-slate-50 flex justify-between">
              <span>${formatDate(s.dateSignalement)}</span>
              <span class="font-bold text-blue-600">${s.email === store.user?.email ? 'Moi' : ''}</span>
            </div>
          </div>
        `);
      markersMap.set(s.id, m);
    }
  });
};

watch(() => store.signalements, () => {
  updateMarkers();
}, { deep: true });

watch(() => filterMine.value, () => {
  updateMarkers();
});

const fetchTypesSignalement = async () => {
  try {
    const q = query(collection(db, 'types_signalement'));
    const querySnapshot = await getDocs(q);
    typesSignalement.value = querySnapshot.docs.map(doc => ({
      ...doc.data()
    } as TypeSignalement)).sort((a, b) => a.id - b.id);
  } catch (err) {
    console.error('Erreur lors de la récupération des types:', err);
  }
};

const carouselRef = ref<HTMLElement | null>(null);
const scrollCarousel = (direction: 'left' | 'right') => {
  if (!carouselRef.value) return;
  const scrollAmount = 150;
  carouselRef.value.scrollBy({
    left: direction === 'left' ? -scrollAmount : scrollAmount,
    behavior: 'smooth'
  });
};

onMounted(async () => {
  // 1. Écouter les notifications FCM en premier plan (SEULEMENT si connecté)
  try {
    const messaging = await getMessaging();
    if (messaging) {
      onMessage(messaging, (payload) => {
        console.log('🔔 Notification FCM reçue en premier plan:', payload);
        
        if (store.user) {
          showModernToast(
            payload.notification?.title || 'Mise à jour',
            payload.notification?.body || 'Un changement a été détecté',
            'info'
          );
        }
      });
      console.log('✅ Listener FCM foreground configuré (attente connexion)');
    }
  } catch (err) {
    console.warn("⚠️ Erreur lors de l'initialisation du listener FCM:", err);
  }

  // 2. Auth & Token logic
  if (store.user && store.user.postgresId) {
    try {
      const userDocRef = doc(db, 'utilisateurs', store.user.postgresId);
      await requestFcmToken(userDocRef);
      
      // 3. Configurer le listener temps réel sur les notifications
      if (store.user.email) {
        await setupNotificationListener(store.user.email);
      }
    } catch (err) {
      console.error("❌ Erreur lors de la récupération du token au montage:", err);
    }
  }

  // 4. Initialiser la carte
  setTimeout(() => {
    initMap();
    updateMarkers();
    fetchTypesSignalement();
  }, 100);
});

// Nettoyer le listener lors du démontage
onUnmounted(() => {
  if (unsubscribeNotifications) {
    console.log('🔕 Désactivation du listener notifications');
    unsubscribeNotifications();
  }
});
</script>

<style>
/* Leaflet Popup Styling */
.modern-notification-toast {
  --border-radius: 12px;
  --box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  font-family: 'Inter', sans-serif;
}

.modern-notification-toast::part(header) {
  font-weight: 700;
  font-size: 1rem;
}

.modern-notification-toast::part(message) {
  font-size: 0.9rem;
  opacity: 0.9;
}

.leaflet-popup-content-wrapper {
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0,0,0,0.05);
}
.leaflet-popup-tip {
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

/* Custom Notification Toast Styling */
.notification-toast {
  --border-radius: 16px;
  --box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  --background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
}

.notification-toast::part(header) {
  font-weight: 700;
  font-size: 14px;
  padding-bottom: 4px;
}

.notification-toast::part(message) {
  font-size: 13px;
  line-height: 1.5;
}

.notification-toast::part(button) {
  font-weight: 600;
  font-size: 12px;
}

/* Animation pour les toasts */
@keyframes slideInTop {
  from {
    transform: translateY(-100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.notification-toast {
  animation: slideInTop 0.3s ease-out;
}
</style>
