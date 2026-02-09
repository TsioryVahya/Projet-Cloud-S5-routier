import { createApp } from 'vue'
import App from './App.vue'
import { IonicVue } from '@ionic/vue';
import router from './router';
import './tailwind.css';
import { defineCustomElements } from '@ionic/pwa-elements/loader';
import { notificationService } from './services/notificationService';
import { auth } from './firebase/config';
import { onAuthStateChanged } from 'firebase/auth';

// Call the element loader before the render call
defineCustomElements(window);

/* Core CSS required for Ionic components to work properly */
import '@ionic/vue/css/core.css';

/* Basic CSS for apps built with Ionic */
import '@ionic/vue/css/normalize.css';
import '@ionic/vue/css/structure.css';
import '@ionic/vue/css/typography.css';

/* Optional CSS utils that can be commented out */
import '@ionic/vue/css/padding.css';
import '@ionic/vue/css/float-elements.css';
import '@ionic/vue/css/text-alignment.css';
import '@ionic/vue/css/text-transformation.css';
import '@ionic/vue/css/flex-utils.css';
import '@ionic/vue/css/display.css';

const app = createApp(App)
  .use(IonicVue)
  .use(router);

// Initialiser les notifications quand l'utilisateur est connecté
onAuthStateChanged(auth, async (user) => {
  if (user) {
    console.log('👤 Utilisateur connecté:', user.email);
    console.log('🔔 Initialisation des notifications...');
    
    // Initialiser FCM
    await notificationService.initialize();
    
    // Réessayer de sauvegarder le token si nécessaire
    await notificationService.retryTokenSave();
    
    // Charger les notifications
    await notificationService.loadNotifications();
  } else {
    console.log('👤 Utilisateur déconnecté, nettoyage des notifications...');
    notificationService.cleanup();
  }
});

router.isReady().then(() => {
  app.mount('#app');
});
