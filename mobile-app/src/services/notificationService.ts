import { getMessaging, getToken, onMessage, Messaging } from 'firebase/messaging';
import { doc, setDoc, collection, query, where, orderBy, onSnapshot, Timestamp, updateDoc } from 'firebase/firestore';
import { db, auth } from '../firebase/config';
import { store } from '../store';
import { ref, Ref } from 'vue';

export interface Notification {
  id: string;
  userId: string;
  signalementId: string;
  titre: string;
  message: string;
  type: 'status_change' | 'other';
  oldStatus?: string;
  newStatus: string;
  dateCreation: any;
  lu: boolean;
}

class NotificationService {
  private messaging: Messaging | null = null;
  private unsubscribeSnapshot: (() => void) | null = null;
  public notifications: Ref<Notification[]> = ref([]);
  public unreadCount: Ref<number> = ref(0);

  async initialize() {
    try {
      // Initialiser Firebase Messaging
      this.messaging = getMessaging();
      
      // Demander la permission pour les notifications
      const permission = await Notification.requestPermission();
      if (permission === 'granted') {
        console.log('✅ Permission de notification accordée');
        await this.saveFCMToken();
        this.setupMessageListener();
      } else {
        console.warn('⚠️ Permission de notification refusée');
      }
    } catch (error) {
      console.error('❌ Erreur lors de l\'initialisation des notifications:', error);
    }
  }

  private async saveFCMToken() {
    try {
      if (!this.messaging) {
        console.warn('⚠️ Messaging non initialisé');
        return;
      }

      // Utiliser soit Firebase Auth, soit le store custom
      const userEmail = auth.currentUser?.email || store.user?.email;
      const userId = auth.currentUser?.uid || store.user?.postgresId;

      if (!userEmail || !userId) {
        console.warn('⚠️ Utilisateur non identifié, impossible de sauvegarder le token');
        return;
      }
      
      console.log('👤 Utilisateur:', userEmail);
      console.log('🆔 ID utilisé:', userId);

      // 1. Sauvegarder d'abord l'email dans Firestore (important pour le backend)
      // On le fait avant getToken car getToken peut échouer en localhost
      try {
        const userDocRef = doc(db, 'users', userId);
        await setDoc(userDocRef, {
          email: userEmail,
          lastTokenUpdate: Timestamp.now()
        }, { merge: true });
        console.log('✅ Email utilisateur sauvegardé dans Firestore');
      } catch (err) {
        console.error('❌ Erreur lors de la sauvegarde de l\'email dans Firestore:', err);
      }
      
      console.log('🔐 Tentative d\'obtention du FCM token...');
      
      try {
        const currentToken = await getToken(this.messaging, {
          vapidKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI'
        });

        if (currentToken) {
          console.log('📱 FCM Token obtenu:', currentToken);
          
          // Sauvegarder le token dans Firestore
          const userDocRef = doc(db, 'users', userId);
          await setDoc(userDocRef, {
            fcmToken: currentToken,
            lastTokenUpdate: Timestamp.now()
          }, { merge: true });
          
          console.log('✅ FCM Token sauvegardé dans Firestore pour l\'utilisateur:', userId);
        } else {
          console.warn('⚠️ Impossible d\'obtenir le FCM token');
          console.warn('💡 Cela peut être normal en développement local (localhost)');
        }
      } catch (tokenError: any) {
        console.warn('⚠️ Erreur lors de l\'obtention du FCM token (normal en localhost):', tokenError.message || tokenError);
        console.warn('💡 L\'email est quand même sauvegardé, donc les notifications Firestore fonctionneront.');
      }
    } catch (error: any) {
      console.error('❌ Erreur globale dans saveFCMToken:', error.code || error.message);
    }
  }

  private setupMessageListener() {
    if (!this.messaging) return;

    // Écouter les messages en premier plan
    onMessage(this.messaging, (payload) => {
      console.log('📬 Message FCM reçu en premier plan:', payload);
      
      // Afficher une notification locale
      if (payload.notification) {
        console.log('📢 Affichage notification locale:', payload.notification.title);
        new Notification(payload.notification.title || 'Nouvelle notification', {
          body: payload.notification.body,
          icon: '/assets/icon/favicon.png'
        });
      } else {
        console.log('⚠️ Message FCM reçu sans contenu de notification');
      }
      
      // Recharger les notifications
      this.loadNotifications();
    });
  }

