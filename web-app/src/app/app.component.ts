import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <h1>Bienvenue sur l'application de signalement routier</h1>
    <router-outlet></router-outlet>
  `,
  styles: [],
})
export class AppComponent {
  title = 'web-app';
}
