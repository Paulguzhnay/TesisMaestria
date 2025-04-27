import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SettingsComponent } from './settings/settings.component';
import { ShellComponent } from './shell/shell.component';
import { BackupsComponent } from './backups/backups.component';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
 //
 
import { MatDividerModule } from '@angular/material/divider';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

//
import { MatTableModule } from '@angular/material/table';
 
 
 



@NgModule({
  declarations: [
    DashboardComponent,
    SettingsComponent,
    ShellComponent,
    BackupsComponent,


  ],
  imports: [
    CommonModule,
    MatProgressBarModule,
    MatCardModule,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatTooltipModule,
    MatTableModule
  ],
  exports: [
    DashboardComponent,
    SettingsComponent
    ]
})
export class FeaturesModule { }