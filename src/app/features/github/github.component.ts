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
  commitsByBranch: { [key: string]: string[] } = {}; // Para almacenar los commits por rama

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.getBranches(); // Obtener las ramas al iniciar
  }

  getBranches() {
    this.http.get<string[]>('http://localhost:8080/api/github/branches')
      .subscribe(
        data => {
          this.branches = data;
          this.getCommitsForAllBranches(); // Una vez obtenidas las ramas, obtenemos los commits de todas
        },
        error => console.error('Error al obtener las ramas:', error)
      );
  }

  getCommitsForAllBranches() {
    this.branches.forEach(branch => {
      this.http.get<string[]>(`http://localhost:8080/api/github/commits/${branch}`)
        .subscribe(
          data => {
            this.commitsByBranch[branch] = data; // Almacena los commits por rama
          },
          error => console.error('Error al obtener los commits:', error)
        );
    });
  }
}
