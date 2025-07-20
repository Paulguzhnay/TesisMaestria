// src/app/core/create-instance-dialog/create-instance-dialog.component.ts

import { Component, Inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { OdooService } from '../../services/odoo.service';

export interface CreateInstanceData {
  projectId: number;
  projectName: string;
  category: string;


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
  categoryValue: string = "";

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateInstanceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateInstanceData,
    private odoo: OdooService,
  ) { }

  ngOnInit(): void {
    const rawName = this.data?.projectName ?? '';
    const defaultCat = this.data?.category ?? this.categories[0];

    this.categoryValue = defaultCat;

    this.form = this.fb.group({
      name: [`${rawName.toLowerCase()}-${defaultCat.toLowerCase()}`, Validators.required],
      category: [defaultCat, Validators.required],
      neutralize: [false]
    });
  }

  submit(): void {
    if (this.form.invalid) return;

    const { name, category, neutralize } = this.form.value;

    this.odoo.createOdooInstance(name, category, this.data.projectId, neutralize).subscribe({
      next: inst => this.dialogRef.close(inst),
      error: err => this.form.setErrors({ server: err.error?.message })
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
