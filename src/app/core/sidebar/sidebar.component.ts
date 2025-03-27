import { Component } from '@angular/core';
import { OdooService } from '../../services/odoo.service';


@Component({
  selector: 'app-sidebar',
  standalone: false,
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  branches = [
    { name: 'master', category: 'PRODUCTION', status: 'green' },
    { name: 'saas-24', category: 'STAGING', status: 'green' },
 
    { name: 'master ', category: 'DEVELOPMENT', status: 'blue' }
  ];

constructor (private odooService: OdooService){}


addOdooInstance() {
  const instanceName = prompt("Ingrese el nombre de la nueva instancia de Odoo:");
  if (instanceName) {
    // Abrimos una nueva pestaña con un mensaje de carga
    const newTab = window.open("", "_blank");
    if (newTab) {
      newTab.document.write("<p style='font-size:20px; text-align:center;'>Creando la instancia de Odoo... Por favor, espere.</p>");
    }

    this.odooService.createOdooInstance(instanceName).subscribe(response => {
      alert(response.message); // Mostramos el mensaje de éxito

      if (response.url) {
        console.log("Redirigiendo a: ", response.url);
        if (newTab) {
          newTab.location.href = response.url; // Redirigir la pestaña ya abierta a la URL de Odoo
        } else {
          alert("No se pudo abrir automáticamente. Acceda a: " + response.url);
        }
      } else {
        alert("Error: La URL de Odoo no está disponible.");
      }

      // Agregar la nueva rama en la UI
      this.branches.push({ name: instanceName, category: 'DEVELOPMENT', status: 'blue' });
    });
  }
}









}
