<template>
  <ion-page>
    <ion-content :fullscreen="true" class="ion-no-padding">
      <!-- Background Moderne avec Formes Organiques -->
      <div class="fixed inset-0 z-0 overflow-hidden bg-slate-50">
        <div class="absolute -top-[10%] -right-[10%] w-[70%] h-[40%] bg-blue-600/10 rounded-full blur-[120px] animate-pulse"></div>
        <div class="absolute top-[20%] -left-[10%] w-[60%] h-[50%] bg-indigo-500/5 rounded-full blur-[100px]"></div>
        <div class="absolute -bottom-[10%] right-[20%] w-[50%] h-[40%] bg-blue-400/10 rounded-full blur-[80px]"></div>
      </div>

      <div class="relative z-10 min-h-full flex flex-col justify-center px-8 py-12">
        <div class="sm:mx-auto sm:w-full sm:max-w-md">
          <div class="w-24 h-24 bg-blue-600 rounded-[32px] shadow-2xl shadow-blue-500/40 flex items-center justify-center mx-auto mb-10 transform -rotate-6 animate-in fade-in zoom-in duration-700">
            <ion-icon :icon="trailSignOutline" class="text-5xl text-white" />
          </div>
          <h2 class="text-center text-4xl font-black text-slate-900 tracking-tight mb-2">
            Lalana
          </h2>
          <p class="text-center text-sm text-slate-400 font-bold uppercase tracking-[0.2em]">
            Accès Agent
          </p>
        </div>

        <div class="mt-12 sm:mx-auto sm:w-full sm:max-w-md">
          <div class="bg-white/70 backdrop-blur-2xl py-10 px-8 shadow-[0_20px_50px_rgba(0,0,0,0.05)] rounded-[40px] border border-white/50 space-y-8">
            <div class="space-y-6">
              <div class="group">
                <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3 ml-1 transition-colors group-focus-within:text-blue-600">
                  Identifiant Email
                </label>
                <div class="relative flex items-center">
                  <div class="absolute left-5 text-slate-400 transition-colors group-focus-within:text-blue-500">
                    <ion-icon :icon="mailOutline" class="text-xl" />
                  </div>
                  <input 
                    v-model="loginEmail" 
                    type="email" 
                    placeholder="votre@email.com" 
                    class="w-full pl-14 pr-6 py-6 bg-slate-50/50 border-2 border-slate-100 rounded-[24px] focus:outline-none focus:ring-8 focus:ring-blue-500/5 focus:border-blue-500 focus:bg-white transition-all text-slate-700 placeholder:text-slate-300 font-bold shadow-sm text-base"
                  >
                </div>
              </div>

              <div class="group">
                <label class="block text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3 ml-1 transition-colors group-focus-within:text-blue-600">
                  Mot de passe
                </label>
                <div class="relative flex items-center">
                  <div class="absolute left-5 text-slate-400 transition-colors group-focus-within:text-blue-500">
                    <ion-icon :icon="lockClosedOutline" class="text-xl" />
                  </div>
                  <input 
                    v-model="loginPassword" 
                    type="password" 
                    placeholder="••••••••" 
                    class="w-full pl-14 pr-6 py-6 bg-slate-50/50 border-2 border-slate-100 rounded-[24px] focus:outline-none focus:ring-8 focus:ring-blue-500/5 focus:border-blue-500 focus:bg-white transition-all text-slate-700 placeholder:text-slate-300 font-bold shadow-sm text-base"
                  >
                </div>
              </div>
            </div>

            <div v-if="authError" class="bg-red-50/80 backdrop-blur-sm border border-red-100 text-red-600 text-[13px] p-4.5 rounded-2xl flex items-center gap-3 animate-in fade-in slide-in-from-top-2 duration-300">
              <ion-icon :icon="alertCircleOutline" class="text-xl flex-shrink-0" />
              <p class="font-bold leading-snug">{{ authError }}</p>
            </div>

            <div class="space-y-4 pt-2">
              <button 
                @click="handleLogin" 
                :disabled="isAuthLoading" 
                class="w-full py-5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white rounded-2xl font-black shadow-xl shadow-blue-500/30 transition-all transform active:scale-[0.97] flex items-center justify-center gap-3 uppercase tracking-widest text-xs"
              >
                <ion-icon v-if="isAuthLoading" :icon="syncOutline" class="animate-spin text-xl" />
                <span>{{ isAuthLoading ? 'Connexion...' : 'Se connecter' }}</span>
              </button>

              <button 
                @click="goBack" 
                class="w-full py-4 text-slate-400 font-black hover:text-slate-600 transition-colors flex items-center justify-center gap-2 text-[11px] uppercase tracking-widest"
              >
                <ion-icon :icon="chevronBackOutline" class="text-sm" />
                <span>Retour</span>
              </button>
            </div>
          </div>

          <div class="mt-12 text-center">
            <p class="text-[10px] text-slate-400 leading-relaxed uppercase tracking-[0.2em] font-black">
              L'inscription se fait uniquement via le Manager<br>sur l'application Web
            </p>
          </div>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { IonPage, IonContent, IonIcon } from '@ionic/vue';
