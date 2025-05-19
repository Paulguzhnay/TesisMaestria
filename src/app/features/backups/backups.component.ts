import { Component, OnInit } from '@angular/core';
import { BackupService } from '../../services/backup.service';
import { OdooService } from '../../services/odoo.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router } from '@angular/router';
import { OdooInstance } from '../../models/odoo-instance.model';

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

  constructor(
    private backupService: BackupService,
    private odooService: OdooService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

    ngOnInit(): void {
      const idParam = this.route.snapshot.queryParamMap.get('id');
      if (idParam) {
        this.projectId = Number(idParam);
        this.loadBackups();
        this.loadInstances();
      } else {
        console.error('❌ No se encontró projectId en la URL.');
      }
    }

  loadBackups(): void {
    this.backupService.getBackupsForProject(this.projectId).subscribe({
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

        this.groupedBackups = Object.entries(grouped).map(([timestamp, backups], index) => {
          const dbBackup = backups.find(b => b.name.includes('db_backup'));

          let category = '';
          let instanceName = '';
          if (dbBackup) {
            const parts = dbBackup.name.split('_');
            if (parts.length >= 4) {
              category = parts[2];
              instanceName = parts[3];
            }
          }

          return {
            timestamp,
            backups,
            category,
            name: instanceName,
            colorClass: index % 2 === 0 ? 'backup-group-a' : 'backup-group-b'
          };
        });
      },
      error: (error) => console.error('Error al obtener backups:', error)
    });
  }

  loadInstances(): void {
    this.odooService.getInstances().subscribe({
      next: (data) => {
        this.instances = data;
      },
      error: (error) => console.error('Error al cargar instancias:', error)
    });
  }

  createBackupForInstance(instance: OdooInstance): void {
    const payload = {
      name: instance.name,
      category: instance.category,
      projectId: this.projectId
    };
    this.backupService.createBackupForInstance(payload).subscribe({
      next: (response) => {
        this.showNotification('✅ Backup creado correctamente', 'success');
        this.loadBackups();
      },
      error: (error) => {
        console.error('Error al crear backup:', error);
        this.showNotification('❌ Error al crear backup', 'error');
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

    // Método de logout
logout(): void {
  localStorage.removeItem('token'); // o el nombre exacto de tu token
  this.router.navigate(['/login']); // Ajusta la ruta si tu login está en otra
}
}

