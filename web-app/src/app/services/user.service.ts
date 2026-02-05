import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Utilisateur, Role, StatutUtilisateur } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = 'http://localhost:8081/api/utilisateurs';
  private authUrl = 'http://localhost:8081/api/auth';
  private rolesUrl = 'http://localhost:8081/api/roles';
  private statutsUrl = 'http://localhost:8081/api/statuts-utilisateur';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(this.apiUrl);
  }

  getBlockedUsers(): Observable<Utilisateur[]> {
    return this.http.get<Utilisateur[]>(`${this.apiUrl}/blocked`);
  }

  getRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(this.rolesUrl);
  }

  getStatuts(): Observable<StatutUtilisateur[]> {
    return this.http.get<StatutUtilisateur[]>(this.statutsUrl);
  }

  createUser(user: any): Observable<Utilisateur> {
    return this.http.post<Utilisateur>(this.apiUrl, user);
  }

  updateUser(id: string, user: any): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.apiUrl}/${id}`, user);
  }

  syncUsers(): Observable<any> {
    return this.http.post(`${this.apiUrl}/sync`, {});
  }

  syncUsersToFirebase(): Observable<any> {
    return this.http.post(`${this.apiUrl}/sync-to-firebase`, {});
  }

  unblockUser(email: string): Observable<any> {
    return this.http.post(`${this.authUrl}/unblock`, { email });
  }

  deleteUser(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
