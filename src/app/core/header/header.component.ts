import { Component } from '@angular/core';
import { OnInit } from '@angular/core';
import { BackupService } from '../../services/backup.service';


@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.component.html',
  styleUrl: './header.component.css'
})
export class HeaderComponent implements OnInit {
  backups: string[] = [];
  constructor(private backupService: BackupService) {}

  ngOnInit(): void {
    this.loadBackups();
  }
  loadBackups() {
    this.backupService.getBackups().subscribe((data: string[]) => {
      this.backups = data;
    });
  }

  // Método de logout
  logout() {

    console.log('Logging out...');

  }
}
