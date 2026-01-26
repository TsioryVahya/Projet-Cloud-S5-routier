<template>
  <ion-page>
    <!-- Overlay de Connexion (Modale) -->
    <div v-if="showLoginModal" class="absolute inset-0 z-[100] bg-slate-900/40 backdrop-blur-md flex items-center justify-center p-6">
      <div class="w-full max-w-md bg-white rounded-3xl shadow-2xl p-8 border border-slate-100 relative animate-in zoom-in duration-200">
        <button @click="showLoginModal = false" class="absolute top-4 right-4 w-8 h-8 flex items-center justify-center bg-slate-100 rounded-full text-slate-400 hover:text-slate-600">
          ✕
        </button>

        <div class="text-center mb-8">
          <div class="w-20 h-20 bg-blue-600 rounded-2xl shadow-xl shadow-blue-200 flex items-center justify-center mx-auto mb-4">
            <ion-icon :icon="trailSignOutline" class="text-4xl text-white" />
          </div>
          <h2 class="text-2xl font-bold text-slate-800">Se connecter</h2>
          <p class="text-slate-500 text-sm mt-1">Accès réservé aux agents</p>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5 ml-1">Email</label>
            <input v-model="loginEmail" type="email" placeholder="votre@email.com" class="w-full px-5 py-4 bg-slate-50 border border-slate-100 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all text-slate-700">
          </div>
          <div>
            <label class="block text-xs font-bold text-slate-400 uppercase tracking-wider mb-1.5 ml-1">Mot de passe</label>
            <input v-model="loginPassword" type="password" placeholder="••••••••" class="w-full px-5 py-4 bg-slate-50 border border-slate-100 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all text-slate-700">
          </div>
          
          <p v-if="authError" class="text-red-500 text-xs font-medium bg-red-50 p-3 rounded-xl border border-red-100">
            {{ authError }}
          </p>

          <button @click="handleLogin" :disabled="isAuthLoading" class="w-full py-4 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white rounded-2xl font-bold shadow-lg shadow-blue-500/30 transition-all transform active:scale-[0.98] flex items-center justify-center">
            <span v-if="isAuthLoading" class="animate-spin mr-2">
              <ion-icon :icon="constructOutline" />
            </span>
            {{ isAuthLoading ? 'Connexion...' : 'Se connecter' }}
          </button>
        </div>
        
        <p class="text-center text-[10px] text-slate-400 mt-8 leading-relaxed uppercase tracking-widest font-bold">
          L'inscription se fait uniquement via le Manager sur l'application Web
        </p>
      </div>
    </div>

    <ion-content :fullscreen="true" class="ion-no-padding">
      <!-- Carte en plein écran -->
      <div id="map" class="absolute inset-0 z-0 h-full w-full"></div>

      <!-- Overlay Gradient pour la lisibilité -->
      <div class="absolute inset-0 pointer-events-none bg-gradient-to-b from-black/10 via-transparent to-black/30 z-10"></div>

      <!-- Header Flottant Moderne -->
      <div class="absolute top-0 left-0 right-0 p-4 z-20 flex flex-col gap-3">
        <div class="bg-white/20 backdrop-blur-md rounded-2xl shadow-xl border border-white/20 p-4 flex items-center justify-between">
          <div>
            <h1 class="text-xl font-bold text-slate-800 tracking-tight">Signalement Routier</h1>
            <p v-if="store.user" class="text-[10px] font-bold text-blue-600 uppercase tracking-widest">{{ store.user.email }}</p>
            <p v-else class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Mode Visiteur</p>
          </div>
          <div class="flex gap-2">
            <button v-if="!store.user" @click="showLoginModal = true" class="bg-blue-600 px-4 py-2 rounded-xl text-white text-xs font-bold shadow-lg shadow-blue-500/20 active:scale-95 transition-all">
              Se connecter
            </button>
            <button v-else @click="handleLogout" class="bg-red-50 p-2.5 rounded-xl text-red-500 hover:bg-red-100 transition-colors active:scale-95 border border-red-100 shadow-sm">
              <ion-icon :icon="logOutOutline" class="text-xl" />
            </button>
          </div>
        </div>
        
        <!-- Filtres et Actions -->
        <div class="flex gap-2 overflow-x-auto no-scrollbar pb-2">
          <button 
            v-if="store.user"
            @click="filterMine = !filterMine"
            :class="filterMine ? 'bg-blue-600 text-white shadow-blue-500/30' : 'bg-white/30 text-slate-700 border-white/20'"
            class="backdrop-blur-md px-4 py-2 rounded-full shadow-lg border whitespace-nowrap flex items-center gap-2 font-bold text-sm transition-all"
          >
            <ion-icon :icon="personOutline" /> Mes signalements
          </button>
          <div class="bg-white/30 backdrop-blur-md px-4 py-2 rounded-full shadow-lg border border-white/20 whitespace-nowrap flex items-center gap-2">
            <span class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></span>
            <span class="text-sm font-semibold text-slate-700">{{ filteredSignalements.length }} Signalements</span>
          </div>
        </div>
      </div>

      <!-- Instructions pour signaler (Visiteur uniquement) -->
      <div v-if="!newSignalementPoint && !store.user" class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 pointer-events-none z-10 text-center">
        <div class="bg-blue-600/90 backdrop-blur-sm text-white px-6 py-3 rounded-full text-xs font-bold shadow-2xl animate-bounce">
          Connectez-vous pour signaler
        </div>
      </div>

      <!-- Modal de Signalement Rapide -->
      <div v-if="newSignalementPoint" class="absolute inset-0 z-50 flex items-center justify-center p-6 bg-black/40 backdrop-blur-sm">
        <div class="w-full max-w-sm bg-white rounded-3xl shadow-2xl overflow-hidden animate-in zoom-in duration-200">
          <div class="bg-blue-600 p-6 text-white text-center">
            <div class="text-4xl mb-2 flex justify-center">
              <ion-icon :icon="constructOutline" />
            </div>
            <h3 class="text-xl font-bold">Signaler un problème</h3>
            <p class="text-blue-100 text-xs">Aidez-nous à améliorer les routes</p>
          </div>
          <div class="p-6 space-y-4">
            <div>
              <label class="block text-xs font-bold text-slate-400 uppercase mb-2 ml-1">Description</label>
              <textarea v-model="reportDescription" rows="2" placeholder="Ex: Nid de poule profond, route inondée..." class="w-full px-4 py-3 bg-slate-50 border border-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 text-slate-700 text-sm resize-none"></textarea>
            </div>
            <div>
              <label class="block text-xs font-bold text-slate-400 uppercase mb-2 ml-1">Photo URL (Optionnel)</label>
              <input v-model="reportPhotoUrl" type="text" placeholder="https://..." class="w-full px-4 py-3 bg-slate-50 border border-slate-100 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 text-slate-700 text-sm">
            </div>
            <div class="flex gap-3">
              <button @click="newSignalementPoint = null" class="flex-1 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl active:scale-95 transition-all">Annuler</button>
              <button @click="submitReport" :disabled="isSubmitting" class="flex-1 py-3 bg-blue-600 text-white font-bold rounded-xl shadow-lg shadow-blue-500/20 active:scale-95 transition-all flex items-center justify-center">
                <span v-if="isSubmitting" class="animate-spin mr-2">
                  <ion-icon :icon="sendOutline" />
                </span>
                {{ isSubmitting ? 'Envoi...' : 'Envoyer' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { IonPage, IonContent, IonIcon } from '@ionic/vue';
import { 
  logOutOutline, 
  personOutline, 
  constructOutline, 
  closeOutline, 
  sendOutline, 
  trailSignOutline,
  eyeOutline,
  locationOutline
} from 'ionicons/icons';
import * as L from 'leaflet';
import { db, auth } from '../firebase/config';
import { collection, addDoc, serverTimestamp } from 'firebase/firestore';
import { signInWithEmailAndPassword, signOut } from 'firebase/auth';
import { store } from '../store';

// UI State
const showLoginModal = ref(false);
const filterMine = ref(false);

// Auth State (Local for login form)
const loginEmail = ref('');
const loginPassword = ref('');
const authError = ref('');
const isAuthLoading = ref(false);

// Signalement State
const newSignalementPoint = ref<{lat: number, lng: number} | null>(null);
const reportDescription = ref('');
const isSubmitting = ref(false);

let map: L.Map | null = null;
const markersMap = new Map<string, L.Marker>();

const filteredSignalements = computed(() => {
  if (filterMine.value && store.user) {
    return store.signalements.filter(s => s.email === store.user?.email);
  }
  return store.signalements;
});

// Auth Methods
const handleLogin = async () => {
  if (!loginEmail.value || !loginPassword.value) {
    authError.value = "Veuillez remplir tous les champs";
    return;
  }
  isAuthLoading.value = true;
  authError.value = "";
  try {
    await signInWithEmailAndPassword(auth, loginEmail.value, loginPassword.value);
    showLoginModal.value = false;
    loginEmail.value = '';
    loginPassword.value = '';
  } catch (err: any) {
    authError.value = "Email ou mot de passe incorrect";
  } finally {
    isAuthLoading.value = false;
  }
};

const handleLogout = () => {
  signOut(auth);
  filterMine.value = false;
};

// Map & Signalement Methods
const initMap = () => {
  if (map) return;
  
  const tana = { lat: -18.8792, lng: 47.5079 };
  map = L.map('map', {
    zoomControl: false,
    attributionControl: false
  }).setView([tana.lat, tana.lng], 13);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 19,
  }).addTo(map);

  map.on('click', (e: L.LeafletMouseEvent) => {
    if (store.user) {
      newSignalementPoint.value = { lat: e.latlng.lat, lng: e.latlng.lng };
    } else {
      showLoginModal.value = true;
    }
  });

  const iconDefault = L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41]
  });
  L.Marker.prototype.options.icon = iconDefault;

  setTimeout(() => map?.invalidateSize(), 500);
};

