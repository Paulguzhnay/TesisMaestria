import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { GithubRoutingModule } from './github-routing.module';
import { GithubComponent } from './github.component';


@NgModule({
  declarations: [
    GithubComponent
  ],
  imports: [
    CommonModule,
    GithubRoutingModule
  ]
})
export class GithubModule { }
