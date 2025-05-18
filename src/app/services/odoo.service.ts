import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OdooInstance } from '../models/odoo-instance.model';

@Injectable({
  providedIn: 'root'
})
export class OdooService {
  private apiUrl = 'http://localhost:8080/api/odoo'; // Ajusta la URL según tu backend
  constructor(private http: HttpClient) {}

  createOdooInstance(instanceName: string, category: string , projectId: number): Observable<{ message: string; url?: string }> {
    return this.http.post<{ message: string; url?: string }>(
      `${this.apiUrl}/create`, 
      { name: instanceName, category, projectId }
    );
  }

  getInstances(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/instances`);
  }
  //------------
deleteInstance(name: string, category: string): Observable<string> {
  return this.http.delete<string>(`http://localhost:8080/api/odoo/delete?name=${encodeURIComponent(name)}&category=${encodeURIComponent(category)}`);
}

//------------------
mergeInstances(sourceName: string, targetName: string, category: string): Observable<string> {
  const payload = { source: sourceName, target: targetName, category };
  return this.http.post<string>(`${this.apiUrl}/merge`, payload, { responseType: 'text' as 'json' });
}
//---------------------
getByProject(projectName: string): Observable<OdooInstance[]> {
  const url = `http://localhost:8080/api/odoo/instances/by-project/${projectName}`;
  return this.http.get<OdooInstance[]>(url);
}
}

