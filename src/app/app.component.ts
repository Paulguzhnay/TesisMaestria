import { Component } from '@angular/core';
import { Router, NavigationEnd,UrlTree} from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
   standalone: false,
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'frontend';
  currentUrl: string = '';

  constructor(public router: Router) {
 
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.currentUrl = event.urlAfterRedirects;
    });
  }

  showSidebar(): boolean {
     
    const tree: UrlTree = this.router.parseUrl(this.currentUrl);
    const segments = tree.root.children['primary']?.segments.map(s => s.path) || [];

    return segments[0] === 'projects';
  }
}
