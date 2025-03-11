import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-github',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './github.component.html',
  styleUrls: ['./github.component.css']
})
export class GithubComponent implements OnInit {
  branches: string[] = [];
  commitsByBranch: { [key: string]: string[] } = {}; // Commits por rama
  draggedBranch: string | null = null; // Rama arrastrada

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.getBranches();
  }

  getBranches() {
    this.http.get<string[]>('http://localhost:8080/api/github/branches')
      .subscribe(
        data => {
          this.branches = data;
          this.getCommitsForAllBranches();
        },
        error => console.error('Error al obtener las ramas:', error)
      );
  }

  getCommitsForAllBranches() {
    this.branches.forEach(branch => {
      this.http.get<string[]>(`http://localhost:8080/api/github/commits/${branch}`)
        .subscribe(
          data => {
            this.commitsByBranch[branch] = data;
          },
          error => console.error('Error al obtener los commits:', error)
        );
    });
  }

  dragStart(branch: string) {
    this.draggedBranch = branch;
  }

  drop(targetBranch: string) {
    if (this.draggedBranch && this.draggedBranch !== targetBranch) {
      this.mergeBranches(this.draggedBranch, targetBranch);
    }
    this.draggedBranch = null;
  }

mergeBranches(fromBranch: string, toBranch: string) {
  this.http.post('http://localhost:8080/api/github/merge', { fromBranch, toBranch }, { responseType: 'text' }) // <- Indicar que la respuesta es texto
    .subscribe(
      response => {
        console.log('Merge exitoso:', response);
        alert('✅ ' + response); // Mostrar alerta con la respuesta
      },
      error => {
        console.error('Error al hacer merge:', error);
        alert('❌ Error en el merge: ' + error.message);
      }
    );
}


  pull() {
    this.http.post<string>('http://localhost:8080/api/github/pull', {})
      .subscribe(response => alert(response), error => console.error('Error en pull:', error));
  }

  push() {
    this.http.post<string>('http://localhost:8080/api/github/push', {})
      .subscribe(response => alert(response), error => console.error('Error en push:', error));
  }
}
