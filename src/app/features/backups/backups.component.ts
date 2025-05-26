import { Component, OnInit } from '@angular/core';
import { BackupService } from '../../services/backup.service';
import { OdooService } from '../../services/odoo.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { OdooInstance } from '../../models/odoo-instance.model';
import { Project } from '../../models/project.model';
@Component({
  selector: 'app-backups',
  standalone: false,
  templateUrl: './backups.component.html',
  styleUrls: ['./backups.component.css']
})
export class BackupsComponent implements OnInit {
  isRestoring = false;
  restoringGroupId: string | null = null;
  currentStep = '';
  notificationMessage: string | null = null;
  notificationType: 'success' | 'error' | null = null;
  projectId = 0;

  backups: any[] = [];
  backupsFlatList: any[] = [];
  groupedBackups: {
    timestamp: string;
    backups: any[];
    colorClass: string;
    category?: string;
    name?: string;
  }[] = [];

  instances: OdooInstance[] = [];

  projectName = '';
  instanceName = '';
  category = '';
  filteredInstance: OdooInstance | null = null;
  

  constructor(
    private backupService: BackupService,
    private odooService: OdooService,
    private route: ActivatedRoute,
    private router: Router,
    private projectService: ProjectService,
  ) {}

    ngOnInit(): void {
      this.route.parent?.paramMap.subscribe(params => {
        this.projectName = params.get('projectName') || '';
        this.instanceName = params.get('instanceName') || '';

        if (!this.projectName || !this.instanceName) {
          console.error('❌ projectName o instanceName está vacío. No se puede continuar.');
          return;
        }

        this.category = this.route.snapshot.queryParamMap.get('category') || '';
        
        this.loadInstances();
        this.loadProjectIdByName();
      });
    }

      loadInstances(): void {
        this.odooService.getInstancesByProject(this.projectName).subscribe({
          next: (data) => {
            this.instances = data;
            this.filteredInstance = this.instances.find(i =>
              i.name === this.instanceName && i.category === this.category
            ) || null;
          },
          error: (error) => console.error('Error al cargar instancias:', error)
        });
      }
        
      loadBackups(): void {
        this.backupService.getBackupsForInstance(this.instanceName, this.category).subscribe({
          next: (data) => {
            const transformed = data.map((backup: any) => {
              const match = backup.name.match(/\d{13}/);
              const timestamp = match ? match[0] : 'unknown';
              return {
                name: backup.name,
                time: backup.time || new Date(Number(timestamp)).toISOString(),
                branch: backup.branch || 'Desconocido',
                version: backup.version || '15.0',
                type: backup.type || 'Manual',
                revision: backup.revision || 'N/A',
                groupId: timestamp
              };
            });

            const grouped: { [key: string]: any[] } = {};
            transformed.forEach(b => {
              if (!grouped[b.groupId]) grouped[b.groupId] = [];
              grouped[b.groupId].push(b);
            });

            this.groupedBackups = Object.entries(grouped).map(([timestamp, backups], index) => ({
              timestamp,
              backups,
              category: this.category,
              name: this.instanceName,
              colorClass: index % 2 === 0 ? 'backup-group-a' : 'backup-group-b'
            }));
          },
          error: (error) => console.error('❌ Error al obtener backups:', error)
        });
      }

 



    createBackupForInstance(instance: OdooInstance): void {
      const payload = {
        name: instance.name,
        category: instance.category,
        projectId: this.projectId
      };

      console.log(" Payload enviado para backup:", payload);

      this.backupService.createBackupForInstance(payload).subscribe({
        next: (response) => {
          console.log('Backup creado:', response);
          this.showNotification(response.message, 'success');
          this.loadBackups();
        },
        error: (error) => {
          console.log('Error al crear backup:', error)
          console.error('Error al crear backup:', error);
          const msg = error.error?.message || '❌ Error al crear backup';
          this.showNotification(msg, 'error');
        }
      });
    }


    loadProjectIdByName(): void {
      this.projectService.getAll().subscribe({
        next: (projects: Project[]) => {
          const found = projects.find((p: Project) => p.name === this.projectName);
          if (found) {
            this.projectId = found.id ?? 0;
            this.loadBackups(); // cargar backups solo si ya tienes el ID correcto
          } else {
            console.error('❌ No se encontró el proyecto con nombre:', this.projectName);
          }
        },
        error: (err: any) => {
          console.error('Error al obtener proyectos:', err);
        }
      });
    }

 

