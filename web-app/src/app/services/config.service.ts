import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Configuration {
  cle: string;
  valeur: string;
  description?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private apiUrl = 'http://localhost:8081/api/configurations';

  constructor(private http: HttpClient) {}

  getAllConfigs(): Observable<Configuration[]> {
    return this.http.get<Configuration[]>(this.apiUrl);
  }

  updateConfig(cle: string, valeur: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${cle}`, { cle, valeur });
  }

  syncToFirebase(): Observable<any> {
    return this.http.post(`${this.apiUrl}/sync-to-firebase`, {});
  }
}
