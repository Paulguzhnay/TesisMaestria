import { Component, OnInit} from '@angular/core';
import { OdooService } from '../../services/odoo.service';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ChangeDetectorRef } from '@angular/core';
import { OdooInstance } from '../../models/odoo-instance.model';
@Component({
  selector: 'app-import-database',
  standalone: false,
  templateUrl: './import-database.component.html',
  styleUrl: './import-database.component.css'
})
export class ImportDatabaseComponent implements OnInit {
  instances: OdooInstance[] = [];
  selectedInstance: OdooInstance | null = null;
  selectedFile: File | null = null;
  projectName: string = '';
  projectId: number = 0;
  instanceName = '';
  category = '';
  selectedDbFile: File | null = null;
  selectedOdooFile: File | null = null;


  constructor(
    private odooService: OdooService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

    ngOnInit(): void {
      const routeParams = this.route.parent?.paramMap;
      const queryParams = this.route.snapshot.queryParamMap;

      routeParams?.subscribe((params) => {
        const projectName = params.get('projectName');
        const instanceName = params.get('instanceName');
        const category = queryParams.get('category');

        console.log('🧭 Parámetros de ruta:', projectName, instanceName, category);

        if (projectName && instanceName && category) {
          this.projectName = decodeURIComponent(projectName);
          this.instanceName = decodeURIComponent(instanceName);
          this.category = category;

          this.loadInstance(); // ✅ Cargar la instancia seleccionada automáticamente
        }
      });
    }

    loadInstance(): void {
      this.odooService.getByProject(this.projectName).subscribe({
        next: (data) => {
          this.instances = data;
          console.log('📦 Datos de instancias recibidos:', data);
          const found = data.find(i => i.name === this.instanceName && i.category === this.category);
          if (found) {
            this.selectedInstance = found;
            this.projectId = found.project?.id || 0;
            console.log('✅ Instancia encontrada:', this.selectedInstance);
            console.log('🆔 ID del proyecto desde instancia:', this.projectId);
          }
        },
        error: () => console.error('❌ Error al cargar instancias del proyecto')
      });
    }

onFilesSelected(event: any): void {
  const files: FileList = event.target.files;

  this.selectedDbFile = null;
  this.selectedOdooFile = null;

  for (let i = 0; i < files.length; i++) {
    const file = files[i];
    if (file.name.endsWith('.dump')) {
      this.selectedDbFile = file;
    } else if (file.name.endsWith('.tar.gz')) {
      this.selectedOdooFile = file;
    }
  }

  if (!this.selectedDbFile) {
    this.snackBar.open('❌ Debes seleccionar un archivo .dump para la base de datos.', 'Cerrar', {
      duration: 6000,
      panelClass: ['error-snackbar']
    });
  }
}

    importDatabase(): void {
      if (!this.selectedDbFile || !this.selectedInstance) return;

      const formData = new FormData();
      formData.append('dbFile', this.selectedDbFile);
      formData.append('name', this.selectedInstance.name);
      formData.append('category', this.selectedInstance.category);

      if (this.selectedOdooFile) {
        formData.append('odooFile', this.selectedOdooFile);
      }

      this.http.post('http://localhost:8080/docker/import-db-advanced', formData, {
        reportProgress: true,
        observe: 'events',
        responseType: 'text'
      }).subscribe({
        next: (event) => {
          if (event.type === HttpEventType.Response) {
            const responseText = event.body || '';
            this.snackBar.open(responseText, 'Cerrar', { duration: 6000 });
          }
        },
        error: (err) => {
          console.error('❌ Error al importar:', err);
          this.snackBar.open('❌ Error al importar.', 'Cerrar', { duration: 6000 });
        }
      });
    }
}

