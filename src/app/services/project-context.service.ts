import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ProjectContextService {
  private _projectName: string = '';

  setProjectName(name: string) {
    this._projectName = name;
  }

  getProjectName(): string {
    return this._projectName;
  }
}
