import { TranslationDictionary } from './i18n.models';

export const TRANSLATIONS_EN: TranslationDictionary = {
  app: {
    title: 'HelpFlow',
    subtitle: 'IT Support Management',
    loading: 'Loading data...',
    confirm: 'Confirm',
    cancel: 'Cancel',
    save: 'Save Changes',
    close: 'Close',
    back: 'Back',
    actions: 'Actions',
    search: 'Search',
    filter: 'Filter',
    reset: 'Reset Filters',
    all: 'All',
    yes: 'Yes',
    no: 'No',
    status: 'Status',
    role: 'Role',
    active: 'Active',
    inactive: 'Inactive'
  },
  nav: {
    dashboard: 'Dashboard',
    tickets: 'Tickets',
    newTicket: 'New Ticket',
    slaPolicies: 'SLA Policies',
    users: 'Users Management',
    technicians: 'Technicians',
    profile: 'My Profile',
    logout: 'Log Out',
    login: 'Log In',
    register: 'Register'
  },
  enums: {
    categories: {
      HARDWARE: 'Hardware',
      SOFTWARE: 'Software',
      RED: 'Network',
      ACCESOS: 'Access & Accounts',
      OTRO: 'General / Other'
    },
    priorities: {
      BAJA: 'Low',
      MEDIA: 'Medium',
      ALTA: 'High',
      CRITICA: 'Critical'
    },
    statuses: {
      ABIERTO: 'Open',
      ASIGNADO: 'Assigned',
      EN_PROGRESO: 'In Progress',
      RESUELTO: 'Resolved',
      CERRADO: 'Closed',
      CANCELADO: 'Cancelled'
    },
    userStatuses: {
      ACTIVO: 'Active',
      INACTIVO: 'Inactive'
    },
    roles: {
      ROLE_USUARIO: 'Requester',
      ROLE_TECNICO: 'Technician',
      ROLE_ADMIN: 'Administrator'
    },
    sources: {
      DISPATCH: 'Dispatch Service',
      RULES: 'Business Rules',
      MANUAL: 'Admin Decision'
    },
    actions: {
      CREATED: 'Ticket Created',
      ASSIGNED: 'Technician Assigned',
      REASSIGNED: 'Technician Reassigned',
      ASSIGNMENT_PENDING: 'Assignment Pending',
      STATUS_CHANGED: 'Status Changed',
      ESCALATED: 'Priority Escalated',
      SLA_BREACHED: 'SLA Breached'
    },
    availability: {
      ALTA: 'High (0-2h)',
      MEDIA: 'Medium (3-8h)',
      BAJA: 'Low (>8h)'
    }
  },
  validation: {
    required: 'This field is required',
    email: 'Must be a valid email address',
    corporateEmail: 'Must be a corporate email (public domains like gmail/hotmail are not allowed)',
    minLength: 'Minimum length not reached',
    maxLength: 'Maximum length exceeded',
    range: 'Value is out of allowed range',
    documentFormat: 'Must be 5 to 30 characters (letters, numbers, hyphens)',
    cancelReasonMin: 'Cancellation requires at least 15 characters of observations'
  },
  auth: {
    loginTitle: 'Welcome to HelpFlow',
    loginSubtitle: 'Log in with your corporate credentials',
    loginButton: 'Log In',
    registerTitle: 'Create an Account',
    registerSubtitle: 'Self-registration for corporate employees',
    registerButton: 'Register Account',
    noAccount: "Don't have an account? Self-register here",
    haveAccount: 'Already registered? Log in here',
    registerSuccess: 'Registration successful! You can now log in.',
    emailLabel: 'Corporate Email',
    passwordLabel: 'Password',
    documentLabel: 'National Document / ID',
    fullNameLabel: 'Full Name',
    siteLabel: 'Corporate Site / Office',
    areaLabel: 'Area / Department'
  },
  dashboard: {
    title: 'Operations Dashboard',
    subtitle: 'Real-time overview of ticket statuses and SLA metrics',
    totalTickets: 'Visible Tickets',
    activeTickets: 'Active Tickets',
    overdueTickets: 'Overdue Tickets',
    urgentTicketsTitle: 'Most Urgent Open Tickets (SLA Due Soonest)',
    noUrgentTickets: 'No urgent tickets pending resolution. Great job!',
    slaCountdown: 'SLA Status',
    overdueBy: 'Overdue by',
    timeLeft: 'Time remaining',
    viewAllTickets: 'View All Tickets',
    createTicketCta: 'Create New Ticket',
    runReviewCta: 'Run SLA Review'
  },
  tickets: {
    listTitle: 'Ticket Registry',
    createTitle: 'Create Support Ticket',
    detailTitle: 'Ticket Details',
    id: 'ID',
    titleField: 'Title',
    descriptionField: 'Description',
    categoryField: 'Category',
    priorityField: 'Priority',
    siteField: 'Site',
    assetField: 'Affected Asset',
    statusField: 'Status',
    requesterField: 'Requester',
    technicianField: 'Technician',
    createdAtField: 'Created At',
    slaDueField: 'SLA Deadline',
    unassigned: 'Waiting for assignment',
    overdueOnly: 'Show overdue tickets only',
    requesterFilter: 'Requester ID',
    technicianFilter: 'Technician ID',
    changeStatus: 'Change Status',
    reassign: 'Assign / Reassign',
    historyTitle: 'Audit Trail & History',
    assignmentsTitle: 'Assignment Records',
    noHistory: 'No history entries recorded yet.',
    noAssignments: 'No assignments recorded yet.',
    assignedToBadge: 'Assigned to',
    slaEscalatedBadge: 'Auto-Escalated',
    slaBreachedBadge: 'SLA Breached',
    timeRemaining: 'Time left',
    overdueWarning: 'Deadline exceeded',
    createdSuccess: 'Ticket registered successfully!',
    statusChangedSuccess: 'Ticket status updated successfully',
    reassignedSuccess: 'Ticket successfully reassigned',
    changeStatusDialogTitle: 'Change Ticket Status',
    targetStatusLabel: 'Target Status',
    observationsLabel: 'Observations',
    observationsHint: 'Minimum 15 characters if cancelling ticket',
    reassignDialogTitle: 'Assign Technician to Ticket',
    selectTechnicianLabel: 'Select Eligible Technician',
    estimatedHoursLabel: 'Estimated Hours',
    withinSlaLabel: 'Fits SLA'
  },
  sla: {
    title: 'SLA Policies & Escalation Rules',
    priorityCol: 'Priority',
    resolutionHoursCol: 'Resolution Window',
    thresholdCol: 'Escalation Threshold',
    escalateAtCol: 'Escalates At',
    editPolicy: 'Edit Policy',
    runReview: 'Run SLA Review Now',
    reviewSuccess: 'SLA review completed successfully',
    reviewModalTitle: 'SLA Review Execution Summary',
    reviewedCount: 'Tickets Inspected',
    escalatedCount: 'Tickets Escalated',
    breachedCount: 'Tickets Flagged Breached',
    failedCount: 'Review Failures'
  },
  users: {
    title: 'User Management',
    activateConfirm: 'Are you sure you want to activate this user?',
    deactivateConfirm: 'Are you sure you want to deactivate this user? They will not be able to log in or create tickets.',
    cannotDeactivateSelf: 'You cannot deactivate your own administrator account',
    statusUpdated: 'User status successfully updated'
  },
  technicians: {
    title: 'Technician Team',
    createTitle: 'Create Technician Account & Profile',
    editTitle: 'Edit Technician Profile',
    codeCol: 'Code',
    nameCol: 'Full Name',
    specialtyCol: 'Specialty',
    siteCol: 'Site',
    capacityCol: 'Active Capacity',
    statusCol: 'Status',
    createdSuccess: 'Technician created successfully',
    updatedSuccess: 'Technician profile updated successfully'
  },
  profile: {
    title: 'User Profile',
    accountDetails: 'Account Information',
    editDetails: 'Edit Profile Information',
    updatedSuccess: 'Profile updated successfully'
  },
  problem: {
    alertTitle: 'Error occurred',
    traceId: 'Trace ID',
    copyTraceId: 'Copy Trace ID',
    traceIdCopied: 'Trace ID copied to clipboard'
  }
};
