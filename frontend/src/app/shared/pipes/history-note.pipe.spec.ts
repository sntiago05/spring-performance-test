import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { I18nService } from '../../core/i18n/i18n.service';
import { HistoryNotePipe } from './history-note.pipe';

describe('HistoryNotePipe', () => {
  let pipe: HistoryNotePipe;
  let i18n: I18nService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [I18nService, HistoryNotePipe] });
    i18n = TestBed.inject(I18nService);
    pipe = TestBed.inject(HistoryNotePipe);
  });

  it('leaves the note untouched in English', () => {
    i18n.setLanguage('en');
    expect(pipe.transform('Assigned to TEC-008 (RULES, estimated 2h)')).toBe('Assigned to TEC-008 (RULES, estimated 2h)');
  });

  it('translates an assignment with the dispatch estimate', () => {
    i18n.setLanguage('es');
    expect(pipe.transform('Assigned to TEC-008 (DISPATCH, estimated 2h)')).toContain('Asignado a TEC-008');
    expect(pipe.transform('Assigned to TEC-008 (DISPATCH, estimated 2h)')).toContain('estimado 2 h');
  });

  it('translates the dispatch rejection appended to an assignment', () => {
    i18n.setLanguage('es');
    const text = pipe.transform('Assigned to TEC-022 (RULES). Dispatch suggestion TEC-015 rejected by assignment rules');
    expect(text).toContain('Asignado a TEC-022');
    expect(text).toContain('Sugerencia del dispatch TEC-015 rechazada');
  });

  it('translates the creation note and the automatic escalation', () => {
    i18n.setLanguage('es');
    expect(pipe.transform('Ticket registered with SLA deadline 2026-10-28T08:34:23.798667Z')).toMatch(/^Ticket registrado con plazo SLA /);
    expect(pipe.transform('Auto-escalated at 80% of the SLA window (priority MEDIA -> ALTA)')).toContain('Escalado automáticamente al 80 %');
  });

  it('does not touch text typed by people or unknown notes', () => {
    i18n.setLanguage('es');
    expect(pipe.transform('Reiniciado el router')).toBe('Reiniciado el router');
    expect(pipe.transform(null)).toBe('');
  });
});
