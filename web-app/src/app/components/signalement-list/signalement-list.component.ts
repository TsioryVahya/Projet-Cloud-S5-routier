import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalementService } from '../../services/signalement.service';
import { Signalement, StatutSignalement } from '../../models/signalement.model';

@Component({
  selector: 'app-signalement-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './signalement-list.component.html',
  styleUrls: ['./signalement-list.component.css']
})
export class SignalementListComponent implements OnInit {
  signalements: Signalement[] = [];
  statuses: StatutSignalement[] = [];

  constructor(private signalementService: SignalementService) {}

  ngOnInit(): void {
    this.loadSignalements();
    this.loadStatuses();
  }

  loadSignalements(): void {
    this.signalementService.getAllSignalements().subscribe({
      next: (data) => this.signalements = data,
      error: (err) => console.error(err)
    });
  }

  loadStatuses(): void {
    this.signalementService.getAllStatuses().subscribe({
      next: (data) => this.statuses = data,
      error: (err) => console.error(err)
    });
  }

  onStatusChange(signalement: Signalement, newStatusId: string): void {
    const updateData = {
      statutId: parseInt(newStatusId),
      latitude: signalement.latitude,
      longitude: signalement.longitude,
      description: signalement.description
    };

    this.signalementService.updateSignalement(signalement.id, updateData).subscribe({
      next: () => {
        console.log('Statut mis à jour');
        this.loadSignalements();
      },
      error: (err) => console.error(err)
    });
  }

  deleteSignalement(id: string): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce signalement ?')) {
      this.signalementService.deleteSignalement(id).subscribe({
        next: () => this.loadSignalements(),
        error: (err) => console.error(err)
      });
    }
  }

  // Autres méthodes pour éditer ou changer le statut...
}
