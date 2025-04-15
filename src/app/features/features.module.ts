import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SettingsComponent } from './settings/settings.component';
import { ShellComponent } from './shell/shell.component';
import { BackupsComponent } from './backups/backups.component';
import { MatProgressBarModule } from '@angular/material/progress-bar';




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
  ],
  exports: [
    DashboardComponent,
    SettingsComponent
    ]
})
export class FeaturesModule { }
