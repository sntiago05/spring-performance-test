import { Component, Inject, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EscalationSummaryResponse } from '../../../core/models/sla.models';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-escalation-summary-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './escalation-summary-dialog.component.html'
})
export class EscalationSummaryDialogComponent {
  readonly i18n = inject(I18nService);

  constructor(
    public dialogRef: MatDialogRef<EscalationSummaryDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EscalationSummaryResponse
  ) {}
}
