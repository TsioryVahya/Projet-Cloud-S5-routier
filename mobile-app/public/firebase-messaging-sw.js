importScripts('https://www.gstatic.com/firebasejs/10.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.0.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
  authDomain: "projet-cloud-s5-routier.firebaseapp.com",
  projectId: "projet-cloud-s5-routier",
  storageBucket: "projet-cloud-s5-routier.firebasestorage.app",
  messagingSenderId: "792049548362",
  appId: "1:792049548362:web:6ab3ce65b1584730c63ab3"
});

const messaging = firebase.messaging();

// Installation immédiate
self.addEventListener('install', (event) => {
  console.log('[firebase-messaging-sw.js] Service Worker en cours d\'installation...');
  self.skipWaiting(); // Forcer l'installation immédiate
});

// Activation immédiate et prise de contrôle des clients
self.addEventListener('activate', (event) => {
  console.log('[firebase-messaging-sw.js] Service Worker activé et prend le contrôle...');
  event.waitUntil(
    clients.claim().then(() => {
      console.log('[firebase-messaging-sw.js] Contrôle de tous les clients établi');
    })
  );
});

// Gérer les messages depuis la page (pour forcer l'activation)
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    console.log('[firebase-messaging-sw.js] SKIP_WAITING reçu, activation forcée');
    self.skipWaiting();
  }
});

// Gérer les notifications quand l'application est en arrière-plan
messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Message reçu en arrière-plan ', payload);
  
  // Extraire les infos du payload (notification ou data)
  const title = payload.notification?.title || payload.data?.title || 'Mise à jour Signalement';
  const body = payload.notification?.body || payload.data?.body || 'Un changement a été détecté sur votre signalement.';
  
  const notificationOptions = {
    body: body,
    icon: '/favicon.ico', // Utiliser un chemin plus sûr pour le test
    badge: '/favicon.ico',
    tag: 'signalement-update-' + (payload.data?.signalementId || Date.now()),
    renotify: true,
    data: payload.data,
    vibrate: [200, 100, 200]
  };

  console.log('[firebase-messaging-sw.js] Tentative d\'affichage de la notification:', title);
  
  return self.registration.showNotification(title, notificationOptions)
    .then(() => console.log('[firebase-messaging-sw.js] Notification affichée avec succès'))
    .catch(err => console.error('[firebase-messaging-sw.js] Erreur lors de l\'affichage de la notification:', err));
});

// Éviter que le Service Worker ne s'arrête prématurément
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    clients.openWindow('/')
  );
});