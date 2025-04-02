import { Component, OnInit } from '@angular/core';
import { OdooService } from '../../services/odoo.service';

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

  constructor(private odooService: OdooService) {}

  ngOnInit(): void {
    this.loadInstances();
  }

  loadInstances(): void {
    this.odooService.getInstances().subscribe(instances => {
      this.productionBranches = instances.filter(inst => inst.category.toUpperCase() === 'PRODUCTION');
      this.stagingBranches = instances.filter(inst => inst.category.toUpperCase() === 'STAGING');
      this.developmentBranches = instances.filter(inst => inst.category.toUpperCase() === 'DEVELOPMENT');
    });
  }

  addOdooInstance(category: string) {
    const instanceName = prompt(`Ingrese el nombre de la nueva instancia en ${category}:`);
    if (instanceName) {
      const newTab = window.open("", "_blank");
      if (newTab) {
        newTab.document.write("<p style='font-size:20px; text-align:center;'>Creando la instancia de Odoo... Por favor, espere.</p>");
      }

      this.odooService.createOdooInstance(instanceName, category).subscribe(response => {
        alert(response.message);

        if (response.url) {
          console.log("Redirigiendo a: ", response.url);
          if (newTab) {
            newTab.location.href = response.url;
          } else {
            alert("No se pudo abrir automáticamente. Acceda a: " + response.url);
          }

          // **Agregar la nueva instancia y actualizar la lista**
          const newBranch = { name: instanceName, category, url: response.url };
          if (category === 'PRODUCTION') {
            this.productionBranches.push(newBranch);
          } else if (category === 'STAGING') {
            this.stagingBranches.push(newBranch);
          } else {
            this.developmentBranches.push(newBranch);
          }
        } else {
          alert("Error: La URL de Odoo no está disponible.");
        }
      });
    }
  }
}
