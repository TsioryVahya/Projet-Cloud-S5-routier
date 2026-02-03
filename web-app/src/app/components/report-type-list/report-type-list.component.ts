import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ReportTypeService } from '../../services/report-type.service';
import { TypeSignalement } from '../../models/signalement.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-report-type-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './report-type-list.component.html',
  styleUrls: ['./report-type-list.component.css']
})
export class ReportTypeListComponent implements OnInit {
  reportTypes: TypeSignalement[] = [];
  loading = true;
  error: string | null = null;
  syncing = false;

  constructor(
    private reportTypeService: ReportTypeService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.loadReportTypes();
  }

  loadReportTypes(): void {
    this.loading = true;
    this.reportTypeService.getAllTypes().subscribe({
      next: (types) => {
        this.reportTypes = types;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des types de signalement:', err);
        this.error = 'Impossible de charger les types de signalement.';
        this.loading = false;
      }
    });
  }

  getSafeSvg(svgString: string): SafeHtml {
    if (!svgString) return '';
    
    let finalSvg = svgString.trim();
    
    // Si c'est un chemin (commence par M, m ou une commande de dessin) et ne contient pas de balises
    if (!finalSvg.startsWith('<svg') && (finalSvg.toLowerCase().startsWith('m') || finalSvg.includes(' '))) {
      finalSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-full h-full"><path d="${finalSvg}"/></svg>`;
    } else if (!finalSvg.startsWith('<svg')) {
      // Si ce n'est ni un SVG complet ni un path valide, on ne l'affiche pas pour éviter les erreurs
      return '';
    }
    
    return this.sanitizer.bypassSecurityTrustHtml(finalSvg);
  }

  deleteType(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer ce type de signalement ?')) {
      this.reportTypeService.deleteType(id).subscribe({
        next: () => {
          this.reportTypes = this.reportTypes.filter(t => t.id !== id);
        },
        error: (err) => {
          console.error('Erreur lors de la suppression:', err);
          alert('Erreur lors de la suppression du type.');
        }
      });
    }
  }

  syncToFirebase(): void {
    if (this.syncing) return;

    this.syncing = true;
    this.reportTypeService.syncToFirebase().subscribe({
      next: (result) => {
        this.syncing = false;
        alert(`Synchronisation réussie ! ${result.syncedTypes} types ont été envoyés vers Firebase.`);
      },
      error: (err) => {
        console.error('Erreur lors de la synchronisation:', err);
        this.syncing = false;
        alert('Erreur lors de la synchronisation vers Firebase.');
      }
    });
  }
}