const submitReport = async () => {
  if (!newSignalementPoint.value || !store.user) return;
  
  isSubmitting.value = true;
  try {
    await addDoc(collection(db, 'signalements'), {
      latitude: newSignalementPoint.value.lat,
      longitude: newSignalementPoint.value.lng,
      description: reportDescription.value,
      email: store.user.email,
      statut: 'Nouveau',
      dateSignalement: new Date().toISOString(),
      createdAt: serverTimestamp()
    });

    // Backend Sync (Try-catch to not block UI)
    try {
      await fetch('http://localhost:8081/api/signalements', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          latitude: newSignalementPoint.value.lat,
          longitude: newSignalementPoint.value.lng,
          description: reportDescription.value,
          email: store.user.email,
          statutId: 'STATUT_NOUVEAU'
        })
      });
    } catch (e) { console.warn("Sync backend failed"); }

    newSignalementPoint.value = null;
    reportDescription.value = '';
  } catch (err) {
    console.error(err);
  } finally {
    isSubmitting.value = false;
  }
};

const getStatusTextColor = (status: string) => {
  switch(status?.toLowerCase()) {
    case 'nouveau': return '#2563EB'; // text-blue-700
    case 'en cours':
    case 'en_cours': return '#B45309'; // text-amber-700
    case 'terminé':
    case 'termine': return '#047857'; // text-emerald-700
    default: return '#334155'; // text-slate-700
  }
};

