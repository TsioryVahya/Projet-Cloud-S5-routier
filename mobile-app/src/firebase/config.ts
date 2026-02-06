import { initializeApp } from 'firebase/app';
import { getFirestore } from 'firebase/firestore';
import { getAuth } from 'firebase/auth';
import { getMessaging } from 'firebase/messaging';

// Configuration Firebase pour le module mobile
// Note: Dans un projet réel, ces valeurs devraient être dans un fichier .env
const firebaseConfig = {
  apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
  authDomain: "projet-cloud-s5-routier.firebaseapp.com",
  projectId: "projet-cloud-s5-routier",
  storageBucket: "projet-cloud-s5-routier.firebasestorage.app",
  messagingSenderId: "792049548362",
  appId: "1:792049548362:web:6ab3ce65b1584730c63ab3" // ID Web déduit (ou Android si Web non créé)
};

const app = initializeApp(firebaseConfig);
export const db = getFirestore(app);
export const auth = getAuth(app);

// Initialiser le messaging seulement si on est dans le navigateur et si le Service Worker est supporté
let messagingInstance: any = null;

const initMessaging = async () => {
  if (typeof window === 'undefined' || !('serviceWorker' in navigator)) {
    console.warn('[config.ts] Service Worker non supporté dans cet environnement');
    return null;
  }

  try {
    // Vérifier si le Service Worker contrôle déjà la page
    if (!navigator.serviceWorker.controller) {
      console.warn('[config.ts] Service Worker pas encore en contrôle');
      
      // Vérifier si un Service Worker est enregistré
      const registration = await navigator.serviceWorker.getRegistration('/firebase-messaging-sw.js');
      
      if (!registration) {
        // Enregistrer le Service Worker
        console.log('[config.ts] Enregistrement du Service Worker...');
        await navigator.serviceWorker.register('/firebase-messaging-sw.js', { scope: '/' });
        console.log('[config.ts] Service Worker enregistré. Un rechargement de la page est recommandé.');
      }
      
      // Essayer quand même d'initialiser le messaging
      // Firebase peut parfois fonctionner même sans contrôle immédiat
      console.log('[config.ts] Tentative d\'initialisation du messaging sans contrôle de page...');
      try {
        messagingInstance = getMessaging(app);
        console.log('[config.ts] Firebase Messaging initialisé (mode limité)');
        return messagingInstance;
      } catch (msgError) {
        console.error('[config.ts] Impossible d\'initialiser messaging sans contrôle:', msgError);
        return null;
      }
    }

    // Le Service Worker contrôle déjà la page
    console.log('[config.ts] Service Worker en contrôle, initialisation du messaging');
    
    // Petite pause pour s'assurer que tout est stable
    await new Promise(resolve => setTimeout(resolve, 300));
    
    messagingInstance = getMessaging(app);
    console.log('[config.ts] Firebase Messaging initialisé avec succès');
    return messagingInstance;
    
  } catch (error) {
    console.error('[config.ts] Erreur lors de l\'initialisation du messaging:', error);
    return null;
  }
};

// Exporter une fonction pour obtenir l'instance de messaging
export const getMessagingInstance = async () => {
  if (!messagingInstance) {
    messagingInstance = await initMessaging();
  }
  return messagingInstance;
};

// Pour la compatibilité avec l'ancien code
export const messaging = messagingInstance;
