import { TestBed } from '@angular/core/testing';
import { I18nService } from './i18n.service';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('I18nService', () => {
  let service: I18nService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [I18nService]
    });
    service = TestBed.inject(I18nService);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should default to English language when no storage present', () => {
    expect(service.currentLang()).toBe('en');
    expect(service.t().app.title).toBe('HelpFlow');
    expect(service.t().enums.statuses['ABIERTO']).toBe('Open');
  });

  it('should switch to Spanish and update translation dictionary', () => {
    service.setLanguage('es');
    expect(service.currentLang()).toBe('es');
    expect(service.t().enums.statuses['ABIERTO']).toBe('Abierto');
    expect(sessionStorage.getItem('helpflow_lang')).toBe('es');
  });

  it('should format dates based on active language', () => {
    const testDate = new Date('2026-10-27T15:30:00Z');
    service.setLanguage('en');
    const formattedEn = service.formatDate(testDate);
    expect(formattedEn).toBeDefined();
    expect(formattedEn.length).toBeGreaterThan(0);

    service.setLanguage('es');
    const formattedEs = service.formatDate(testDate);
    expect(formattedEs).toBeDefined();
    expect(formattedEs.length).toBeGreaterThan(0);
  });
});
