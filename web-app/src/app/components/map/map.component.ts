import { Component, OnInit, AfterViewInit, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { trigger, transition, style, animate, state } from '@angular/animations';
import * as L from 'leaflet';
import { SignalementService } from '../../services/signalement.service';
import { Signalement } from '../../models/signalement.model';
import { AuthService } from '../../services/auth.service';
import { Router } from '@angular/router';

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
  public isUpdatingStatus = false;
  public searchQuery = '';
  public activeFilters: Set<string> = new Set(['nouveau', 'en cours', 'terminé']);
  public searchSuggestions: Signalement[] = [];
  public showSuggestions = false;
  
  public getFirstPhotoUrl(s: Signalement | null): string | null {
    if (s && s.galerie && s.galerie.length > 0) {
      return s.galerie[0].url;
    }
    return null;
  }
  
  private readonly INITIAL_CENTER: L.LatLngExpression = [-18.8792, 47.5079];
  private readonly INITIAL_ZOOM = 13;
  
  private markersMap: Map<string, L.Marker> = new Map();
  private signalementService = inject(SignalementService);
  public authService = inject(AuthService);
  private router = inject(Router);

  @HostListener('document:click', ['$event'])
  onClickOutside(event: Event) {
    const searchBar = document.querySelector('.relative.pointer-events-auto');
    if (searchBar && !searchBar.contains(event.target as Node)) {
      this.showSuggestions = false;
    }
  }

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
    this.map = L.map('map', {
      zoomControl: false,
      attributionControl: false
    }).setView(this.INITIAL_CENTER, this.INITIAL_ZOOM);

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
    if (this.searchQuery.length > 1) {
      this.searchSuggestions = this.signalements
        .filter(s => 
          s.description?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
          s.id?.toString().includes(this.searchQuery)
        )
        .slice(0, 5); // Limit to 5 suggestions
      this.showSuggestions = this.searchSuggestions.length > 0;
    } else {
      this.searchSuggestions = [];
      this.showSuggestions = false;
    }
    this.applyFiltersAndSearch();
  }

  public selectSuggestion(s: Signalement): void {
    this.searchQuery = s.description || s.id || '';
    this.showSuggestions = false;
    this.selectSignalement(s);
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
    // Reset previous selected marker if any
    if (this.selectedSignalement && this.selectedSignalement.id) {
      const prevMarker = this.markersMap.get(this.selectedSignalement.id);
      if (prevMarker) {
        prevMarker.setIcon(this.createCustomIcon(this.selectedSignalement, false));
      }
    }

    this.selectedSignalement = s;
    this.isPanelOpen = true;
    
    if (this.map && s.latitude && s.longitude) {
      this.map.flyTo([s.latitude, s.longitude], 16, {
        duration: 1.5,
        easeLinearity: 0.25
      });
      
      const marker = this.markersMap.get(s.id!);
      if (marker) {
        marker.setIcon(this.createCustomIcon(s, true));
        marker.openPopup();
      }
    }
  }

  public closePanel(): void {
    if (this.selectedSignalement && this.selectedSignalement.id) {
      const marker = this.markersMap.get(this.selectedSignalement.id);
      if (marker) {
        marker.setIcon(this.createCustomIcon(this.selectedSignalement, false));
      }
    }

    this.isPanelOpen = false;
    this.selectedSignalement = null;
    
    // Reset map view to initial state
    if (this.map) {
      this.map.flyTo(this.INITIAL_CENTER, this.INITIAL_ZOOM, {
        duration: 1.5,
        easeLinearity: 0.25
      });
    }
  }

  private createCustomIcon(s: Signalement, isSelected: boolean = false): L.DivIcon {
    const status = String(s.statut || 'Nouveau').toLowerCase();
    const typeColor = s.typeCouleur || this.getStatusColor(status);
    let innerIcon = '';

    // Utiliser l'icône du type si elle existe, sinon icône par défaut selon le statut
    if (s.typeIcone) {
      innerIcon = `
        <g transform="translate(7.5, 5.5) scale(0.4)">
          <path d="${s.typeIcone}" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        </g>
      `;
    } else {
      if (status.includes('nouveau')) {
        innerIcon = `
          <g transform="translate(7.5, 5.5) scale(0.4)">
            <path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" 
                  stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          </g>
        `;
      } else if (status.includes('cours')) {
        innerIcon = `
          <g transform="translate(7.5, 5.5) scale(0.4)">
            <path d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" 
                  stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="12" cy="12" r="3" stroke="white" stroke-width="3"/>
          </g>
        `;
      } else {
        innerIcon = `
          <g transform="translate(7.5, 5.5) scale(0.4)">
            <path d="M5 13l4 4L19 7" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
          </g>
        `;
      }
    }

    // Badge de statut (en haut à droite)
    let badgeHtml = '';
    let strokeColor = 'white';
    let strokeWidth = '1.5';

    if (status.includes('termin')) {
      strokeColor = '#10b981'; // Vert pour terminé
      strokeWidth = '2.5';
      badgeHtml = `
        <div class="absolute -top-1 -right-1 w-4 h-4 bg-emerald-500 rounded-full border-2 border-white flex items-center justify-center shadow-lg z-10">
          <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="4" d="M5 13l4 4L19 7" />
          </svg>
        </div>
      `;
    } else if (status.includes('cours')) {
      strokeColor = '#f59e0b'; // Or/Gold pour en cours
      strokeWidth = '2.5';
      badgeHtml = `
        <div class="absolute -top-1 -right-1 w-4 h-4 bg-amber-500 rounded-full border-2 border-white flex items-center justify-center shadow-lg z-10">
          <svg class="w-2.5 h-2.5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
          </svg>
        </div>
      `;
    }

    return L.divIcon({
      className: `custom-div-icon ${isSelected ? 'marker-selected' : ''}`,
      html: `
        <div class="marker-container relative flex items-center justify-center">
          ${isSelected ? `<div class="ping-animation absolute w-10 h-10 rounded-full opacity-20 animate-ping" style="background-color: ${typeColor}"></div>` : ''}
          ${badgeHtml}
          <svg class="pin-svg relative w-8 h-8 drop-shadow-2xl transition-all duration-300" viewBox="0 0 24 24" fill="none">
            <defs>
              <linearGradient id="grad-${typeColor.replace('#','')}" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" style="stop-color:${typeColor};stop-opacity:1" />
                <stop offset="100%" style="stop-color:${this.darkenColor(typeColor)};stop-opacity:1" />
              </linearGradient>
            </defs>
            <path d="M12 21C15.5 17.4 19 14.1764 19 10.2C19 6.22355 15.866 3 12 3C8.13401 3 5 6.22355 5 10.2C5 14.1764 8.5 17.4 12 21Z" 
                  fill="url(#grad-${typeColor.replace('#','')})" 
                  stroke="${strokeColor}" 
                  stroke-width="${strokeWidth}"/>
            ${innerIcon}
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
      
      const marker = L.marker([s.latitude, s.longitude], {
        icon: this.createCustomIcon(s)
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

  public logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  public getStatusColor(status: string): string {
    const st = String(status || '').toLowerCase();
    if (st.includes('nouveau')) return '#3b82f6';
    if (st.includes('cours')) return '#f59e0b';
    if (st.includes('termin')) return '#10b981';
    return '#64748b';
  }

  public getNextStatusLabel(): string {
    const current = this.selectedSignalement?.statut.toLowerCase() || '';
    if (current.includes('nouveau')) return 'Passer en cours';
    if (current.includes('cours')) return 'Marquer comme terminé';
    return '';
  }

  public updateStatus(): void {
    if (!this.selectedSignalement || !this.selectedSignalement.id || this.isUpdatingStatus) return;

    const id = this.selectedSignalement.id;
    const current = this.selectedSignalement.statut.toLowerCase();
    let nextStatus = '';

    if (current.includes('nouveau')) nextStatus = 'En cours';
    else if (current.includes('cours')) nextStatus = 'Terminé';
    else return; // Déjà terminé ou inconnu

    this.isUpdatingStatus = true;
    
    // On prépare l'objet de mise à jour. On ne change que le statut.
    const updateData = { ...this.selectedSignalement, statut: nextStatus };

    this.signalementService.updateSignalement(id, updateData).subscribe({
      next: () => {
        if (this.selectedSignalement) {
          this.selectedSignalement.statut = nextStatus;
          
          // Mettre à jour dans la liste principale pour la synchronisation des filtres/marqueurs
          const index = this.signalements.findIndex(s => s.id === id);
          if (index !== -1) {
            this.signalements[index].statut = nextStatus;
            
            // Mettre à jour le marqueur visuellement
            const marker = this.markersMap.get(id);
            if (marker) {
              marker.setIcon(this.createCustomIcon(this.selectedSignalement, true));
              marker.setPopupContent(`
                <div class="p-3 font-sans">
                  <p class="text-xs font-black text-slate-800 uppercase mb-1">${nextStatus}</p>
                  <p class="text-[10px] text-slate-500">${new Date(this.selectedSignalement.dateSignalement).toLocaleDateString()}</p>
                </div>
              `);
            }
          }
        }
        this.isUpdatingStatus = false;
        this.applyFiltersAndSearch();
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour du statut:', err);
        this.isUpdatingStatus = false;
      }
    });
  }
}
