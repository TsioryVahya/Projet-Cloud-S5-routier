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
        <div class="mb-6 pt-4">
          <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Mes Signalements</h1>
          <p class="text-slate-500 text-sm mt-1 font-medium" v-if="store.user">
            {{ mySignalements.length }} signalement{{ mySignalements.length > 1 ? 's' : '' }} envoyé{{ mySignalements.length > 1 ? 's' : '' }}
          </p>
        </div>

        <!-- État non connecté -->
        <div v-if="!store.user" class="flex flex-col items-center justify-center py-20 text-center">
          <div class="w-24 h-24 bg-blue-50 rounded-full flex items-center justify-center mb-6">
            <ion-icon :icon="personOutline" class="text-4xl text-blue-500" />
          </div>
          <h3 class="text-lg font-bold text-slate-700">Non connecté</h3>
          <p class="text-slate-400 text-sm mt-2 max-w-[240px] mb-6">Connectez-vous pour voir et gérer vos propres signalements.</p>
          <button 
            @click="router.push('/tabs/map')"
            class="bg-blue-600 text-white px-8 py-3 rounded-2xl font-bold shadow-lg shadow-blue-500/30 active:scale-95 transition-all"
          >
            Aller à la carte
          </button>
        </div>

        <!-- Liste vide -->
        <div v-else-if="mySignalements.length === 0" class="flex flex-col items-center justify-center py-20 text-center">
          <div class="w-24 h-24 bg-slate-100 rounded-full flex items-center justify-center mb-4 grayscale opacity-50">
            <ion-icon :icon="constructOutline" class="text-4xl text-slate-400" />
          </div>
          <h3 class="text-lg font-bold text-slate-700">Aucun signalement</h3>
          <p class="text-slate-400 text-sm mt-2 max-w-[200px]">Vous n'avez pas encore envoyé de signalement.</p>
        </div>

        <!-- Liste des cartes -->
        <div v-else class="space-y-4">
          <div 
            v-for="s in mySignalements" 
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

            <!-- Détails (Sync Postgres) -->
            <div v-if="s.entreprise || s.surface_m2" class="grid grid-cols-2 gap-2 mb-3">
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
                <div class="w-6 h-6 rounded-full bg-blue-50 flex items-center justify-center">
                  <ion-icon :icon="personOutline" class="text-[10px] text-blue-500" />
                </div>
                <span class="text-xs font-bold text-blue-600">Moi</span>
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
  cameraOutline,
  closeOutline 
} from 'ionicons/icons';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { store } from '../store';

const router = useRouter();
const selectedImageUrl = ref<string | null>(null);

const mySignalements = computed(() => {
  if (!store.user) return [];
  return store.signalements.filter(s => s.email === store.user?.email);
});

const goToMap = (s: any) => {
  router.push('/tabs/map');
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
