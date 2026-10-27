import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTableModule } from '@angular/material/table';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';
import { TicketService } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import {
  AssignmentResponse,
  HistoryResponse,
  TicketResponse
} from '../../../core/models/ticket.models';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { PriorityBadgeComponent } from '../../../shared/components/priority-badge/priority-badge.component';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ProblemAlertComponent } from '../../../shared/components/problem-alert/problem-alert.component';
import { LocalizedDatePipe } from '../../../shared/pipes/localized-date.pipe';
import { SlaCountdownPipe } from '../../../shared/pipes/sla-countdown.pipe';
import { StatusChangeDialogComponent } from './dialogs/status-change-dialog.component';
import { ReassignDialogComponent } from './dialogs/reassign-dialog.component';
import { getValidNextStatuses } from '../utils/ticket-status-transitions';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatTableModule,
    StatusBadgeComponent,
    PriorityBadgeComponent,
    LoadingComponent,
    EmptyStateComponent,
    ProblemAlertComponent,
    LocalizedDatePipe,
    SlaCountdownPipe
  ],
  templateUrl: './ticket-detail.component.html'
})
export class TicketDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ticketService = inject(TicketService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly ticket = signal<TicketResponse | null>(null);
  readonly history = signal<HistoryResponse[]>([]);
  readonly assignments = signal<AssignmentResponse[]>([]);
  readonly loading = signal(true);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly assignmentColumns: string[] = [
    'source',
    'technicianId',
    'assignedBy',
    'assignedAt',
    'estimatedHours',
    'withinSla',
    'active'
  ];

  readonly canChangeStatus = computed(() => {
    const t = this.ticket();
    const role = this.authService.role();
    if (!t || !role) return false;
    if (t.status === 'CERRADO' || t.status === 'CANCELADO') return false;

    const validStates = getValidNextStatuses(t.status, role);
    if (validStates.length === 0) return false;

    if (role === 'ROLE_ADMIN') return true;

    if (role === 'ROLE_TECNICO') {
      const user = this.authService.user();
      // Technician can change status only if ticket is assigned to them
      if (!t.technician) return false;
      return t.technician.fullName === user?.fullName;
    }

    return false;
  });

  readonly canReassign = computed(() => {
    const t = this.ticket();
    const role = this.authService.role();
    if (!t || role !== 'ROLE_ADMIN') return false;
    return t.status !== 'CERRADO' && t.status !== 'CANCELADO';
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.router.navigate(['/tickets']);
      return;
    }
    this.loadTicketData(id);
  }

  loadTicketData(id: number): void {
    this.loading.set(true);
    this.problem.set(null);

    forkJoin({
      ticket: this.ticketService.get(id),
      history: this.ticketService.history(id),
      assignments: this.ticketService.assignments(id)
    }).subscribe({
      next: res => {
        this.ticket.set(res.ticket);
        this.history.set(res.history);
        this.assignments.set(res.assignments);
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
        this.loading.set(false);
      }
    });
  }

  openChangeStatusDialog(): void {
    const t = this.ticket();
    const role = this.authService.role();
    if (!t || !role) return;

    const dialogRef = this.dialog.open(StatusChangeDialogComponent, {
      data: { ticket: t, userRole: role },
      width: '480px'
    });

    dialogRef.afterClosed().subscribe((updated: TicketResponse | null) => {
      if (updated) {
        this.ticket.set(updated);
        this.snackBar.open(this.i18n.t().tickets.statusChangedSuccess, 'OK', { duration: 4000 });
        this.loadTicketData(t.id);
      }
    });
  }

  openReassignDialog(): void {
    const t = this.ticket();
    if (!t) return;

    const dialogRef = this.dialog.open(ReassignDialogComponent, {
      data: { ticket: t },
      width: '520px'
    });

    dialogRef.afterClosed().subscribe((updated: TicketResponse | null) => {
      if (updated) {
        this.ticket.set(updated);
        this.snackBar.open(this.i18n.t().tickets.reassignedSuccess, 'OK', { duration: 4000 });
        this.loadTicketData(t.id);
      }
    });
  }
}
