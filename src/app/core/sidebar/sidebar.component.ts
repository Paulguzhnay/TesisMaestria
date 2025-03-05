import { Component } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';

@Component({
  selector: 'app-sidebar',
  standalone: false,
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent {
  menuItems = [
    { name: 'Dashboard', path: '/dashboard' },
    { name: 'Settings', path: '/settings' },
    { name: 'Reports', path: '/reports' },
    { name: 'Users', path: '/users' }
  ];

  drop(event: CdkDragDrop<string[]>) {
    moveItemInArray(this.menuItems, event.previousIndex, event.currentIndex);
    this.saveOrder();
  }

  saveOrder() {
    // Aquí deberíamos llamar a un servicio para guardar el nuevo orden en el backend
    console.log('Nuevo orden guardado:', this.menuItems);
  }
}
