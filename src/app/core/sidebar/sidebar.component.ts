import { Component, OnInit } from '@angular/core';
import { OdooService } from '../../services/odoo.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatIcon } from '@angular/material/icon';
import { OdooInstance } from '../../models/odoo-instance.model';
import {
  CreateInstanceData,
  CreateInstanceDialogComponent
} from '../create-instance-dialog/create-instance-dialog.component';

@Component({
  selector: 'app-sidebar',
  standalone: false,
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  productionBranches: any[] = [];
  stagingBranches: any[] = [];
  developmentBranches: any[] = [];
  allInstances: any[] = [];
  projectName!: string;
  projectId!: number;

  selectedSource: any = null;
  selectedTarget: any = null;
  isProcessing = false;

  constructor(
    private odooService: OdooService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {
    this.router.events.subscribe(() => {
      const current = this.router.url;
      const urlTree = this.router.parseUrl(current);
      const segments = urlTree.root.children['primary']?.segments;

      const nameSegment = segments?.[1]?.path;
      const idParam = urlTree.queryParams['id'];

      if (nameSegment) {
        this.projectName = decodeURIComponent(nameSegment);
        console.log(" Proyecto detectado (parseado manual):", this.projectName);
      }

      if (idParam) {
        this.projectId = +idParam;
        console.log(" ID del proyecto detectado (parseado manual):", this.projectId);
      }

      if (this.projectName && this.projectId) {
        this.loadInstancesByProject(this.projectName);
      }
    });
  }

  ngOnInit(): void {
    console.log("✅ ngOnInit de SidebarComponent se ha ejecutado");
    this.route.paramMap.subscribe(params => {
      console.log("Parámetros de la ruta:", params.get('name'));
      const nameFromRoute = params.get('name');
      if (nameFromRoute) {
        console.log("Nombre del proyecto desde la ruta:", nameFromRoute);
        this.projectName = decodeURIComponent(nameFromRoute);
        console.log("Proyecto detectado:", this.projectName);

        this.route.queryParamMap.subscribe(queryParams => {
          const idFromQuery = queryParams.get('id');
          if (idFromQuery) {
            this.projectId = +idFromQuery;
            console.log("IID del proyecto detectado:", this.projectId);
            console.log("Cargando instancias para el proyecto:", this.projectName);
            this.loadInstancesByProject(this.projectName);
          }
        });
      }
    });
  }

  loadInstancesByProject(projectName: string): void {
    console.log("Cargando instancias para el proyecto:", projectName);
    this.odooService.getByProject(projectName).subscribe({
      next: (instances) => {
        console.log("Instancias cargadas:", instances);
        this.allInstances = instances;
        this.productionBranches = instances.filter(inst => inst.category === 'PRODUCTION');
        this.stagingBranches = instances.filter(inst => inst.category === 'STAGING');
        this.developmentBranches = instances.filter(inst => inst.category === 'DEVELOPMENT');
      },
      error: (error) => {
        console.error('Error al hacer merge:', error);

        if (error.status === 401) {
          this.snackBar.open('⚠️ Token expirado. Redirigiendo a GitHub...', 'Cerrar', {
            duration: 6000,
            panelClass: ['error-snackbar']
          });
          this.odooService.handleUnauthorized();
        } else {
          this.snackBar.open('❌ Error grave al hacer merge. Revisa los logs del backend.', 'Cerrar', {
            duration: 6000,
            panelClass: ['error-snackbar']
          });
        }
      }
    });
  }

  openCreateDialog(category: string): void {
    this.isProcessing = true;
    const ref = this.dialog.open<CreateInstanceDialogComponent, CreateInstanceData>(
      CreateInstanceDialogComponent, {
      width: '400px',
      data: {
        projectId: this.projectId,
        projectName: this.projectName,
        category: category
      }
    }
    );

    ref.afterClosed().subscribe(instance => {
      if (instance) {
        this.snackBar.open(`Instancia  creada correctamente`, 'Cerrar', { duration: 3000 });
        this.loadInstancesByProject(this.projectName);
      }
      this.isProcessing = false;
    });
  }

  loadInstances(): void {
    console.log("Cargando todas las instancias...");
    this.odooService.getInstances().subscribe({
      next: (instances) => {
        this.allInstances = instances;

        this.productionBranches = instances.filter(inst => inst.category.toUpperCase() === 'PRODUCTION');
        this.stagingBranches = instances.filter(inst => inst.category.toUpperCase() === 'STAGING');
        this.developmentBranches = instances.filter(inst => inst.category.toUpperCase() === 'DEVELOPMENT');
      },
      error: (error) => {
        console.error('Error al cargar instancias:', error);

        if (error.status === 401) {
          this.snackBar.open('⚠️ Token de GitHub expirado. Redirigiendo a login...', 'Cerrar', {
            duration: 4000,
            panelClass: 'error-snackbar'
          });
          this.odooService.handleUnauthorized();
        }
      }
    });
  }

  mergeBranches(): void {
    if (!this.selectedSource || !this.selectedTarget) {
      this.snackBar.open('Selecciona instancia origen y destino para hacer el merge.', 'Cerrar', { duration: 4000 });
      return;
    }

    if (this.selectedSource.name === this.selectedTarget.name) {
      this.snackBar.open('No puedes hacer merge de la misma instancia sobre sí misma.', 'Cerrar', { duration: 4000 });
      return;
    }

    const confirmacion = confirm(`¿Seguro que quieres hacer merge de ${this.selectedSource.name} ➡️ ${this.selectedTarget.name}?`);
    if (!confirmacion) return;

    this.isProcessing = true; // ⬅️ Activamos loading aquí

    console.log(" Enviando payload de merge:", {
      source: this.selectedSource.name,
      target: this.selectedTarget.name,
      projectId: this.projectId
    });

    this.odooService.mergeInstances(
      this.selectedSource.name,
      this.selectedTarget.name,
      this.projectId).subscribe({
        next: (response) => {
          console.log('Respuesta del merge:', response);

          if (response.includes('✅')) {
            this.snackBar.open(` Merge exitoso: ${response}`, 'Cerrar', { duration: 6000, panelClass: ['success-snackbar'] });
          } else if (response.includes('⚠️')) {
            this.snackBar.open(` Merge con advertencias: ${response}`, 'Cerrar', { duration: 6000, panelClass: ['warning-snackbar'] });
          } else if (response.includes('❌')) {
            this.snackBar.open(` Error en el merge: ${response}`, 'Cerrar', { duration: 6000, panelClass: ['error-snackbar'] });
          } else {
            this.snackBar.open(` Resultado del merge: ${response}`, 'Cerrar', { duration: 6000 });
          }

          this.loadInstances();
        },
        error: (error) => {
          console.error('Error al hacer merge:', error);

          if (error.status === 401) {
            this.snackBar.open('⚠️ Token expirado. Redirigiendo a GitHub...', 'Cerrar', {
              duration: 6000,
              panelClass: ['error-snackbar']
            });
            this.odooService.handleUnauthorized();
          } else {
            this.snackBar.open('❌ Error grave al hacer merge. Revisa los logs del backend.', 'Cerrar', {
              duration: 6000,
              panelClass: ['error-snackbar']
            });
          }
        },
        complete: () => {
          this.isProcessing = false; // ⬅ Finaliza loading cuando el observable termina
        }
      });
  }


  installCustomModules(instance: OdooInstance): void {
    this.isProcessing = true;
    if (!instance || !this.projectId) return;

    this.odooService.installModules(instance.name, instance.category, this.projectId).subscribe({
      next: (msg) => {
        this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
      },
      error: (err) => {
        console.error(err);
        this.snackBar.open('Error al instalar módulos', 'Cerrar', { duration: 4000 });
      },
      complete: () => this.isProcessing = false
    });
  }
}
