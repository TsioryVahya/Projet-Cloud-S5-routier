import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, map, of } from 'rxjs';
import { Session, Utilisateur } from '../models/user.model';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);
  private apiUrl = 'http://localhost:8081/api/auth';

  private currentSession: Session | null = null;

  constructor() {
    const savedSession = localStorage.getItem('session');
    if (savedSession) {
      this.currentSession = JSON.parse(savedSession);
    }
  }

  login(email: string, password: string): Observable<Session> {
    return this.http.post<Session>(`${this.apiUrl}/login`, { email, password }).pipe(
      tap(session => {
        this.currentSession = session;
        localStorage.setItem('session', JSON.stringify(session));
      })
    );
  }

  logout(): void {
    this.currentSession = null;
    localStorage.removeItem('session');
    this.router.navigate(['/login']);
  }

  isLoggedIn(): boolean {
    if (!this.currentSession) return false;
    
    // Vérifier l'expiration (optionnel mais recommandé)
    const expiration = new Date(this.currentSession.dateExpiration);
    if (expiration < new Date()) {
      this.logout();
      return false;
    }
    
    return true;
  }

  getCurrentUser(): Utilisateur | null {
    return this.currentSession ? this.currentSession.utilisateur : null;
  }

  getToken(): string | null {
    return this.currentSession ? this.currentSession.tokenAcces : null;
  }
}
