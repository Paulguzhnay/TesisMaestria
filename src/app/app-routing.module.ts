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
import { ShellDbComponent } from './features/shell-db/shell-db.component';
import { InstanceDetailComponent } from './features/instance-detail/instance-detail.component';
import { GithubCallbackComponent } from './features/github-callback/github-callback.component';
import { InstructionsComponent } from './features/instructions/instructions.component';

const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'project-setup', component: ProjectSetupComponent },
  { path: 'projects/:name/settings', component: SettingsComponent },
  { path: 'projects/:name/github', component: GithubComponent },
 
  { path: 'login', component: LoginComponent },
  { path: 'projects/:name/logs', component: ShellComponent },
  { path: 'projects/:name', component: ProjectDashboardComponent },
  { path: 'projects/:name/import', component: ImportDatabaseComponent },
  { path: 'projects/:name/shelldb', component: ShellDbComponent },
  { path: 'github-callback', component: GithubCallbackComponent },
  {path: 'instructions', component: InstructionsComponent},
  {
    path: 'projects/:projectName/instance/:instanceName',
    component: InstanceDetailComponent,
    children: [
      { path: 'logs', component: ShellComponent },
      { path: 'shell-db', component: ShellDbComponent },
      { path: 'backups', component: BackupsComponent },
      { path: 'import-db', component: ImportDatabaseComponent }
    ]
  },
  { path: '**', redirectTo: 'project-login' }
];
@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