  async loadNotifications() {
    try {
      const userId = auth.currentUser?.uid || store.user?.postgresId;

      if (!userId) {
        console.warn('⚠️ Utilisateur non identifié, impossible de charger les notifications');
        this.notifications.value = [];
        this.unreadCount.value = 0;
        return;
      }

      // Se désabonner de l'ancien listener si existant
      if (this.unsubscribeSnapshot) {
        this.unsubscribeSnapshot();
      }

      console.log('📬 Chargement des notifications pour:', userId);

      // Créer une requête pour les notifications de l'utilisateur
      const notificationsRef = collection(db, 'notifications');
      
      // Requête simple sans orderBy pour éviter l'erreur d'index
      const q = query(
        notificationsRef,
        where('userId', '==', userId)
      );

      // Écouter les changements en temps réel
      console.log('📡 Écoute des changements Firestore activée pour les notifications...');
      this.unsubscribeSnapshot = onSnapshot(q, (snapshot) => {
        console.log(`🔔 Changement détecté dans Firestore (snapshot size: ${snapshot.size})`);
        const notifs: Notification[] = [];
        let unreadCount = 0;

        snapshot.forEach((doc) => {
          const data = doc.data();
          console.log(`  📄 Notif ID: ${doc.id}, lu: ${data.lu}, titre: ${data.titre}`);
          const notif: Notification = {
            id: doc.id,
            userId: data.userId,
            signalementId: data.signalementId,
            titre: data.titre,
            message: data.message,
            type: data.type || 'other',
            oldStatus: data.oldStatus,
            newStatus: data.newStatus,
            dateCreation: data.dateCreation,
            lu: data.lu || false
          };
          
          notifs.push(notif);
          if (!notif.lu) {
            unreadCount++;
          }
        });

        // Trier manuellement par date (plus récent en premier)
        notifs.sort((a, b) => {
          const dateA = a.dateCreation?.toDate?.() || new Date(a.dateCreation);
          const dateB = b.dateCreation?.toDate?.() || new Date(b.dateCreation);
          return dateB.getTime() - dateA.getTime();
        });

        this.notifications.value = notifs;
        this.unreadCount.value = unreadCount;
        
        console.log(`📬 ${notifs.length} notifications chargées (${unreadCount} non lues)`);
      }, (error: any) => {
        console.error('❌ Erreur lors de l\'écoute des notifications:', error);
        
        if (error.code === 'failed-precondition' && error.message?.includes('index')) {
          console.warn('⚠️ Index Firestore manquant');
          console.warn('💡 Cliquez sur le lien dans l\'erreur pour créer l\'index automatiquement');
          console.warn('💡 Ou créez l\'index manuellement dans Firebase Console');
          console.warn('💡 En attendant, les notifications fonctionnent sans tri par date');
        }
      });
    } catch (error) {
      console.error('❌ Erreur lors du chargement des notifications:', error);
    }
  }

  async markAsRead(notificationId: string) {
    try {
      const notifRef = doc(db, 'notifications', notificationId);
      await updateDoc(notifRef, {
        lu: true
      });
      console.log(`✅ Notification ${notificationId} marquée comme lue`);
    } catch (error) {
      console.error('❌ Erreur lors du marquage de la notification:', error);
    }
  }

  async markAllAsRead() {
    try {
      const unreadNotifs = this.notifications.value.filter(n => !n.lu);
      
      for (const notif of unreadNotifs) {
        await this.markAsRead(notif.id);
      }
      
      console.log(`✅ ${unreadNotifs.length} notifications marquées comme lues`);
    } catch (error) {
      console.error('❌ Erreur lors du marquage de toutes les notifications:', error);
    }
  }

  cleanup() {
    if (this.unsubscribeSnapshot) {
      this.unsubscribeSnapshot();
      this.unsubscribeSnapshot = null;
    }
    this.notifications.value = [];
    this.unreadCount.value = 0;
  }

  // Méthode pour réessayer la sauvegarde du token si l'utilisateur s'est connecté après l'init
  async retryTokenSave() {
    if ((auth.currentUser || store.user) && this.messaging) {
      await this.saveFCMToken();
    }
  }
}

export const notificationService = new NotificationService();
