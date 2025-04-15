import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root' // ✅ Esto permite la inyección en toda la app
})
export class BackupService {
  private apiUrl = 'http://localhost:8080/docker/backups'; // Ajusta la URL según tu backend

  constructor(private http: HttpClient) {}

  getBackups(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }
  createBackup(): Observable<string> {
    return this.http.post<string>(this.apiUrl, {}, { responseType: 'text' as 'json' }); // 👈 Asegurar respuesta en texto
  }

 

  restoreSpecific(payload: any): Observable<string> {
    console.log(payload)
    return this.http.post('http://localhost:8080/docker/restore-specific', payload, {
      responseType: 'text' as const 
    });
  }
  
  
  
  
  
  
}
