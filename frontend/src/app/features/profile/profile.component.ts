import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { UserResponse } from '../../core/models/user.models';
import { ProblemDetail } from '../../core/models/problem-detail.models';
import { ProblemAlertComponent } from '../../shared/components/problem-alert/problem-alert.component';
import { LoadingComponent } from '../../shared/components/loading/loading.component';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ProblemAlertComponent,
    LoadingComponent
  ],
  templateUrl: './profile.component.html'
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);
  private readonly snackBar = inject(MatSnackBar);
  readonly i18n = inject(I18nService);

  readonly user = signal<UserResponse | null>(null);
  readonly loading = signal(true);
  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);

  readonly form: FormGroup = this.fb.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    site: ['', [Validators.required, Validators.maxLength(60)]],
    area: ['', [Validators.required, Validators.maxLength(60)]]
  });

  ngOnInit(): void {
    this.fetchProfile();
  }

  fetchProfile(): void {
    this.loading.set(true);
    this.problem.set(null);

    this.userService.me().subscribe({
      next: res => {
        this.user.set(res);
        this.form.patchValue({
          fullName: res.fullName,
          site: res.site,
          area: res.area
        });
        this.loading.set(false);
      },
      error: (err: ProblemDetail) => {
        this.problem.set(err);
        this.loading.set(false);
      }
    });
  }

  onSubmit(): void {
    const currentUser = this.user();
    if (!currentUser || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.problem.set(null);

    this.userService.updateProfile(currentUser.id, this.form.value).subscribe({
      next: updated => {
        this.user.set(updated);
        this.authService.updateCurrentUser(updated);
        this.isSubmitting.set(false);
        this.snackBar.open(this.i18n.t().profile.updatedSuccess, 'OK', { duration: 4000 });
      },
      error: (err: ProblemDetail) => {
        this.isSubmitting.set(false);
        this.problem.set(err);
      }
    });
  }
}
