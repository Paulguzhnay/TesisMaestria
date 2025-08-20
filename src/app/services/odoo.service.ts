import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OdooInstance } from '../models/odoo-instance.model';
import { CreateOdooInstancePayload } from '../models/CreateOdooInstancePayload';

@Injectable({
  providedIn: 'root'
})
export class OdooService {
  private apiUrl = 'http://localhost:8080/api/odoo'; // Ajusta la URL según tu backend
  constructor(private http: HttpClient) { }

  createOdooInstance(payload: CreateOdooInstancePayload): Observable<{ message: string; url?: string }> {
    return this.http.post<{ message: string; url?: string }>(
      `${this.apiUrl}/create`,
      payload
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
mergeInstances(sourceName: string, targetName: string, projectId: number, sourceCategory: string, targetCategory: string): Observable<string> {
  const payload = {
    projectId,
    source: sourceName,
    target: targetName,
    sourceCategory,
    targetCategory
  };
  console.log(" Payload de fusión:", payload);
  return this.http.post<string>(`${this.apiUrl}/merge`, payload, { responseType: 'text' as 'json' });
}
  //---------------------
  getByProject(projectName: string): Observable<OdooInstance[]> {
    const url = `http://localhost:8080/api/odoo/instances/by-project/${projectName}`;
    return this.http.get<OdooInstance[]>(url);
  }
  //-----------------
  getInstancesByProject(projectName: string): Observable<OdooInstance[]> {
    return this.http.get<OdooInstance[]>(`http://localhost:8080/api/odoo/instances/by-project/${projectName}`);
  }
  //-----------------------
  handleUnauthorized(): void {
    alert('⚠️ Tu sesión de GitHub ha expirado. Redirigiendo para iniciar sesión nuevamente...');
    window.location.href = 'http://localhost:8080/api/auth/github';
  }
  //-----------------------
  installModules(name: string, category: string, projectId: number): Observable<string> {
    const params = new HttpParams()
      .set('name', name)
      .set('category', category)
      .set('projectId', projectId.toString());

    return this.http.post(`${this.apiUrl}/install-modules`, null, {
      params,
      responseType: 'text'
    });
  }
}

