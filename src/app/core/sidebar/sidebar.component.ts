import { Component, OnInit } from '@angular/core';
import { OdooService } from '../../services/odoo.service';
import { MatSnackBar } from '@angular/material/snack-bar'; 
import { ActivatedRoute, Router } from '@angular/router';
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

  selectedSource: any = null;
  selectedTarget: any = null;
  showSidebar = false;

  constructor(
    private odooService: OdooService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.router.events.subscribe(() => {
      const currentUrl = this.router.url;
      if (currentUrl.includes('/projects/')) {
        this.showSidebar = true;
        const projectName = currentUrl.split('/projects/')[1];
        this.loadInstancesByProject(projectName);
      } else {
        this.showSidebar = false;
        this.productionBranches = [];
        this.stagingBranches = [];
        this.developmentBranches = [];
      }
    });
  }
  loadInstancesByProject(projectName: string): void {
    this.odooService.getByProject(projectName).subscribe({
      next: (instances) => {
        this.allInstances = instances;
        this.productionBranches = instances.filter(inst => inst.category === 'PRODUCTION');
        this.stagingBranches = instances.filter(inst => inst.category === 'STAGING');
        this.developmentBranches = instances.filter(inst => inst.category === 'DEVELOPMENT');
      },
      error: (error) => {
        console.error('Error al cargar instancias:', error);
      }
    });
  }

  loadInstances(): void {
    this.odooService.getInstances().subscribe({
      next: (instances) => {
        this.allInstances = instances;

        this.productionBranches = instances.filter(inst => inst.category.toUpperCase() === 'PRODUCTION');
        this.stagingBranches = instances.filter(inst => inst.category.toUpperCase() === 'STAGING');
        this.developmentBranches = instances.filter(inst => inst.category.toUpperCase() === 'DEVELOPMENT');
      },
      error: (error) => {
        console.error('Error al cargar instancias:', error);
      }
    });
  }

  addOdooInstance(category: string): void {
    const instanceName = prompt(`Ingrese el nombre de la nueva instancia en ${category}:`);
    if (instanceName) {
      const newTab = window.open("", "_blank");
      if (newTab) {
        newTab.document.write("<p style='font-size:20px; text-align:center;'>Creando la instancia de Odoo... Por favor, espere.</p>");
      }

      this.odooService.createOdooInstance(instanceName, category).subscribe({
        next: (response) => {
          alert(response.message);

          if (response.url) {
            console.log("Redirigiendo a:", response.url);
            if (newTab) {
              newTab.location.href = response.url;
            } else {
              alert("No se pudo abrir automáticamente. Acceda a: " + response.url);
            }

            this.loadInstances(); // 🔥 Recargar para actualizar la lista
          } else {
            alert("Error: La URL de Odoo no está disponible.");
          }
        },
        error: (error) => {
          console.error('Error al crear instancia:', error);
          alert('❌ Error creando instancia.');
        }
      });
    }
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
  
    this.odooService.mergeInstances(this.selectedSource.name, this.selectedTarget.name, this.selectedTarget.category).subscribe({
      next: (response) => {
        console.log('Respuesta del merge:', response);
  
        if (response.includes('✅')) {
          this.snackBar.open(`✅ Merge exitoso: ${response}`, 'Cerrar', { duration: 6000, panelClass: ['success-snackbar'] });
        } else if (response.includes('⚠️')) {
          this.snackBar.open(`⚠️ Merge con advertencias: ${response}`, 'Cerrar', { duration: 6000, panelClass: ['warning-snackbar'] });
        } else if (response.includes('❌')) {
          this.snackBar.open(`❌ Error en el merge: ${response}`, 'Cerrar', { duration: 6000, panelClass: ['error-snackbar'] });
        } else {
          this.snackBar.open(`ℹ️ Resultado del merge: ${response}`, 'Cerrar', { duration: 6000 });
        }
  
        this.loadInstances();
      },
      error: (error) => {
        console.error('Error al hacer merge:', error);
        this.snackBar.open('❌ Error grave al hacer merge. Revisa los logs del backend.', 'Cerrar', { duration: 6000, panelClass: ['error-snackbar'] });
      }
    });
  }
  
}
