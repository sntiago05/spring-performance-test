import { Pipe, PipeTransform, inject } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';

@Pipe({
  name: 'localizedDate',
  standalone: true,
  pure: false // React to language change
})
export class LocalizedDatePipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(value: string | Date | null | undefined, format: 'short' | 'medium' | 'long' | 'full' = 'medium'): string {
    return this.i18n.formatDate(value, format);
  }
}
