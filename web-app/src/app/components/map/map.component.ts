import { Component, OnInit, AfterViewInit, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { trigger, transition, style, animate, state } from '@angular/animations';
import * as L from 'leaflet';
import { SignalementService } from '../../services/signalement.service';
import { Signalement } from '../../models/signalement.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css'],
  animations: [
    trigger('panelSlide', [
      state('void', style({ transform: 'translateX(100%)', opacity: 0 })),
      state('*', style({ transform: 'translateX(0)', opacity: 1 })),
      transition('void <=> *', animate('400ms cubic-bezier(0.25, 0.8, 0.25, 1)'))
    ]),
    trigger('fadeIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(10px)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ])
    ])
  ]
})
export class MapComponent implements OnInit, AfterViewInit {
  public map: any;
  public signalements: Signalement[] = [];
  public filteredSignalements: Signalement[] = [];
  public selectedSignalement: Signalement | null = null;
  public isPanelOpen = false;
  public isLoading = true;
  public searchQuery = '';
  public activeFilters: Set<string> = new Set(['nouveau', 'en cours', 'terminé']);
  
  private markersMap: Map<string, L.Marker> = new Map();
  private signalementService = inject(SignalementService);
  public authService = inject(AuthService);

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
    setTimeout(() => {
      if (this.map) {
        this.map.invalidateSize();
      }
    }, 500);
  }

  private initMap(): void {
    const tana = { lat: -18.8792, lng: 47.5079 };

    this.map = L.map('map', {
      zoomControl: false,
      attributionControl: false
    }).setView([tana.lat, tana.lng], 13);

    L.tileLayer('http://localhost:8082/styles/basic-preview/{z}/{x}/{y}.png', {
      maxZoom: 20
    }).addTo(this.map);

    L.control.zoom({ position: 'bottomright' }).addTo(this.map);
  }

  public toggleFilter(status: string): void {
    const s = status.toLowerCase();
    if (this.activeFilters.has(s)) {
      this.activeFilters.delete(s);
    } else {
      this.activeFilters.add(s);
    }
    this.applyFiltersAndSearch();
  }

  public onSearchChange(): void {
    this.applyFiltersAndSearch();
  }

  private applyFiltersAndSearch(): void {
    this.filteredSignalements = this.signalements.filter(s => {
      const matchesFilter = this.activeFilters.has(String(s.statut || 'Nouveau').toLowerCase());
      const matchesSearch = !this.searchQuery || 
        s.description?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        s.id?.toString().includes(this.searchQuery);
      return matchesFilter && matchesSearch;
    });
    this.updateMarkersVisibility();
  }

  private updateMarkersVisibility(): void {
    this.markersMap.forEach((marker, id) => {
      const isVisible = this.filteredSignalements.some(s => s.id === id);
      if (isVisible) {
        marker.addTo(this.map);
      } else {
        marker.remove();
      }
    });
  }

  public selectSignalement(s: Signalement): void {
    this.selectedSignalement = s;
    this.isPanelOpen = true;
    
    if (this.map && s.latitude && s.longitude) {
      this.map.flyTo([s.latitude, s.longitude], 16, {
        duration: 1.5,
        easeLinearity: 0.25
      });
      
      const marker = this.markersMap.get(s.id!);
      if (marker) {
        // Reset previous selected marker if any
        this.markersMap.forEach(m => m.getElement()?.classList.remove('marker-selected'));
        marker.getElement()?.classList.add('marker-selected');
        marker.openPopup();
      }
    }
  }

  public closePanel(): void {
    this.isPanelOpen = false;
    this.selectedSignalement = null;
    this.markersMap.forEach(m => m.getElement()?.classList.remove('marker-selected'));
  }

  private createCustomIcon(color: string, isSelected: boolean = false): L.DivIcon {
    return L.divIcon({
      className: `custom-div-icon ${isSelected ? 'marker-selected' : ''}`,
      html: `
        <div class="marker-container relative flex items-center justify-center">
          <div class="ping-animation absolute w-10 h-10 rounded-full opacity-20 animate-ping" style="background-color: ${color}"></div>
          <svg class="pin-svg relative w-8 h-8 drop-shadow-2xl transition-all duration-300" viewBox="0 0 24 24" fill="none">
            <defs>
              <linearGradient id="grad-${color.replace('#','')}" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:${color};stop-opacity:1" />
                <stop offset="100%" style="stop-color:${this.darkenColor(color)};stop-opacity:1" />
              </linearGradient>
            </defs>
            <path d="M12 21C15.5 17.4 19 14.1764 19 10.2C19 6.22355 15.866 3 12 3C8.13401 3 5 6.22355 5 10.2C5 14.1764 8.5 17.4 12 21Z" 
                  fill="url(#grad-${color.replace('#','')})" stroke="white" stroke-width="1.5"/>
            <circle cx="12" cy="10" r="2.5" fill="white"/>
          </svg>
        </div>
      `,
      iconSize: [32, 32],
      iconAnchor: [16, 32],
      popupAnchor: [0, -32]
    });
  }

  private darkenColor(color: string): string {
    // Simple darkening logic for gradients
    return color === '#3b82f6' ? '#1d4ed8' : 
           color === '#f59e0b' ? '#b45309' : 
           color === '#10b981' ? '#047857' : '#334155';
  }

  private loadSignalements(): void {
    this.isLoading = true;
    this.signalementService.getAllSignalements().subscribe({
      next: (data) => {
        this.signalements = data;
        this.filteredSignalements = [...data];
        this.addMarkers();
        this.isLoading = false;
      },
      error: (err) => {
        console.error(err);
        this.isLoading = false;
      }
    });
  }

  private addMarkers(): void {
    if (!this.map) return;

    this.markersMap.forEach(m => m.remove());
    this.markersMap.clear();

    this.signalements.forEach(s => {
      const statusName = String(s.statut || 'Nouveau');
      const color = this.getStatusColor(s.statut);
      
      const marker = L.marker([s.latitude, s.longitude], {
        icon: this.createCustomIcon(color)
      })
      .on('click', () => this.selectSignalement(s))
      .addTo(this.map)
      .bindPopup(`
        <div class="p-3 font-sans">
          <p class="text-xs font-black text-slate-800 uppercase mb-1">${statusName}</p>
          <p class="text-[10px] text-slate-500">${new Date(s.dateSignalement).toLocaleDateString()}</p>
        </div>
      `, { className: 'minimal-popup' });

      this.markersMap.set(s.id!, marker);
    });

    this.map.invalidateSize();
  }

  public getStatusColor(status: string): string {
    const st = String(status || '').toLowerCase();
    if (st.includes('nouveau')) return '#3b82f6';
    if (st.includes('cours')) return '#f59e0b';
    if (st.includes('termin')) return '#10b981';
    return '#64748b';
  }
}
