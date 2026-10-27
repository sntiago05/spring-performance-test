import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule],
  templateUrl: './loading.component.html'
})
export class LoadingComponent {
  private readonly i18n = inject(I18nService);

  @Input() message?: string;
  @Input() diameter: number = 44;

  get defaultMessage(): string {
    return this.message || this.i18n.t().app.loading;
  }
}
