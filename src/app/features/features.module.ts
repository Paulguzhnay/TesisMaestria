import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SettingsComponent } from './settings/settings.component';
import { ShellComponent } from './shell/shell.component';
import { BackupsComponent } from './backups/backups.component';
import { ProjectSetupComponent } from './project-setup/project-setup.component';
import { ProjectDetailComponent } from './project-detail/project-detail.component';
import { ProjectDashboardComponent } from './project-dashboard/project-dashboard.component';

import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// Angular Material
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { ProjectCreateDialogComponent } from './project-create-dialog/project-create-dialog.component';
 
import { MatDialogModule } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { ImportDatabaseComponent } from './import-database/import-database.component';
 
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { LoginComponent } from './login/login.component';
import { ShellDbComponent } from './shell-db/shell-db.component';

import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { InstanceDetailComponent } from './instance-detail/instance-detail.component';
import { GithubCallbackComponent } from './github-callback/github-callback.component';


@NgModule({
  declarations: [
    DashboardComponent,
    SettingsComponent,
    ShellComponent,
    BackupsComponent,
    ProjectSetupComponent,
    ProjectDetailComponent,
    ProjectDashboardComponent,
    ProjectCreateDialogComponent,
    ImportDatabaseComponent,
    LoginComponent,
    ShellDbComponent,
    InstanceDetailComponent,
    GithubCallbackComponent
  ],

  imports: [
    CommonModule,
    FormsModule,               
    ReactiveFormsModule,
    RouterModule,
 
    MatProgressBarModule,
    MatCardModule,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatTooltipModule,
    MatTableModule,
    MatDialogModule,
    MatInputModule,
    MatSelectModule,
    MatOptionModule,
    MatProgressSpinnerModule,
  ],
  exports: [
    DashboardComponent,
    SettingsComponent,
    ]
})
export class FeaturesModule { }