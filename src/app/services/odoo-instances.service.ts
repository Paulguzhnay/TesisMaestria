import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OdooInstance } from '../models/odoo-instance.model';

@Injectable({
  providedIn: 'root'
})
export class OdooInstancesService {
  private apiUrl = 'http://localhost:8080/api/instances';

  constructor(private http: HttpClient) {}

  getByProject(projectName: string): Observable<OdooInstance[]> {
    const url = `${this.apiUrl}/by-project/${projectName}`;
    return this.http.get<OdooInstance[]>(url);
  }
}
