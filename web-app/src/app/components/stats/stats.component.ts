import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SignalementService } from '../../services/signalement.service';
import { Signalement } from '../../models/signalement.model';

@Component({
  selector: 'app-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stats.component.html'
})
export class StatsComponent implements OnInit {
  private signalementService = inject(SignalementService);
  
  stats = {
    total: 0,
    nouveau: 0,
    enCours: 0,
    termine: 0,
    totalBudget: 0,
    totalSurface: 0
  };

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.signalementService.getAllSignalements().subscribe({
      next: (data) => {
        this.calculateStats(data);
      },
      error: (err) => console.error(err)
    });
  }

  private calculateStats(data: Signalement[]): void {
    this.stats.total = data.length;
    this.stats.nouveau = data.filter(s => s.statut.nom.toLowerCase() === 'nouveau').length;
    this.stats.enCours = data.filter(s => s.statut.nom.toLowerCase() === 'en cours' || s.statut.nom.toLowerCase() === 'en_cours').length;
    this.stats.termine = data.filter(s => s.statut.nom.toLowerCase() === 'terminé' || s.statut.nom.toLowerCase() === 'termine').length;
    
    this.stats.totalBudget = data.reduce((acc, s) => acc + (s.budget || 0), 0);
    this.stats.totalSurface = data.reduce((acc, s) => acc + (s.surfaceM2 || 0), 0);
  }
}
