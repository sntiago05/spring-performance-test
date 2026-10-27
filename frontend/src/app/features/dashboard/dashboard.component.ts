import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { forkJoin } from 'rxjs';
import { TicketService } from '../../core/services/ticket.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TicketResponse, TicketStatus } from '../../core/models/ticket.models';
import { ProblemDetail } from '../../core/models/problem-detail.models';
import { StatusBadgeComponent } from '../../shared/components/status-badge/status-badge.component';
import { PriorityBadgeComponent } from '../../shared/components/priority-badge/priority-badge.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ProblemAlertComponent } from '../../shared/components/problem-alert/problem-alert.component';
import { SlaCountdownPipe } from '../../shared/pipes/sla-countdown.pipe';
import { LocalizedDatePipe } from '../../shared/pipes/localized-date.pipe';
import { EscalationSummaryDialogComponent } from '../../shared/components/escalation-summary-dialog/escalation-summary-dialog.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    StatusBadgeComponent,
    PriorityBadgeComponent,
    LoadingComponent,
    EmptyStateComponent,
    ProblemAlertComponent,
    SlaCountdownPipe,
    LocalizedDatePipe
  ],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  private readonly ticketService = inject(TicketService);
  readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly loading = signal(true);
  readonly problem = signal<ProblemDetail | null>(null);
  readonly overdueCount = signal(0);
  readonly urgentTickets = signal<TicketResponse[]>([]);

  readonly statusCounts = signal<Record<TicketStatus, number>>({
    ABIERTO: 0,
    ASIGNADO: 0,
    EN_PROGRESO: 0,
    RESUELTO: 0,
    CERRADO: 0,
    CANCELADO: 0
  });

  readonly activeTicketsCount = computed(() => {
    const s = this.statusCounts();
    return s.ABIERTO + s.ASIGNADO + s.EN_PROGRESO;
  });

  readonly totalVisibleCount = computed(() => {
    const s = this.statusCounts();
    return s.ABIERTO + s.ASIGNADO + s.EN_PROGRESO + s.RESUELTO + s.CERRADO + s.CANCELADO;
  });

  readonly statusesList: TicketStatus[] = [
    'ABIERTO',
    'ASIGNADO',
    'EN_PROGRESO',
    'RESUELTO',
    'CERRADO',
    'CANCELADO'
  ];

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading.set(true);
    this.problem.set(null);

    forkJoin({
      abierto: this.ticketService.list({ status: 'ABIERTO', size: 1 }),
      asignado: this.ticketService.list({ status: 'ASIGNADO', size: 1 }),
      enProgreso: this.ticketService.list({ status: 'EN_PROGRESO', size: 1 }),
      resuelto: this.ticketService.list({ status: 'RESUELTO', size: 1 }),
      cerrado: this.ticketService.list({ status: 'CERRADO', size: 1 }),
      cancelado: this.ticketService.list({ status: 'CANCELADO', size: 1 }),
      overdue: this.ticketService.list({ overdue: true, size: 1 }),
      urgent: this.ticketService.list({ sort: 'slaDueAt,asc', size: 10 })
    }).subscribe({
      next: res => {
        this.statusCounts.set({
          ABIERTO: res.abierto.totalElements,
          ASIGNADO: res.asignado.totalElements,
          EN_PROGRESO: res.enProgreso.totalElements,
          RESUELTO: res.resuelto.totalElements,
          CERRADO: res.cerrado.totalElements,
          CANCELADO: res.cancelado.totalElements
        });
        this.overdueCount.set(res.overdue.totalElements);

        // Filter urgent tickets that are still open / in SLA
        const openUrgent = res.urgent.content
          .filter(t => t.status === 'ABIERTO' || t.status === 'ASIGNADO' || t.status === 'EN_PROGRESO')
          .slice(0, 5);

        this.urgentTickets.set(openUrgent);
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
        this.loading.set(false);
      }
    });
  }

  filterByStatus(status: TicketStatus): void {
    this.router.navigate(['/tickets'], { queryParams: { status } });
  }

  filterByOverdue(): void {
    this.router.navigate(['/tickets'], { queryParams: { overdue: true } });
  }

  openTicket(ticket: TicketResponse): void {
    this.router.navigate(['/tickets', ticket.id]);
  }

  runSlaReview(): void {
    this.ticketService.runEscalations().subscribe({
      next: summary => {
        this.dialog.open(EscalationSummaryDialogComponent, {
          data: summary,
          width: '450px'
        });
        this.loadDashboardData();
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
      }
    });
  }
}
