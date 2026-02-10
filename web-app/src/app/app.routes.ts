import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login.component';
import { MapComponent } from './components/map/map.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'map', component: MapComponent },
  { 
    path: 'dashboard', 
    component: DashboardComponent,
    canActivate: [authGuard],
    children: [
      { 
        path: 'overview', 
        loadComponent: () => import('./components/stats/stats.component').then(m => m.StatsComponent) 
      },
      { path: 'map', component: MapComponent },
      { 
        path: 'signalements', 
        loadComponent: () => import('./components/signalement-list/signalement-list.component').then(m => m.SignalementListComponent) 
      },
      { 
        path: 'users', 
        loadComponent: () => import('./components/user-list/user-list.component').then(m => m.UserListComponent) 
      },
      { 
        path: 'config', 
        loadComponent: () => import('./components/config/config.component').then(m => m.ConfigComponent) 
      },
      {
        path: 'types-signalement',
        loadComponent: () => import('./components/report-type-list/report-type-list.component').then(m => m.ReportTypeListComponent)
      },
      {
        path: 'types-signalement/new',
        loadComponent: () => import('./components/report-type-form/report-type-form.component').then(m => m.ReportTypeFormComponent)
      },
      {
        path: 'types-signalement/edit/:id',
        loadComponent: () => import('./components/report-type-form/report-type-form.component').then(m => m.ReportTypeFormComponent)
      },
      {
        path: 'prix-m2',
        loadComponent: () => import('./components/prix-m2-list/prix-m2-list.component').then(m => m.PrixM2ListComponent)
      },
      { path: '', redirectTo: 'overview', pathMatch: 'full' }
    ]
  },
  { path: '', redirectTo: '/map', pathMatch: 'full' }
];
