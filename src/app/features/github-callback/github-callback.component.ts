import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface AuthResponse {
  token: string;
  username: string;
  avatarUrl: string;
}

@Component({
  selector: 'app-github-callback',
  standalone: false,
  templateUrl: './github-callback.component.html',
  styleUrl: './github-callback.component.css'
})
export class GithubCallbackComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    const code = this.route.snapshot.queryParamMap.get('code');
    if (code) {
      this.http.get<AuthResponse>(`http://localhost:8080/auth/github/callback?code=${code}`)
        .subscribe({
          next: (res) => {
            localStorage.setItem('token', res.token);
            localStorage.setItem('username', res.username);
            localStorage.setItem('avatarUrl', res.avatarUrl);
            this.router.navigate(['/dashboard']);
          },
          error: (err) => {
            alert('❌ Error al iniciar sesión con GitHub');
            this.router.navigate(['/login']);
          }
        });
    }
  }
}
