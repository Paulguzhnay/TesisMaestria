import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class GithubService {
  private apiUrl = 'http://localhost:8080/api/github/branches'; // Ruta al backend

  constructor(private http: HttpClient) {}

  getBranches(): Observable<string[]> {
    return this.http.get<string[]>(this.apiUrl);
  }
}
