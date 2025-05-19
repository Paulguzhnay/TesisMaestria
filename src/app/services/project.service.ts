import { Injectable } from '@angular/core';
import { Project } from '../models/project.model';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OdooInstance } from '../models/odoo-instance.model';
@Injectable({
  providedIn: 'root'
})
export class ProjectService {
  private apiUrl = 'http://localhost:8080/api/projects';
  private projects: Project[] = [];

  constructor(private http: HttpClient) {}

  getAll(): Observable<Project[]> {
    return this.http.get<Project[]>(this.apiUrl);
  }

  create(project: Project): Observable<Project> {
    return this.http.post<Project>(this.apiUrl, project);
  }

  delete(id: number): Observable<void> {
  return this.http.delete<void>(`${this.apiUrl}/${id}`);
}
getInstancesByProject(projectName: string): Observable<OdooInstance[]> {
  
return this.http.get<OdooInstance[]>(`http://localhost:8080/api/odoo/instances/by-project/${projectName}`);
}

getByUser(): Observable<Project[]> {
  return this.http.get<Project[]>(`http://localhost:8080/api/projects`);
}

}
