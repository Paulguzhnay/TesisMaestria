import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class OdooService {
  private apiUrl = 'http://localhost:8080/api/odoo'; // Ajusta la URL según tu backend
  constructor(private http: HttpClient) {}

  createOdooInstance(instanceName: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/create`, { name: instanceName });
  }
}
