import { Component, Inject, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { SlaService } from '../../../core/services/sla.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { SlaPolicyResponse } from '../../../core/models/sla.models';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { ProblemAlertComponent } from '../../../shared/components/problem-alert/problem-alert.component';

export interface SlaEditDialogData {
  policy: SlaPolicyResponse;
}

@Component({
  selector: 'app-sla-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    ProblemAlertComponent
  ],
  templateUrl: './sla-edit-dialog.component.html'
})
export class SlaEditDialogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly slaService = inject(SlaService);
  readonly dialogRef = inject(MatDialogRef<SlaEditDialogComponent>);
  readonly i18n = inject(I18nService);

  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly form: FormGroup;

  constructor(@Inject(MAT_DIALOG_DATA) public data: SlaEditDialogData) {
    const p = data.policy;
    this.form = this.fb.group({
      resolutionHours: [p.resolutionHours, [Validators.required, Validators.min(1), Validators.max(720)]],
      escalationThresholdPercent: [p.escalationThresholdPercent, [Validators.required, Validators.min(1), Validators.max(99)]]
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

    this.slaService.update(this.data.policy.priority, this.form.value).subscribe({
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
