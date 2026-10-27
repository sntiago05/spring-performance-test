import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';

export interface SlaCountdownResult {
  text: string;
  isOverdue: boolean;
}

@Pipe({
  name: 'slaCountdown',
  standalone: true,
  pure: false
})
export class SlaCountdownPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(slaDueAt: string | Date | null | undefined, isResolved: boolean = false): SlaCountdownResult {
    if (!slaDueAt || isResolved) {
      return { text: '—', isOverdue: false };
    }

    const dueDate = typeof slaDueAt === 'string' ? new Date(slaDueAt) : slaDueAt;
    if (isNaN(dueDate.getTime())) {
      return { text: '—', isOverdue: false };
    }

    const diffMs = dueDate.getTime() - Date.now();
    const isOverdue = diffMs < 0;
    const absDiffMs = Math.abs(diffMs);

    const hours = Math.floor(absDiffMs / (1000 * 60 * 60));
    const minutes = Math.floor((absDiffMs % (1000 * 60 * 60)) / (1000 * 60));

    const t = this.i18n.t();
    const timeFormatted = hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`;

    if (isOverdue) {
      return {
        text: `${t.dashboard.overdueBy} ${timeFormatted}`,
        isOverdue: true
      };
    }

    return {
      text: `${timeFormatted} ${t.dashboard.timeLeft}`,
      isOverdue: false
    };
  }
}
