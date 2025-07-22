import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { DragDropModule } from '@angular/cdk/drag-drop';

import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule }     from '@angular/material/input';
import { MatSelectModule }    from '@angular/material/select';
import { MatOptionModule }    from '@angular/material/core';
import { MatButtonModule }    from '@angular/material/button';
import { MatDialogModule }    from '@angular/material/dialog';
import { MatSnackBarModule }  from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBar } from '@angular/material/progress-bar';
import { HeaderComponent } from './header/header.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { FooterComponent } from './footer/footer.component';
import { MatSpinner } from '@angular/material/progress-spinner';
import { CreateInstanceDialogComponent } from './create-instance-dialog/create-instance-dialog.component';

@NgModule({
  declarations: [
    HeaderComponent,
    SidebarComponent,
    CreateInstanceDialogComponent,
    FooterComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    DragDropModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatOptionModule,
    MatButtonModule,
    MatDialogModule,
    MatSnackBarModule,
    MatCheckboxModule,
    MatIconModule,
    MatProgressBar,
    MatSpinner
  ],
  exports: [
    HeaderComponent,
    SidebarComponent,
    FooterComponent
  ]
})
export class CoreModule {}
