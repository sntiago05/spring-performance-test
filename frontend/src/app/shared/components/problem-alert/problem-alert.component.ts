import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-problem-alert',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './problem-alert.component.html'
})
export class ProblemAlertComponent {
  private readonly snackBar = inject(MatSnackBar);
  readonly i18n = inject(I18nService);

  @Input() problem: ProblemDetail | null = null;
  @Input() dismissible: boolean = true;
  @Output() dismissed = new EventEmitter<void>();

  copyTraceId(): void {
    if (this.problem?.traceId) {
      navigator.clipboard.writeText(this.problem.traceId).then(() => {
        this.snackBar.open(this.i18n.t().problem.traceIdCopied, 'OK', {
          duration: 3000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
      });
    }
  }

  onClose(): void {
    this.dismissed.emit();
  }
}
