import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { SettingsComponent } from './features/settings/settings.component';
import {ShellComponent} from './features/shell/shell.component';
import{GithubComponent} from './features/github/github.component'
import { BackupsComponent } from './features/backups/backups.component';
import { ProjectSetupComponent } from './features/project-setup/project-setup.component';
import { ProjectDetailComponent } from './features/project-detail/project-detail.component';
import { ProjectDashboardComponent } from './features/project-dashboard/project-dashboard.component';
const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'project-setup', component: ProjectSetupComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'settings', component: SettingsComponent },
  { path: 'shell', component: ShellComponent },
  { path: 'github', component: GithubComponent },
  { path: 'backups', component: BackupsComponent },
  { path: 'projects/:name', component: ProjectDetailComponent },
  { path: '**', redirectTo: 'project-setup' },
  { path: 'projects/:name', component: ProjectDashboardComponent },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
