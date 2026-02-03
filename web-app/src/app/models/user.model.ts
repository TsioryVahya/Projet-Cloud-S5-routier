export interface Role {
  id: number;
  nom: string;
}

export interface StatutUtilisateur {
  id: number;
  nom: string;
}

export interface Utilisateur {
  id: string;
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
