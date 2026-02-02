export interface StatutSignalement {
  id: number;
  nom: string;
}

export interface TypeSignalement {
  id: number;
  nom: string;
  description: string;
  iconePath: string;
  couleur: string;
}

export interface Signalement {
  id?: string;
  postgresId?: string;
  idFirebase?: string;
  dateSignalement: string;
  statut: string;
  typeNom?: string;
  typeIcone?: string;
  typeCouleur?: string;
  latitude: number;
  longitude: number;
  description?: string;
  surfaceM2?: number;
  budget?: any;
  entrepriseConcerne?: string;
  entrepriseNom?: string;
  photoUrl?: string;
  utilisateur?: {
    email: string;
  };
}
