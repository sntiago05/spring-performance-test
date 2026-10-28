import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';

/**
 * Audit notes written by the system (ticket creation, automatic assignment, SLA review) are stored by the backend in
 * English. This pipe shows them in the current language by recognising their fixed patterns; notes typed by people
 * (status-change observations, reassignment reasons) and unknown texts are shown unchanged.
 */
@Pipe({
  name: 'historyNote',
  standalone: true,
  pure: false // React to language change
})
export class HistoryNotePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(note: string | null | undefined): string {
    if (!note || this.i18n.currentLang() === 'en') {
      return note ?? '';
    }
    const ui = this.i18n.t().ui;
    const fill = (template: string, values: Record<string, string>) =>
      template.replace(/\{(\w+)\}/g, (_, key: string) => values[key] ?? '');
    const date = (iso: string) => this.i18n.formatDate(iso, 'medium');
    const priority = (value: string) => this.i18n.t().enums.priorities[value] ?? value;
    const source = (value: string) => this.i18n.t().enums.sources[value] ?? value;

    let match = /^Ticket registered with SLA deadline (\S+)$/.exec(note);
    if (match) {
      return fill(ui.noteRegistered, { date: date(match[1]) });
    }
    match = /^Assigned to (TEC-\d+) \((\w+)(?:, estimated (\d+)h)?\)(?:\. (.+))?$/.exec(note);
    if (match) {
      const estimate = match[3] ? fill(ui.noteEstimated, { hours: match[3] }) : '';
      const base = fill(ui.noteAssigned, { tech: match[1], source: source(match[2]), estimate });
      return match[4] ? `${base}. ${this.translateExtra(match[4])}` : base;
    }
    match = /^Auto-escalated at (\d+)% of the SLA window \(priority (\w+) -> (\w+)\)$/.exec(note);
    if (match) {
      return fill(ui.noteAutoEscalated, { pct: match[1], from: priority(match[2]), to: priority(match[3]) });
    }
    match = /^SLA deadline (\S+) exceeded$/.exec(note);
    if (match) {
      return fill(ui.noteSlaExceeded, { date: date(match[1]) });
    }
    match = /^Resolved after the SLA deadline (\S+)$/.exec(note);
    if (match) {
      return fill(ui.noteResolvedLate, { date: date(match[1]) });
    }
    if (note === 'No eligible technician available; waiting for a manual assignment') {
      return ui.noteNoEligible;
    }
    return this.translateExtra(note);
  }

  private translateExtra(text: string): string {
    const ui = this.i18n.t().ui;
    if (text === 'Dispatch suggestion unavailable') {
      return ui.noteDispatchUnavailable;
    }
    const rejected = /^Dispatch suggestion (TEC-\d+) rejected by assignment rules$/.exec(text);
    return rejected ? ui.noteDispatchRejected.replace('{tech}', rejected[1]) : text;
  }
}
