import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { I18nService } from '../../../core/i18n/i18n.service';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule],
  templateUrl: './empty-state.component.html'
})
export class EmptyStateComponent {
  @Input() icon: string = 'inbox';
  readonly i18n = inject(I18nService);
  @Input() title?: string;
  @Input() message: string = '';
  @Input() actionText?: string;
  @Output() actionClicked = new EventEmitter<void>();
}
