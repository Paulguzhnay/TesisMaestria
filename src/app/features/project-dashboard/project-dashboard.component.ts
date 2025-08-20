import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { OdooInstancesService } from '../../services/odoo-instances.service';
import { Project } from '../../models/project.model';
import { OdooInstance } from '../../models/odoo-instance.model';
import { ProjectContextService } from '../../services/project-context.service';
import { MatDialog } from '@angular/material/dialog';
import { CreateInstanceDialogComponent } from '../../core/create-instance-dialog/create-instance-dialog.component';
import { MatSnackBar } from '@angular/material/snack-bar';


@Component({
  selector: 'app-project-dashboard',
  standalone: false,
  templateUrl: './project-dashboard.component.html',
  styleUrl: './project-dashboard.component.css'
})
export class ProjectDashboardComponent implements OnInit {
  projectName = '';
  project: Project | null = null;
  instances: OdooInstance[] = [];
  loading = false;
  error = '';
  isProcessing = false;


  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private instanceService: OdooInstancesService,
    private projectContext: ProjectContextService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.projectName = this.route.snapshot.paramMap.get('name') || '';
    if (this.projectName) {
      this.projectContext.setProjectName(this.projectName);
      this.loadProject();
      this.loadInstances();
    }
  }

  loadProject(): void {
    this.projectService.getAll().subscribe({
      next: (projects: Project[]) => {
        const found = projects.find(p => p.name === this.projectName);
        if (found) {
          this.project = found;
        } else {
          this.error = 'Proyecto no encontrado';
        }
      },
      error: () => {
        this.error = 'Error al cargar proyecto';
      }
    });
  }

  loadInstances(): void {
    this.loading = true;
    this.instanceService.getByProject(this.projectName).subscribe({
      next: (data: OdooInstance[]) => {
        this.instances = [...data]; //  Crea nueva referencia para que Angular detecte el cambio
        this.loading = false;
      },
      error: () => {
        this.error = 'Error al cargar instancias';
        this.loading = false;
      }
    });
  }
  get categorizedInstances(): { [key: string]: OdooInstance[] } {
    const categorized: { [key: string]: OdooInstance[] } = {};

    for (const inst of this.instances) {
      const cat = inst.category || 'UNASSIGNED';
      if (!categorized[cat]) categorized[cat] = [];
      categorized[cat].push(inst);
    }

    return categorized;
  }

  deleteInstance(instance: OdooInstance): void {
    const confirmDelete = confirm(`¿Estás seguro de eliminar la instancia ${instance.name}?`);
    if (!confirmDelete) return;

    this.isProcessing = true;

    this.snackBar.open(`🕐 Eliminando la instancia ${instance.name}...`, '', {
      duration: 2000
    });

    this.instanceService.delete(instance.name, instance.category).subscribe({
      next: () => {
        this.snackBar.open(
          `✅ Instancia ${instance.name} eliminada correctamente`,
          'Cerrar',
          { duration: 2500 }
        );

        setTimeout(() => {
          this.isProcessing = false;
          window.location.reload();  // Recarga completa de la página
        }, 1500); // Espera para que se vea el snackbar antes del reload
      },
      error: () => {
        this.isProcessing = false;
        this.snackBar.open(
          `❌ Error al eliminar la instancia ${instance.name}`,
          'Cerrar',
          { duration: 4000 }
        );
      }
    });
  }


  openCreateInstanceDialog(category: string): void {
    if (!this.project) return;

    const dialogRef = this.dialog.open(CreateInstanceDialogComponent, {
      width: '500px',
      data: {
        projectId: this.project.id,
        projectName: this.project.name,
        category: category
      }
    });

    dialogRef.componentInstance.instanceCreated.subscribe(() => {
      this.loadInstances(); //  recargar al crear
    });
  }

}
