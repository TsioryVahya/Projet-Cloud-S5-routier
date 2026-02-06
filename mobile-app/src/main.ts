import { createApp } from 'vue'
import App from './App.vue'
import { IonicVue } from '@ionic/vue';
import router from './router';
import './tailwind.css';
import { defineCustomElements } from '@ionic/pwa-elements/loader';

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

// Enregistrer le Service Worker Firebase dès le démarrage de l'application
if ('serviceWorker' in navigator) {
  console.log('[main.ts] Service Worker supporté par le navigateur');
  
  window.addEventListener('load', async () => {
    try {
      console.log('[main.ts] Enregistrement précoce du Service Worker Firebase...');
      console.log('[main.ts] URL actuelle:', window.location.href);
      console.log('[main.ts] Controller actuel:', navigator.serviceWorker.controller);
      
      const registration = await navigator.serviceWorker.register('/firebase-messaging-sw.js', { 
        scope: '/',
        updateViaCache: 'none' // Forcer la vérification des mises à jour
      });
      
      console.log('[main.ts] ✅ Service Worker Firebase enregistré:', registration.scope);
      console.log('[main.ts] État:', {
        installing: registration.installing?.state,
        waiting: registration.waiting?.state,
        active: registration.active?.state
      });
      
      // Attendre l'activation
      if (registration.installing) {
        console.log('[main.ts] Service Worker en installation...');
        await new Promise<void>((resolve) => {
          registration.installing!.addEventListener('statechange', (e: any) => {
            console.log('[main.ts] État SW changé:', e.target.state);
            if (e.target.state === 'activated') {
              console.log('[main.ts] ✅ Service Worker activé');
              resolve();
            }
          });
        });
      } else if (registration.waiting) {
        console.log('[main.ts] ⏳ Service Worker en attente');
      } else if (registration.active) {
        console.log('[main.ts] ✅ Service Worker déjà actif');
      }
      
      // Assurer le contrôle de la page
      if (!navigator.serviceWorker.controller) {
        console.warn('[main.ts] ⚠️ Service Worker n\'a pas encore le contrôle de la page');
        console.warn('[main.ts] 💡 Un rechargement de la page (F5) est nécessaire pour activer le contrôle');
      } else {
        console.log('[main.ts] ✅ Service Worker contrôle la page');
      }
    } catch (error) {
      console.error('[main.ts] ❌ Erreur lors de l\'enregistrement du Service Worker:', error);
    }
  });
} else {
  console.error('[main.ts] ❌ Service Worker NON supporté par ce navigateur');
}

const app = createApp(App)
  .use(IonicVue)
  .use(router);

router.isReady().then(() => {
  app.mount('#app');
});
