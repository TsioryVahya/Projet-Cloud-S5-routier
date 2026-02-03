import { reactive, ref } from 'vue';
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
  entreprise?: string | null;
  photo_url?: string;
  
  [key: string]: any;
}

export interface AppUser {
  email: string;
  role?: string;
  statut?: string;
  postgresId?: string;
}

// État global simple
export const store = reactive({
  user: null as AppUser | null,
  signalements: [] as Signalement[],
  loading: true
});

export const setSignalements = (data: Signalement[]) => {
  store.signalements = data;
};

export const setUser = (u: AppUser | null) => {
  store.user = u;
};
