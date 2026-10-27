import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Priority } from '../../../core/models/ticket.models';
import { I18nService } from '../../../core/i18n/i18n.service';

const PRIORITY_CLASSES: Record<Priority, string> = {
  BAJA: 'bg-emerald-50 text-emerald-700 border-emerald-300 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-800',
  MEDIA: 'bg-sky-50 text-sky-700 border-sky-300 dark:bg-sky-950/70 dark:text-sky-300 dark:border-sky-800',
  ALTA: 'bg-orange-50 text-orange-700 border-orange-300 dark:bg-orange-950/70 dark:text-orange-300 dark:border-orange-800',
  CRITICA: 'bg-red-50 text-red-700 border-red-300 dark:bg-red-950/70 dark:text-red-300 dark:border-red-800 font-bold',
};

@Component({
  selector: 'app-priority-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './priority-badge.component.html'
})
export class PriorityBadgeComponent {
  private readonly i18n = inject(I18nService);

  @Input({ required: true }) priority!: Priority;

  get label(): string {
    return this.i18n.t().enums.priorities[this.priority] ?? this.priority;
  }

  get priorityClass(): string {
    return PRIORITY_CLASSES[this.priority] ?? 'bg-slate-100 text-slate-700 border-slate-300 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700';
  }
}
