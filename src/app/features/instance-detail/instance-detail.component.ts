import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router} from '@angular/router';
@Component({
  selector: 'app-instance-detail',
  standalone: false,
  templateUrl: './instance-detail.component.html',
  styleUrl: './instance-detail.component.css'
})
export class InstanceDetailComponent implements OnInit {
  projectName = '';
  instanceName = '';
  category = '';
  currentTab = '';

  constructor(private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    this.projectName = this.route.snapshot.paramMap.get('projectName') || '';
    this.instanceName = this.route.snapshot.paramMap.get('instanceName') || '';
    this.category = this.route.snapshot.queryParamMap.get('category') || '';
    this.currentTab = this.route.snapshot.url.slice(-1)[0]?.path || '';
  }
}
