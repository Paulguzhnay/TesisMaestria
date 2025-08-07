
import { Component, EventEmitter, Inject, OnInit, Output } from '@angular/core';
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
  @Output() instanceCreated = new EventEmitter<void>(); // ✅ Evento emitido al crear
  form!: FormGroup;
  categories = ['DEVELOPMENT', 'STAGING', 'PRODUCTION'];
  sourceCategories = ['DEVELOPMENT', 'STAGING', 'PRODUCTION'];
  categoryValue: string = '';
  isProcessing = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateInstanceDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateInstanceData,
    private odoo: OdooService,
  ) { }

  ngOnInit(): void {
    const defaultCat = this.data?.category ?? this.categories[0];
    const defaultName = `${this.data.projectName.toLowerCase()}-${defaultCat.toLowerCase()}`;
    this.categoryValue = defaultCat;

    this.form = this.fb.group({
      name: [defaultName, Validators.required],
      category: [defaultCat, Validators.required],
      neutralize: [false],
      codeSourceCategory: [null], // solo visible según categoría
      copyDataFromProduction: [false] // solo visible en STAGING
    });
    const validSources = this.getValidSourceCategories();
    if (validSources.length === 1) {
      this.form.get('codeSourceCategory')?.setValue(validSources[0]);
    }
  }

  submit(): void {
    if (this.form.invalid) return;

    const payload = {
      ...this.form.value,
      projectId: this.data.projectId
    };

    this.isProcessing = true;

    this.odoo.createOdooInstance(payload).subscribe({
      next: inst => {
        this.instanceCreated.emit(); //  Notificar al dashboard
        this.dialogRef.close(inst); // cerrar modal
      },
      error: err => {
        this.isProcessing = false;
        this.form.setErrors({ server: err.error?.message });
      }
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  getValidSourceCategories(): string[] {
    const category = this.form?.get('category')?.value || this.categoryValue;
    switch (category) {
      case 'STAGING':
        return ['DEVELOPMENT', 'PRODUCTION'];
      case 'PRODUCTION':
        return ['STAGING', 'DEVELOPMENT'];
      case 'DEVELOPMENT':
        return ['STAGING', 'PRODUCTION'];
      default:
        return [];
    }
  }
  get currentCategory(): string {
    return this.form?.get('category')?.value || this.categoryValue;
  }
}
