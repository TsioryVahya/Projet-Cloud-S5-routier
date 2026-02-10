import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Signalement, StatutSignalement, TypeSignalement } from '../models/signalement.model';

@Injectable({
  providedIn: 'root'
})
export class SignalementService {
  private apiUrl = 'http://localhost:8081/api/signalements';
  private statusUrl = 'http://localhost:8081/api/statuts-signalement';
  private entrepriseUrl = 'http://localhost:8081/api/entreprises';
  private typeUrl = 'http://localhost:8081/api/types-signalement';
  private historiqueUrl = 'http://localhost:8081/api/historique-signalements';

  constructor(private http: HttpClient) {}

  getAverageProcessingTime(): Observable<any[]> {
    return this.http.get<any[]>(`${this.historiqueUrl}/stats/avg-time`);
  }

  getRecapitulatif(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/recap`);
  }

  getAllSignalements(): Observable<Signalement[]> {
    return this.http.get<Signalement[]>(this.apiUrl);
  }

  getAllTypes(): Observable<TypeSignalement[]> {
    return this.http.get<TypeSignalement[]>(this.typeUrl);
  }

  getAllEntreprises(): Observable<any[]> {
    return this.http.get<any[]>(this.entrepriseUrl);
  }

  syncData(): Observable<any> {
    return this.http.post(`${this.apiUrl}/sync`, {});
  }

  getAllStatuses(): Observable<StatutSignalement[]> {
    return this.http.get<StatutSignalement[]>(this.statusUrl);
  }

  getSignalementById(id: string): Observable<Signalement> {
    return this.http.get<Signalement>(`${this.apiUrl}/${id}`);
  }

  createSignalement(data: any): Observable<any> {
    return this.http.post(this.apiUrl, data);
  }

  updateSignalement(id: string, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}`, data);
  }

  deleteSignalement(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
