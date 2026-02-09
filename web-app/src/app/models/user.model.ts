export interface Role {
  id: number;
  nom: string;
}

export interface StatutUtilisateur {
  id: number;
  nom: string;
}

export interface Utilisateur {
  firebaseUid: string; // Utiliser le Firebase UID comme identifiant principal
  email: string;
  role: Role;
  statutActuel: StatutUtilisateur;
  derniereConnexion?: string;
}

export interface Session {
  id: string;
  utilisateur: Utilisateur;
  tokenAcces: string;
  refreshToken: string;
  dateExpiration: string;
}
