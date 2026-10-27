import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SlaService } from '../../core/services/sla.service';
import { TicketService } from '../../core/services/ticket.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { SlaPolicyResponse } from '../../core/models/sla.models';
import { ProblemDetail } from '../../core/models/problem-detail.models';
import { PriorityBadgeComponent } from '../../shared/components/priority-badge/priority-badge.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { ProblemAlertComponent } from '../../shared/components/problem-alert/problem-alert.component';
import { EscalationSummaryDialogComponent } from '../../shared/components/escalation-summary-dialog/escalation-summary-dialog.component';
import { SlaEditDialogComponent } from './sla-edit-dialog/sla-edit-dialog.component';

@Component({
  selector: 'app-sla-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    PriorityBadgeComponent,
    LoadingComponent,
    ProblemAlertComponent
  ],
  templateUrl: './sla-list.component.html'
})
export class SlaListComponent implements OnInit {
  private readonly slaService = inject(SlaService);
  private readonly ticketService = inject(TicketService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly policies = signal<SlaPolicyResponse[]>([]);
  readonly loading = signal(true);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly displayedColumns: string[] = [
    'priority',
    'resolutionHours',
    'escalationThresholdPercent',
    'escalatesAt',
    'actions'
  ];

  ngOnInit(): void {
    this.fetchPolicies();
  }

  fetchPolicies(): void {
    this.loading.set(true);
    this.problem.set(null);

    this.slaService.list().subscribe({
      next: list => {
        this.policies.set(list);
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
        this.loading.set(false);
      }
    });
  }

  calculateEscalationPoint(policy: SlaPolicyResponse): string {
    const hours = (policy.resolutionHours * policy.escalationThresholdPercent) / 100;
    return `${hours.toFixed(1)} h`;
  }

  openEditDialog(policy: SlaPolicyResponse): void {
    const dialogRef = this.dialog.open(SlaEditDialogComponent, {
      data: { policy },
      width: '450px'
    });

    dialogRef.afterClosed().subscribe((updated: SlaPolicyResponse | null) => {
      if (updated) {
        this.snackBar.open('SLA policy updated successfully', 'OK', { duration: 4000 });
        this.fetchPolicies();
      }
    });
  }

  runReview(): void {
    this.ticketService.runEscalations().subscribe({
      next: summary => {
        this.dialog.open(EscalationSummaryDialogComponent, {
          data: summary,
          width: '450px'
        });
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
      }
    });
  }
}
