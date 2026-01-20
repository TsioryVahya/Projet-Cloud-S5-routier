import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink } from '@angular/router';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <nav *ngIf="authService.isLoggedIn()" class="navbar">
      <div class="nav-brand">Routier Cloud</div>
      <div class="nav-links">
        <span>Connecté en tant que: {{ authService.getCurrentUser()?.email }}</span>
        <button (click)="onLogout()" class="logout-btn">Déconnexion</button>
      </div>
    </nav>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .navbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 2rem;
      background-color: #333;
      color: white;
    }
    .nav-brand {
      font-size: 1.25rem;
      font-weight: bold;
    }
    .nav-links {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .logout-btn {
      padding: 0.5rem 1rem;
      background-color: #dc3545;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }
    main {
      padding: 20px;
    }
  `],
})
export class AppComponent {
  authService = inject(AuthService);

  onLogout() {
    this.authService.logout();
  }
}
