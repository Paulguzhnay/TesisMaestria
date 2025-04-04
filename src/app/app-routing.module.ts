import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SettingsComponent } from './features/settings/settings.component';
import {ShellComponent} from './features/shell/shell.component';
import{GithubComponent} from './features/github/github.component'
import { BackupsComponent } from './features/backups/backups.component';
const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent },
  { path: 'settings', component: SettingsComponent },
  { path: 'shell', component: ShellComponent },
  { path: 'github', component: GithubComponent },
  {path:'backups', component:BackupsComponent},
  { path: '**', redirectTo: 'dashboard' } // Ruta por defecto
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
