import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TicketStatus } from '../../../core/models/ticket.models';
import { I18nService } from '../../../core/i18n/i18n.service';

const STATUS_CLASSES: Record<TicketStatus, string> = {
  ABIERTO: 'bg-sky-50 text-sky-700 border-sky-300 dark:bg-sky-950/70 dark:text-sky-300 dark:border-sky-800',
  ASIGNADO: 'bg-purple-50 text-purple-700 border-purple-300 dark:bg-purple-950/70 dark:text-purple-300 dark:border-purple-800',
  EN_PROGRESO: 'bg-amber-50 text-amber-700 border-amber-300 dark:bg-amber-950/70 dark:text-amber-300 dark:border-amber-800',
  RESUELTO: 'bg-emerald-50 text-emerald-700 border-emerald-300 dark:bg-emerald-950/70 dark:text-emerald-300 dark:border-emerald-800',
  CERRADO: 'bg-slate-100 text-slate-600 border-slate-300 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
  CANCELADO: 'bg-red-50 text-red-700 border-red-300 dark:bg-red-950/70 dark:text-red-300 dark:border-red-800',
};

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './status-badge.component.html'
})
export class StatusBadgeComponent {
  private readonly i18n = inject(I18nService);

  @Input({ required: true }) status!: TicketStatus;

  get label(): string {
    return this.i18n.t().enums.statuses[this.status] ?? this.status;
  }

  get statusClass(): string {
    return STATUS_CLASSES[this.status] ?? 'bg-slate-100 text-slate-700 border-slate-300 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700';
  }
}
