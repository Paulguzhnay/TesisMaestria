import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OdooInstance } from '../models/odoo-instance.model';

@Injectable({
  providedIn: 'root'
})
export class OdooInstancesService {
  private apiUrl = 'http://localhost:8080/api/odoo/instances';

  constructor(private http: HttpClient) {}

  getByProject(projectName: string): Observable<OdooInstance[]> {
    return this.http.get<OdooInstance[]>(`${this.apiUrl}/by-project/${projectName}`);
  }

    delete(name: string, category: string) {
      return this.http.delete(`/api/odoo/delete?name=${name}&category=${category}`, {
        responseType: 'text'
      });
    }
}
