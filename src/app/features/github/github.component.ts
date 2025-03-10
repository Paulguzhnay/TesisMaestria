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
  commits: string[] = [];
  selectedBranch: string = ''; // Para almacenar la rama seleccionada

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.getBranches();
  }

  getBranches() {
    this.http.get<string[]>('http://localhost:8080/api/github/branches')
      .subscribe(
        data => this.branches = data,
        error => console.error('Error al obtener las ramas:', error)
      );
  }

  // Modificación para obtener los commits de la rama seleccionada
  getCommits(branch: string) {
    this.http.get<string[]>(`http://localhost:8080/api/github/commits/${branch}`)
      .subscribe(
        data => this.commits = data,
        error => console.error('Error al obtener los commits:', error)
      );
  }

  // Método para cambiar la rama seleccionada
  onSelectBranch(branch: string) {
    this.selectedBranch = branch; // Guardamos la rama seleccionada
    this.getCommits(branch); // Obtener los commits de la rama seleccionada
  }
}
