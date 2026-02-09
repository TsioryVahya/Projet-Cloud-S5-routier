import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { SignalementService } from '../../services/signalement.service';
import { Signalement, StatutSignalement, TypeSignalement } from '../../models/signalement.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-signalement-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './signalement-edit.component.html',
  styleUrls: ['./signalement-edit.component.css']
})
export class SignalementEditComponent implements OnInit {
  signalementId: string | null = null;
  signalement: any = null;
  statuses: StatutSignalement[] = [];
  entreprises: any[] = [];
  types: TypeSignalement[] = [];
  isSaving = false;
  isLoading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private signalementService: SignalementService
  ) {}

  ngOnInit(): void {
    this.signalementId = this.route.snapshot.paramMap.get('id');
    if (this.signalementId) {
      this.loadData();
    } else {
      this.router.navigate(['/dashboard/signalements']);
    }
  }

  loadData(): void {
    this.isLoading = true;
    
    // Charger d'abord les référentiels
    forkJoin({
      statuses: this.signalementService.getAllStatuses(),
      entreprises: this.signalementService.getAllEntreprises(),
      types: this.signalementService.getAllTypes()
    }).subscribe({
      next: (refs) => {
        this.statuses = refs.statuses;
        this.entreprises = refs.entreprises;
        this.types = refs.types;

        // Charger ensuite le signalement
        if (this.signalementId) {
          this.signalementService.getSignalementById(this.signalementId).subscribe({
            next: (data: any) => {
              // Trouver le statut correspondant
              const signalementStatutNom = String(data.statut?.nom || data.statut || '').toLowerCase();
              const currentStatus = this.statuses.find(st => String(st.nom || '').toLowerCase() === signalementStatutNom);

              this.signalement = {
                id: data.postgresId || data.id,
                latitude: data.latitude,
                longitude: data.longitude,
                statutId: currentStatus ? currentStatus.id : (data.statut?.id || 1),
                description: data.details?.description || data.description || '',
                budget: data.details?.budget || data.budget || 0,
                surfaceM2: data.details?.surfaceM2 || data.surfaceM2 || 0,
                entrepriseConcerne: data.details?.entreprise?.id || data.entrepriseConcerne || '',
                photoUrl: data.details?.photoUrl || data.photoUrl || '',
                typeId: data.type?.id || data.id_type_signalement || 1
              };
              this.isLoading = false;
            },
            error: (err) => {
              console.error('Erreur lors du chargement du signalement:', err);
              this.isLoading = false;
              this.router.navigate(['/dashboard/signalements']);
            }
          });
        }
      },
      error: (err) => {
        console.error('Erreur lors du chargement des référentiels:', err);
        this.isLoading = false;
      }
    });
  }

  save(): void {
    if (!this.signalement || !this.signalementId) return;
    
    this.isSaving = true;
    this.signalementService.updateSignalement(this.signalementId, this.signalement).subscribe({
      next: () => {
        this.isSaving = false;
        this.router.navigate(['/dashboard/signalements']);
      },
      error: (err) => {
        console.error(err);
        this.isSaving = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/dashboard/signalements']);
  }
}
