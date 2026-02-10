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


      <!-- Bouton GPS -->
      <div class="absolute bottom-24 right-4 z-20">
        <button 
          @click="locateUser" 
          :class="isLocating ? 'bg-blue-600 text-white' : 'bg-white text-blue-600'"
          class="w-11 h-11 rounded-xl shadow-2xl flex items-center justify-center active:scale-95 transition-all border border-slate-100"
        >
          <ion-icon :icon="locateOutline" class="text-xl" />
        </button>
      </div>

      <!-- Modal de Signalement Rapide -->
      <div v-if="newSignalementPoint" class="absolute inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
        <div class="w-full max-w-sm bg-white rounded-3xl shadow-2xl flex flex-col max-h-[90vh] animate-in zoom-in duration-200">
          <div class="bg-blue-600 p-5 text-white text-center flex-shrink-0">
            <div class="text-3xl mb-1 flex justify-center">
              <ion-icon :icon="constructOutline" />
            </div>
            <h3 class="text-lg font-bold">Signaler un problème</h3>
            <p class="text-blue-100 text-[10px]">Aidez-nous à améliorer les routes</p>
          </div>
          
          <div class="p-5 space-y-4 overflow-y-auto flex-1">
            <div>
              <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2 ml-1">Description</label>
              <textarea v-model="reportDescription" rows="2" placeholder="Décrivez le problème..." class="w-full px-4 py-3 bg-slate-50 border border-slate-100 rounded-2xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all text-sm text-slate-700 resize-none"></textarea>
            </div>

            <!-- Sélection du type de signalement -->
            <div>
              <label class="block text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-2 ml-1">Type de problème</label>
              <div class="relative group">
                <div 
                  ref="carouselRef"
                  class="flex gap-2 overflow-x-auto no-scrollbar scroll-smooth pb-1 px-1"
                >
                  <div 
                    v-for="type in typesSignalement" 
                    :key="type.id"
                    @click="selectedTypeId = type.id"
                    :class="[
                      'flex-shrink-0 w-24 h-24 rounded-2xl p-2 flex flex-col items-center justify-between cursor-pointer transition-all border-2',
                      selectedTypeId === type.id ? 'border-blue-500 scale-95 bg-blue-50/50' : 'border-transparent bg-slate-50'
                    ]"
                  >
                    <div 
                      class="w-10 h-10 rounded-xl flex items-center justify-center shadow-sm"
                      :style="{ backgroundColor: type.couleur + '20' }"
                    >
                      <svg viewBox="0 0 24 24" class="w-6 h-6" :style="{ fill: type.couleur }">
                        <path :d="type.icone_path" />
                      </svg>
                    </div>
                    <span class="text-[9px] font-bold text-center leading-tight text-slate-700 uppercase tracking-tighter line-clamp-2">
                      {{ type.nom }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div>
              <div class="flex justify-between items-center mb-2 ml-1">
                <label class="block text-[10px] font-bold text-slate-400 uppercase">Photos ({{ reportPhotos.length }})</label>
                <button v-if="reportPhotos.length > 0" @click="reportPhotos = []" class="text-[10px] font-bold text-red-500 uppercase hover:underline">Tout effacer</button>
              </div>
              
              <div class="grid grid-cols-3 gap-2">
                <!-- Miniatures existantes -->
                <div v-for="(photo, index) in reportPhotos" :key="index" class="relative aspect-square rounded-xl overflow-hidden group border border-slate-100 shadow-sm">
                  <img :src="photo" class="w-full h-full object-cover">
                  <button @click="removePhoto(index)" class="absolute top-1 right-1 z-10 bg-red-500/80 backdrop-blur-sm text-white w-6 h-6 rounded-full flex items-center justify-center shadow-lg active:scale-90 transition-transform">
                    <ion-icon :icon="closeOutline" class="text-sm" />
                  </button>
                </div>
                
                <!-- Boutons d'ajout -->
                <div @click="takePhoto" class="aspect-square bg-slate-50 border-2 border-dashed border-slate-200 rounded-xl flex flex-col items-center justify-center text-slate-400 cursor-pointer hover:bg-slate-100 hover:border-blue-200 transition-colors">
                  <ion-icon :icon="cameraOutline" class="text-xl" />
                  <span class="text-[8px] font-bold mt-1 uppercase">Caméra</span>
                </div>

                <div @click="triggerFileSelect" class="aspect-square bg-slate-50 border-2 border-dashed border-slate-200 rounded-xl flex flex-col items-center justify-center text-slate-400 cursor-pointer hover:bg-slate-100 hover:border-blue-200 transition-colors">
                  <ion-icon :icon="eyeOutline" class="text-xl" />
                  <span class="text-[8px] font-bold mt-1 uppercase">Galerie</span>
                </div>

                <input 
                  type="file" 
                  ref="fileInputRef" 
                  multiple 
                  accept="image/*" 
                  class="hidden" 
                  @change="handleFileSelect"
                >
              </div>
            </div>
          </div>

          <div class="p-5 bg-slate-50/50 border-t border-slate-100 flex gap-3 flex-shrink-0">
            <button @click="newSignalementPoint = null" class="flex-1 py-3.5 bg-white text-slate-600 font-bold rounded-2xl border border-slate-200 active:scale-95 transition-all text-sm">Annuler</button>
            <button @click="submitReport" :disabled="isSubmitting" class="flex-1 py-3.5 bg-blue-600 text-white font-bold rounded-2xl shadow-lg shadow-blue-500/30 active:scale-95 transition-all flex items-center justify-center text-sm">
              <span v-if="isSubmitting" class="animate-spin mr-2">
                <ion-icon :icon="sendOutline" />
              </span>
              {{ isSubmitting ? 'Envoi...' : 'Envoyer' }}
            </button>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { IonPage, IonContent, IonIcon } from '@ionic/vue';

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
  trashOutline
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
const reportPhotos = ref<string[]>([]);
const fileInputRef = ref<HTMLInputElement | null>(null);
const selectedTypeId = ref<number | null>(null);
const typesSignalement = ref<TypeSignalement[]>([]);
const isSubmitting = ref(false);
const isLocating = ref(false);

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

const handleLogin = async () => {
  if (!loginEmail.value || !loginPassword.value) {
    authError.value = "Veuillez remplir tous les champs";
    return;
  }
  isAuthLoading.value = true;
  authError.value = "";
  try {
    const email = loginEmail.value.trim();
    const password = loginPassword.value.trim();

    // 1. Récupérer la configuration de la limite (max_tentatives_connexion)
    const configDoc = await getDoc(doc(db, 'configurations', 'max_tentatives_connexion'));
    const maxTentatives = configDoc.exists() ? parseInt(configDoc.data().valeur) : 3;

    // 2. Chercher l'utilisateur dans Firestore
    // On cherche par email dans tous les documents car l'ID du document est maintenant l'UUID Postgres
    const usersRef = collection(db, 'utilisateurs');
    const q = query(usersRef, where('email', '==', email));
    const querySnapshot = await getDocs(q);

    if (querySnapshot.empty) {
      authError.value = "Email ou mot de passe incorrect";
      return;
    }

    const userDoc = querySnapshot.docs[0];
    const userDocRef = userDoc.ref;
    const userData = userDoc.data();

    console.log("Utilisateur trouvé dans Firestore:", userData.email);
    console.log("MDP Firestore:", userData.motDePasse, "| MDP saisi:", password);

    // 2.5 Récupérer la durée de session (duree_session_heures)
    const sessionConfigDoc = await getDoc(doc(db, 'configurations', 'duree_session_heures'));
    const dureeHeures = sessionConfigDoc.exists() ? parseFloat(sessionConfigDoc.data().valeur) : 24;

    // 3. Vérifier si le compte est bloqué
    if (userData.statut === 'BLOQUE') {
      authError.value = "Votre compte est bloqué. Contactez un administrateur.";
      return;
    }

    const appUser = {
      email: userData.email,
      role: userData.role,
      statut: userData.statut,
      firebaseUid: userDoc.id
    };
    // 4. Vérifier le mot de passe
    if (userData.motDePasse === password) {
      // Succès : réinitialiser les tentatives
      await updateDoc(userDocRef, {
        tentatives_connexion: 0,
        derniereConnexion: new Date().toISOString()
      });

      const expiresAt = new Date(Date.now() + dureeHeures * 3600 * 1000).toISOString();

      const appUser = {
        email: userData.email,
        role: userData.role,
        statut: userData.statut,
        firebaseUid: userDoc.id, // L'ID du document est le Firebase UID
        expiresAt: expiresAt
      };

      setUser(appUser);
      localStorage.setItem('app_user', JSON.stringify(appUser));
      
      console.log('✅ Connexion réussie, authentification Firebase Auth...');
      
      // Authentifier avec Firebase Auth pour permettre aux notifications de fonctionner
      try {
        await signInWithEmailAndPassword(auth, email, password);
        console.log('✅ Firebase Auth réussie');
        
        // Les notifications s'initialiseront automatiquement via onAuthStateChanged dans main.ts
        console.log('✅ Les notifications vont s\'initialiser automatiquement...');
      } catch (authError: any) {
        console.warn('⚠️ Erreur Firebase Auth (normal si le compte n\'existe pas dans Firebase Auth):', authError.message);
        console.log('💡 Tentative d\'initialisation manuelle des notifications...');
        
        // Fallback : initialiser manuellement si Firebase Auth échoue
        try {
          await notificationService.initialize();
          await notificationService.loadNotifications();
          console.log('✅ Service de notifications initialisé manuellement');
        } catch (error) {
          console.error('❌ Erreur lors de l\'initialisation manuelle des notifications:', error);
        }
      }
      
      showLoginModal.value = false;
      loginEmail.value = '';
      loginPassword.value = '';
    } else {
      // Échec : incrémenter les tentatives
      const nouvellesTentatives = (userData.tentatives_connexion || 0) + 1;
      
      const updates: any = {
        tentatives_connexion: nouvellesTentatives
      };

      if (nouvellesTentatives >= maxTentatives) {
        updates.statut = 'BLOQUE';
        authError.value = `Compte bloqué après ${nouvellesTentatives} tentatives infructueuses.`;
      } else {
        authError.value = `Email ou mot de passe incorrect (${nouvellesTentatives}/${maxTentatives} tentatives)`;
      }

      await updateDoc(userDocRef, updates);
    }
  } catch (e: any) {
    console.error('Erreur Auth Firestore:', e);
    authError.value = "Une erreur est survenue lors de l'authentification";
  } finally {
    isAuthLoading.value = false;
  }
};

const handleLogout = async () => {
  setUser(null);
  localStorage.removeItem('app_user');
  filterMine.value = false;
  
  // Nettoyer les notifications
  notificationService.cleanup();
  
  // Se déconnecter de Firebase Auth si nécessaire
  try {
    await auth.signOut();
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

    // Reset form
    newSignalementPoint.value = null;
    reportDescription.value = '';
    reportPhotos.value = [];
    selectedTypeId.value = null;
    
    alert('Signalement envoyé avec succès !');
  } catch (err: any) {
    console.error('Erreur lors de l\'envoi:', err);
    if (err.message?.includes('too large')) {
      alert('Erreur : Le signalement est trop lourd. Essayez de mettre moins de photos ou des photos plus petites.');
    } else {
      alert('Erreur lors de l\'envoi du signalement : ' + err.message);
    }
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
      // Déterminer l'image à afficher (priorité à la galerie, puis photo_url)
      let displayPhoto = '';
      if (s.galerie && s.galerie.length > 0) {
        displayPhoto = s.galerie[0].url;
      } else if (s.photo_url) {
        displayPhoto = s.photo_url;
      }

      const m = L.marker([s.latitude, s.longitude])
        .addTo(map!)
        .bindPopup(`
          <div class="p-2 min-w-[160px]">
            <div class="text-[10px] font-bold uppercase mb-1" style="color: ${getStatusTextColor(s.statut)}">${s.statut}</div>
            ${displayPhoto ? `<div class="w-full h-24 rounded-lg overflow-hidden mb-2 bg-slate-100"><img src="${displayPhoto}" class="w-full h-full object-cover"></div>` : ''}
            <div class="font-bold text-slate-800 text-sm mb-1">${s.description || 'Signalement'}</div>
            ${s.entreprise ? `<div class="text-[9px] text-blue-600 font-bold italic mb-1">Entreprise: ${s.entreprise}</div>` : ''}
            <div class="text-[10px] text-slate-400 mt-2 pt-2 border-t border-slate-50 flex justify-between">
              <span>${formatDate(s.dateSignalement)}</span>
              <span class="font-bold text-blue-600">${
                (s.firebase_uid_utilisateur && s.firebase_uid_utilisateur === store.user?.firebaseUid) || 
                (!s.firebase_uid_utilisateur && s.email === store.user?.email) 
                ? 'Moi' : ''
              }</span>
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
