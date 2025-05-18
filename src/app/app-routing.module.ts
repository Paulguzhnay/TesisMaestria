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
import { ImportDatabaseComponent } from './features/import-database/import-database.component';
import { LoginComponent } from './features/login/login.component';

const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'project-setup', component: ProjectSetupComponent },
  { path: 'projects/:name/settings', component: SettingsComponent },
  { path: 'projects/:name/github', component: GithubComponent },
  { path: 'projects/:name/backups', component: BackupsComponent },
  { path: 'login', component: LoginComponent },
  { path: 'projects/:name/shell', component: ShellComponent },
  { path: 'projects/:name', component: ProjectDashboardComponent },
  { path: 'projects/:name/import', component: ImportDatabaseComponent },
  { path: '**', redirectTo: 'project-login' }
];
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
