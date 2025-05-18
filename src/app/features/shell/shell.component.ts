import { Component, OnInit, OnDestroy } from '@angular/core';
import { Terminal } from 'xterm';
import { FitAddon } from 'xterm-addon-fit';
import { HttpClient } from '@angular/common/http';
import { ProjectService } from '../../services/project.service';
import { ActivatedRoute } from '@angular/router';
import { OdooInstance } from '../../models/odoo-instance.model';

@Component({
  selector: 'app-shell',
  standalone: false,
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.css']
})
export class ShellComponent implements OnInit, OnDestroy {
  terminal!: Terminal;
  fitAddon!: FitAddon;
  instances: OdooInstance[] = [];
  selectedContainer: string = '';
  projectName: string = '';
  loadingLogs = false;
  private intervalId: any;

  constructor(
    private http: HttpClient,
    private projectService: ProjectService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.initTerminal();

    const currentUrl = window.location.pathname;
    if (currentUrl.includes('/projects/')) {
      const parts = currentUrl.split('/projects/')[1].split('/');
      this.projectName = decodeURIComponent(parts[0]);

      this.loadInstances();
    } else {
      this.terminal.writeln(' No se detectó un proyecto activo.');
    }
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  initTerminal(): void {
    this.terminal = new Terminal({
      theme: { background: '#1e1e1e', foreground: '#ffffff' },
      fontSize: 14,
      disableStdin: true
    });
    this.fitAddon = new FitAddon();
    this.terminal.loadAddon(this.fitAddon);

    const container = document.querySelector('.terminal');
    if (container) this.terminal.open(container as HTMLElement);
  }

  loadInstances(): void {
    this.projectService.getInstancesByProject(this.projectName).subscribe({
      next: (res) => {
        this.instances = res;
        this.terminal.writeln(` Instancias encontradas: ${res.length}`);
      },
      error: () => {
        this.terminal.writeln(' Error al cargar instancias.');
      }
    });
  }

  verLogs(containerName: string): void {
    this.selectedContainer = containerName;
    this.terminal.clear();
    this.terminal.writeln(`📦 Viendo logs de: ${containerName}...`);

    // Limpiar cualquier intervalo anterior
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }

    // Cargar logs inicialmente
    this.fetchLogs(containerName);

    // Actualizar cada 5 segundos
    this.intervalId = setInterval(() => {
      this.fetchLogs(containerName);
    }, 5000);
  }

  fetchLogs(containerName: string): void {
    this.loadingLogs = true;
    this.http.get(`http://localhost:8080/api/shell/logs?containerName=${encodeURIComponent(containerName)}`, {
      responseType: 'text'
    }).subscribe({
      next: (logs) => {
        this.terminal.clear(); // Borra y vuelve a mostrar todo
        const lines = logs.split('\n');
        for (const line of lines) {
          this.terminal.writeln(line);
        }
        this.terminal.scrollToBottom();
        this.loadingLogs = false;
      },
      error: (err) => {
        this.terminal.writeln('❌ Error al obtener logs.');
        console.error(err);
        this.loadingLogs = false;
      }
    });
  }
}
