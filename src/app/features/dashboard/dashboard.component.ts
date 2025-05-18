import { Component, OnInit } from '@angular/core';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';
import { Router } from '@angular/router';
import { ProjectCreateDialogComponent } from '../project-create-dialog/project-create-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog/confirm-dialog.component';


@Component({
  selector: 'app-dashboard',
  standalone: false,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  projects: Project[] = [];
 

  constructor(private projectService: ProjectService, 
    private router: Router, 
    private dialog: MatDialog,
    private snackBar: MatSnackBar) {}

  ngOnInit(): void {
    this.projectService.getAll().subscribe({
      next: (projects) => {
        this.projects = projects;
      },
      error: (error) => {
        console.error('Error cargando proyectos:', error);
        alert(' Error cargando proyectos');
      }
    });
  }

  goToProject(name: string): void {
    this.router.navigate([`/projects/${encodeURIComponent(name)}`]);
  }

    openCreateDialog(): void {
        const dialogRef = this.dialog.open(ProjectCreateDialogComponent, {
          width: '400px'
        });

      dialogRef.afterClosed().subscribe((result: Project | undefined) => {
        if (result) {
          this.projectService.create(result).subscribe({
            next: (created) => {
              this.snackBar.open('Proyecto creado', 'Cerrar', {
                duration: 3000,
                panelClass: 'snackbar-success'
              });
              this.projects.push(created);
            },
            error: (err) => {
              console.error('Error al crear proyecto', err);
              this.snackBar.open('Error al crear proyecto', 'Cerrar', {
                duration: 3000,
                panelClass: 'snackbar-error'
              });
            }
          });
        }
      });
    } 
deleteProject(project:Project): void {
  const dialogRef = this.dialog.open(ConfirmDialogComponent, {
    width: '350px',
data: { projectName: project.name }
  });

  dialogRef.afterClosed().subscribe(result => {
    if (result) {
      this.projectService.delete(project.id!).subscribe({
        next: () => {
          this.projects = this.projects.filter(p => p.id !== project.id);
          this.snackBar.open('Proyecto eliminado', 'Cerrar', {
            duration: 3000,
            panelClass: 'snackbar-success'
          });
        },
        error: (err) => {
          console.error('Error al eliminar proyecto', err);
          this.snackBar.open('Error al eliminar proyecto', 'Cerrar', {
            duration: 3000,
            panelClass: 'snackbar-error'
          });
        }
      });
    }
  });
}
}
