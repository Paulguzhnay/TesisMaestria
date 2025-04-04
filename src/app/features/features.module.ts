import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SettingsComponent } from './settings/settings.component';
import { ShellComponent } from './shell/shell.component';
import { BackupsComponent } from './backups/backups.component';




@NgModule({
  declarations: [
    DashboardComponent,
    SettingsComponent,
    ShellComponent,
    BackupsComponent,

  ],
  imports: [
    CommonModule
  ],
  exports: [
    DashboardComponent,
    SettingsComponent
    ]
})
export class FeaturesModule { }
