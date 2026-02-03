import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { Utilisateur, Role, StatutUtilisateur } from '../../models/user.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit {
  users: Utilisateur[] = [];
  roles: Role[] = [];
  statuts: StatutUtilisateur[] = [];
  
  showModal = false;
  isEditMode = false;
  isSyncing = false;
  newUser: any = {
    id: null,
    email: '',
    motDePasse: '',
    role: { id: 1 },
    statutActuel: { id: 1 }
  };

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
    this.loadMetadata();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (data) => this.users = data,
      error: (err) => console.error(err)
    });
  }

  loadMetadata(): void {
    this.userService.getRoles().subscribe(data => this.roles = data);
    this.userService.getStatuts().subscribe(data => this.statuts = data);
  }

  openModal(): void {
    this.showModal = true;
    this.isEditMode = false;
    this.newUser = {
      id: null,
      email: '',
      motDePasse: '',
      role: this.roles.length > 0 ? { id: this.roles[0].id } : { id: 1 },
      statutActuel: this.statuts.length > 0 ? { id: this.statuts[0].id } : { id: 1 }
    };
  }

  editUser(user: Utilisateur): void {
    this.showModal = true;
    this.isEditMode = true;
    this.newUser = {
      id: user.id,
      email: user.email,
      motDePasse: '', 
      role: user.role ? { id: user.role.id } : (this.roles.length > 0 ? { id: this.roles[0].id } : null),
      statutActuel: user.statutActuel ? { id: user.statutActuel.id } : (this.statuts.length > 0 ? { id: this.statuts[0].id } : null)
    };
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveUser(): void {
    if (this.isEditMode) {
      // Préparer l'objet pour l'update sans changer l'ID
      const updateData = {
        email: this.newUser.email,
        motDePasse: this.newUser.motDePasse,
        role: { id: parseInt(this.newUser.role.id) },
        statutActuel: { id: parseInt(this.newUser.statutActuel.id) }
      };

      this.userService.updateUser(this.newUser.id, updateData).subscribe({
        next: () => {
          this.closeModal();
          this.loadUsers();
          alert('Utilisateur mis à jour avec succès');
        },
        error: (err) => {
          console.error('Erreur update:', err);
          alert('Erreur lors de la mise à jour');
        }
      });
    } else {
      // Création d'un nouvel utilisateur
      const createData = {
        email: this.newUser.email,
        motDePasse: this.newUser.motDePasse,
        role: { id: parseInt(this.newUser.role.id) },
        statutActuel: { id: parseInt(this.newUser.statutActuel.id) }
      };

      this.userService.createUser(createData).subscribe({
        next: () => {
          this.closeModal();
          this.loadUsers();
          alert('Utilisateur créé avec succès');
        },
        error: (err) => {
          console.error('Erreur création:', err);
          alert('Erreur lors de la création');
        }
      });
    }
  }

  globalSync(): void {
    this.isSyncing = true;
    // On synchronise d'abord Postgres -> Firebase (export)
    // PUIS Firebase -> Postgres (import) pour éviter d'écraser les modifs récentes
    this.userService.syncUsersToFirebase().subscribe({
      next: (resExport: any) => {
        this.userService.syncUsers().subscribe({
          next: (resImport: any) => {
            this.isSyncing = false;
            this.loadUsers();
            alert(`Synchronisation complète terminée !\n- Exportés : ${resExport.syncedUsers}\n- Importés : ${resImport.utilisateurs}`);
          },
          error: (err) => {
            this.isSyncing = false;
            console.error(err);
            alert('Erreur lors de l\'importation');
          }
        });
      },
      error: (err) => {
        this.isSyncing = false;
        console.error(err);
        alert('Erreur lors de l\'exportation');
      }
    });
  }

  syncUsers(): void {
    this.userService.syncUsers().subscribe({
      next: (res) => {
        this.loadUsers();
        alert(`Synchronisation terminée : ${res.utilisateurs} utilisateurs importés.`);
      },
      error: (err) => {
        console.error(err);
        alert('Erreur lors de la synchronisation');
      }
    });
  }

  syncToFirebase(): void {
    this.userService.syncUsersToFirebase().subscribe({
      next: (res) => {
        alert(`Synchronisation vers Firebase réussie : ${res.syncedUsers} utilisateurs exportés.`);
      },
      error: (err) => {
        console.error(err);
        alert('Erreur lors de la synchronisation vers Firebase');
      }
    });
  }

  unblock(email: string): void {
    this.userService.unblockUser(email).subscribe({
      next: () => {
        alert('Utilisateur débloqué avec succès');
        this.loadUsers();
      },
      error: (err) => console.error(err)
    });
  }

  deleteUser(id: string): void {
    if (confirm('Supprimer cet utilisateur ?')) {
      this.userService.deleteUser(id).subscribe({
        next: () => this.loadUsers(),
        error: (err) => console.error(err)
      });
    }
  }
}
