import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { OdooInstancesService } from '../../services/odoo-instances.service';
import { Project } from '../../models/project.model';
import { OdooInstance } from '../../models/odoo-instance.model';
import { ProjectContextService } from '../../services/project-context.service';


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

  constructor(
    private route: ActivatedRoute,
    private projectService: ProjectService,
    private instanceService: OdooInstancesService,
    private projectContext: ProjectContextService 
  ) {}

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
        this.instances = data;
        this.loading = false;
      },
      error: () => {
        this.error = 'Error al cargar instancias';
        this.loading = false;
      }
    });
  }
  get categorizedInstances(): { [key: string]: OdooInstance[] } {
    console.log('Categorizing instances:', this.instances);
    
    return this.instances.reduce((acc, inst) => {
    
      const cat = inst.category || 'UNASSIGNED';
      console.log(inst.url);
      if (!acc[cat]) acc[cat] = [];
      acc[cat].push(inst);
      return acc;
    }, {} as { [key: string]: OdooInstance[] });
  }

  deleteInstance(inst: OdooInstance): void {
  const confirmed = confirm(`¿Estás seguro de eliminar la instancia '${inst.name}'?`);
  if (!confirmed) return;

  this.instanceService.delete(inst.name, inst.category).subscribe({
    next: () => {
      this.instances = this.instances.filter(i => !(i.name === inst.name && i.category === inst.category));

    },
    error: (err) => {
      console.error('Error al eliminar:', err);
      alert('❌ Error al eliminar la instancia');
    }
  });
}
}
