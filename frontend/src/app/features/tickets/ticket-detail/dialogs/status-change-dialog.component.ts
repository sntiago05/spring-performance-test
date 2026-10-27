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
import { I18nService } from '../../../../core/i18n/i18n.service';
import { TicketResponse, TicketStatus } from '../../../../core/models/ticket.models';
import { Role } from '../../../../core/models/user.models';
import { ProblemDetail } from '../../../../core/models/problem-detail.models';
import { getValidNextStatuses } from '../../utils/ticket-status-transitions';
import { ProblemAlertComponent } from '../../../../shared/components/problem-alert/problem-alert.component';

export interface StatusChangeDialogData {
  ticket: TicketResponse;
  userRole: Role;
}

@Component({
  selector: 'app-status-change-dialog',
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
  templateUrl: './status-change-dialog.component.html'
})
export class StatusChangeDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);
  readonly i18n = inject(I18nService);

  readonly availableStatuses: TicketStatus[];
  readonly form: FormGroup;
  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);

  constructor(
    public dialogRef: MatDialogRef<StatusChangeDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: StatusChangeDialogData
  ) {
    this.availableStatuses = getValidNextStatuses(data.ticket.status, data.userRole);

    const initialTarget = this.availableStatuses.length > 0 ? this.availableStatuses[0] : '';
    this.form = this.fb.group({
      status: [initialTarget, [Validators.required]],
      observations: ['', [Validators.required, Validators.maxLength(1000)]]
    });
  }

  ngOnInit(): void {
    this.updateObservationValidators(this.form.get('status')?.value);

    this.form.get('status')?.valueChanges.subscribe(targetStatus => {
      this.updateObservationValidators(targetStatus);
    });
  }

  private updateObservationValidators(targetStatus: TicketStatus): void {
    const obsControl = this.form.get('observations');
    if (!obsControl) return;

    if (targetStatus === 'CANCELADO') {
      obsControl.setValidators([Validators.required, Validators.minLength(15), Validators.maxLength(1000)]);
    } else {
      obsControl.setValidators([Validators.required, Validators.minLength(1), Validators.maxLength(1000)]);
    }
    obsControl.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.problem.set(null);

    this.ticketService.changeStatus(this.data.ticket.id, this.form.value).subscribe({
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
