import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { UserService, UserSearchFilters } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { Role, UserResponse, UserStatus } from '../../core/models/user.models';
import { ProblemDetail } from '../../core/models/problem-detail.models';
import { ConfirmDialogComponent } from '../../shared/components/confirm-dialog/confirm-dialog.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';
import { EmptyStateComponent } from '../../shared/components/empty-state/empty-state.component';
import { ProblemAlertComponent } from '../../shared/components/problem-alert/problem-alert.component';

@Component({
  selector: 'app-user-list',
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
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    LoadingComponent,
    EmptyStateComponent,
    ProblemAlertComponent
  ],
  templateUrl: './user-list.component.html'
})
export class UserListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  readonly authService = inject(AuthService);
  readonly i18n = inject(I18nService);

  readonly filterForm: FormGroup = this.fb.group({
    status: [''],
    role: [''],
    site: ['']
  });

  readonly users = signal<UserResponse[]>([]);
  readonly totalElements = signal(0);
  readonly pageIndex = signal(0);
  readonly pageSize = signal(10);
  readonly sortField = signal('id');
  readonly sortDirection = signal<'asc' | 'desc'>('asc');
  readonly loading = signal(true);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly displayedColumns: string[] = [
    'id',
    'document',
    'fullName',
    'email',
    'site',
    'area',
    'role',
    'status',
    'actions'
  ];

  readonly rolesList: Role[] = ['ROLE_USUARIO', 'ROLE_TECNICO', 'ROLE_ADMIN'];
  readonly statusesList: UserStatus[] = ['ACTIVO', 'INACTIVO'];

  ngOnInit(): void {
    this.fetchUsers();
  }

  fetchUsers(): void {
    this.loading.set(true);
    this.problem.set(null);

    const raw = this.filterForm.value;
    const filters: UserSearchFilters = {
      status: raw.status || undefined,
      role: raw.role || undefined,
      site: raw.site || undefined,
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: `${this.sortField()},${this.sortDirection()}`
    };

    this.userService.list(filters).subscribe({
      next: page => {
        this.users.set(page.content);
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
    this.fetchUsers();
  }

  resetFilters(): void {
    this.filterForm.reset({ status: '', role: '', site: '' });
    this.pageIndex.set(0);
    this.fetchUsers();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.fetchUsers();
  }

  onSortChange(sort: Sort): void {
    this.sortField.set(sort.active || 'id');
    this.sortDirection.set((sort.direction as 'asc' | 'desc') || 'asc');
    this.pageIndex.set(0);
    this.fetchUsers();
  }

  toggleUserStatus(user: UserResponse): void {
    const isDeactivating = user.status === 'ACTIVO';
    const newStatus: UserStatus = isDeactivating ? 'INACTIVO' : 'ACTIVO';
    const confirmMsg = isDeactivating
      ? this.i18n.t().users.deactivateConfirm
      : this.i18n.t().users.activateConfirm;

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: `${isDeactivating ? this.i18n.t().ui.deactivateUserTitle : this.i18n.t().ui.activateUserTitle}: ${user.fullName}`,
        message: confirmMsg,
        confirmText: isDeactivating ? this.i18n.t().ui.deactivate : this.i18n.t().ui.activate,
        isDestructive: isDeactivating
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.userService.changeStatus(user.id, { status: newStatus }).subscribe({
          next: () => {
            this.snackBar.open(this.i18n.t().users.statusUpdated, 'OK', { duration: 4000 });
            this.fetchUsers();
          },
          error: (err: ProblemDetail) => {
            this.problem.set(err);
          }
        });
      }
    });
  }

  isCurrentUser(user: UserResponse): boolean {
    return user.id === this.authService.user()?.id;
  }
}
