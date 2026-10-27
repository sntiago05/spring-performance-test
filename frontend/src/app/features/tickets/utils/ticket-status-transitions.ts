import { TicketStatus } from '../../../core/models/ticket.models';
import { Role } from '../../../core/models/user.models';

/**
 * Returns allowed next ticket statuses for status change operations based on role.
 * Notice:
 * - ASIGNADO is reached only through assignment/reassignment, so it is never an option here.
 * - Technicians can never cancel tickets (CANCELADO is administrator-only).
 * - CERRADO and CANCELADO are terminal states.
 */
export function getValidNextStatuses(currentStatus: TicketStatus, role: Role | null): TicketStatus[] {
  if (!role || role === 'ROLE_USUARIO') {
    return [];
  }

  if (role === 'ROLE_ADMIN') {
    switch (currentStatus) {
      case 'ABIERTO':
        return ['CANCELADO'];
      case 'ASIGNADO':
        return ['EN_PROGRESO', 'CANCELADO'];
      case 'EN_PROGRESO':
        return ['RESUELTO', 'CANCELADO'];
      case 'RESUELTO':
        return ['EN_PROGRESO', 'CERRADO'];
      case 'CERRADO':
      case 'CANCELADO':
        return [];
    }
  }

  if (role === 'ROLE_TECNICO') {
    switch (currentStatus) {
      case 'ABIERTO':
        return [];
      case 'ASIGNADO':
        return ['EN_PROGRESO'];
      case 'EN_PROGRESO':
        return ['RESUELTO'];
      case 'RESUELTO':
        return ['EN_PROGRESO', 'CERRADO'];
      case 'CERRADO':
      case 'CANCELADO':
        return [];
    }
  }

  return [];
}

/**
 * Validates observation constraints.
 * Cancelling requires at least 15 characters.
 * Any other status change requires non-empty observations.
 */
export function validateStatusObservations(targetStatus: TicketStatus, observations: string | null | undefined): boolean {
  const trimmed = observations?.trim() ?? '';
  if (!trimmed) {
    return false;
  }
  if (targetStatus === 'CANCELADO') {
    return trimmed.length >= 15;
  }
  return trimmed.length >= 1 && trimmed.length <= 1000;
}
