import { Component, OnInit } from '@angular/core';
import { BackupService } from '../../services/backup.service';
import { OdooService } from '../../services/odoo.service';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-backups',
  standalone: false,
  templateUrl: './backups.component.html',
  styleUrls: ['./backups.component.css']
})
export class BackupsComponent implements OnInit {
  isRestoring: boolean = false;
  restoringGroupId: string | null = null;
  currentStep: string = '';
  notificationMessage: string | null = null;
  notificationType: 'success' | 'error' | null = null;

  backups: any[] = [];

  //
  displayedColumns: string[] = ['name', 'time', 'branch', 'version', 'type', 'revision', 'actions'];
  backupsFlatList: any[] = [];  
//
groupedBackups: {
  timestamp: string;
  backups: any[];
  colorClass: string;
  category?: string;
  name?: string;
}[] = [];

  instances: any[] = []; // 🚀 Nueva propiedad

  constructor(private backupService: BackupService, private odooService: OdooService) { }

  ngOnInit(): void {
    this.loadBackups();
    this.loadInstances();
  }

  loadBackups(): void {
    this.backupService.getBackups().subscribe({
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
  
        // Agrupar por timestamp
        const grouped: { [key: string]: any[] } = {};
        transformed.forEach(b => {
          if (!grouped[b.groupId]) grouped[b.groupId] = [];
          grouped[b.groupId].push(b);
        });
  
        // 🛠 Enriquecer cada grupo con category + instanceName
        this.groupedBackups = Object.entries(grouped).map(([timestamp, backups], index) => {
          // Buscar el archivo de base de datos
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
  
  

  loadInstances() {
    this.odooService.getInstances().subscribe({
      next: (data) => {
        console.log('Instancias cargadas:', data);
        this.instances = data;
      },
      error: (error) => {
        console.error('Error al cargar instancias:', error);
      }
    });
  }

  createBackup(instance: any) {
    this.backupService.createBackupForInstance({ name: instance.name, category: instance.category }).subscribe({
      next: (message) => {
        console.log('✅ Backup creado:', message);
        this.showNotification('✅ Backup creado correctamente.', 'success');
      },
      error: (error) => {
        console.error('❌ Error creando backup:', error);
        this.showNotification('❌ Error creando backup.', 'error');
      }
    });
  }
  
  deleteInstance(instance: any) {
    if (!confirm(`¿Estás seguro de eliminar la instancia ${instance.name}?`)) return;
  
    this.odooService.deleteInstance(instance.name, instance.category).subscribe({
      next: (message) => {
        console.log('✅ Instancia eliminada:', message);
        this.showNotification('✅ Instancia eliminada.', 'success');
        this.loadInstances(); // Volver a cargar lista
      },
      error: (error) => {
        console.error('❌ Error eliminando instancia:', error);
        this.showNotification('❌ Error eliminando instancia.', 'error');
      }
    });
  }


  //-------------

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

  downloadBackup(backup: any) {
    const fileName = encodeURIComponent(backup.name);
    const downloadUrl = `http://localhost:8080/docker/download/${fileName}`;
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  deleteBackup(backup: any) {
    if (!confirm(`¿Seguro que deseas eliminar ${backup.name}?`)) {
      return;
    }
    this.backupService.deleteBackup(backup.name).subscribe({
      next: (response) => {
        this.showNotification('✅ Backup eliminado', 'success');
        this.loadBackups();
      },
      error: (error) => {
        console.error('Error al eliminar backup:', error);
        this.showNotification('❌ Error al eliminar backup', 'error');
      }
    });
  }

  createBackupForInstance(instance: any) {
    const payload = { name: instance.name, category: instance.category };
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
//RESTORE BACKUP
  restoreBackupGroup(group: any): void {
    this.isRestoring = true;
    this.currentStep = 'Iniciando...';
    this.restoringGroupId = group.timestamp;

    const dbFile = group.backups.find((b: { name: string }) => b.name.includes('db_backup'))?.name;
    const odooFile = group.backups.find((b: { name: string }) => b.name.includes('odoo_data'))?.name;

    if (!dbFile || !odooFile) {
      this.showNotification('❌ No se encontraron archivos válidos.', 'error');
      this.isRestoring = false;
      return;
    }

    const match = dbFile.match(/^db_backup_([A-Z]+)_(.+?)_/);
    let category = 'UNKNOWN';
    let name = 'undefined';

    if (match && match.length >= 3) {
      category = match[1];
      name = match[2];
    } else {
      this.showNotification('❌ No se pudo determinar la categoría.', 'error');
      this.isRestoring = false;
      return;
    }

    const payload = {
      name,
      category,
      dbBackupFileName: dbFile,
      odooBackupFileName: odooFile
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
    const timestampMatch = dbFile.match(/\d{13}/); // Capturar timestamp en el nombre
    if (!timestampMatch) {
      this.showNotification('❌ No se pudo identificar el grupo del backup.', 'error');
      this.isRestoring = false;
      return;
    }
    const timestamp = timestampMatch[0];
  
    const odooFile = this.backupsFlatList.find(b => 
      b.name.includes('odoo_data') && b.name.includes(timestamp)
    )?.name;
  
    if (!odooFile) {
      this.showNotification('❌ No se encontró el backup de archivos Odoo asociado.', 'error');
      this.isRestoring = false;
      return;
    }
  
    const match = dbFile.match(/^db_backup_([A-Z]+)_(.+?)_/);
    let category = 'UNKNOWN';
    let name = 'undefined';
  
    if (match && match.length >= 3) {
      category = match[1];
      name = match[2];
    } else {
      this.showNotification('❌ No se pudo determinar la categoría.', 'error');
      this.isRestoring = false;
      return;
    }
  
    const payload = {
      name,
      category,
      dbBackupFileName: dbFile,
      odooBackupFileName: odooFile
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
  
 
  
}
