<template>
  <ion-app>
    <ion-router-outlet />
  </ion-app>
</template>

<script setup lang="ts">
import { IonApp, IonRouterOutlet } from '@ionic/vue';
import { onMounted } from 'vue';
import { db, messaging } from './firebase/config';
import { collection, onSnapshot, query, orderBy, doc, updateDoc } from 'firebase/firestore';
import { getToken, onMessage } from 'firebase/messaging';
import { store, setSignalements, setUser } from './store';

onMounted(async () => {
  // 0. Inscription explicite du Service Worker pour le débogage
  if ('serviceWorker' in navigator) {
    try {
      console.log('Tentative d\'inscription du Service Worker...');
      // Vérifier si le fichier est accessible avant de l'inscrire
      const swResponse = await fetch('/firebase-messaging-sw.js');
      if (!swResponse.ok) {
        throw new Error(`Le fichier Service Worker n'est pas accessible (Status: ${swResponse.status})`);
      }
      
      const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js', {
        scope: '/'
      });
      console.log('Service Worker inscrit avec succès, scope:', registration.scope);
      
      // Attendre que le SW soit actif
      if (registration.installing) {
        console.log('Service Worker en cours d\'installation...');
      } else if (registration.waiting) {
        console.log('Service Worker installé et en attente.');
      } else if (registration.active) {
        console.log('Service Worker actif.');
      }
    } catch (err: any) {
      console.error('Échec critique de l\'inscription du Service Worker:', err.message);
    }
  }

  // 1. Écouter les signalements en temps réel pour TOUTE l'application
  const q = query(collection(db, 'signalements'), orderBy('dateSignalement', 'desc'));
  onSnapshot(q, (snapshot) => {
    const data = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    })) as any[];
    setSignalements(data);
  });

  // 2. Gérer la session persistante locale
  const savedUserStr = localStorage.getItem('app_user');
  if (savedUserStr) {
    const savedUser = JSON.parse(savedUserStr);
    
    // Vérifier l'expiration de la session
    if (savedUser.expiresAt) {
      const expirationDate = new Date(savedUser.expiresAt);
      if (expirationDate < new Date()) {
        console.log("Session expirée, déconnexion...");
        localStorage.removeItem('app_user');
        setUser(null);
      } else {
        setUser(savedUser);
        // Rafraîchir le token FCM si l'utilisateur est connecté
        if (messaging && savedUser.postgresId) {
          // Vérifier la permission avant de demander le token
          if (Notification.permission === 'granted') {
            const getValidToken = async () => {
              try {
                const currentToken = await getToken(messaging, { vapidKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI' });
                if (currentToken) {
                  const userDocRef = doc(db, 'utilisateurs', savedUser.postgresId);
                  await updateDoc(userDocRef, { fcmToken: currentToken });
                  console.log("Token rafraîchi au démarrage");
                }
              } catch (err: any) {
                if (err.message.includes('no active Service Worker')) {
                  console.warn("SW non actif au démarrage, attente...");
                  const reg = await navigator.serviceWorker.ready;
                  if (reg.active) {
                    const token = await getToken(messaging, { vapidKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI' });
                    if (token) {
                      const userDocRef = doc(db, 'utilisateurs', savedUser.postgresId);
                      await updateDoc(userDocRef, { fcmToken: token });
                    }
                  }
                } else {
                  console.error("Erreur rafraîchissement token:", err);
                }
              }
            };
            getValidToken();
          } else {
            console.log("Permission de notification non accordée, impossible de rafraîchir le token.");
          }
        }
      }
    } else {
      setUser(savedUser);
    }
  }

  // 3. Écouter les messages FCM au premier plan
  if (messaging) {
    onMessage(messaging, (payload) => {
      console.log('Message FCM reçu au premier plan:', payload);
      // Optionnel : Afficher une alerte ou un toast local
      if (payload.notification) {
        alert(`${payload.notification.title}\n${payload.notification.body}`);
      }
    });
  }
});
</script>

<style>
:root {
  --ion-font-family: 'Inter', sans-serif;
}

body {
  font-family: 'Inter', sans-serif;
  background-color: #f8fafc;
}

/* Masquer la scrollbar par défaut */
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
