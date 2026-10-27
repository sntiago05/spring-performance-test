import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TicketService } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { Category, Priority, TicketResponse, TicketSearchFilters, TicketStatus } from '../../../core/models/ticket.models';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { StatusBadgeComponent } from '../../../shared/components/status-badge/status-badge.component';
import { PriorityBadgeComponent } from '../../../shared/components/priority-badge/priority-badge.component';
import { LoadingComponent } from '../../../shared/components/loading/loading.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ProblemAlertComponent } from '../../../shared/components/problem-alert/problem-alert.component';
import { LocalizedDatePipe } from '../../../shared/pipes/localized-date.pipe';
import { SlaCountdownPipe } from '../../../shared/pipes/sla-countdown.pipe';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatCheckboxModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    StatusBadgeComponent,
    PriorityBadgeComponent,
    LoadingComponent,
    EmptyStateComponent,
    ProblemAlertComponent,
    LocalizedDatePipe,
    SlaCountdownPipe
  ],
  templateUrl: './ticket-list.component.html'
})
export class TicketListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly ticketService = inject(TicketService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly filterForm: FormGroup = this.fb.group({
    status: [''],
    priority: [''],
    category: [''],
    overdue: [false],
    requesterId: [null],
    technicianId: [null]
  });

  readonly tickets = signal<TicketResponse[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sortField = signal('createdAt');
  readonly sortDirection = signal<'asc' | 'desc'>('desc');
  readonly loading = signal(true);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly displayedColumns: string[] = [
    'id',
    'title',
    'category',
    'priority',
    'status',
    'requester',
    'technician',
    'slaDueAt',
    'createdAt'
  ];

  readonly categoriesList: Category[] = ['HARDWARE', 'SOFTWARE', 'RED', 'ACCESOS', 'OTRO'];
  readonly prioritiesList: Priority[] = ['BAJA', 'MEDIA', 'ALTA', 'CRITICA'];
  readonly statusesList: TicketStatus[] = [
    'ABIERTO',
    'ASIGNADO',
    'EN_PROGRESO',
    'RESUELTO',
    'CERRADO',
    'CANCELADO'
  ];

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.syncFromQueryParams(params);
      this.fetchTickets();
    });
  }

  private syncFromQueryParams(params: Record<string, any>): void {
    this.filterForm.patchValue({
      status: params['status'] || '',
      priority: params['priority'] || '',
      category: params['category'] || '',
      overdue: params['overdue'] === 'true' || params['overdue'] === true,
      requesterId: params['requesterId'] ? Number(params['requesterId']) : null,
      technicianId: params['technicianId'] ? Number(params['technicianId']) : null
    }, { emitEvent: false });

    this.pageIndex.set(params['page'] ? Number(params['page']) : 0);
    this.pageSize.set(params['size'] ? Number(params['size']) : 10);

    if (params['sort']) {
      const [field, direction] = params['sort'].split(',');
      this.sortField.set(field || 'createdAt');
      this.sortDirection.set((direction as 'asc' | 'desc') || 'desc');
    }
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.updateUrlParams();
  }

  resetFilters(): void {
    this.filterForm.reset({
      status: '',
      priority: '',
      category: '',
      overdue: false,
      requesterId: null,
      technicianId: null
    });
    this.pageIndex.set(0);
    this.updateUrlParams();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.updateUrlParams();
  }

  onSortChange(sort: Sort): void {
    this.sortField.set(sort.active || 'createdAt');
    this.sortDirection.set((sort.direction as 'asc' | 'desc') || 'desc');
    this.pageIndex.set(0);
    this.updateUrlParams();
  }

  private updateUrlParams(): void {
    const raw = this.filterForm.value;
    const queryParams: Record<string, any> = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: `${this.sortField()},${this.sortDirection()}`
    };

    if (raw.status) queryParams['status'] = raw.status;
    if (raw.priority) queryParams['priority'] = raw.priority;
    if (raw.category) queryParams['category'] = raw.category;
    if (raw.overdue) queryParams['overdue'] = 'true';
    if (raw.requesterId) queryParams['requesterId'] = raw.requesterId;
    if (raw.technicianId) queryParams['technicianId'] = raw.technicianId;

    this.router.navigate([], {
      relativeTo: this.route,
      queryParams,
      queryParamsHandling: ''
    });
  }

  fetchTickets(): void {
    this.loading.set(true);
    this.problem.set(null);

    const raw = this.filterForm.value;
    const filters: TicketSearchFilters = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: `${this.sortField()},${this.sortDirection()}`,
      status: raw.status || undefined,
      priority: raw.priority || undefined,
      category: raw.category || undefined,
      overdue: raw.overdue ? true : undefined,
      requesterId: raw.requesterId || undefined,
      technicianId: raw.technicianId || undefined
    };

    this.ticketService.list(filters).subscribe({
      next: page => {
        this.tickets.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
        this.loading.set(false);
      }
    });
  }

  onRowClick(ticket: TicketResponse): void {
    this.router.navigate(['/tickets', ticket.id]);
  }
}