  deleteInstance(instance: OdooInstance): void {
    if (!confirm(`¿Estás seguro de eliminar la instancia ${instance.name}?`)) return;

    this.odooService.deleteInstance(instance.name, instance.category).subscribe({
      next: () => {
        this.showNotification('✅ Instancia eliminada.', 'success');
        this.loadInstances();
      },
      error: (error) => {
        console.error('❌ Error eliminando instancia:', error);
        this.showNotification('❌ Error eliminando instancia.', 'error');
      }
    });
  }

  restoreBackupGroup(group: any): void {
    this.isRestoring = true;
    this.currentStep = 'Iniciando...';
    this.restoringGroupId = group.timestamp;

    const dbFile = group.backups.find((b: any) => b.name.includes('db_backup'))?.name;
    const odooFile = group.backups.find((b: any) => b.name.includes('odoo_data'))?.name;

    if (!dbFile || !odooFile) {
      this.showNotification('❌ No se encontraron archivos válidos.', 'error');
      this.isRestoring = false;
      return;
    }

    // Nuevo patrón: db_backup_<project>_<category>_<instance>_<timestamp>
      const match = dbFile.match(/^db_backup_([^_]+)_([A-Z]+)_([^_]+)_/);
      if (!match || match.length < 4) {
        this.showNotification('❌ No se pudo determinar el proyecto, categoría o instancia.', 'error');
        this.isRestoring = false;
        return;
      }

      const [, projectFromFile, categoryFromFile, instanceFromFile] = match;

      const payload = {
        name: instanceFromFile,
        category: categoryFromFile,
        dbBackupFileName: dbFile,
        odooBackupFileName: odooFile,
        projectId: this.projectId
      };
      console.log('Payload para restauración:', payload);

    this.backupService.restoreSpecific(payload).subscribe({
      next: (response: string) => {
        this.showNotification(response, 'success');
        this.isRestoring = false;
        this.currentStep = '';
      },
      error: (error) => {
        console.error('Error al restaurar:', error);
        this.showNotification('❌ Error durante la restauración', 'error');
        this.isRestoring = false;
        this.currentStep = '';
      }
    });
  }


      restoreBackup(backup: any): void {
        this.isRestoring = true;
        this.currentStep = 'Iniciando...';

        const dbFile = backup.name;
        const timestamp = dbFile.match(/\d{13}/)?.[0];
        if (!timestamp) {
          this.showNotification('❌ No se pudo identificar el grupo del backup.', 'error');
          this.isRestoring = false;
          return;
        }

        const odooFile = this.backupsFlatList.find(b => b.name.includes('odoo_data') && b.name.includes(timestamp))?.name;
        if (!odooFile) {
          this.showNotification('❌ No se encontró el backup de archivos Odoo asociado.', 'error');
          this.isRestoring = false;
          return;
        }

        const match = dbFile.match(/^db_backup_([A-Z]+)_(.+?)_/);
        if (!match || match.length < 3) {
          this.showNotification('❌ No se pudo determinar la categoría.', 'error');
          this.isRestoring = false;
          return;
        }

        const [_, category, name] = match;

        const payload = {
          name,
          category,
          dbBackupFileName: dbFile,
          odooBackupFileName: odooFile,
          projectId: this.projectId
        };

        this.backupService.restoreSpecific(payload).subscribe({
          next: (response: string) => {
            this.showNotification(response, 'success');
            this.isRestoring = false;
            this.currentStep = '';
          },
          error: (error) => {
            console.error('Error al restaurar:', error);
            this.showNotification('❌ Error durante la restauración', 'error');
            this.isRestoring = false;
            this.currentStep = '';
          }
        });
      }

  showNotification(message: string, type: 'success' | 'error') {
    this.notificationMessage = message;
    this.notificationType = type;
    setTimeout(() => {
      this.notificationMessage = null;
      this.notificationType = null;
    }, 5000);
  }

  formatTimestamp(timestamp: string): string {
    const date = new Date(Number(timestamp));
    return date.toLocaleString('es-EC', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });
  }

  deleteBackup(backup: any): void {
    if (!confirm(`¿Seguro que deseas eliminar ${backup.name}?`)) return;
    this.backupService.deleteBackup(backup.name).subscribe({
      next: () => {
        this.showNotification('✅ Backup eliminado', 'success');
        this.loadBackups();
      },
      error: (error) => {
        console.error('Error al eliminar backup:', error);
        this.showNotification('❌ Error al eliminar backup', 'error');
      }
    });
  }

  downloadBackup(backup: any): void {
    const fileName = encodeURIComponent(backup.name);
    const downloadUrl = `http://localhost:8080/docker/download/${fileName}`;
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  logout(): void {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }
}


