import { Component, OnInit} from '@angular/core';
import { ShellService } from '../../services/shell.service';
import { Router, ActivatedRoute } from '@angular/router';
 

@Component({
  selector: 'app-shell-db',
  standalone: false,
  templateUrl: './shell-db.component.html',
  styleUrl: './shell-db.component.css'
})
export class ShellDbComponent implements OnInit {
  command: string = 'SELECT * FROM res_users;';
  result: string = '';
  isLoading: boolean = false;

  projectName = '';
  instanceName = '';
  category = '';
  csvRows: string[][] = [];

  constructor(
    private route: ActivatedRoute,
    private shellService: ShellService
  ) {}

    ngOnInit(): void {
      const parentRoute = this.route.parent;
      if (!parentRoute) {
        this.result = '❌ No se pudo acceder a la ruta padre.';
        return;
      }

      parentRoute.paramMap.subscribe(params => {
        this.projectName = params.get('projectName') || '';
        this.instanceName = params.get('instanceName') || '';
        this.category = parentRoute.snapshot.queryParamMap.get('category') || '';

        console.log('🔍 Parámetros:', {
          projectName: this.projectName,
          instanceName: this.instanceName,
          category: this.category
        });

        if (this.instanceName && this.category) {
          this.executeCommand(); // Ejecutar automáticamente
        }
      });
    }

  executeCommand(): void {
    if (!this.command.trim()) {
      this.result = '❗ El comando no puede estar vacío';
      return;
    }

    this.isLoading = true;
    this.result = '⏳ Ejecutando...';
    this.csvRows = [];

    const payload = {
      command: this.command,
      name: this.instanceName,
      category: this.category
    };

    console.log('➡ Enviando comando con:', payload);

    this.shellService.executeDbCommand(payload).subscribe({
      next: (output: string) => {
        this.result = output;
        this.isLoading = false;

        // Intentar parsear como CSV
        this.csvRows = output
          .trim()
          .split('\n')
          .map(row => row.split(',').map(cell => cell.replace(/^"|"$/g, '').trim()));
      },
      error: (err) => {
        this.result = '❌ Error ejecutando comando: ' + (err?.error || 'Error desconocido');
        this.isLoading = false;
        this.csvRows = [];
      }
    });
  }
}

