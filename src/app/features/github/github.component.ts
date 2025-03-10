import { Component, OnInit } from '@angular/core';
import {HttpClient} from '@angular/common/http'
import { CommonModule } from '@angular/common';
@Component({
  selector: 'app-github',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './github.component.html',
  styleUrl: './github.component.css'
})
export class GithubComponent implements OnInit {
  branches: string[] = [];

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.getBranches();
  }

  getBranches() {
    this.http.get<any>('http://localhost:8080/api/github/branches').subscribe(
      (data) => {
        this.branches = data;
      },
      (error) => {
        console.error('Error obteniendo ramas de GitHub', error);
      }
    );
  }
}
