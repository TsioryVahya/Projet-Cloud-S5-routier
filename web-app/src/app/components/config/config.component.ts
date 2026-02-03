import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfigService, Configuration } from '../../services/config.service';

@Component({
  selector: 'app-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './config.component.html',
  styleUrls: ['./config.component.css']
})
export class ConfigComponent implements OnInit {
  configs: Configuration[] = [];

  constructor(private configService: ConfigService) {}

  ngOnInit(): void {
    this.loadConfigs();
  }

  loadConfigs(): void {
    this.configService.getAllConfigs().subscribe({
      next: (data) => this.configs = data,
      error: (err) => console.error(err)
    });
  }

  saveConfig(config: Configuration): void {
    this.configService.updateConfig(config.cle, config.valeur).subscribe({
      next: () => alert(`Configuration '${config.cle}' mise à jour`),
      error: (err) => console.error(err)
    });
  }

  syncToFirebase(): void {
    if (confirm('Voulez-vous synchroniser ces configurations avec Firebase ?')) {
      this.configService.syncToFirebase().subscribe({
        next: () => alert('Configurations synchronisées avec succès !'),
        error: (err) => {
          console.error(err);
          alert('Erreur lors de la synchronisation : ' + (err.error?.message || err.message));
        }
      });
    }
  }
}
