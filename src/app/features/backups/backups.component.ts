import { Component, OnInit } from '@angular/core';
import { BackupService } from '../../services/backup.service';

@Component({
  selector: 'app-backups',
  standalone: false,
  templateUrl: './backups.component.html',
  styleUrl: './backups.component.css'
})
export class BackupsComponent implements OnInit {
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

  restoreBackup(backup: any) {
    const instanceName = backup.name.includes('db') ? backup.name.replace('db_backup_', '').replace('.sql', '') : backup.name;
    const category = 'DEVELOPMENT'; // puedes ajustar si es dinámico
  
    const payload = {
      name: instanceName,
      category: category,
      file: backup.name
    };
  
    this.backupService.restoreBackup(payload).subscribe({
      next: (res) => console.log('Restauración completada:', res),
      error: (err) => console.error('Error al restaurar:', err)
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
