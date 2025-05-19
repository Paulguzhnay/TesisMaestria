import { Component } from '@angular/core';
import { OnInit } from '@angular/core';
import { BackupService } from '../../services/backup.service';
import { Router, ActivatedRoute } from '@angular/router';
import { ProjectContextService } from '../../services/project-context.service';
@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit {
  backups: string[] = [];
  projectName: string = '';
  projectId!: number;
constructor(private backupService: BackupService, private router: Router,
  private context: ProjectContextService,
  private route: ActivatedRoute) {}

    ngOnInit(): void {
      this.router.events.subscribe(() => {
        const pathParts = this.router.url.split('/');
        const projectIndex = pathParts.indexOf('projects');
        if (projectIndex >= 0 && pathParts.length > projectIndex + 1) {
          const rawName = pathParts[projectIndex + 1];
          if (rawName && !rawName.includes('?')) {
            this.projectName = decodeURIComponent(rawName);
          } else {
            this.projectName = decodeURIComponent(rawName.split('?')[0]);
          }

          // Extraer id manualmente si no llega por paramMap
          const url = new URL(window.location.href);
          const id = url.searchParams.get('id');
          if (id) {
            this.projectId = +id;
          }
        }
      });

      this.loadBackups();
    }
loadBackups() {
  this.backupService.getBackups().subscribe((data) => {
    this.backups = data.map(b => b.name);  
  });
}

  // Método de logout
logout(): void {
  localStorage.removeItem('token'); // o el nombre exacto de tu token
  this.router.navigate(['/login']); // Ajusta la ruta si tu login está en otra
}
}
