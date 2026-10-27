import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { I18nService } from '../../../core/i18n/i18n.service';
import { ProblemDetail } from '../../../core/models/problem-detail.models';
import { ProblemAlertComponent } from '../../../shared/components/problem-alert/problem-alert.component';

export function corporateEmailValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const val = String(control.value).toLowerCase().trim();
  const forbiddenDomains = ['@gmail.com', '@hotmail.com', '@yahoo.com', '@outlook.com', '@live.com', '@icloud.com'];
  if (forbiddenDomains.some(d => val.endsWith(d))) {
    return { corporateEmail: true };
  }
  return null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    ProblemAlertComponent
  ],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  readonly i18n = inject(I18nService);

  readonly registerForm: FormGroup = this.fb.group({
    document: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]{5,30}$/)]],
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, corporateEmailValidator]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    site: ['', [Validators.required, Validators.maxLength(60)]],
    area: ['', [Validators.required, Validators.maxLength(60)]]
  });

  readonly isSubmitting = signal(false);
  readonly problem = signal<ProblemDetail | null>(null);
  readonly hidePassword = signal(true);

  onSubmit(): void {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.problem.set(null);

    this.authService.register(this.registerForm.value).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.snackBar.open(this.i18n.t().auth.registerSuccess, 'OK', {
          duration: 5000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom'
        });
        this.router.navigate(['/login']);
      },
      error: (err: ProblemDetail) => {
        this.isSubmitting.set(false);
        this.problem.set(err);

        if (err.errors && Array.isArray(err.errors)) {
          for (const fe of err.errors) {
            const control = this.registerForm.get(fe.field);
            if (control) {
              control.setErrors({ serverError: fe.message });
            }
          }
        }
      }
    });
  }
}
