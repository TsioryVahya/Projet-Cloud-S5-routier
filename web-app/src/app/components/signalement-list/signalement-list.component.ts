import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SignalementService } from '../../services/signalement.service';
import { PrixM2Service, PrixM2 } from '../../services/prix-m2.service';
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
  showDetailsModal = false;
  showEvalModal = false;
  activeMenuId: string | null = null;
  selectedSignalement: Signalement | null = null;
  selectedPhotoUrl = '';
  selectedPhotos: string[] = [];
  currentPhotoIndex = 0;
  editingSignalement: any = null;
  isSaving = false;

  // Evaluation state
  evalData: any = {
    niveau: null,
    surfaceM2: 0,
    entrepriseNom: '',
    budget: 0
  };
  currentPrixM2: number = 0;

  // Notification state
  notification: { message: string, type: 'success' | 'error' } | null = null;

  // State for synchronization
  isSyncing = false;
  syncMessage = '';

  constructor(
    private signalementService: SignalementService,
    private prixM2Service: PrixM2Service
  ) {}

  ngOnInit(): void {
    this.loadSignalements();
    this.loadStatuses();
    this.loadEntreprises();
    this.loadCurrentPrixM2();
  }

  loadCurrentPrixM2(): void {
    this.prixM2Service.getAll().subscribe({
      next: (prices) => {
        if (prices && prices.length > 0) {
          // On prend le dernier prix valide (le plus récent par date de début)
          const validPrices = prices.sort((a, b) => 
            new Date(b.dateDebut).getTime() - new Date(a.dateDebut).getTime()
          );
          this.currentPrixM2 = validPrices[0].montant;
        }
      },
      error: (err) => console.error('Erreur lors du chargement du prix m2', err)
    });
  }

  loadSignalements(): void {
    this.signalementService.getAllSignalements().subscribe({
      next: (data) => {
        console.log('Données reçues du backend:', data);
        // Trier par date et heure décroissante (Modification si dispo, sinon Signalement)
        this.signalements = data.sort((a, b) => {
          const dateA = new Date(a.dateModification || a.dateSignalement).getTime();
          const dateB = new Date(b.dateModification || b.dateSignalement).getTime();
          return dateB - dateA;
        });
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
      id: signalement.postgresId || signalement.id,
      latitude: signalement.latitude,
      longitude: signalement.longitude,
      statutId: currentStatus ? currentStatus.id : 1, // Fallback au premier statut si non trouvé
      description: signalement.description || '',
      budget: signalement.budget || 0,
      surfaceM2: signalement.surfaceM2 || 0,
      niveau: signalement.niveau,
      entrepriseNom: signalement.entrepriseNom || '',
      dateModification: new Date().toISOString().slice(0, 16) // Format YYYY-MM-DDTHH:mm
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
        this.showNotification('Signalement mis à jour avec succès', 'success');
      },
      error: (err) => {
        console.error(err);
        this.isSaving = false;
        this.showNotification('Erreur lors de la mise à jour du signalement', 'error');
      }
    });
  }

  showNotification(message: string, type: 'success' | 'error'): void {
    this.notification = { message, type };
    setTimeout(() => {
      this.notification = null;
    }, 3000);
  }

  onStatusChange(signalement: Signalement, newStatusId: string): void {
    const id = signalement.postgresId || signalement.id;
    if (!id) return;

    const updateData = {
      statutId: parseInt(newStatusId),
      latitude: signalement.latitude,
      longitude: signalement.longitude,
      description: signalement.description,
      budget: signalement.budget,
      surfaceM2: signalement.surfaceM2,
      niveau: signalement.niveau || 1,
      entrepriseNom: signalement.entrepriseNom,
      // Par défaut, on prend "maintenant" pour le changement rapide
      dateModification: new Date().toISOString()
    };

    this.signalementService.updateSignalement(id, updateData).subscribe({
      next: () => {
        this.loadSignalements();
      },
      error: (err) => console.error(err)
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
      return 'bg-blue-50 text-blue-600 border-blue-100 focus:ring-blue-500';
    } else if (s === 'en cours' || s === 'en_cours') {
      return 'bg-amber-50 text-amber-600 border-amber-100 focus:ring-amber-500';
    }
    return 'bg-green-50 text-green-600 border-green-100 focus:ring-green-500';
  }

  getNiveauClass(niveau: number | undefined): string {
    if (!niveau) return 'bg-slate-100 text-slate-500 border-slate-200';
    
    if (niveau <= 3) return 'bg-emerald-50 text-emerald-600 border-emerald-100 shadow-emerald-100/50';
    if (niveau <= 5) return 'bg-blue-50 text-blue-600 border-blue-100 shadow-blue-100/50';
    if (niveau <= 7) return 'bg-amber-50 text-amber-600 border-amber-100 shadow-amber-100/50';
    if (niveau <= 9) return 'bg-orange-50 text-orange-600 border-orange-100 shadow-orange-100/50';
    return 'bg-red-50 text-red-600 border-red-100 shadow-red-100/50 animate-pulse';
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
      const idStr = (s.postgresId || s.id || '').toString().toLowerCase();
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

  // Action Menu & Details Methods
  toggleMenu(event: Event, id: string | undefined): void {
    event.stopPropagation();
    if (!id) return;
    this.activeMenuId = this.activeMenuId === id ? null : id;
  }

  openDetailsModal(signalement: Signalement): void {
    this.selectedSignalement = signalement;
    this.showDetailsModal = true;
    this.activeMenuId = null;
    document.body.style.overflow = 'hidden';
  }

  closeDetailsModal(): void {
    this.showDetailsModal = false;
    this.selectedSignalement = null;
    document.body.style.overflow = 'auto';
  }

  editFromMenu(signalement: Signalement): void {
    this.openEditModal(signalement);
    this.activeMenuId = null;
  }

  evalFromMenu(signalement: Signalement): void {
    this.openEvalModal(signalement);
    this.activeMenuId = null;
  }

  deleteFromMenu(signalement: Signalement): void {
    const id = signalement.postgresId || signalement.id;
    this.deleteSignalement(id);
    this.activeMenuId = null;
  }

  openEvalModal(signalement: Signalement): void {
    this.selectedSignalement = signalement;
    this.selectedPhotos = [];
    if (signalement.galerie && signalement.galerie.length > 0) {
      this.selectedPhotos = signalement.galerie.map(g => g.url);
    }
    
    this.evalData = {
      niveau: signalement.niveau,
      surfaceM2: signalement.surfaceM2 || 0,
      entrepriseNom: signalement.entrepriseNom || '',
      budget: signalement.budget || 0
    };
    
    this.calculateBudget();
    this.showEvalModal = true;
    document.body.style.overflow = 'hidden';
  }

  closeEvalModal(): void {
    this.showEvalModal = false;
    this.selectedSignalement = null;
    document.body.style.overflow = 'auto';
  }

  calculateBudget(): void {
    this.evalData.budget = this.currentPrixM2 * this.evalData.niveau * this.evalData.surfaceM2;
  }

  saveEvaluation(): void {
    if (!this.selectedSignalement) return;
    
    const id = this.selectedSignalement.postgresId || this.selectedSignalement.id;
    if (!id) return;

    this.isSaving = true;
    
    // Trouver l'ID du statut "En cours"
    const enCoursStatus = this.statuses.find(st => {
      const nom = String(st.nom || '').toLowerCase();
      return nom === 'en cours' || nom === 'en_cours';
    });

    const updateData = {
      id: id,
      latitude: this.selectedSignalement.latitude,
      longitude: this.selectedSignalement.longitude,
      statutId: enCoursStatus ? enCoursStatus.id : 2, // 2 est souvent "En cours"
      description: this.selectedSignalement.description || '',
      budget: this.evalData.budget,
      surfaceM2: this.evalData.surfaceM2,
      niveau: this.evalData.niveau,
      entrepriseNom: this.evalData.entrepriseNom,
      dateModification: new Date().toISOString()
    };

    this.signalementService.updateSignalement(id, updateData).subscribe({
      next: () => {
        this.isSaving = false;
        this.closeEvalModal();
        this.loadSignalements();
        this.showNotification('Évaluation enregistrée et statut mis à jour', 'success');
      },
      error: (err) => {
        console.error(err);
        this.isSaving = false;
        this.showNotification('Erreur lors de l\'enregistrement de l\'évaluation', 'error');
      }
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
