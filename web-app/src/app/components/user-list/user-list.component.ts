import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../services/user.service';
import { Utilisateur, Role, StatutUtilisateur } from '../../models/user.model';

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
      motDePasse: '', // On ne récupère pas le mot de passe
      role: { id: user.role.id },
      statutActuel: { id: user.statutActuel.id }
    };
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveUser(): void {
    if (this.isEditMode) {
      this.userService.updateUser(this.newUser.id, this.newUser).subscribe({
        next: () => {
          this.closeModal();
          this.loadUsers();
          alert('Utilisateur mis à jour avec succès');
        },
        error: (err) => {
          console.error(err);
          alert('Erreur lors de la mise à jour');
        }
      });
    } else {
      this.userService.createUser(this.newUser).subscribe({
        next: () => {
          this.closeModal();
          this.loadUsers();
          alert('Utilisateur créé avec succès');
        },
        error: (err) => {
          console.error(err);
          alert('Erreur lors de la création');
        }
      });
    }
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
