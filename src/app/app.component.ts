import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  standalone: false,
  styleUrl: './app.component.css'
})
export class AppComponent {
  constructor(public router: Router) {}
  title = 'frontend';

  showSidebar(): boolean {
    
    return !this.router.url.includes('/dashboard') && !this.router.url.includes('/project-setup');
  }
}
