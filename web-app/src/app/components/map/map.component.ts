import { Component, OnInit, AfterViewInit, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';
import { SignalementService } from '../../services/signalement.service';
import { Signalement } from '../../models/signalement.model';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css']
})
export class MapComponent implements OnInit, AfterViewInit {
  private map: any;
  private signalements: Signalement[] = [];
  private signalementService = inject(SignalementService);

  // Form state
  showForm = false;
  newSignalement = {
    latitude: 0,
    longitude: 0,
    description: '',
    email: ''
  };
  isSubmitting = false;

  @HostListener('window:resize', ['$event'])
  onResize() {
    if (this.map) {
      this.map.invalidateSize();
    }
  }

  ngOnInit(): void {
    this.loadSignalements();
  }

  ngAfterViewInit(): void {
    this.initMap();
    // Force Leaflet to recalculate container size after a small delay
    setTimeout(() => {
      if (this.map) {
        this.map.invalidateSize();
      }
    }, 500);
  }

  private initMap(): void {
    const tana = { lat: -18.8792, lng: 47.5079 };

    this.map = L.map('map').setView([tana.lat, tana.lng], 13);

    L.tileLayer('http://localhost:8082/styles/basic-preview/{z}/{x}/{y}.png', {
      maxZoom: 20,
      attribution: '© OpenStreetMap contributors'
    }).addTo(this.map);

    // Click handler for reporting
    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.onMapClick(e);
    });

    const iconDefault = L.icon({
      iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
      iconUrl: 'assets/leaflet/marker-icon.png',
      shadowUrl: 'assets/leaflet/marker-shadow.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      tooltipAnchor: [16, -28],
      shadowSize: [41, 41]
    });
    L.Marker.prototype.options.icon = iconDefault;
  }

  private onMapClick(e: L.LeafletMouseEvent): void {
    this.newSignalement.latitude = e.latlng.lat;
    this.newSignalement.longitude = e.latlng.lng;
    this.showForm = true;
  }

  submitSignalement(): void {
    if (!this.newSignalement.description.trim() || !this.newSignalement.email.trim()) return;

    this.isSubmitting = true;
    const data = {
      ...this.newSignalement,
      statutId: 1 // Nouveau par défaut
    };

    this.signalementService.createSignalement(data).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.showForm = false;
        this.newSignalement.description = '';
        this.newSignalement.email = '';
        this.loadSignalements();
      },
      error: (err) => {
        console.error(err);
        this.isSubmitting = false;
      }
    });
  }

  cancelReport(): void {
    this.showForm = false;
    this.newSignalement.description = '';
    this.newSignalement.email = '';
  }

  private loadSignalements(): void {
    this.signalementService.getAllSignalements().subscribe({
      next: (data) => {
        this.signalements = data;
        this.addMarkers();
      },
      error: (err) => console.error(err)
    });
  }

  private addMarkers(): void {
    if (!this.map) return;

    // Clear existing markers if any (optional, but good for refresh)
    this.map.eachLayer((layer: any) => {
      if (layer instanceof L.Marker) {
        this.map.removeLayer(layer);
      }
    });

    this.signalements.forEach(s => {
      const color = this.getStatusColor(s.statut.nom);
      
      const marker = L.marker([s.latitude, s.longitude])
        .addTo(this.map)
        .bindPopup(`
          <div class="p-2">
            <div class="flex items-center mb-2">
              <span class="w-3 h-3 rounded-full mr-2" style="background-color: ${color}"></span>
              <strong class="text-slate-800">${s.statut.nom}</strong>
            </div>
            <p class="text-sm text-slate-600 mb-1">${s.description || 'Pas de description'}</p>
            <div class="text-[10px] text-slate-400 border-t pt-1">
              ${new Date(s.dateSignalement).toLocaleString()}
            </div>
          </div>
        `);
    });

    // Final refresh to ensure everything is visible
    this.map.invalidateSize();
  }

  private getStatusColor(status: string): string {
    switch(status.toUpperCase()) {
      case 'NOUVEAU': return '#3b82f6'; // blue-500
      case 'EN_COURS': return '#eab308'; // yellow-500
      case 'TERMINE': return '#22c55e'; // green-500
      default: return '#94a3b8'; // slate-400
    }
  }
}
