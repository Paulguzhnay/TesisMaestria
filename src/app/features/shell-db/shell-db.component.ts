import { Component } from '@angular/core';
import { ShellService } from '../../services/shell.service';
import { Router, ActivatedRoute } from '@angular/router';
 

@Component({
  selector: 'app-shell-db',
  standalone: false,
  templateUrl: './shell-db.component.html',
  styleUrl: './shell-db.component.css'
})
export class ShellDbComponent {
  command: string = '';
  result: string = '';
  isLoading: boolean = false;
  projectName: string = '';
  instanceName: string = '';
  category: string = ''; // 🔧 propiedad que antes causaba el error

  constructor(
    private shellService: ShellService,
    private route: ActivatedRoute
  ) {
    // Ruta: /projects/:name/shell-db?instance=test-01&category=DEVELOPMENT
    const nameFromUrl = this.route.snapshot.paramMap.get('name');
    if (nameFromUrl) {
      this.projectName = decodeURIComponent(nameFromUrl);
    }

    const queryParams = this.route.snapshot.queryParamMap;
    this.instanceName = queryParams.get('instance') || '';
    this.category = queryParams.get('category') || '';
  }

  executeCommand(): void {
    if (!this.command.trim()) {
      this.result = '❗ El comando no puede estar vacío';
      return;
    }

    this.isLoading = true;
    this.result = '⏳ Ejecutando...';

    const payload = {
      command: this.command,
      name: this.instanceName,
      category: this.category
    };

    this.shellService.executeDbCommand(payload).subscribe({
      next: (output: string) => {
        this.result = output;
        this.isLoading = false;
      },
      error: (err) => {
        this.result = '❌ Error ejecutando comando: ' + (err?.error || 'Error desconocido');
        this.isLoading = false;
      }
    });
  }
}

