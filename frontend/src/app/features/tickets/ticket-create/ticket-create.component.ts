import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TicketService } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { Category, Priority } from '../../../core/models/ticket.models';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { ProblemAlertComponent } from '../../../shared/components/problem-alert/problem-alert.component';

@Component({
  selector: 'app-ticket-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ProblemAlertComponent
  ],
  templateUrl: './ticket-create.component.html'
})
export class TicketCreateComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  readonly i18n = inject(I18nService);

  readonly categories: Category[] = ['HARDWARE', 'SOFTWARE', 'RED', 'ACCESOS', 'OTRO'];
  readonly priorities: Priority[] = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'];

  readonly ticketForm: FormGroup = this.fb.group({
    title: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(4000)]],
    category: ['SOFTWARE', [Validators.required]],
    priority: ['MEDIA', [Validators.required]],
    site: ['', [Validators.required, Validators.maxLength(60)]],
    affectedAsset: ['', [Validators.required, Validators.maxLength(120)]]
  });

  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);

  ngOnInit(): void {
    // Pre-populate user's default site if available
    const user = this.authService.user();
    if (user?.site) {
      this.ticketForm.patchValue({ site: user.site });
    }
  }

  onSubmit(): void {
    if (this.ticketForm.invalid) {
      this.ticketForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.problem.set(null);

    this.ticketService.create(this.ticketForm.value).subscribe({
      next: created => {
        this.isSubmitting.set(false);
        const msg = created.technician
          ? `${this.i18n.t().tickets.createdSuccess} ${this.i18n.t().tickets.assignedToBadge} ${created.technician.fullName} (${created.technician.code})`
          : `${this.i18n.t().tickets.createdSuccess} ${this.i18n.t().tickets.unassigned}`;

        this.snackBar.open(msg, 'OK', {
          duration: 6000,
          horizontalPosition: 'right',
          verticalPosition: 'top'
        });

        this.router.navigate(['/tickets', created.id]);
      },
      error: (err: ProblemDetail) => {
        this.isSubmitting.set(false);
        this.problem.set(err);

        if (err.errors && Array.isArray(err.errors)) {
          for (const fe of err.errors) {
            const control = this.ticketForm.get(fe.field);
            if (control) {
              control.setErrors({ serverError: fe.message });
            }
          }
        }
      }
    });
  }
}
