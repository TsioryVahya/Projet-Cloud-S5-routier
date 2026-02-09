import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loginForm!: FormGroup;
  error: string | null = null;
  loading = false;

  ngOnInit() {
    this.loginForm = this.fb.group({
      email: [localStorage.getItem('rememberedEmail') || 'manager@routier.mg', [Validators.required, Validators.email]],
      password: ['manager123', [Validators.required, Validators.minLength(6)]],
      rememberMe: [!!localStorage.getItem('rememberedEmail')]
    });
  }

  onSubmit() {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = null;
    
    const { email, password, rememberMe } = this.loginForm.value;

    this.authService.login(email, password).subscribe({
      next: () => {
        if (rememberMe) {
          localStorage.setItem('rememberedEmail', email);
        } else {
          localStorage.removeItem('rememberedEmail');
        }
        this.router.navigate(['/dashboard']);
      },
      error: (err: any) => {
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