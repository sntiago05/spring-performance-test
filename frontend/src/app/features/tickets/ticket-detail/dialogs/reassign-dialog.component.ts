import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TicketService } from '../../../../core/services/ticket.service';
import { TechnicianService } from '../../../../core/services/technician.service';
import { I18nService } from '../../../../core/i18n/i18n.service';
import { TicketResponse } from '../../../../core/models/ticket.models';
import { TechnicianResponse } from '../../../../core/models/technician.models';
import { ProblemDetail } from '../../../../core/models/problem-detail.models';
import { ProblemAlertComponent } from '../../../../shared/components/problem-alert/problem-alert.component';

export interface ReassignDialogData {
  ticket: TicketResponse;
}

@Component({
  selector: 'app-reassign-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    ProblemAlertComponent
  ],
  templateUrl: './reassign-dialog.component.html'
})
export class ReassignDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);
  private readonly technicianService = inject(TechnicianService);
  readonly i18n = inject(I18nService);

  readonly technicians = signal<TechnicianResponse[]>([]);
  readonly loadingTechs = signal(true);
  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly form: FormGroup = this.fb.group({
    technicianId: [null, [Validators.required]],
    observations: ['', [Validators.required, Validators.maxLength(1000)]]
  });

  constructor(
    public dialogRef: MatDialogRef<ReassignDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ReassignDialogData
  ) {}

  ngOnInit(): void {
    this.technicianService.list({ status: 'ACTIVO', size: 100 }).subscribe({
      next: page => {
        this.technicians.set(page.content);
        this.loadingTechs.set(false);
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
        this.loadingTechs.set(false);
      }
    });
  }

  isCompatible(tech: TechnicianResponse): boolean {
    return tech.specialty === this.data.ticket.category || tech.specialty === 'OTRO';
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.problem.set(null);

    this.ticketService.reassign(this.data.ticket.id, this.form.value).subscribe({
      next: updated => {
        this.isSubmitting.set(false);
        this.dialogRef.close(updated);
      },
      error: (err: ProblemDetail) => {
        this.isSubmitting.set(false);
        this.problem.set(err);
      }
    });
  }
}
