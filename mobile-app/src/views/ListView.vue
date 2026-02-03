<template>
  <ion-page>
    <!-- Modale pour agrandir l'image -->
    <div v-if="selectedImageUrl" class="absolute inset-0 z-[100] bg-black/90 backdrop-blur-xl flex flex-col items-center justify-center p-4 animate-in fade-in duration-200">
      <button @click="selectedImageUrl = null" class="absolute top-6 right-6 w-12 h-12 bg-white/10 hover:bg-white/20 rounded-full flex items-center justify-center text-white transition-all">
        <ion-icon :icon="closeOutline" class="text-2xl" />
      </button>
      <img :src="selectedImageUrl" class="max-w-full max-h-[80vh] rounded-2xl shadow-2xl object-contain">
    </div>

    <ion-content :fullscreen="true" class="bg-slate-50">
      <div class="p-6 pb-32">
        <!-- En-tête -->
        <div class="flex items-center justify-between mb-6 pt-4">
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

        <!-- Filtres par statut -->
        <div class="flex gap-2 overflow-x-auto no-scrollbar mb-8 pb-1">
          <button 
            v-for="f in filterOptions" 
            :key="f.id"
            @click="activeStatusFilter = f.id"
            class="px-4 py-2 rounded-full text-xs font-bold whitespace-nowrap transition-all border"
            :class="activeStatusFilter === f.id 
              ? 'bg-slate-800 text-white border-slate-800 shadow-lg shadow-slate-200' 
              : 'bg-white text-slate-500 border-slate-100'"
          >
            {{ f.label }}
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

            <!-- Nouveaux détails (Sync Postgres) -->
            <div v-if="s.entreprise || s.budget || s.surface_m2" class="grid grid-cols-2 gap-2 mb-3">
              <div v-if="s.entreprise" class="bg-slate-50 p-2 rounded-lg border border-slate-100">
                <p class="text-[8px] font-bold text-slate-400 uppercase">Entreprise</p>
                <p class="text-[10px] font-medium text-slate-600 truncate">{{ s.entreprise }}</p>
              </div>
              <div v-if="s.surface_m2" class="bg-slate-50 p-2 rounded-lg border border-slate-100">
                <p class="text-[8px] font-bold text-slate-400 uppercase">Surface</p>
                <p class="text-[10px] font-medium text-slate-600">{{ s.surface_m2 }} m²</p>
              </div>
            </div>

            <div class="flex items-center justify-between pt-3 border-t border-slate-50">
              <div class="flex items-center gap-2">
                <div class="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center">
                  <ion-icon :icon="store.user && s.email === store.user.email ? personOutline : eyeOutline" class="text-[10px]" />
                </div>
                <span class="text-xs font-medium text-slate-500 truncate max-w-[120px]">
                  {{ store.user && s.email === store.user.email ? 'Moi' : s.email }}
                </span>
              </div>
              
              <div class="flex gap-2">
                <button 
                  v-if="s.photo_url"
                  @click="selectedImageUrl = s.photo_url"
                  class="text-slate-600 text-xs font-bold flex items-center gap-1 bg-slate-100 px-3 py-1.5 rounded-lg hover:bg-slate-200 transition-colors"
                >
                  <ion-icon :icon="cameraOutline" />
                  Photo
                </button>
                <button 
                  @click="goToMap(s)"
                  class="text-blue-600 text-xs font-bold flex items-center gap-1 bg-blue-50 px-3 py-1.5 rounded-lg hover:bg-blue-100 transition-colors"
                >
                  <ion-icon :icon="locationOutline" />
                  Voir
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { IonPage, IonContent, IonIcon } from '@ionic/vue';
import { 
  personOutline, 
  constructOutline, 
  locationOutline, 
  eyeOutline, 
  cameraOutline,
  closeOutline 
} from 'ionicons/icons';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { store } from '../store';

const router = useRouter();
const filterMine = ref(false);
const activeStatusFilter = ref('all');
const selectedImageUrl = ref<string | null>(null);

const filterOptions = [
  { id: 'all', label: 'Tous' },
  { id: 'nouveau', label: 'Nouveaux' },
  { id: 'en cours', label: 'En cours' },
  { id: 'termine', label: 'Terminés' },
];

const filteredSignalements = computed(() => {
  let list = store.signalements;
  
  // Filtre par utilisateur
  if (filterMine.value && store.user) {
    list = list.filter(s => s.email === store.user.email);
  }
  
  // Filtre par statut
  if (activeStatusFilter.value !== 'all') {
    list = list.filter(s => {
      const sStatut = s.statut?.toLowerCase() || 'nouveau';
      if (activeStatusFilter.value === 'termine') {
        return sStatut.includes('fini') || sStatut.includes('termine');
      }
      return sStatut.includes(activeStatusFilter.value);
    });
  }
  
  return list;
});

const toggleFilter = () => {
  filterMine.value = !filterMine.value;
};

const goToMap = (s: any) => {
  router.push('/tabs/map');
  // On pourrait ajouter une logique pour centrer la carte sur ce point ici via le store
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
