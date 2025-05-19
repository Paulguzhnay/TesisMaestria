import { Component } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { Project } from '../../models/project.model';
@Component({
  selector: 'app-project-create-dialog',
  standalone: false,
  templateUrl: './project-create-dialog.component.html',
  styleUrl: './project-create-dialog.component.css'
})
export class ProjectCreateDialogComponent {
    project: Project = {
    name: '',
    version: '17.0'
  };


  constructor(
    public dialogRef: MatDialogRef<ProjectCreateDialogComponent>
  ) {}

  save(): void {
    if (this.project.name?.trim()) {
      this.dialogRef.close(this.project);
    } else {
      alert(' El nombre del proyecto es requerido');
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
