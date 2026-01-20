import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  credentials = {
    email: '',
    password: ''
  };
  
  error: string | null = null;
  loading = false;

  onSubmit() {
    this.loading = true;
    this.error = null;
    
    this.authService.login(this.credentials.email, this.credentials.password).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.error = "Email ou mot de passe incorrect";
        } else if (err.status === 403) {
          this.error = err.error || "Accès refusé";
        } else {
          this.error = "Une erreur est survenue lors de la connexion";
        }
      }
    });
  }
}
