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
    this.odooService.createOdooInstance(instanceName).subscribe(response => {
      alert(response.message); // Muestra el mensaje que recibimos del backend
      
      // Extraer la URL correctamente
      const odooUrl = response.message.split('URL: ')[1];  // Obtener la URL de la respuesta

      // Verificar si la URL es válida antes de abrirla
      if (odooUrl) {
        console.log("Redirigiendo a: ", odooUrl);  // Verifica que la URL sea correcta
        window.open(odooUrl, '_blank');  // Abre Odoo en una nueva ventana
      } else {
        alert("Error: La URL de Odoo no está disponible.");
      }

      // Agregar la nueva rama
      this.branches.push({ name: instanceName, category: 'DEVELOPMENT', status: 'blue' });
    });
  }
}

}
