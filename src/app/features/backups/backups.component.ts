import { Component, OnInit } from '@angular/core';
import { BackupService } from '../../services/backup.service';
import { MatProgressBarModule } from '@angular/material/progress-bar';


@Component({
  selector: 'app-backups',
  standalone: false,
  templateUrl: './backups.component.html',
  styleUrls: ['./backups.component.css'] // ✅

})
export class BackupsComponent implements OnInit {
  isRestoring: boolean = false; // Para mostrar spinner o bloquear botones
  restoringGroupId: string | null = null; // Para saber cuál grupo está en restauración
  currentStep: string = '';
  notificationMessage: string | null = null;
  notificationType: 'success' | 'error' | null = null;


  backups: any[] = [];
  groupedBackups: {
    timestamp: string;
    backups: any[];
    colorClass: string;
  }[] = [];
  constructor(private backupService: BackupService) { }

  ngOnInit(): void {

    this.loadBackups();
  }

  loadBackups(): void {
    this.backupService.getBackups().subscribe({
      next: (data) => {
        console.log('Backups cargados:', data);
  
        //  Transformar cada backup a estructura con datos enriquecidos
        const transformed = data.map((backup: any) => {
          const match = backup.name.match(/\d+/); // Extraer el timestamp del nombre
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
  
        //   Agrupar por timestamp
        const grouped: { [key: string]: any[] } = {};
        transformed.forEach(b => {
          if (!grouped[b.groupId]) grouped[b.groupId] = [];
          grouped[b.groupId].push(b);
        });
  
        //   Aplicar clases alternas para colores
        this.groupedBackups = Object.entries(grouped).map(([timestamp, files], index) => ({
          timestamp,
          backups: files,
          colorClass: index % 2 === 0 ? 'backup-group-a' : 'backup-group-b'
        }));
  
        console.log('Backups agrupados:', this.groupedBackups);
      },
      error: (error) => console.error('Error al obtener backups:', error)
    });
  }
  
  showNotification(message: string, type: 'success' | 'error') {
    this.notificationMessage = message;
    this.notificationType = type;
  
    setTimeout(() => {
      this.notificationMessage = null;
      this.notificationType = null;
    }, 5000); // Mostrar notificación 5 segundos
  }
  
  formatTimestamp(timestamp: string): string {
    const date = new Date(Number(timestamp));
    return date.toLocaleString('es-EC', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
  }
  
  downloadBackup(backup: any) {
    if (!backup || typeof backup.name !== 'string') {
      console.error('❌ El backup no tiene un campo "name" válido:', backup);
      return;
    }
  
    const fileName = encodeURIComponent(backup.name);
    const downloadUrl = `http://localhost:8080/docker/download/${fileName}`;
  
    console.log('✅ Descargando archivo:', fileName, 'URL:', downloadUrl);
  
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  }

  restoreBackupGroup(group: any): void {
    this.isRestoring = true;
    this.currentStep = 'Iniciando...';
    this.restoringGroupId = group.timestamp;
  
    const dbFile = group.backups.find((b: { name: string }) => b.name.includes('db_backup'))?.name;
    const odooFile = group.backups.find((b: { name: string }) => b.name.includes('odoo_data'))?.name;
  
    if (!dbFile || !odooFile) {
      this.showNotification('❌ No se encontraron archivos de backup válidos', 'error');
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
      this.showNotification('❌ No se pudo determinar la categoría desde el nombre del archivo.', 'error');
      this.isRestoring = false;
      return;
    }
  
    const payload = {
      name,
      category,
      dbBackupFileName: dbFile,
      odooBackupFileName: odooFile
    };
  
    console.log('🔁 Enviando payload de restauración:', payload);
  
    this.backupService.restoreSpecific(payload).subscribe({
      next: (response: string) => {
        this.showNotification(response, 'success');
        this.isRestoring = false;
        this.currentStep = '';
      },
      error: (error) => {
        console.error('❌ Error durante la restauración:', error);
        this.showNotification('❌ Error durante la restauración', 'error');
        this.isRestoring = false;
        this.currentStep = '';
      }
    });
  }
  
  
  
  
  
  
  
  
  
  
  

  

  createBackup() {
    this.backupService.createBackup().subscribe({
      next: (response: string) => {
        console.log('Backup creado:', response);
  
        const match = response.match(/Backup creado exitosamente:.*\\(.+\.zip)/);
        const fileName = match ? match[1] : 'unknown.zip';
  
        this.backups.push({ 
          time: new Date().toISOString(), 
          branch: 'master-live',
          version: '1.0',
          type: 'Manual',
          revision: '1234567',
          name: fileName // ✅ ahora sí el nombre correcto
        });
      },
      error: (err) => console.error('Error al crear backup:', err)
    });
  }
 
  
  
  
 

  


}
