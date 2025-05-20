import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ShellService {
  constructor(private http: HttpClient) {}

  executeDbCommand(payload: { command: string, name: string, category: string }): Observable<string> {
    return this.http.post('http://localhost:8080/shell/db', payload, {
      responseType: 'text'
    });
  }
  getInstancesByProject(projectName: string): Observable<any[]> {
  return this.http.get<any[]>(`http://localhost:8080/api/odoo/instances/by-project/${projectName}`);
}
}
