import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PrixM2 {
  id?: number;
  montant: number;
  dateDebut: string;
  dateFin?: string;
  dateCreation?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PrixM2Service {
  private apiUrl = 'http://localhost:8081/api/prix-m2';

  constructor(private http: HttpClient) {}

  getAll(): Observable<PrixM2[]> {
    return this.http.get<PrixM2[]>(this.apiUrl);
  }

  create(prix: PrixM2): Observable<PrixM2> {
    return this.http.post<PrixM2>(this.apiUrl, prix);
  }

  update(id: number, prix: PrixM2): Observable<PrixM2> {
    return this.http.put<PrixM2>(`${this.apiUrl}/${id}`, prix);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
