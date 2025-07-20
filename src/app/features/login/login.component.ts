import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth-service.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-login',
  standalone: false,
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';

  constructor(private authService: AuthService, private router: Router) { }

  onLogin(): void {
    console.log('Intentando iniciar sesión con:', this.username, this.password);
    this.authService.login(this.username, this.password).subscribe({
      next: (response) => {
        localStorage.setItem('token', response.token);
        this.router.navigate(['/dashboard']); // redirige después del login
      },
      error: () => {
        this.errorMessage = '❌ Usuario o contraseña incorrectos.';
      }
    });
  }
  loginWithGitHub(): void {
    window.location.href = 'http://localhost:8080/auth/login/github';
  }
}
