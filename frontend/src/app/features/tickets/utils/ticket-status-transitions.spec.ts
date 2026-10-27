import { describe, it, expect } from 'vitest';
import { getValidNextStatuses, validateStatusObservations } from './ticket-status-transitions';

describe('ticketStatusTransitions', () => {
  describe('getValidNextStatuses', () => {
    it('should return admin transition paths correctly', () => {
      expect(getValidNextStatuses('ABIERTO', 'ROLE_ADMIN')).toEqual(['CANCELADO']);
      expect(getValidNextStatuses('ASIGNADO', 'ROLE_ADMIN')).toEqual(['EN_PROGRESO', 'CANCELADO']);
      expect(getValidNextStatuses('EN_PROGRESO', 'ROLE_ADMIN')).toEqual(['RESUELTO', 'CANCELADO']);
      expect(getValidNextStatuses('RESUELTO', 'ROLE_ADMIN')).toEqual(['EN_PROGRESO', 'CERRADO']);
      expect(getValidNextStatuses('CERRADO', 'ROLE_ADMIN')).toEqual([]);
      expect(getValidNextStatuses('CANCELADO', 'ROLE_ADMIN')).toEqual([]);
    });

    it('should return technician transition paths without CANCELADO', () => {
      expect(getValidNextStatuses('ABIERTO', 'ROLE_TECNICO')).toEqual([]);
      expect(getValidNextStatuses('ASIGNADO', 'ROLE_TECNICO')).toEqual(['EN_PROGRESO']);
      expect(getValidNextStatuses('EN_PROGRESO', 'ROLE_TECNICO')).toEqual(['RESUELTO']);
      expect(getValidNextStatuses('RESUELTO', 'ROLE_TECNICO')).toEqual(['EN_PROGRESO', 'CERRADO']);
      expect(getValidNextStatuses('CERRADO', 'ROLE_TECNICO')).toEqual([]);
      expect(getValidNextStatuses('CANCELADO', 'ROLE_TECNICO')).toEqual([]);
    });

    it('should return no transitions for regular requesters', () => {
      expect(getValidNextStatuses('ABIERTO', 'ROLE_USUARIO')).toEqual([]);
      expect(getValidNextStatuses('ASIGNADO', 'ROLE_USUARIO')).toEqual([]);
      expect(getValidNextStatuses('EN_PROGRESO', 'ROLE_USUARIO')).toEqual([]);
    });
  });

  describe('validateStatusObservations', () => {
    it('should require minimum 15 characters to cancel a ticket', () => {
      expect(validateStatusObservations('CANCELADO', 'short')).toBe(false);
      expect(validateStatusObservations('CANCELADO', '12345678901234')).toBe(false);
      expect(validateStatusObservations('CANCELADO', 'Cancellation reason with more than 15 chars')).toBe(true);
    });

    it('should require non-empty observations for other transitions', () => {
      expect(validateStatusObservations('EN_PROGRESO', '')).toBe(false);
      expect(validateStatusObservations('EN_PROGRESO', '   ')).toBe(false);
      expect(validateStatusObservations('EN_PROGRESO', 'Started diagnosis')).toBe(true);
    });
  });
});
