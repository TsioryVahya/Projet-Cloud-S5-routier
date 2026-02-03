import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { ReportTypeService } from '../../services/report-type.service';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-report-type-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './report-type-form.component.html',
  styleUrls: ['./report-type-form.component.css']
})
export class ReportTypeFormComponent implements OnInit {
  reportTypeForm: FormGroup;
  isEditMode = false;
  typeId?: number;
  submitting = false;

  predefinedColors = [
    { name: 'Rouge', hex: '#ef4444' },
    { name: 'Orange', hex: '#f97316' },
    { name: 'Jaune', hex: '#f59e0b' },
    { name: 'Vert', hex: '#22c55e' },
    { name: 'Bleu', hex: '#3b82f6' },
    { name: 'Indigo', hex: '#6366f1' },
    { name: 'Violet', hex: '#a855f7' },
    { name: 'Rose', hex: '#ec4899' },
    { name: 'Gris', hex: '#64748b' },
    { name: 'Noir', hex: '#0f172a' }
  ];

  constructor(
    private fb: FormBuilder,
    private reportTypeService: ReportTypeService,
    private route: ActivatedRoute,
    private router: Router,
    private sanitizer: DomSanitizer
  ) {
    this.reportTypeForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(3)]],
      description: ['', [Validators.required]],
      iconePath: ['', [Validators.required]],
      couleur: ['#3b82f6', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.typeId = +params['id'];
        this.loadTypeData(this.typeId);
      }
    });
  }

  loadTypeData(id: number): void {
    this.reportTypeService.getTypeById(id).subscribe({
      next: (type) => {
        this.reportTypeForm.patchValue(type);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des données:', err);
        alert('Erreur lors du chargement du type de signalement.');
        this.router.navigate(['/dashboard/types-signalement']);
      }
    });
  }

  get previewIcon(): SafeHtml {
    const svg = this.reportTypeForm.get('iconePath')?.value;
    if (!svg) return '';

    let finalSvg = svg.trim();
    
    // Vérification plus stricte du contenu
    if (!finalSvg.startsWith('<svg') && (finalSvg.toLowerCase().startsWith('m') || finalSvg.includes(' '))) {
      finalSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="w-full h-full"><path d="${finalSvg}"/></svg>`;
    } else if (!finalSvg.startsWith('<svg')) {
      return '';
    }

    return this.sanitizer.bypassSecurityTrustHtml(finalSvg);
  }

  onSubmit(): void {
    if (this.reportTypeForm.invalid) {
      return;
    }

    this.submitting = true;
    const formData = this.reportTypeForm.value;

    if (this.isEditMode && this.typeId) {
      this.reportTypeService.updateType(this.typeId, formData).subscribe({
        next: () => {
          this.submitting = false;
          this.router.navigate(['/dashboard/types-signalement']);
        },
        error: (err) => {
          this.submitting = false;
          console.error('Erreur lors de la mise à jour:', err);
          alert('Erreur lors de la mise à jour du type.');
        }
      });
    } else {
      this.reportTypeService.createType(formData).subscribe({
        next: () => {
          this.submitting = false;
          this.router.navigate(['/dashboard/types-signalement']);
        },
        error: (err) => {
          this.submitting = false;
          console.error('Erreur lors de la création:', err);
          alert('Erreur lors de la création du type.');
        }
      });
    }
  }
}