import { 
  trailSignOutline, 
  alertCircleOutline, 
  syncOutline,
  chevronBackOutline,
  mailOutline,
  lockClosedOutline
} from 'ionicons/icons';
import { db, auth } from '../firebase/config';
import { 
  collection, 
  query, 
  where, 
  getDocs, 
  doc, 
  getDoc, 
  updateDoc 
} from 'firebase/firestore';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { setUser } from '../store';
import { notificationService } from '../services/notificationService';

const router = useRouter();
const loginEmail = ref('');
const loginPassword = ref('');
const authError = ref('');
const isAuthLoading = ref(false);

const goBack = () => {
  router.back();
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

    // 1. Récupérer la configuration de la limite
    const configDoc = await getDoc(doc(db, 'configurations', 'max_tentatives_connexion'));
    const maxTentatives = configDoc.exists() ? parseInt(configDoc.data().valeur) : 3;

    // 2. Chercher l'utilisateur dans Firestore
    const usersRef = collection(db, 'utilisateurs');
    const q = query(usersRef, where('email', '==', email));
    const querySnapshot = await getDocs(q);

    if (querySnapshot.empty) {
      authError.value = "Email ou mot de passe incorrect";
      isAuthLoading.value = false;
      return;
    }

    const userDoc = querySnapshot.docs[0];
    const userDocRef = userDoc.ref;
    const userData = userDoc.data();

    // 3. Vérifier si le compte est bloqué
    if (userData.statut === 'BLOQUE') {
      authError.value = "Votre compte est bloqué. Contactez un administrateur.";
      isAuthLoading.value = false;
      return;
    }

    // 4. Vérifier la durée de session
    const sessionConfigDoc = await getDoc(doc(db, 'configurations', 'duree_session_heures'));
    const dureeHeures = sessionConfigDoc.exists() ? parseFloat(sessionConfigDoc.data().valeur) : 24;

    // 5. Vérifier le mot de passe
    if (userData.motDePasse === password) {
      await updateDoc(userDocRef, {
        tentatives_connexion: 0,
        derniereConnexion: new Date().toISOString()
      });

      const expiresAt = new Date(Date.now() + dureeHeures * 3600 * 1000).toISOString();

      const appUser = {
        email: userData.email,
        role: userData.role,
        statut: userData.statut,
        postgresId: userData.postgresId,
        firebaseUid: userDoc.id,
        expiresAt: expiresAt
      };

      setUser(appUser);
      localStorage.setItem('app_user', JSON.stringify(appUser));
      
      try {
        await signInWithEmailAndPassword(auth, email, password);
      } catch (authError: any) {
        console.warn('Firebase Auth failed, continuing with session only:', authError.message);
        await notificationService.initialize();
        await notificationService.loadNotifications();
      }

      router.replace('/tabs/map');
    } else {
      // Gérer l'échec de connexion (tentatives)
      const nouvellesTentatives = (userData.tentatives_connexion || 0) + 1;
      const updates: any = { tentatives_connexion: nouvellesTentatives };
      
      if (nouvellesTentatives >= maxTentatives) {
        updates.statut = 'BLOQUE';
        authError.value = "Compte bloqué après trop de tentatives.";
      } else {
        authError.value = `Mot de passe incorrect (${nouvellesTentatives}/${maxTentatives})`;
      }
      
      await updateDoc(userDocRef, updates);
    }
  } catch (error: any) {
    console.error('Login error:', error);
    authError.value = "Une erreur est survenue lors de la connexion";
  } finally {
    isAuthLoading.value = false;
  }
};
</script>

<style scoped>
ion-content {
  --background: #f8fafc;
}
</style>
