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
  filteredSignalements: Signalement[] = [];
  statuses: StatutSignalement[] = [];
  entreprises: any[] = [];
  
  // Search & Filter
  searchTerm: string = '';

  // Modal States
  showEditModal = false;
  showPhotoModal = false;
  selectedPhotoUrl = '';
  selectedPhotos: string[] = [];
  currentPhotoIndex = 0;
  editingSignalement: any = null;
  isSaving = false;

  // State for synchronization
  isSyncing = false;
  syncMessage = '';

  constructor(private signalementService: SignalementService) {}

  ngOnInit(): void {
    this.loadSignalements();
    this.loadStatuses();
    this.loadEntreprises();
  }

  loadSignalements(): void {
    this.signalementService.getAllSignalements().subscribe({
      next: (data) => {
        console.log('Données reçues du backend:', data);
        this.signalements = data;
        this.applyFilter();
        this.updateStats();
      },
      error: (err) => {
        console.error('Erreur lors du chargement des signalements', err);
      }
    });
  }

  stats = {
    total: 0,
    nouveau: 0,
    enCours: 0
  };

  updateStats(): void {
    this.stats.total = this.signalements.length;
    this.stats.nouveau = this.signalements.filter(s => {
      const st = String(s.statut || '').toLowerCase();
      return st === 'nouveau';
    }).length;
    this.stats.enCours = this.signalements.filter(s => {
      const st = String(s.statut || '').toLowerCase();
      return st === 'en cours' || st === 'en_cours';
    }).length;
  }

  openPhotoModal(s: Signalement): void {
    this.selectedPhotos = [];
    if (s.galerie && s.galerie.length > 0) {
      this.selectedPhotos = s.galerie.map(g => g.url);
    } else if (s.photoUrl) {
      this.selectedPhotos = [s.photoUrl];
    }
    
    this.currentPhotoIndex = 0;
    this.selectedPhotoUrl = this.selectedPhotos.length > 0 ? this.selectedPhotos[0] : '';
    this.showPhotoModal = true;
    
    // Empêcher le scroll du body quand la modal est ouverte
    document.body.style.overflow = 'hidden';
  }

  closePhotoModal(): void {
    this.showPhotoModal = false;
    this.selectedPhotoUrl = '';
    this.selectedPhotos = [];
    this.currentPhotoIndex = 0;
    document.body.style.overflow = 'auto';
  }

  selectPhoto(index: number): void {
    this.currentPhotoIndex = index;
    this.selectedPhotoUrl = this.selectedPhotos[index];
  }

  nextPhoto(event?: Event): void {
    if (event) event.stopPropagation();
    if (this.currentPhotoIndex < this.selectedPhotos.length - 1) {
      this.currentPhotoIndex++;
      this.selectedPhotoUrl = this.selectedPhotos[this.currentPhotoIndex];
    }
  }

  prevPhoto(event?: Event): void {
    if (event) event.stopPropagation();
    if (this.currentPhotoIndex > 0) {
      this.currentPhotoIndex--;
      this.selectedPhotoUrl = this.selectedPhotos[this.currentPhotoIndex];
    }
  }

  loadStatuses(): void {
    this.signalementService.getAllStatuses().subscribe({
      next: (data) => this.statuses = data,
      error: (err) => console.error(err)
    });
  }
  
  loadEntreprises(): void {
    this.signalementService.getAllEntreprises().subscribe({
      next: (data) => this.entreprises = data,
      error: (err) => console.error(err)
    });
  }

  openEditModal(signalement: Signalement): void {
    // Trouver l'ID du statut correspondant au nom du statut
    const signalementStatut = String(signalement.statut || '').toLowerCase();
    const currentStatus = this.statuses.find(st => String(st.nom || '').toLowerCase() === signalementStatut);
    
    this.editingSignalement = {
      id: signalement.id,
      latitude: signalement.latitude,
      longitude: signalement.longitude,
      statutId: currentStatus ? currentStatus.id : 1, // Fallback au premier statut si non trouvé
      description: signalement.description || '',
      budget: signalement.budget || 0,
      surfaceM2: signalement.surfaceM2 || 0,
      entrepriseNom: signalement.entrepriseNom || signalement.entrepriseConcerne || '',
      dateModification: signalement.dateDerniereModification 
        ? new Date(signalement.dateDerniereModification).toISOString().slice(0, 16)
        : new Date().toISOString().slice(0, 16)
    };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingSignalement = null;
  }

  saveSignalement(): void {
    if (!this.editingSignalement) return;
    
    this.isSaving = true;
    
    // On prépare les données pour l'envoi
    const updateData = {
      ...this.editingSignalement,
      // Conversion de la date locale (YYYY-MM-DDTHH:mm) en ISO String pour le backend
      dateModification: new Date(this.editingSignalement.dateModification).toISOString()
    };
    
    this.signalementService.updateSignalement(this.editingSignalement.id, updateData).subscribe({
      next: () => {
        this.isSaving = false;
        this.closeEditModal();
        this.loadSignalements();
      },
      error: (err) => {
        console.error(err);
        this.isSaving = false;
      }
    });
  }

  syncFromMobile(): void {
    this.isSyncing = true;
    this.syncMessage = 'Synchronisation en cours...';
    
    this.signalementService.syncData().subscribe({
      next: (result) => {
        this.isSyncing = false;
        const count = result.signalements || 0;
        this.syncMessage = `${count} nouveau(x) signalement(s) synchronisé(s).`;
        this.loadSignalements();
        // Clear message after 5 seconds
        setTimeout(() => this.syncMessage = '', 5000);
      },
      error: (err) => {
        console.error('Erreur de synchronisation:', err);
        this.isSyncing = false;
        this.syncMessage = 'Erreur lors de la synchronisation.';
        setTimeout(() => this.syncMessage = '', 5000);
      }
    });
  }

  getStatusId(name: string): number {
    const searchName = String(name || '').toLowerCase();
    const status = this.statuses.find(s => String(s.nom || '').toLowerCase() === searchName);
    return status ? status.id : 1;
  }

  getStatusClass(statut: any): string {
    const s = String(statut || '').toLowerCase();
    if (s === 'nouveau') {
      return 'bg-blue-50 text-blue-600 border-blue-100';
    } else if (s === 'en cours' || s === 'en_cours') {
      return 'bg-amber-50 text-amber-600 border-amber-100';
    }
    return 'bg-green-50 text-green-600 border-green-100';
  }

  deleteSignalement(id: string | undefined): void {
    if (!id) return;
    if (confirm('Êtes-vous sûr de vouloir supprimer ce signalement ?')) {
      this.signalementService.deleteSignalement(id).subscribe({
        next: () => this.loadSignalements(),
        error: (err) => console.error(err)
      });
    }
  }

  applyFilter(): void {
    if (!this.searchTerm) {
      this.filteredSignalements = [...this.signalements];
      return;
    }

    const term = this.searchTerm.toLowerCase().trim();
    this.filteredSignalements = this.signalements.filter(s => {
      const idStr = (s.id || '').toString().toLowerCase();
      const typeStr = (s.typeNom || '').toLowerCase();
      const descStr = (s.description || '').toLowerCase();
      const emailStr = (s.utilisateur?.email || '').toLowerCase();
      const statusStr = (s.statut || '').toLowerCase();

      return idStr.includes(term) || 
             typeStr.includes(term) || 
             descStr.includes(term) || 
             emailStr.includes(term) ||
             statusStr.includes(term);
    });
  }

  getProgressByStatus(statut: any): number {
    const s = String(statut || '').toLowerCase();
    if (s === 'nouveau') return 0;
    if (s === 'en cours' || s === 'en_cours') return 50;
    if (s === 'terminé' || s === 'termine') return 100;
    return 0;
  }

  // Autres méthodes pour éditer ou changer le statut...
}
