<template>
  <ion-page>
    <ion-content :fullscreen="true" class="ion-no-padding">
      <!-- Carte en plein écran -->
      <div id="map" class="absolute inset-0 z-0 h-full w-full"></div>

      <!-- Overlay Gradient pour la lisibilité -->
      <div class="absolute inset-0 pointer-events-none bg-gradient-to-b from-black/10 via-transparent to-black/30 z-10"></div>

      <!-- Header Flottant Moderne -->
      <div class="absolute top-0 left-0 right-0 p-4 z-20 flex flex-col gap-3">
        <div class="bg-white/80 backdrop-blur-xl rounded-2xl shadow-2xl border border-white/50 p-4 flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 bg-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/30">
              <ion-icon :icon="trailSignOutline" class="text-white text-xl" />
            </div>
            <div>
              <h1 class="text-lg font-black text-slate-800 tracking-tight leading-none mb-1">Lalana</h1>
              <div class="flex items-center gap-1.5">
                <span v-if="store.user" class="flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                  <p class="text-[10px] font-bold text-slate-500 truncate max-w-[120px]">{{ store.user.email }}</p>
                </span>
                <span v-else class="flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-slate-300"></span>
                  <p class="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Visiteur</p>
                </span>
              </div>
            </div>
          </div>
          <div class="flex gap-2">
            <button 
              @click="locateUser" 
              :class="isLocating ? 'bg-blue-600 text-white shadow-blue-500/40' : 'bg-slate-50 text-slate-500 shadow-sm border border-slate-100'"
              class="w-10 h-10 rounded-xl flex items-center justify-center active:scale-95 transition-all"
            >
              <ion-icon :icon="locateOutline" :class="isLocating ? 'text-white' : 'text-slate-500'" class="text-xl" />
            </button>
            <button v-if="!store.user" @click="router.push('/login')" class="bg-blue-600 px-4 py-2.5 rounded-xl text-white text-xs font-black shadow-lg shadow-blue-500/20 active:scale-95 transition-all">
              Se connecter
            </button>
            <button v-else @click="handleLogout" class="bg-slate-50 p-2.5 rounded-xl text-slate-400 hover:text-red-500 hover:bg-red-50 transition-all active:scale-95 border border-slate-100">
              <ion-icon :icon="logOutOutline" class="text-xl" />
            </button>
          </div>
        </div>
        
        <!-- Filtres et Actions -->
        <div class="flex gap-2 overflow-x-auto no-scrollbar pb-2">
          <button 
            v-if="store.user"
            @click="filterMine = !filterMine"
            :class="filterMine ? 'bg-blue-600 text-white border-blue-500 shadow-blue-500/30' : 'bg-white/80 text-slate-700 border-white/50'"
            class="backdrop-blur-md px-5 py-2.5 rounded-full shadow-lg border whitespace-nowrap flex items-center gap-2 font-bold text-xs transition-all active:scale-95"
          >
            <ion-icon :icon="personOutline" class="text-sm" /> Mes signalements
          </button>
          <div class="bg-white/80 backdrop-blur-md px-5 py-2.5 rounded-full shadow-lg border border-white/50 whitespace-nowrap flex items-center gap-2">
            <span class="relative flex h-2 w-2">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
            </span>
            <span class="text-xs font-bold text-slate-700 uppercase tracking-wider">{{ filteredSignalements.length }} Signalements</span>
          </div>
        </div>
      </div>


      <!-- Toast de Notification -->
      <ion-toast
        :is-open="showSuccessToast"
        :message="toastMessage"
        :duration="3000"
        :color="toastColor"
        position="top"
        @didDismiss="showSuccessToast = false"
        class="custom-toast"
      ></ion-toast>

    </ion-content>

    <!-- Modal de Signalement Rapide (Style Bottom Sheet) -->
    <div v-if="newSignalementPoint" class="fixed inset-0 z-[9999] flex items-end justify-center bg-black/60 backdrop-blur-md transition-all duration-300">
      <div class="w-full bg-white rounded-t-[40px] shadow-2xl flex flex-col max-h-[95vh] animate-slide-up overflow-hidden pb-safe">
        <!-- Handle de drag (visuel) -->
        <div class="w-full flex justify-center pt-4 pb-2">
          <div class="w-12 h-1.5 bg-slate-200 rounded-full"></div>
        </div>

        <div class="px-6 pb-4 flex items-center justify-between border-b border-slate-50">
          <div>
            <h3 class="text-xl font-black text-slate-800">Nouveau signalement</h3>
            <p class="text-xs font-medium text-slate-400">Position : {{ newSignalementPoint.lat.toFixed(4) }}, {{ newSignalementPoint.lng.toFixed(4) }}</p>
          </div>
          <button @click="newSignalementPoint = null" class="w-10 h-10 bg-slate-50 rounded-full flex items-center justify-center text-slate-400 active:scale-90 transition-all">
            <ion-icon :icon="closeOutline" class="text-2xl" />
          </button>
        </div>
        
        <div class="p-6 space-y-6 overflow-y-auto flex-1 no-scrollbar">
          <!-- Sélection du type de signalement -->
          <div>
            <label class="block text-xs font-black text-slate-800 uppercase tracking-wider mb-4">Quel est le problème ?</label>
            <div class="grid grid-cols-3 gap-3">
              <div 
                v-for="type in typesSignalement" 
                :key="type.id"
                @click="selectedTypeId = type.id"
                :class="[
                  'relative rounded-2xl p-4 flex flex-col items-center gap-2 transition-all border-2 group',
                  selectedTypeId === type.id ? 'border-blue-500 bg-blue-50 shadow-lg shadow-blue-500/10' : 'border-slate-100 bg-white hover:border-slate-200'
                ]"
              >
                <div 
                  class="w-12 h-12 rounded-xl flex items-center justify-center shadow-sm transition-transform group-active:scale-90"
                  :style="{ backgroundColor: type.couleur + (selectedTypeId === type.id ? '30' : '15') }"
                >
                  <svg viewBox="0 0 24 24" class="w-7 h-7" :style="{ fill: type.couleur }">
                    <path :d="type.icone_path" />
                  </svg>
                </div>
                <span :class="[
                  'text-[10px] font-black text-center leading-tight uppercase tracking-tight',
                  selectedTypeId === type.id ? 'text-blue-700' : 'text-slate-500'
                ]">
                  {{ type.nom }}
                </span>
                <div v-if="selectedTypeId === type.id" class="absolute -top-1 -right-1 w-5 h-5 bg-blue-600 rounded-full flex items-center justify-center shadow-lg border-2 border-white">
                  <ion-icon :icon="checkmarkOutline" class="text-white text-[10px]" />
                </div>
              </div>
            </div>
          </div>

          <div>
            <label class="block text-xs font-black text-slate-800 uppercase tracking-wider mb-3">Description (Optionnel)</label>
            <textarea 
              v-model="reportDescription" 
              rows="3" 
              placeholder="Ex: Nid de poule profond, éclairage en panne..." 
              class="w-full px-5 py-4 bg-slate-50 border border-slate-100 rounded-2xl focus:outline-none focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 transition-all text-sm font-medium text-slate-700 resize-none"
            ></textarea>
          </div>

          <div>
            <div class="flex justify-between items-center mb-3">
              <label class="block text-xs font-black text-slate-800 uppercase tracking-wider">Preuve photo</label>
              <span v-if="reportPhotos.length > 0" class="text-[10px] font-bold text-slate-400">{{ reportPhotos.length }}/5</span>
            </div>
            
            <div class="grid grid-cols-3 gap-3">
              <!-- Miniatures existantes -->
              <div v-for="(photo, index) in reportPhotos" :key="index" class="relative aspect-square rounded-2xl overflow-hidden group border border-slate-100 shadow-sm animate-in zoom-in duration-200">
                <img :src="photo" class="w-full h-full object-cover">
                <button @click="removePhoto(index)" class="absolute top-1.5 right-1.5 z-10 bg-red-500 text-white w-7 h-7 rounded-full flex items-center justify-center shadow-lg active:scale-90 transition-transform">
                  <ion-icon :icon="closeOutline" class="text-lg" />
                </button>
              </div>
              
              <!-- Bouton d'ajout principal -->
              <div 
                v-if="reportPhotos.length < 5"
                @click="showPhotoActionSheet = true" 
                class="aspect-square bg-blue-50 border-2 border-dashed border-blue-200 rounded-2xl flex flex-col items-center justify-center text-blue-600 cursor-pointer active:bg-blue-100 transition-colors"
              >
                <ion-icon :icon="cameraOutline" class="text-2xl mb-1" />
                <span class="text-[9px] font-black uppercase tracking-tighter">Ajouter</span>
              </div>
            </div>
          </div>
        </div>

        <div class="p-6 bg-white border-t border-slate-50 flex gap-4">
          <button @click="newSignalementPoint = null" class="flex-1 py-4 bg-slate-50 text-slate-500 font-black rounded-2xl active:scale-95 transition-all text-sm uppercase tracking-wider">Annuler</button>
          <button 
            @click="submitReport" 
            :disabled="isSubmitting || !selectedTypeId" 
            :class="[
              'flex-[2] py-4 font-black rounded-2xl shadow-xl transition-all flex items-center justify-center text-sm uppercase tracking-widest',
              isSubmitting || !selectedTypeId ? 'bg-slate-200 text-slate-400 shadow-none' : 'bg-blue-600 text-white shadow-blue-500/30 active:scale-95'
            ]"
          >
            <span v-if="isSubmitting" class="animate-spin mr-3">
              <ion-icon :icon="syncOutline" class="text-xl" />
            </span>
            <span v-else class="mr-2">
              <ion-icon :icon="sendOutline" />
            </span>
            {{ isSubmitting ? 'Envoi...' : 'Signaler' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Action Sheet pour Photos -->
    <div v-if="showPhotoActionSheet" class="fixed inset-0 z-[10000] flex items-end justify-center bg-black/60 backdrop-blur-sm px-4 pb-6 transition-all duration-300" @click.self="showPhotoActionSheet = false">
      <div class="w-full max-w-sm space-y-3 animate-slide-up">
        <div class="bg-white rounded-3xl overflow-hidden shadow-2xl">
          <button @click="takePhoto(); showPhotoActionSheet = false" class="w-full py-5 px-6 flex items-center gap-4 text-slate-700 active:bg-slate-50 border-b border-slate-50">
            <div class="w-10 h-10 bg-blue-50 rounded-xl flex items-center justify-center text-blue-600">
              <ion-icon :icon="cameraOutline" class="text-xl" />
            </div>
            <span class="font-bold text-sm">Prendre une photo</span>
          </button>
          <button @click="triggerFileSelect(); showPhotoActionSheet = false" class="w-full py-5 px-6 flex items-center gap-4 text-slate-700 active:bg-slate-50">
            <div class="w-10 h-10 bg-indigo-50 rounded-xl flex items-center justify-center text-indigo-600">
              <ion-icon :icon="imageOutline" class="text-xl" />
            </div>
            <span class="font-bold text-sm">Choisir dans la galerie</span>
          </button>
        </div>
        <button @click="showPhotoActionSheet = false" class="w-full py-5 bg-white text-slate-500 font-black rounded-3xl shadow-xl active:scale-95 transition-all text-sm uppercase tracking-widest">
          Annuler
        </button>
      </div>
    </div>

    <input 
      type="file" 
      ref="fileInputRef" 
      multiple 
      accept="image/*" 
      class="hidden" 
      @change="handleFileSelect"
    >
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import { IonPage, IonContent, IonIcon, IonToast } from '@ionic/vue';

interface TypeSignalement {
  id: number;
  nom: string;
  description: string;
  icone_path: string;
  couleur: string;
}

import { Camera, CameraResultType, CameraSource } from '@capacitor/camera';
import { 
  logOutOutline, 
  personOutline, 
  constructOutline, 
  closeOutline, 
  sendOutline, 
  trailSignOutline,
  eyeOutline,
  locationOutline,
  locateOutline,
  chevronBackOutline,
  chevronForwardOutline,
  cameraOutline,
  trashOutline,
  addOutline,
  listOutline,
  checkmarkOutline,
  imageOutline,
  syncOutline
} from 'ionicons/icons';
import * as L from 'leaflet';
import { db } from '../firebase/config';
import { 
  collection, 
  addDoc, 
  serverTimestamp, 
  query, 
  where, 
  getDocs,
  doc,
  getDoc,
  updateDoc
} from 'firebase/firestore';
import { store, setUser } from '../store';
import { notificationService } from '../services/notificationService';
import { auth } from '../firebase/config';
import { signInWithEmailAndPassword } from 'firebase/auth';

const router = useRouter();

// UI State
const filterMine = ref(false);
const showPhotoActionSheet = ref(false);

// Signalement State
const newSignalementPoint = ref<{lat: number, lng: number} | null>(null);
const reportDescription = ref('');
const reportPhotos = ref<string[]>([]);
const fileInputRef = ref<HTMLInputElement | null>(null);
const selectedTypeId = ref<number | null>(null);
const typesSignalement = ref<TypeSignalement[]>([]);
const isSubmitting = ref(false);
const isLocating = ref(false);
const showSuccessToast = ref(false);
const toastMessage = ref('');
const toastColor = ref('success');
const selectedSignalementId = ref<string | null>(null);

// Methods
const startNewReport = () => {
  if (!store.user) {
    router.push('/login');
    return;
  }
  
  if (isLocating.value && map) {
    const center = map.getCenter();
    newSignalementPoint.value = { lat: center.lat, lng: center.lng };
  } else if (map) {
    // Si pas de localisation, on prend le centre de la carte
    const center = map.getCenter();
    newSignalementPoint.value = { lat: center.lat, lng: center.lng };
    
    toastMessage.value = "Position fixée au centre de la carte";
    toastColor.value = "primary";
    showSuccessToast.value = true;
  }
};

let map: L.Map | null = null;
const markersMap = new Map<string, L.Marker>();
let userLocationMarker: L.LayerGroup | null = null;

const filteredSignalements = computed(() => {
  if (filterMine.value && store.user) {
    return store.signalements.filter(s => 
      (s.firebase_uid_utilisateur && s.firebase_uid_utilisateur === store.user?.firebaseUid) || 
      (!s.firebase_uid_utilisateur && s.email === store.user?.email)
    );
  }
  return store.signalements;
});

// Photo Methods
const triggerFileSelect = () => {
  fileInputRef.value?.click();
};

const handleFileSelect = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (!input.files) return;

  const files = Array.from(input.files);
  files.forEach(file => {
    const reader = new FileReader();
    reader.onload = async (e) => {
      if (e.target?.result) {
        const resized = await resizeImage(e.target.result as string);
        reportPhotos.value.push(resized);
      }
    };
    reader.readAsDataURL(file);
  });
  
  // Reset input value to allow selecting same files again
  input.value = '';
};

const takePhoto = async () => {
  console.log('Tentative de prise de photo...');
  try {
    const photo = await Camera.getPhoto({
      quality: 60,
      allowEditing: false,
      resultType: CameraResultType.Base64,
      source: CameraSource.Camera
    });
    
    if (photo.base64String) {
      const mimeType = photo.format === 'png' ? 'image/png' : 'image/jpeg';
      const base64 = `data:${mimeType};base64,${photo.base64String}`;
      const resized = await resizeImage(base64);
      reportPhotos.value.push(resized);
    }
  } catch (e) {
    console.error('Erreur Camera:', e);
  }
};

const removePhoto = (index: number) => {
  reportPhotos.value.splice(index, 1);
};

// Fonction utilitaire pour redimensionner une image base64
const resizeImage = (base64: string, maxWidth = 800, maxHeight = 800, quality = 0.6): Promise<string> => {
  return new Promise((resolve) => {
    const img = new Image();
    img.src = base64;
    img.onload = () => {
      let width = img.width;
      let height = img.height;

      if (width > height) {
        if (width > maxWidth) {
          height *= maxWidth / width;
          width = maxWidth;
        }
      } else {
        if (height > maxHeight) {
          width *= maxHeight / height;
          height = maxHeight;
        }
      }

      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      ctx?.drawImage(img, 0, 0, width, height);
      resolve(canvas.toDataURL('image/jpeg', quality));
    };
  });
};

const handleLogout = async () => {
  setUser(null);
  sessionStorage.removeItem('app_user');
  filterMine.value = false;
  
  // Nettoyer les notifications
  notificationService.cleanup();
  
  // Se déconnecter de Firebase Auth si nécessaire
  try {
    await auth.signOut();
    router.replace('/login');
  } catch (error) {
    console.warn('⚠️ Erreur lors de la déconnexion Firebase Auth:', error);
  }
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
      router.push('/login');
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
  if (!selectedTypeId.value) {
    alert("Veuillez sélectionner un type de signalement");
    return;
  }
  
  isSubmitting.value = true;
  try {
    // Créer une copie simple des données pour éviter les proxys Vue
    // Firestore n'aime pas les objets réactifs complexes
    const galerieData = reportPhotos.value.map(url => ({ 
      url: String(url) 
    }));

    const reportData = {
      latitude: Number(newSignalementPoint.value.lat),
      longitude: Number(newSignalementPoint.value.lng),
      description: String(reportDescription.value || ""),
      galerie: galerieData,
      firebase_uid_utilisateur: store.user.firebaseUid || null,
      email_utilisateur: String(store.user.email),
      id_type_signalement: Number(selectedTypeId.value),
      statut: 'nouveau',
      dateSignalement: new Date().toISOString(),
      createdAt: serverTimestamp()
    };

    console.log('Envoi du signalement avec', galerieData.length, 'photos');

    const docRef = await addDoc(collection(db, 'signalements'), reportData);
    console.log('Signalement ajouté avec ID:', docRef.id);

    // Reset form and close modal
    const pointToReset = newSignalementPoint.value;
    newSignalementPoint.value = null;
    reportDescription.value = '';
    reportPhotos.value = [];
    selectedTypeId.value = null;
    
    // Afficher toast et fermer
    toastMessage.value = 'Signalement envoyé avec succès !';
    toastColor.value = 'success';
    showSuccessToast.value = true;
  } catch (err: any) {
    console.error('Erreur lors de l\'envoi:', err);
    toastColor.value = 'danger';
    if (err.message?.includes('too large')) {
      toastMessage.value = 'Erreur : Le signalement est trop lourd.';
    } else {
      toastMessage.value = 'Erreur lors de l\'envoi du signalement';
    }
    showSuccessToast.value = true;
  } finally {
    isSubmitting.value = false;
  }
};

const locateUser = () => {
  if (!map) return;
  
  if (isLocating.value) {
    // Dézoomer en état normal (Tana par défaut)
    const tana = { lat: -18.8792, lng: 47.5079 };
    map.setView([tana.lat, tana.lng], 13);
    
    // Nettoyer le marqueur
    if (userLocationMarker) {
      map.removeLayer(userLocationMarker);
      userLocationMarker = null;
    }
    isLocating.value = false;
    return;
  }
  
  if ("geolocation" in navigator) {
    isLocating.value = true;
    navigator.geolocation.getCurrentPosition((position) => {
      const { latitude, longitude } = position.coords;
      
      // Zoomer sur la position
      map?.setView([latitude, longitude], 16);
      
      // Nettoyer l'ancien marqueur s'il existe
      if (userLocationMarker) {
        map?.removeLayer(userLocationMarker);
      }
      
      // Créer un nouveau groupe pour le marqueur de position
      userLocationMarker = L.layerGroup().addTo(map!);
      
      // Ajouter un cercle d'incertitude
      L.circle([latitude, longitude], {
        color: '#3b82f6',
        fillColor: '#3b82f6',
        fillOpacity: 0.15,
        radius: 100,
        weight: 1
      }).addTo(userLocationMarker);
      
      // Ajouter le point bleu
      L.circleMarker([latitude, longitude], {
        radius: 8,
        fillColor: "#3b82f6",
        color: "#fff",
        weight: 3,
        opacity: 1,
        fillOpacity: 1
      }).addTo(userLocationMarker);
      
    }, (error) => {
      isLocating.value = false;
      console.error("Erreur de géolocalisation:", error);
      let message = "Impossible de vous localiser";
      if (error.code === error.PERMISSION_DENIED) {
        message = "Permission de géolocalisation refusée";
      }
      alert(message);
    }, {
      enableHighAccuracy: true,
      timeout: 5000,
      maximumAge: 0
    });
  } else {
    alert("La géolocalisation n'est pas supportée par votre appareil");
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

const getStatusColor = (status: string): string => {
  const st = String(status || '').toLowerCase();
  if (st.includes('nouveau')) return '#3b82f6';
  if (st.includes('cours')) return '#f59e0b';
  if (st.includes('termin')) return '#10b981';
  return '#64748b';
};

const darkenColor = (color: string): string => {
  return color === '#3b82f6' ? '#1d4ed8' : 
         color === '#f59e0b' ? '#b45309' : 
         color === '#10b981' ? '#047857' : '#334155';
};

const createCustomIcon = (s: any, isSelected: boolean = false): L.DivIcon => {
  const status = String(s.statut || 'nouveau').toLowerCase();
  const typeColor = s.type_couleur || getStatusColor(status);
  let innerIcon = '';

  // Utiliser l'icône du type si elle existe
  if (s.type_icone_path) {
    innerIcon = `
      <g transform="translate(7.5, 5.5) scale(0.4)">
        <path d="${s.type_icone_path}" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
      </g>
    `;
  } else {
    // Icône par défaut selon le statut
    if (status.includes('nouveau')) {
      innerIcon = `<g transform="translate(7.5, 5.5) scale(0.4)"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"/></g>`;
    } else if (status.includes('cours')) {
      innerIcon = `<g transform="translate(7.5, 5.5) scale(0.4)"><path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" fill="none"/><circle cx="12" cy="12" r="3" stroke="white" stroke-width="3" fill="none"/></g>`;
    } else {
      innerIcon = `<g transform="translate(7.5, 5.5) scale(0.4)"><path d="M5 13l4 4L19 7" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/></g>`;
    }
  }

  // Badge de statut
  let badgeHtml = '';
  let strokeColor = 'white';
  let strokeWidth = '1.5';

  if (status.includes('termin')) {
    strokeColor = '#10b981';
    strokeWidth = '2.5';
    badgeHtml = `
      <div class="absolute -top-1 -right-1 w-4 h-4 bg-emerald-500 rounded-full border-2 border-white flex items-center justify-center shadow-lg z-10">
        <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="4" d="M5 13l4 4L19 7" />
        </svg>
      </div>
    `;
  } else if (status.includes('cours')) {
    strokeColor = '#f59e0b';
    strokeWidth = '2.5';
    badgeHtml = `
      <div class="absolute -top-1 -right-1 w-4 h-4 bg-amber-500 rounded-full border-2 border-white flex items-center justify-center shadow-lg z-10">
        <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
        </svg>
      </div>
    `;
  } else {
    badgeHtml = `
      <div class="absolute -top-1 -right-1 w-4 h-4 bg-blue-500 rounded-full border-2 border-white flex items-center justify-center shadow-lg z-10">
        <div class="w-1 h-1 bg-white rounded-full"></div>
      </div>
    `;
  }

  return L.divIcon({
    className: `custom-div-icon ${isSelected ? 'marker-selected' : ''}`,
    html: `
      <div class="marker-container relative flex items-center justify-center">
        ${isSelected ? `<div class="ping-animation absolute w-10 h-10 rounded-full opacity-20 animate-ping" style="background-color: ${typeColor}"></div>` : ''}
        ${badgeHtml}
        <svg class="pin-svg relative w-8 h-8 drop-shadow-2xl transition-all duration-300" viewBox="0 0 24 24" fill="none">
          <defs>
            <linearGradient id="grad-${typeColor.replace('#','')}" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" style="stop-color:${typeColor};stop-opacity:1" />
              <stop offset="100%" style="stop-color:${darkenColor(typeColor)};stop-opacity:1" />
            </linearGradient>
          </defs>
          <path d="M12 21C15.5 17.4 19 14.1764 19 10.2C19 6.22355 15.866 3 12 3C8.13401 3 5 6.22355 5 10.2C5 14.1764 8.5 17.4 12 21Z" 
                fill="url(#grad-${typeColor.replace('#','')})" 
                stroke="${strokeColor}" 
                stroke-width="${strokeWidth}"/>
          ${innerIcon}
        </svg>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 32],
    popupAnchor: [0, -32]
  });
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
      const m = L.marker([s.latitude, s.longitude], {
        icon: createCustomIcon(s, selectedSignalementId.value === s.id)
      })
      .on('click', () => {
        selectedSignalementId.value = s.id;
        // Décaler légèrement vers le haut pour que la popup ne soit pas cachée par le header
        const targetPoint = map!.project([s.latitude, s.longitude], 16);
        targetPoint.y -= 150; // Décalage de 150 pixels vers le haut
        const targetLatLng = map!.unproject(targetPoint, 16);
        
        map?.flyTo(targetLatLng, 16, { duration: 1 });
      })
      .addTo(map!)
      .bindPopup(`
        <div class="popup-container p-0 overflow-hidden font-sans">
          <!-- Image Header avec Carrousel -->
          <div class="relative h-40 w-full bg-slate-100 overflow-hidden">
            ${s.galerie && s.galerie.length > 0 
              ? `
                <div class="carousel-container flex overflow-x-auto snap-x snap-mandatory no-scrollbar h-full w-full">
                  ${s.galerie.map((img: any, idx: number) => `
                    <div class="carousel-item flex-shrink-0 w-full h-full snap-center relative">
                      <img src="${img.url}" class="w-full h-full object-cover">
                      ${s.galerie.length > 1 ? `
                        <div class="absolute bottom-2 left-1/2 -translate-x-1/2 flex gap-1 z-10">
                          ${s.galerie.map((_: any, i: number) => `
                            <div class="w-1.5 h-1.5 rounded-full ${i === idx ? 'bg-white shadow-sm' : 'bg-white/40'}"></div>
                          `).join('')}
                        </div>
                      ` : ''}
                    </div>
                  `).join('')}
                </div>
                ${s.galerie.length > 1 ? `
                  <div class="absolute top-1/2 -translate-y-1/2 left-2 z-20 w-6 h-6 bg-black/30 backdrop-blur-sm rounded-full flex items-center justify-center text-white text-[10px]">
                    <ion-icon name="chevron-back-outline"></ion-icon>
                  </div>
                  <div class="absolute top-1/2 -translate-y-1/2 right-2 z-20 w-6 h-6 bg-black/30 backdrop-blur-sm rounded-full flex items-center justify-center text-white text-[10px]">
                    <ion-icon name="chevron-forward-outline"></ion-icon>
                  </div>
                ` : ''}
              `
              : `<div class="w-full h-full flex flex-col items-center justify-center text-slate-300">
                   <ion-icon name="image-outline" class="text-3xl mb-1"></ion-icon>
                   <span class="text-[9px] font-black uppercase tracking-widest">Pas d'image</span>
                 </div>`
            }
            <div class="absolute top-2 left-2 px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-widest text-white shadow-lg z-20" 
                 style="background-color: ${getStatusColor(s.statut)}">
              ${s.statut}
            </div>
            ${s.galerie && s.galerie.length > 1 
              ? `<div class="absolute bottom-2 right-2 px-2 py-1 rounded-lg bg-black/60 backdrop-blur-md text-white text-[9px] font-black z-20">${s.galerie.length} photos</div>` 
              : ''}
          </div>

          <!-- Content -->
          <div class="p-4 space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-[9px] font-black text-blue-600 uppercase tracking-widest">${s.type_nom || 'Signalement'}</span>
              <span class="text-[9px] font-bold text-slate-400 uppercase tracking-tight">${formatDate(s.dateSignalement)}</span>
            </div>

            <h3 class="font-bold text-slate-800 text-sm leading-snug line-clamp-2">${s.description || 'Sans description'}</h3>
            
            <div class="flex flex-wrap gap-2 pt-3 border-t border-slate-50">
              ${s.surface_m2 ? `
                <div class="flex-1 min-w-[80px] bg-slate-50 p-2 rounded-xl border border-slate-100">
                  <span class="text-[7px] font-black text-slate-400 uppercase block leading-none mb-1">Surface</span>
                  <span class="text-[10px] font-bold text-slate-700">${s.surface_m2} m²</span>
                </div>
              ` : ''}
              ${s.niveau ? `
                <div class="flex-1 min-w-[80px] bg-orange-50 p-2 rounded-xl border border-orange-100">
                  <span class="text-[7px] font-black text-orange-400 uppercase block leading-none mb-1">Niveau</span>
                  <span class="text-[10px] font-bold text-orange-700">${s.niveau}</span>
                </div>
              ` : ''}
              ${s.budget ? `
                <div class="flex-1 min-w-[80px] bg-emerald-50 p-2 rounded-xl border border-emerald-100">
                  <span class="text-[7px] font-black text-emerald-400 uppercase block leading-none mb-1">Budget</span>
                  <span class="text-[10px] font-bold text-emerald-700">${new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'MGA', maximumFractionDigits: 0 }).format(s.budget)}</span>
                </div>
              ` : ''}
            </div>

            ${s.entreprise ? `
              <div class="bg-blue-50 p-2.5 rounded-xl border border-blue-100 flex items-center gap-2">
                <div class="w-6 h-6 rounded-lg bg-blue-600 flex items-center justify-center text-white">
                  <ion-icon name="business-outline" class="text-[12px]"></ion-icon>
                </div>
                <div>
                  <span class="text-[7px] font-black text-blue-400 uppercase block leading-none">Entreprise</span>
                  <span class="text-[10px] font-bold text-blue-700 truncate block max-w-[160px]">${s.entreprise}</span>
                </div>
              </div>
            ` : ''}
          </div>
        </div>
      `, { className: 'custom-leaflet-popup', maxWidth: 280 });

      markersMap.set(s.id, m);
    }
  });
};

watch(() => selectedSignalementId.value, () => {
  updateMarkers();
});

watch(() => store.signalements, () => {
  updateMarkers();
}, { deep: true });

watch(() => filterMine.value, () => {
  updateMarkers();
});

const fetchTypesSignalement = async () => {
  try {
    const q = query(collection(db, 'types_signalement'));
    const querySnapshot = await getDocs(q);
    typesSignalement.value = querySnapshot.docs.map(doc => ({
      ...doc.data()
    } as TypeSignalement)).sort((a, b) => a.id - b.id);
  } catch (err) {
    console.error('Erreur lors de la récupération des types:', err);
  }
};

const carouselRef = ref<HTMLElement | null>(null);
const scrollCarousel = (direction: 'left' | 'right') => {
  if (!carouselRef.value) return;
  const scrollAmount = 150;
  carouselRef.value.scrollBy({
    left: direction === 'left' ? -scrollAmount : scrollAmount,
    behavior: 'smooth'
  });
};

onMounted(() => {
  setTimeout(() => {
    initMap();
    updateMarkers(); // Initial render if data already exists
    fetchTypesSignalement();
  }, 100);
});
</script>

<style>
/* Animations */
@keyframes slide-up {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.animate-slide-up {
  animation: slide-up 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* Leaflet Popup Styling Customization */
.leaflet-popup-content-wrapper {
  padding: 0;
  overflow: hidden;
  border-radius: 24px !important;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25) !important;
  border: none !important;
}

.leaflet-popup-content {
  margin: 0 !important;
  width: 280px !important;
}

.leaflet-popup-tip-container {
  display: none;
}

/* Marker Animations */
.ping-animation {
  animation: marker-ping 2s cubic-bezier(0, 0, 0.2, 1) infinite;
}

@keyframes marker-ping {
  75%, 100% {
    transform: scale(2);
    opacity: 0;
  }
}

.custom-div-icon {
  background: none !important;
  border: none !important;
}

.marker-selected .pin-svg {
  transform: scale(1.2);
  filter: drop-shadow(0 0 10px rgba(59, 130, 246, 0.5));
}

/* Ionic Toast Customization */
  .custom-toast {
    --background: rgba(255, 255, 255, 0.9);
    --backdrop-filter: blur(10px);
    --color: #1e293b;
    --button-color: #3b82f6;
    --border-radius: 20px;
    --box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
    font-weight: 700;
  }

  /* Carousel Scroll Snap */
  .carousel-container {
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }
  .carousel-container::-webkit-scrollbar {
    display: none;
  }
  .carousel-item {
    scroll-snap-align: center;
  }
</style>
