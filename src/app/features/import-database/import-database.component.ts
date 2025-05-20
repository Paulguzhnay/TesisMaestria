import { Component, OnInit} from '@angular/core';
import { OdooService } from '../../services/odoo.service';
import { HttpClient, HttpEventType } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
@Component({
  selector: 'app-import-database',
  standalone: false,
  templateUrl: './import-database.component.html',
  styleUrl: './import-database.component.css'
})
export class ImportDatabaseComponent implements OnInit {
  instances: any[] = [];
  selectedInstance: any;
  selectedFile: File | null = null;
  projectName: string = '';
  projectId: number = 0;

  constructor(
    private odooService: OdooService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

    ngOnInit(): void {
       console.log("ngOnInit de Import db se ha ejecutado");
      this.route.paramMap.subscribe((params: import('@angular/router').ParamMap) => {
        const nameFromRoute = params.get('name');
        console.log("IDB Nombre del proyecto desde la ruta:", nameFromRoute);
        if (nameFromRoute) {
          this.projectName = decodeURIComponent(nameFromRoute);
          console.log("IDB Proyecto detectado (parseado manual):", this.projectName);

          this.route.queryParamMap.subscribe((queryParams: import('@angular/router').ParamMap) => {
            const idFromQuery = queryParams.get('id');
            console.log("ID del proyecto desde la query:", idFromQuery);
            if (idFromQuery) {
              console.log("ID del proyecto detectado (parseado manual):", idFromQuery);
              this.projectId = +idFromQuery;
              this.loadInstancesForProject();
            }
          });
        }
      });
    }

  loadInstancesForProject(): void {
    console.log('Project name:', this.projectName)
    this.odooService.getByProject(this.projectName).subscribe({
      next: (data) => {
        this.instances = data;
      },
      error: () => console.error('Error al cargar instancias del proyecto')
    });
  }

  onFileSelected(event: any): void {
    this.selectedFile = event.target.files[0] || null;
  }

    importDatabase(): void {
      if (!this.selectedFile || !this.selectedInstance) return;

      const formData = new FormData();
      formData.append('file', this.selectedFile);
      formData.append('name', this.selectedInstance.name);
      formData.append('category', this.selectedInstance.category);

      this.http.post('http://localhost:8080/docker/import-db', formData, {
        reportProgress: true,
        observe: 'events',
        responseType: 'text'
      }).subscribe({
        next: (event) => {
          if (event.type === HttpEventType.Response) {
            const responseText = event.body || '';

            if (responseText.includes('✅')) {
              this.snackBar.open('✅ Base de datos importada exitosamente.', 'Cerrar', {
                duration: 6000,
                panelClass: ['success-snackbar']
              });
            } else if (responseText.includes('⚠️') || responseText.toLowerCase().includes('advertencia')) {
              this.snackBar.open('⚠️ Restauración completada con advertencias.', 'Cerrar', {
                duration: 6000,
                panelClass: ['warning-snackbar']
              });
            } else {
              this.snackBar.open('ℹ️ Resultado: ' + responseText, 'Cerrar', {
                duration: 6000
              });
            }
          }
        },
        error: (err) => {
          console.error('❌ Error al importar base de datos:', err);
          this.snackBar.open('❌ Error al importar la base de datos.', 'Cerrar', {
            duration: 6000,
            panelClass: ['error-snackbar']
          });
        }
      });
    }

}
