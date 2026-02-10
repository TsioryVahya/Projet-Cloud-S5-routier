// Import Firebase scripts for service worker
importScripts('https://www.gstatic.com/firebasejs/10.0.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.0.0/firebase-messaging-compat.js');

// Initialize Firebase in service worker
firebase.initializeApp({
  apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
  authDomain: "projet-cloud-s5-routier.firebaseapp.com",
  projectId: "projet-cloud-s5-routier",
  storageBucket: "projet-cloud-s5-routier.firebasestorage.app",
  messagingSenderId: "792049548362",
  appId: "1:792049548362:web:6ab3ce65b1584730c63ab3"
});

const messaging = firebase.messaging();

// Handle background messages
messaging.onBackgroundMessage((payload) => {
  console.log('📬 Message reçu en arrière-plan:', payload);

  const notificationTitle = payload.notification?.title || 'Nouvelle notification';
  const notificationOptions = {
    body: payload.notification?.body || '',
    icon: '/assets/icon/favicon.png',
    badge: '/assets/icon/favicon.png',
    data: payload.data,
    tag: payload.data?.signalementId || 'notification'
  };

  return self.registration.showNotification(notificationTitle, notificationOptions);
});

// Handle notification click
self.addEventListener('notificationclick', (event) => {
  console.log('🔔 Notification cliquée:', event);
  
  event.notification.close();

  // Ouvrir l'application ou naviguer vers la page appropriée
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList) => {
        // Si l'application est déjà ouverte, la focus
        for (const client of clientList) {
          if (client.url.includes('/tabs') && 'focus' in client) {
            return client.focus();
          }
        }
        // Sinon, ouvrir une nouvelle fenêtre
        if (clients.openWindow) {
          return clients.openWindow('/tabs/notifications');
        }
      })
  );
});
