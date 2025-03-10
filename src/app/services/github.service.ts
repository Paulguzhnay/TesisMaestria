import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class GithubService {
  private apiUrlBranches = 'http://localhost:8080/api/github/branches';
  private apiUrlCommits = 'http://localhost:8080/api/github/commits/';

  constructor(private http: HttpClient) {}

  getBranches(): Observable<string[]> {
    return this.http.get<string[]>(this.apiUrlBranches);
  }

  getCommits(branch: string): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrlCommits}${branch}`);
  }
}
