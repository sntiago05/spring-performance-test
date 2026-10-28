import { Injectable, computed, effect, signal } from '@angular/core';
import { SupportedLanguage, TranslationDictionary } from './i18n.models';
import { TRANSLATIONS_EN } from './translations.en';
import { TRANSLATIONS_ES } from './translations.es';

@Injectable({
  providedIn: 'root'
})
export class I18nService {
  private readonly STORAGE_KEY = 'helpflow_lang';
  private readonly dictionaries: Record<SupportedLanguage, TranslationDictionary> = {
    en: TRANSLATIONS_EN,
    es: TRANSLATIONS_ES
  };

  readonly currentLang = signal<SupportedLanguage>(this.getInitialLanguage());
  readonly t = computed<TranslationDictionary>(() => this.dictionaries[this.currentLang()]);

  constructor() {
    effect(() => {
      document.documentElement.lang = this.currentLang();
    });
  }

  private getInitialLanguage(): SupportedLanguage {
    try {
      const stored = sessionStorage.getItem(this.STORAGE_KEY);
      if (stored === 'en' || stored === 'es') {
        return stored;
      }
      const navLang = navigator.language?.toLowerCase() ?? '';
      if (navLang.startsWith('es')) {
        return 'es';
      }
    } catch {
      // Fallback in environments without window/sessionStorage
    }
    return 'en';
  }

  setLanguage(lang: SupportedLanguage): void {
    this.currentLang.set(lang);
    try {
      sessionStorage.setItem(this.STORAGE_KEY, lang);
    } catch {
      // Ignore storage errors
    }
  }

  formatDate(value: string | Date | null | undefined, format: 'short' | 'medium' | 'long' | 'full' = 'medium'): string {
    if (!value) return '';
    const date = typeof value === 'string' ? new Date(value) : value;
    if (isNaN(date.getTime())) return '';

    const locale = this.currentLang() === 'es' ? 'es-CO' : 'en-US';
    const optionsMap: Record<string, Intl.DateTimeFormatOptions> = {
      short: { dateStyle: 'short', timeStyle: 'short' },
      medium: { dateStyle: 'medium', timeStyle: 'short' },
      long: { dateStyle: 'long', timeStyle: 'medium' },
      full: { dateStyle: 'full', timeStyle: 'long' }
    };

    return new Intl.DateTimeFormat(locale, optionsMap[format]).format(date);
  }
}
