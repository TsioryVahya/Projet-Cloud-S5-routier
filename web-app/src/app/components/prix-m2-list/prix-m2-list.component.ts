import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrixM2, PrixM2Service } from '../../services/prix-m2.service';

@Component({
  selector: 'app-prix-m2-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './prix-m2-list.component.html',
  styleUrl: './prix-m2-list.component.css'
})
export class PrixM2ListComponent implements OnInit {
  prices: PrixM2[] = [];
  loading = false;
  notification: { message: string, type: 'success' | 'error' } | null = null;

  constructor(private prixM2Service: PrixM2Service) {}

  ngOnInit(): void {
    this.loadPrices();
  }

  loadPrices(): void {
    this.loading = true;
    this.prixM2Service.getAll().subscribe({
      next: (data) => {
        this.prices = data.sort((a, b) => {
          const dateA = a.dateDebut ? new Date(a.dateDebut).getTime() : 0;
          const dateB = b.dateDebut ? new Date(b.dateDebut).getTime() : 0;
          return dateB - dateA;
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des prix', err);
        this.showNotification('Erreur lors du chargement des prix', 'error');
        this.loading = false;
      }
    });
  }

  addNewPrice(): void {
    const today = new Date().toISOString().slice(0, 16);
    const newPrice: PrixM2 = {
      montant: 0,
      dateDebut: today
    };
    this.prices.unshift(newPrice);
  }

  savePrice(price: PrixM2): void {
    if (price.montant <= 0) {
      this.showNotification('Le montant doit être supérieur à 0', 'error');
      return;
    }

    if (price.id) {
      this.prixM2Service.update(price.id, price).subscribe({
        next: () => {
          this.showNotification('Prix mis à jour avec succès', 'success');
        },
        error: (err) => {
          console.error('Erreur lors de la mise à jour', err);
          this.showNotification('Erreur lors de la mise à jour', 'error');
        }
      });
    } else {
      this.prixM2Service.create(price).subscribe({
        next: (created) => {
          price.id = created.id;
          price.dateCreation = created.dateCreation;
          this.showNotification('Nouveau prix ajouté avec succès', 'success');
        },
        error: (err) => {
          console.error('Erreur lors de la création', err);
          this.showNotification('Erreur lors de la création', 'error');
        }
      });
    }
  }

  deletePrice(price: PrixM2): void {
    if (!price.id) {
      this.prices = this.prices.filter(p => p !== price);
      return;
    }

    if (confirm('Êtes-vous sûr de vouloir supprimer ce prix ?')) {
      this.prixM2Service.delete(price.id).subscribe({
        next: () => {
          this.prices = this.prices.filter(p => p.id !== price.id);
          this.showNotification('Prix supprimé avec succès', 'success');
        },
        error: (err) => {
          console.error('Erreur lors de la suppression', err);
          this.showNotification('Erreur lors de la suppression', 'error');
        }
      });
    }
  }

  showNotification(message: string, type: 'success' | 'error'): void {
    this.notification = { message, type };
    setTimeout(() => {
      this.notification = null;
    }, 3000);
  }

  formatDateForInput(dateStr: string | undefined): string {
    if (!dateStr) return '';
    try {
      const date = new Date(dateStr);
      return date.toISOString().slice(0, 16);
    } catch (e) {
      return '';
    }
  }

  onDateChange(price: PrixM2, field: 'dateDebut' | 'dateFin', event: any): void {
    const value = event.target.value;
    if (field === 'dateDebut') price.dateDebut = value;
    else price.dateFin = value;
  }
}
