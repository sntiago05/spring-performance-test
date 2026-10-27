import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TechnicianService } from '../../../core/services/technician.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { Category } from '../../../core/models/ticket.models';
import { TechnicianResponse } from '../../../core/models/technician.models';
import { UserStatus } from '../../../core/models/user.models';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { ProblemAlertComponent } from '../../../shared/components/problem-alert/problem-alert.component';

export interface TechnicianEditDialogData {
  technician: TechnicianResponse;
}

@Component({
  selector: 'app-technician-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    ProblemAlertComponent
  ],
  templateUrl: './technician-edit-dialog.component.html'
})
export class TechnicianEditDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly technicianService = inject(TechnicianService);
  readonly dialogRef = inject(MatDialogRef<TechnicianEditDialogComponent>);
  readonly i18n = inject(I18nService);

  readonly categories: Category[] = ['HARDWARE', 'SOFTWARE', 'RED', 'ACCESOS', 'OTRO'];
  readonly statuses: UserStatus[] = ['ACTIVO', 'INACTIVO'];
  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly form: FormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: TechnicianEditDialogData) {
    const tech = data.technician;
    this.form = this.fb.group({
      fullName: [tech.fullName, [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
      specialty: [tech.specialty, [Validators.required]],
      site: [tech.site, [Validators.required, Validators.maxLength(60)]],
      maxActiveTickets: [tech.maxActiveTickets, [Validators.required, Validators.min(1), Validators.max(20)]],
      status: [tech.status, [Validators.required]]
    });
  }

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.problem.set(null);

    this.technicianService.update(this.data.technician.id, this.form.value).subscribe({
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
