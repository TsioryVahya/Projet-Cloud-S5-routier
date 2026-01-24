import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../services/user.service';
import { Utilisateur } from '../../models/user.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit {
  users: Utilisateur[] = [];

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (data) => this.users = data,
      error: (err) => console.error(err)
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
