import { Component, OnInit } from '@angular/core';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';
import { Router } from '@angular/router';


@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  projects: Project[] = [];

  constructor(private projectService: ProjectService, private router: Router) {}

  ngOnInit(): void {
    this.projectService.getAll().subscribe({
      next: (projects) => {
        this.projects = projects;
      },
      error: (error) => {
        console.error('Error cargando proyectos:', error);
        alert('❌ Error cargando proyectos');
      }
    });
  }

  goToProject(name: string): void {
    this.router.navigate([`/projects/${encodeURIComponent(name)}`]);
  }
}
