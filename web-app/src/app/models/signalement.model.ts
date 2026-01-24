export interface StatutSignalement {
  id: number;
  nom: string;
}

export interface Signalement {
  id: string;
  idFirebase?: string;
  dateSignalement: string;
  statut: StatutSignalement;
  latitude: number;
  longitude: number;
  description?: string;
  surfaceM2?: number;
  budget?: number;
  entrepriseConcerne?: string;
  photoUrl?: string;
  utilisateur?: {
    id: string;
    email: string;
  };
}
