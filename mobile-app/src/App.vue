<template>
  <ion-app>
    <ion-router-outlet />
  </ion-app>
</template>

<script setup lang="ts">
import { IonApp, IonRouterOutlet } from '@ionic/vue';
import { onMounted } from 'vue';
import { db } from './firebase/config';
import { collection, onSnapshot, query, orderBy } from 'firebase/firestore';
import { store, setSignalements, setUser } from './store';

onMounted(() => {
  // 1. Écouter les signalements en temps réel pour TOUTE l'application
  const q = query(collection(db, 'signalements'), orderBy('dateSignalement', 'desc'));
  onSnapshot(q, (snapshot) => {
    const data = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    })) as any[];
    setSignalements(data);
  });

  // 2. Gérer la session persistante locale (optionnel)
  const savedUser = localStorage.getItem('app_user');
  if (savedUser) {
    setUser(JSON.parse(savedUser));
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
