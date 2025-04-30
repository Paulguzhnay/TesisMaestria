import { Component, OnInit} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
 
import { OdooInstance } from '../../models/odoo-instance.model';
import { OdooInstancesService } from '../../services/odoo-instances.service';
@Component({
  selector: 'app-project-detail',
  standalone: false,
  templateUrl: './project-detail.component.html',
  styleUrl: './project-detail.component.css'
})
export class ProjectDetailComponent implements OnInit {
  projectName = '';
  instances: OdooInstance[] = [];
  loading = false;
  error = '';

  constructor(private route: ActivatedRoute, private instanceService: OdooInstancesService) {}

  ngOnInit(): void {
    this.projectName = this.route.snapshot.paramMap.get('name') || '';
    if (this.projectName) {
      this.loadInstances();
    }
  }

  loadInstances(): void {
    this.loading = true;
    this.error = '';

    this.instanceService.getByProject(this.projectName).subscribe({
      next: (list: OdooInstance[]) => {
        this.instances = list;
        this.loading = false;
      },
      error: () => {
        this.error = 'Error al cargar instancias';
        this.loading = false;
      }
    });
  }
}
