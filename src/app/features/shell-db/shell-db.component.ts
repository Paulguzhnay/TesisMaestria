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
  command: string = '';
  result: string = '';
  isLoading: boolean = false;

  projectName: string = '';
  instances: any[] = [];
  selectedInstance: any = null;
  csvRows: string[][] = [];

  constructor(
    private shellService: ShellService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const nameFromUrl = params.get('name');
      if (nameFromUrl) {
        this.projectName = decodeURIComponent(nameFromUrl);
        this.loadInstances();
      }
    });
  }

  loadInstances(): void {
    this.shellService.getInstancesByProject(this.projectName).subscribe({
      next: (data) => {
        this.instances = data;
        console.log('📦 Instancias cargadas:', data);
      },
      error: () => {
        this.result = '❌ Error al cargar instancias del proyecto.';
      }
    });
  }

executeCommand(): void {
  if (!this.command.trim()) {
    this.result = '❗ El comando no puede estar vacío';
    return;
  }

  if (!this.selectedInstance) {
    this.result = '❗ Debes seleccionar una instancia.';
    return;
  }

  this.isLoading = true;
  this.result = '⏳ Ejecutando...';
  this.csvRows = [];

  const payload = {
    command: this.command,
    name: this.selectedInstance.name,
    category: this.selectedInstance.category
  };

  console.log('➡ Enviando comando con:', payload);

  this.shellService.executeDbCommand(payload).subscribe({
    next: (output: string) => {
      this.result = output;
      this.isLoading = false;

      // Parsear CSV simple por filas y columnas
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

