<template>
  <ion-page>
    <ion-content :fullscreen="true" class="bg-slate-50">
      <div class="p-6 pb-32">
        <!-- En-tête -->
        <div class="flex items-center justify-between mb-8 pt-4">
          <div>
            <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Travaux</h1>
            <p class="text-slate-500 text-sm mt-1 font-medium">
              {{ filteredSignalements.length }} signalement{{ filteredSignalements.length > 1 ? 's' : '' }}
            </p>
          </div>
          
          <button 
            v-if="store.user"
            @click="toggleFilter"
            class="p-3 rounded-2xl transition-all border shadow-sm flex items-center justify-center"
            :class="filterMine ? 'bg-blue-600 text-white border-blue-500 shadow-blue-500/30' : 'bg-white text-slate-400 border-slate-100'"
          >
            <ion-icon :icon="personOutline" class="text-xl" />
          </button>
        </div>

        <!-- Liste vide -->
        <div v-if="filteredSignalements.length === 0" class="flex flex-col items-center justify-center py-20 text-center">
          <div class="w-24 h-24 bg-slate-100 rounded-full flex items-center justify-center mb-4 grayscale opacity-50">
            <ion-icon :icon="constructOutline" class="text-4xl text-slate-400" />
          </div>
          <h3 class="text-lg font-bold text-slate-700">Aucun travaux</h3>
          <p class="text-slate-400 text-sm mt-2 max-w-[200px]">Il n'y a aucun signalement correspondant à vos critères.</p>
        </div>

        <!-- Liste des cartes -->
        <div v-else class="space-y-4">
          <div 
            v-for="s in filteredSignalements" 
            :key="s.id"
            class="bg-white rounded-2xl p-4 shadow-sm border border-slate-100 active:scale-[0.99] transition-transform"
          >
            <div class="flex justify-between items-start mb-3">
              <div 
                class="px-3 py-1 rounded-lg text-[10px] font-bold uppercase tracking-wider"
                :style="{ backgroundColor: getStatusColor(s.statut).bg, color: getStatusColor(s.statut).text }"
              >
                {{ s.statut || 'Nouveau' }}
              </div>
              <span class="text-[10px] font-bold text-slate-300">{{ formatDate(s.dateSignalement) }}</span>
            </div>

            <h3 class="font-bold text-slate-800 text-base mb-2 line-clamp-2">
              {{ s.description || 'Signalement sans description' }}
            </h3>

            <div class="flex items-center justify-between pt-3 border-t border-slate-50">
              <div class="flex items-center gap-2">
                <div class="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center">
                  <ion-icon :icon="store.user && s.email === store.user.email ? personOutline : eyeOutline" class="text-[10px]" />
                </div>
                <span class="text-xs font-medium text-slate-500 truncate max-w-[150px]">
                  {{ store.user && s.email === store.user.email ? 'Moi' : s.email }}
                </span>
              </div>
              
              <button class="text-blue-600 text-xs font-bold flex items-center gap-1 bg-blue-50 px-3 py-1.5 rounded-lg hover:bg-blue-100 transition-colors">
                <ion-icon :icon="locationOutline" />
                Voir
              </button>
            </div>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonPage, IonContent, IonIcon } from '@ionic/vue';
import { personOutline, constructOutline, locationOutline, eyeOutline } from 'ionicons/icons';
import { computed, ref } from 'vue';
import { store } from '../store';

const filterMine = ref(false);

const filteredSignalements = computed(() => {
  let list = store.signalements;
  if (filterMine.value && store.user) {
    list = list.filter(s => s.email === store.user.email);
  }
  return list;
});

const toggleFilter = () => {
  filterMine.value = !filterMine.value;
};

// Utilitaires
const getStatusColor = (statut: string) => {
  const s = statut?.toLowerCase() || 'nouveau';
  if (s.includes('cours')) return { bg: '#fff7ed', text: '#c2410c' }; // Orange
  if (s.includes('fini') || s.includes('termine')) return { bg: '#f0fdf4', text: '#15803d' }; // Vert
  return { bg: '#eff6ff', text: '#1d4ed8' }; // Bleu (Nouveau)
};

const formatDate = (date: any) => {
  if (!date) return '';
  const d = date.toDate ? date.toDate() : new Date(date);
  return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' });
};
</script>
