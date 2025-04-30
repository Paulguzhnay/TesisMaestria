import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ProjectService } from '../../services/project.service';
import { Project } from '../../models/project.model';
@Component({
  selector: 'app-project-setup',
  standalone: false,
  templateUrl: './project-setup.component.html',
  styleUrl: './project-setup.component.css'
})
export class ProjectSetupComponent {
  projectForm: FormGroup;
  odooVersions = ['17.0'];
  locations = ['Americas', 'Europe', 'Asia'];
  selectedLocation = 'Europe';

  constructor(private fb: FormBuilder, private router: Router, private projectService: ProjectService) {
    this.projectForm = this.fb.group({
      repositoryType: ['new', Validators.required],
      repository: ['', Validators.required],
      odooVersion: ['17.0', Validators.required],
      hostingLocation: ['Europe', Validators.required]   
    });
  }

  deploy(): void {
    if (this.projectForm.valid) {
      const formValue = this.projectForm.value;
  
      const projectData: Project = {
        name: formValue.repository.split('/')[1],
        repositoryType: formValue.repositoryType,
        repository: formValue.repository,
        odooVersion: formValue.odooVersion,
        version: formValue.odooVersion,  
        license: 'Trial',
        status: 'Development',
        location: formValue.hostingLocation
      };
  
      this.projectService.create(projectData).subscribe({
        next: () => this.router.navigate(['/projects', projectData.name]),
        error: () => alert("❌ Error al guardar el proyecto.")
      });
    }
  }
  
}
