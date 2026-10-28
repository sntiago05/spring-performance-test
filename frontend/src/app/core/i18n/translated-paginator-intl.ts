import { Injectable, effect, inject } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { I18nService } from './i18n.service';

/**
 * Material ships the paginator labels ("Items per page", "1 - 10 of 17") in English only; this feeds them from the
 * app dictionary and refreshes them when the language changes.
 */
@Injectable()
export class TranslatedPaginatorIntl extends MatPaginatorIntl {
  private readonly i18n = inject(I18nService);

  constructor() {
    super();
    effect(() => {
      const ui = this.i18n.t().ui;
      this.itemsPerPageLabel = ui.itemsPerPage;
      this.nextPageLabel = ui.nextPage;
      this.previousPageLabel = ui.previousPage;
      this.firstPageLabel = ui.firstPage;
      this.lastPageLabel = ui.lastPage;
      this.changes.next();
    });
  }

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    const of = this.i18n.t().ui.of;
    if (length === 0 || pageSize === 0) {
      return `0 ${of} ${length}`;
    }
    const start = page * pageSize;
    const end = Math.min(start + pageSize, length);
    return `${start + 1} – ${end} ${of} ${length}`;
  };
}
