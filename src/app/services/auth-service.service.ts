// auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

interface AuthResponse {
  token: string;
  username: string;
  avatarUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private authUrl = 'http://localhost:8080/auth';

  constructor(private http: HttpClient) {}

login(username: string, password: string): Observable<AuthResponse> {
  return this.http.post<AuthResponse>(`${this.authUrl}/login`, { username, password }).pipe(
    tap(response => {
      localStorage.setItem('token', response.token);
    }),
    catchError(error => {
      console.error('Error de login:', error);
      return throwError(() => error);
    })
  );
}

  logout(): void {
    localStorage.removeItem('token');
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('token');
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }
}
