import { reactive, ref } from 'vue';
import { User } from 'firebase/auth';

export interface Signalement {
  id: string; // Correspond à id_firebase dans Postgres
  latitude: number;
  longitude: number;
  dateSignalement: any; // Correspond à date_signalement
  statut: string; // Correspond au nom dans statuts_signalement
  email: string; // Utilisé pour retrouver utilisateur_id dans Postgres
  
  // Détails (Correspondent à la table signalements_details)
  description?: string;
  surface_m2?: number;
  budget?: number;
  entreprise_concerne?: string;
  photo_url?: string;
  
  [key: string]: any;
}

// État global simple
export const store = reactive({
  user: null as User | null,
  signalements: [] as Signalement[],
  loading: true
});

export const setSignalements = (data: Signalement[]) => {
  store.signalements = data;
};

export const setUser = (u: User | null) => {
  store.user = u;
};
