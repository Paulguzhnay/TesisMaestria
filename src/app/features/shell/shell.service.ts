import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ShellService {
  private apiUrl = 'http://localhost:8080/api/shell'; // Ajusta la URL según tu backend

  constructor(private http: HttpClient) {}

  executeCommand(command: string): Observable<string> {
    return this.http.post<string>(this.apiUrl, { command });
  }
}