const formatDate = (dateString: string) => {
  if (!dateString) return '';
  try {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'short' }).format(date);
  } catch (e) { return ''; }
};

const updateMarkers = () => {
  if (!map) return;
  
  markersMap.forEach((marker) => marker.remove());
  markersMap.clear();

  filteredSignalements.value.forEach(s => {
    if (s.latitude && s.longitude) {
      const m = L.marker([s.latitude, s.longitude])
        .addTo(map!)
        .bindPopup(`
          <div class="p-2 min-w-[120px]">
            <div class="text-[10px] font-bold uppercase mb-1" style="color: ${getStatusTextColor(s.statut)}">${s.statut}</div>
            <div class="font-bold text-slate-800 text-sm">${s.description || 'Signalement'}</div>
            <div class="text-[10px] text-slate-400 mt-1 flex justify-between">
              <span>${formatDate(s.dateSignalement)}</span>
              <span>${s.email === store.user?.email ? 'Moi' : ''}</span>
            </div>
          </div>
        `);
      markersMap.set(s.id, m);
    }
  });
};

watch(() => store.signalements, () => {
  updateMarkers();
}, { deep: true });

watch(() => filterMine.value, () => {
  updateMarkers();
});

onMounted(() => {
  setTimeout(() => {
    initMap();
    updateMarkers(); // Initial render if data already exists
  }, 100);
});
</script>

<style>
/* Leaflet Popup Styling */
.leaflet-popup-content-wrapper {
  border-radius: 16px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0,0,0,0.05);
}
.leaflet-popup-tip {
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}
</style>
