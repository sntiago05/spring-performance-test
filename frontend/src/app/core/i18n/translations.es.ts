import { TranslationDictionary } from './i18n.models';

export const TRANSLATIONS_ES: TranslationDictionary = {
  app: {
    title: 'HelpFlow',
    subtitle: 'Gestión de Soporte TI',
    loading: 'Cargando datos...',
    confirm: 'Confirmar',
    cancel: 'Cancelar',
    save: 'Guardar Cambios',
    close: 'Cerrar',
    back: 'Volver',
    actions: 'Acciones',
    search: 'Buscar',
    filter: 'Filtrar',
    reset: 'Restablecer',
    all: 'Todos',
    yes: 'Sí',
    no: 'No',
    status: 'Estado',
    role: 'Rol',
    active: 'Activo',
    inactive: 'Inactivo'
  },
  nav: {
    dashboard: 'Panel Principal',
    tickets: 'Tickets',
    newTicket: 'Nuevo Ticket',
    slaPolicies: 'Políticas SLA',
    users: 'Gestión de Usuarios',
    technicians: 'Técnicos',
    profile: 'Mi Perfil',
    logout: 'Cerrar Sesión',
    login: 'Iniciar Sesión',
    register: 'Registrarse'
  },
  enums: {
    categories: {
      HARDWARE: 'Hardware',
      SOFTWARE: 'Software',
      RED: 'Redes y Conectividad',
      ACCESOS: 'Accesos y Cuentas',
      OTRO: 'General / Otro'
    },
    priorities: {
      BAJA: 'Baja',
      MEDIA: 'Media',
      ALTA: 'Alta',
      CRITICA: 'Crítica'
    },
    statuses: {
      ABIERTO: 'Abierto',
      ASIGNADO: 'Asignado',
      EN_PROGRESO: 'En Progreso',
      RESUELTO: 'Resuelto',
      CERRADO: 'Cerrado',
      CANCELADO: 'Cancelado'
    },
    userStatuses: {
      ACTIVO: 'Activo',
      INACTIVO: 'Inactivo'
    },
    roles: {
      ROLE_USUARIO: 'Usuario Solicitante',
      ROLE_TECNICO: 'Técnico de Soporte',
      ROLE_ADMIN: 'Administrador'
    },
    sources: {
      DISPATCH: 'Servicio de Despacho',
      RULES: 'Reglas de Negocio',
      MANUAL: 'Decisión de Administrador'
    },
    actions: {
      CREATED: 'Ticket Creado',
      ASSIGNED: 'Técnico Asignado',
      REASSIGNED: 'Técnico Reasignado',
      ASSIGNMENT_PENDING: 'Asignación Pendiente',
      STATUS_CHANGED: 'Estado Cambiado',
      ESCALATED: 'Prioridad Escalada',
      SLA_BREACHED: 'SLA Incumplido'
    },
    availability: {
      ALTA: 'Alta (0-2h)',
      MEDIA: 'Media (3-8h)',
      BAJA: 'Baja (>8h)'
    }
  },
  validation: {
    required: 'Este campo es obligatorio',
    email: 'Debe ser una dirección de correo válida',
    corporateEmail: 'Debe ser un correo corporativo (no se admiten dominios públicos como gmail/hotmail)',
    minLength: 'Longitud mínima no alcanzada',
    maxLength: 'Longitud máxima superada',
    range: 'El valor está fuera del rango permitido',
    documentFormat: 'Debe contener entre 5 y 30 caracteres (letras, números o guiones)',
    cancelReasonMin: 'Cancelar un ticket requiere observaciones de al menos 15 caracteres'
  },
  auth: {
    loginTitle: 'Bienvenido a HelpFlow',
    loginSubtitle: 'Inicia sesión con tus credenciales corporativas',
    loginButton: 'Iniciar Sesión',
    registerTitle: 'Crear una Cuenta',
    registerSubtitle: 'Autorregistro para colaboradores de la empresa',
    registerButton: 'Registrarse',
    noAccount: '¿No tienes cuenta? Regístrate aquí',
    haveAccount: '¿Ya estás registrado? Inicia sesión aquí',
    registerSuccess: '¡Registro completado! Ya puedes iniciar sesión.',
    emailLabel: 'Correo Electrónico Corporativo',
    passwordLabel: 'Contraseña',
    documentLabel: 'Documento de Identidad',
    fullNameLabel: 'Nombre Completo',
    siteLabel: 'Sede / Oficina',
    areaLabel: 'Área / Departamento'
  },
  dashboard: {
    title: 'Panel de Control Operativo',
    subtitle: 'Visión en tiempo real de estados de tickets y cumplimiento de SLA',
    totalTickets: 'Tickets Visibles',
    activeTickets: 'Tickets Activos',
    overdueTickets: 'Tickets Vencidos',
    urgentTicketsTitle: 'Tickets Más Urgentes (Próximo Vencimiento SLA)',
    noUrgentTickets: 'No hay tickets urgentes pendientes. ¡Excelente trabajo!',
    slaCountdown: 'Estado SLA',
    overdueBy: 'Vencido hace',
    timeLeft: 'Tiempo restante',
    viewAllTickets: 'Ver Todos los Tickets',
    createTicketCta: 'Crear Nuevo Ticket',
    runReviewCta: 'Ejecutar Revisión SLA'
  },
  tickets: {
    listTitle: 'Registro de Tickets',
    createTitle: 'Crear Ticket de Soporte',
    detailTitle: 'Detalle del Ticket',
    id: 'ID',
    titleField: 'Título',
    descriptionField: 'Descripción',
    categoryField: 'Categoría',
    priorityField: 'Prioridad',
    siteField: 'Sede',
    assetField: 'Activo Afectado',
    statusField: 'Estado',
    requesterField: 'Solicitante',
    technicianField: 'Técnico',
    createdAtField: 'Fecha de Creación',
    slaDueField: 'Límite SLA',
    unassigned: 'Esperando asignación',
    overdueOnly: 'Mostrar solo vencidos',
    requesterFilter: 'ID Solicitante',
    technicianFilter: 'ID Técnico',
    changeStatus: 'Cambiar Estado',
    reassign: 'Asignar / Reasignar',
    historyTitle: 'Historial y Auditoría',
    assignmentsTitle: 'Registro de Asignaciones',
    noHistory: 'No hay registros en el historial todavía.',
    noAssignments: 'No hay asignaciones registradas todavía.',
    assignedToBadge: 'Asignado a',
    slaEscalatedBadge: 'Auto-Escalado',
    slaBreachedBadge: 'SLA Incumplido',
    timeRemaining: 'Tiempo restante',
    overdueWarning: 'Plazo límite superado',
    createdSuccess: '¡Ticket registrado con éxito!',
    statusChangedSuccess: 'Estado del ticket actualizado con éxito',
    reassignedSuccess: 'Ticket reasignado con éxito',
    changeStatusDialogTitle: 'Cambiar Estado del Ticket',
    targetStatusLabel: 'Nuevo Estado',
    observationsLabel: 'Observaciones',
    observationsHint: 'Mínimo 15 caracteres si se cancela el ticket',
    reassignDialogTitle: 'Asignar Técnico al Ticket',
    selectTechnicianLabel: 'Seleccionar Técnico Habilitado',
    estimatedHoursLabel: 'Horas Estimadas',
    withinSlaLabel: 'Cumple SLA'
  },
  sla: {
    title: 'Políticas de SLA y Reglas de Escalamiento',
    priorityCol: 'Prioridad',
    resolutionHoursCol: 'Ventana de Resolución',
    thresholdCol: 'Umbral de Escalamiento',
    escalateAtCol: 'Escala a las',
    editPolicy: 'Configurar Política',
    runReview: 'Ejecutar Revisión SLA Ahora',
    reviewSuccess: 'Revisión de SLA completada exitosamente',
    reviewModalTitle: 'Resumen de Ejecución SLA',
    reviewedCount: 'Tickets Inspeccionados',
    escalatedCount: 'Tickets Escalados',
    breachedCount: 'Tickets Marcados Incumplidos',
    failedCount: 'Fallos en Revisión'
  },
  users: {
    title: 'Gestión de Usuarios',
    activateConfirm: '¿Está seguro de que desea activar a este usuario?',
    deactivateConfirm: '¿Está seguro de que desea desactivar a este usuario? No podrá iniciar sesión ni registrar tickets.',
    cannotDeactivateSelf: 'No puede desactivar su propia cuenta de administrador',
    statusUpdated: 'Estado del usuario actualizado con éxito'
  },
  technicians: {
    title: 'Equipo de Técnicos',
    createTitle: 'Crear Cuenta y Perfil de Técnico',
    editTitle: 'Modificar Perfil de Técnico',
    codeCol: 'Código',
    nameCol: 'Nombre Completo',
    specialtyCol: 'Especialidad',
    siteCol: 'Sede',
    capacityCol: 'Capacidad Activa',
    statusCol: 'Estado',
    createdSuccess: 'Técnico creado con éxito',
    updatedSuccess: 'Perfil de técnico modificado con éxito'
  },
  profile: {
    title: 'Perfil de Usuario',
    accountDetails: 'Datos de la Cuenta',
    editDetails: 'Editar Información Personal',
    updatedSuccess: 'Perfil actualizado con éxito'
  },
  problem: {
    alertTitle: 'Ocurrió un error',
    traceId: 'ID de Rastreo (traceId)',
    copyTraceId: 'Copiar ID de Rastreo',
    traceIdCopied: 'ID de rastreo copiado al portapapeles'
  }
};
