import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TypeSignalement } from '../models/signalement.model';

@Injectable({
  providedIn: 'root'
})
export class ReportTypeService {
  private apiUrl = 'http://localhost:8081/api/types-signalement';

  constructor(private http: HttpClient) {}

  getAllTypes(): Observable<TypeSignalement[]> {
    return this.http.get<TypeSignalement[]>(this.apiUrl);
  }

  getTypeById(id: number): Observable<TypeSignalement> {
    return this.http.get<TypeSignalement>(`${this.apiUrl}/${id}`);
  }

  createType(data: Partial<TypeSignalement>): Observable<TypeSignalement> {
    return this.http.post<TypeSignalement>(this.apiUrl, data);
  }

  updateType(id: number, data: Partial<TypeSignalement>): Observable<TypeSignalement> {
    return this.http.put<TypeSignalement>(`${this.apiUrl}/${id}`, data);
  }

  deleteType(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  syncToFirebase(): Observable<any> {
    return this.http.post(`${this.apiUrl}/sync-to-firebase`, {});
  }
}
