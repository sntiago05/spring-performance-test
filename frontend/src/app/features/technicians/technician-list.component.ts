import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TechnicianService, TechnicianSearchFilters } from '../../core/services/technician.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { TechnicianResponse } from '../../core/models/technician.models';
import { Category } from '../../core/models/ticket.models';
import { UserStatus } from '../../core/models/user.models';
import { ProblemDetail } from '../../core/models/problem-detail.models';
import { TechnicianCreateDialogComponent } from './technician-create-dialog/technician-create-dialog.component';
import { TechnicianEditDialogComponent } from './technician-edit-dialog/technician-edit-dialog.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ProblemAlertComponent } from '../../shared/components/problem-alert/problem-alert.component';

@Component({
  selector: 'app-technician-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    LoadingComponent,
    EmptyStateComponent,
    ProblemAlertComponent
  ],
  templateUrl: './technician-list.component.html'
})
export class TechnicianListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly technicianService = inject(TechnicianService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly filterForm: FormGroup = this.fb.group({
    status: [''],
    specialty: ['']
  });

  readonly technicians = signal<TechnicianResponse[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sortField = signal('code');
  readonly sortDirection = signal<'asc' | 'desc'>('asc');
  readonly loading = signal(true);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly displayedColumns: string[] = [
    'code',
    'fullName',
    'specialty',
    'site',
    'maxActiveTickets',
    'status',
    'actions'
  ];

  readonly specialtiesList: Category[] = ['HARDWARE', 'SOFTWARE', 'RED', 'ACCESOS', 'OTRO'];
  readonly statusesList: UserStatus[] = ['ACTIVO', 'INACTIVO'];

  ngOnInit(): void {
    this.fetchTechnicians();
  }

  fetchTechnicians(): void {
    this.loading.set(true);
    this.problem.set(null);

    const raw = this.filterForm.value;
    const filters: TechnicianSearchFilters = {
      status: raw.status || undefined,
      specialty: raw.specialty || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: `${this.sortField()},${this.sortDirection()}`
    };

    this.technicianService.list(filters).subscribe({
      next: page => {
        this.technicians.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    this.pageIndex.set(0);
    this.fetchTechnicians();
  }

  resetFilters(): void {
    this.filterForm.reset({ status: '', specialty: '' });
    this.pageIndex.set(0);
    this.fetchTechnicians();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.fetchTechnicians();
  }

  onSortChange(sort: Sort): void {
    this.sortField.set(sort.active || 'code');
    this.sortDirection.set((sort.direction as 'asc' | 'desc') || 'asc');
    this.pageIndex.set(0);
    this.fetchTechnicians();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(TechnicianCreateDialogComponent, {
      width: '580px'
    });

    dialogRef.afterClosed().subscribe((created: TechnicianResponse | null) => {
      if (created) {
        this.snackBar.open(this.i18n.t().technicians.createdSuccess, 'OK', { duration: 4000 });
        this.fetchTechnicians();
      }
    });
  }

  openEditDialog(technician: TechnicianResponse): void {
    const dialogRef = this.dialog.open(TechnicianEditDialogComponent, {
      data: { technician },
      width: '500px'
    });

    dialogRef.afterClosed().subscribe((updated: TechnicianResponse | null) => {
      if (updated) {
        this.snackBar.open(this.i18n.t().technicians.updatedSuccess, 'OK', { duration: 4000 });
        this.fetchTechnicians();
      }
    });
  }
}
