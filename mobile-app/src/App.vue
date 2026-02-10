<template>
  <ion-app>
    <!-- Splash Screen -->
    <transition name="fade-splash">
      <div v-if="showSplash" class="splash-screen">
        <div class="splash-content">
          <div class="logo-container">
            <ion-icon :icon="trailSignOutline" class="splash-logo" />
          </div>
          <h1 class="splash-text">
            <span v-for="(char, index) in 'Lalana APK'" :key="index" :style="{ animationDelay: (index * 0.1) + 's' }">
              {{ char === ' ' ? '\u00A0' : char }}
            </span>
          </h1>
          <div class="loading-bar-container">
            <div class="loading-bar"></div>
          </div>
        </div>
      </div>
    </transition>

    <ion-router-outlet v-if="!showSplash" />
  </ion-app>
</template>

<script setup lang="ts">
import { IonApp, IonRouterOutlet, IonIcon } from '@ionic/vue';
import { ref, onMounted } from 'vue';
import { trailSignOutline } from 'ionicons/icons';
import { db } from './firebase/config';
import { collection, onSnapshot, query, orderBy } from 'firebase/firestore';
import { store, setSignalements, setUser } from './store';

const showSplash = ref(true);

onMounted(() => {
  // 1. Écouter les signalements en temps réel
  const q = query(collection(db, 'signalements'), orderBy('dateSignalement', 'desc'));
  onSnapshot(q, (snapshot) => {
    const data = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    })) as any[];
    setSignalements(data);
  });

  // 2. Gérer la session persistante
  const savedUserStr = localStorage.getItem('app_user');
  if (savedUserStr) {
    const savedUser = JSON.parse(savedUserStr);
    if (savedUser.expiresAt) {
      const expirationDate = new Date(savedUser.expiresAt);
      if (expirationDate < new Date()) {
        localStorage.removeItem('app_user');
        setUser(null);
      } else {
        setUser(savedUser);
      }
    } else {
      setUser(savedUser);
    }
  }

  // 3. Cacher le splash screen après un délai minimum pour l'animation
  setTimeout(() => {
    showSplash.value = false;
  }, 2500);
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

/* Splash Screen Styles */
.splash-screen {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.splash-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  width: 100%;
  padding: 0 20px;
}

.logo-container {
  width: 80px;
  height: 80px;
  background: white;
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: pulse-logo 2s infinite ease-in-out;
  margin-bottom: 0.5rem;
}

.splash-logo {
  font-size: 3rem;
  color: #2563eb;
}

.splash-text {
  color: white;
  font-size: 8vw; /* Taille responsive basée sur la largeur de l'écran */
  font-weight: 900;
  letter-spacing: -0.05em;
  margin: 0;
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  max-width: 100%;
}

@media (min-width: 640px) {
  .splash-text {
    font-size: 3rem;
  }
  .logo-container {
    width: 100px;
    height: 100px;
    border-radius: 30px;
  }
  .splash-logo {
    font-size: 3.5rem;
  }
}

.splash-text span {
  display: inline-block;
  animation: bounce-text 1.5s infinite;
}

.loading-bar-container {
  width: 180px;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
  overflow: hidden;
  margin: 1rem auto 0;
}

.loading-bar {
  width: 40%;
  height: 100%;
  background: white;
  border-radius: 10px;
  animation: loading-progress 1.5s infinite ease-in-out;
}

/* Animations */
@keyframes pulse-logo {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

@keyframes bounce-text {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes loading-progress {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(250%); }
}

/* Vue Transitions */
.fade-splash-leave-active {
  transition: opacity 0.8s ease-in-out, transform 0.8s ease-in-out;
}

.fade-splash-leave-to {
  opacity: 0;
  transform: scale(1.1);
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
