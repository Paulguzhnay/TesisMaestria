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

  constructor(private route: ActivatedRoute, private router: Router) { }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const username = params['username'];
      const avatar = params['avatar'];


      if (token) {
        localStorage.setItem('token', token);
        localStorage.setItem('username', username);
        localStorage.setItem('avatarUrl', avatar);
        this.router.navigate(['/dashboard']);
      } else {
        alert('❌ Error: No se pudo obtener el token de autenticación');
        this.router.navigate(['/']);
      }
    });
  }
}