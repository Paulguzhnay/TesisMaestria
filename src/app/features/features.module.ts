import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SettingsComponent } from './settings/settings.component';
import { ShellComponent } from './shell/shell.component';




@NgModule({
  declarations: [
    DashboardComponent,
    SettingsComponent,
    ShellComponent,

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
