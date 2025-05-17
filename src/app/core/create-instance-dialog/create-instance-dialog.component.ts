// src/app/core/create-instance-dialog/create-instance-dialog.component.ts

import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA }     from '@angular/material/dialog';
import { OdooService }                        from '../../services/odoo.service';

export interface CreateInstanceData {
  projectId:   number;
  projectName: string;
}

@Component({
  selector: 'app-create-instance-dialog',
  templateUrl: './create-instance-dialog.component.html',
  styleUrls: ['./create-instance-dialog.component.css'],
  standalone: false
})
export class CreateInstanceDialogComponent implements OnInit {
  form!: FormGroup;
  categories = ['DEVELOPMENT', 'STAGING', 'PRODUCTION'];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateInstanceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateInstanceData,
    private odoo: OdooService,
  ) {}

  ngOnInit(): void {
    // Fallback en caso de que no venga projectName
    const rawName    = this.data?.projectName ?? '';
    const defaultCat = this.categories[0].toLowerCase();

    this.form = this.fb.group({
      name:     [`${rawName.toLowerCase()}-${defaultCat}`, Validators.required],
      category: [ this.categories[0],             Validators.required ],
    });
  }

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    const { name, category } = this.form.value;
    this.odoo.createOdooInstance(name, category, this.data.projectId)
      .subscribe({
        next: inst => this.dialogRef.close(inst),
        error: err => this.form.setErrors({ server: err.error?.message })
      });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
