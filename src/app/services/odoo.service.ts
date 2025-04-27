import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class OdooService {
  private apiUrl = 'http://localhost:8080/api/odoo'; // Ajusta la URL según tu backend
  constructor(private http: HttpClient) {}

  createOdooInstance(instanceName: string, category: string): Observable<{ message: string; url?: string }> {
    return this.http.post<{ message: string; url?: string }>(
      `${this.apiUrl}/create`, 
      { name: instanceName, category }
    );
  }

  getInstances(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/instances`);
  }
  //------------
deleteInstance(name: string, category: string): Observable<string> {
  return this.http.delete<string>(`http://localhost:8080/api/odoo/delete?name=${encodeURIComponent(name)}&category=${encodeURIComponent(category)}`);
}
}

