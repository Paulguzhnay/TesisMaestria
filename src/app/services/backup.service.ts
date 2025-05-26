import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BackupInfo {
  name: string;
  time: string;
  category: string;
  version: string;
  type: string;
  revision: string;
}

@Injectable({
  providedIn: 'root'
})
export class BackupService {
  private baseUrl = 'http://localhost:8080/docker';

  constructor(private http: HttpClient) {}

  getBackups(): Observable<BackupInfo[]> {
    return this.http.get<BackupInfo[]>(`${this.baseUrl}/backups`);
  }

  getBackupsForProject(projectId: number): Observable<BackupInfo[]> {
    return this.http.get<BackupInfo[]>(`${this.baseUrl}/backups?projectId=${projectId}`);
  }

  getBackupsForInstance(instanceName: string, category: string): Observable<BackupInfo[]> {
  return this.http.get<BackupInfo[]>(
    `${this.baseUrl}/backups/instance?instanceName=${instanceName}&category=${category}`
  );
}

 

  createBackupForInstance(payload: { name: string; category: string; projectId: number }): Observable<{ message: string }> {
    console.log('Payload for backup service :', payload);
    console.log('URL for backup service:', `${this.baseUrl}/backup`);
    return this.http.post<{ message: string }>(`${this.baseUrl}/backup`, payload);
  }

  restoreSpecific(payload: {
    name: string;
    category: string;
    dbBackupFileName: string;
    odooBackupFileName: string;
    projectId: number;
  }): Observable<string> {
    return this.http.post<string>(`${this.baseUrl}/restore-specific`, payload, {
      responseType: 'text' as 'json'
    });
  }

  deleteBackup(fileName: string): Observable<string> {
    return this.http.delete<string>(`${this.baseUrl}/delete/${encodeURIComponent(fileName)}`, {
      responseType: 'text' as 'json'
    });
  }
}
