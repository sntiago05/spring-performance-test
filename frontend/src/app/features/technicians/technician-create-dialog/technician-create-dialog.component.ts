import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TechnicianService } from '../../../core/services/technician.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { Category } from '../../../core/models/ticket.models';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { corporateEmailValidator } from '../../auth/register/register.component';
import { ProblemAlertComponent } from '../../../shared/components/problem-alert/problem-alert.component';

@Component({
  selector: 'app-technician-create-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ProblemAlertComponent
  ],
  templateUrl: './technician-create-dialog.component.html'
})
export class TechnicianCreateDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly technicianService = inject(TechnicianService);
  readonly dialogRef = inject(MatDialogRef<TechnicianCreateDialogComponent>);
  readonly i18n = inject(I18nService);

  readonly categories: Category[] = ['HARDWARE', 'SOFTWARE', 'RED', 'ACCESOS', 'OTRO'];
  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);
  readonly hidePassword = signal(true);

  readonly form: FormGroup = this.fb.group({
    document: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{5,30}$/)]],
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, corporateEmailValidator]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    site: ['', [Validators.required, Validators.maxLength(60)]],
    area: ['IT Support', [Validators.required, Validators.maxLength(60)]],
    specialty: ['HARDWARE', [Validators.required]],
    maxActiveTickets: [5, [Validators.required, Validators.min(1), Validators.max(20)]]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.problem.set(null);

    this.technicianService.create(this.form.value).subscribe({
      next: created => {
        this.isSubmitting.set(false);
        this.dialogRef.close(created);
      },
      error: (err: ProblemDetail) => {
        this.isSubmitting.set(false);
        this.problem.set(err);

        if (err.errors && Array.isArray(err.errors)) {
          for (const fe of err.errors) {
            const control = this.form.get(fe.field);
            if (control) {
              control.setErrors({ serverError: fe.message });
            }
          }
        }
      }
    });
  }
}
