# HelpFlow Web (`helpflow-web`)

Enterprise-grade Angular single-page application for the **HelpFlow IT Support Management System**.

HelpFlow provides end-to-end incident registration, automated SLA calculation, intelligent technician dispatching and assignment, an immutable audit trail, and SLA escalation monitoring for distributed corporate organizations.

---

## 1. Architecture & Design Principles

The application is built on modern **Angular 22.2** standards:

* **Standalone Architecture:** 100% standalone components, directives, and pipes (no legacy `NgModule`).
* **Signals & Reactivity:** Core state management uses Angular Signals (`signal`, `computed`), with RxJS strictly focused on asynchronous HTTP streams.
* **Control Flow:** Native `@if`, `@for`, `@switch` control flow throughout templates.
* **Feature-Driven Directory Structure:** Clean separation of concerns (`core/`, `shared/`, `features/`).
* **Configurable API Endpoint:** Centralized InjectionToken `API_BASE_URL` with relative `/api` paths.
* **Security & Token Management:** JWT access tokens and session data stored strictly in `sessionStorage` (never in persistent `localStorage`), preventing long-lived credential exposure across browser sessions.
* **Zero-CORS Proxying:**
  * **Development:** `ng serve` leverages `proxy.conf.json` rewriting `/api/*` to `http://localhost:8080/*`.
  * **Production:** Nginx reverse proxies `location /api/` directly to `http://support-ticket-service:8080/`.

---

## 2. Directory Layout

```
frontend/
├── public/                     # Static assets (favicons, icons)
├── src/
│   ├── index.html              # Single-page HTML shell with Google Fonts & Material icons
│   ├── main.ts                 # Bootstrap application entry point
│   ├── tailwind.css            # Tailwind CSS 4 setup, @theme design tokens & component layers
│   ├── styles.scss             # Material 3 theme aligned with tokens, global resets & harmony
│   ├── environments/           # Environment configurations (dev and production)
│   └── app/
│       ├── app.component.ts    # Minimal root container
│       ├── app.config.ts       # Application providers, router, interceptors, animations
│       ├── app.routes.ts       # Lazy-loaded feature routes with guards
│       ├── core/
│       │   ├── config/         # API InjectionToken configuration
│       │   ├── guards/         # Functional authGuard and roleGuard
│       │   ├── i18n/           # Internationalization service & typed EN/ES dictionaries
│       │   ├── interceptors/   # Auth bearer, Accept-Language & RFC 7807 problem interceptors
│       │   ├── models/         # TypeScript models mirroring backend DTOs exactly
│       │   └── services/       # Typed REST services (Auth, Tickets, Users, Technicians, SLA)
│       ├── shared/
│       │   ├── components/     # Layout shell, status/priority badges, problem-alert, dialogs
│       │   └── pipes/          # LocalizedDatePipe and SlaCountdownPipe
│       └── features/
│           ├── auth/           # Login & corporate self-registration
│           ├── dashboard/      # Real-time metrics overview, urgent SLA tickets
│           ├── tickets/        # List with filters, creation form, details & state transitions
│           ├── users/          # Admin user management & activation toggles
│           ├── technicians/    # Admin technician management & capacity tracking
│           ├── sla/            # SLA policies management & manual review execution
│           ├── profile/        # Current user profile inspection and editing
│           └── not-found/      # 404 handler
├── angular.json                # Angular CLI configuration with @angular/build
├── Dockerfile                  # Multi-stage production build (Node 24 + Nginx 1.27)
├── nginx.conf                  # Production reverse proxy, SPA fallback, gzip & CSP headers
├── package.json                # Pinned dependencies and CLI scripts
├── proxy.conf.json             # Dev server reverse proxy config
├── tsconfig.json               # Root TypeScript configuration
├── tsconfig.app.json           # Application compilation configuration
└── tsconfig.spec.json          # Vitest testing configuration
```

---

## 3. Roles & Capabilities Matrix

| Screen / Feature | Route | `ROLE_USUARIO` | `ROLE_TECNICO` | `ROLE_ADMIN` |
|---|---|:---:|:---:|:---:|
| **Login & Self-Registration** | `/login`, `/register` | Public | Public | Public |
| **Dashboard** | `/dashboard` | Own tickets metrics | Assigned tickets metrics | System-wide metrics |
| **Ticket Registry** | `/tickets` | View own | View assigned | View all + filters |
| **Create Ticket** | `/tickets/new` | Yes | No | Yes |
| **Ticket Detail** | `/tickets/:id` | View own | View assigned | View all |
| **Status Change** | `/tickets/:id` (dialog) | No | Assigned (no cancel) | Any valid transition |
| **Assign / Reassign** | `/tickets/:id` (dialog) | No | No | Yes |
| **SLA Policies** | `/sla-policies` | Read | Read | Read & Configure |
| **Run SLA Review** | `/sla-policies`, `/dashboard` | No | No | Yes |
| **Users Management** | `/admin/users` | No | No | Full (activate/deactivate) |
| **Technicians Team** | `/admin/technicians` | No | No | Full (create/edit) |
| **User Profile** | `/profile` | Own profile | Own profile | Own profile |

---

## 4. Ticket Lifecycle & SLA Rules

### Valid State Transitions
1. `ABIERTO` -> `ASIGNADO` (via administrator assignment), `CANCELADO` (admin only).
2. `ASIGNADO` -> `EN_PROGRESO` (technician or admin), `CANCELADO` (admin only).
3. `EN_PROGRESO` -> `RESUELTO` (technician or admin), `CANCELADO` (admin only).
4. `RESUELTO` -> `EN_PROGRESO` (reopen), `CERRADO` (final closure).
5. `CERRADO` & `CANCELADO`: Terminal states.

* **Observations Validation:** Cancellation requires observations of at least 15 characters. Other status transitions require mandatory non-empty observations.
* **SLA Calculation:** SLA deadlines are calculated in calendar hours based on priority:
  * `CRITICA`: 4 hours (escalates at 80% = 3.2 h)
  * `ALTA`: 8 hours (escalates at 80% = 6.4 h)
  * `MEDIA`: 24 hours (escalates at 80% = 19.2 h)
  * `BAJA`: 72 hours (escalates at 80% = 57.6 h)
* The UI displays real-time countdown badges (`SlaCountdownPipe`) indicating remaining time or overdue duration in bold red.

---

## 5. Internationalization (i18n)

The application provides a zero-external-dependency, typed runtime internationalization architecture:
* **Languages:** **English** (`en`, default) and **Spanish** (`es`).
* **Persistence:** Persisted in `sessionStorage` (`helpflow_lang`).
* **Switchable:** Seamless toggle directly from the top toolbar at any time.
* **Backend Localization:** The `authInterceptor` passes the active language in the standard `Accept-Language` header with every HTTP request. All RFC 7807 problem details returned by the Spring backend arrive translated in the active language.
* **Localized Dates:** `LocalizedDatePipe` and `I18nService.formatDate()` format calendar dates and timestamps according to the active locale (`en-US` or `es-CO`).

---

## 6. How to Run Locally

### Prerequisites
* Node.js 24+
* npm 11+
* Running `support-ticket-service` backend on `http://localhost:8080`

### Steps
1. Install dependencies:
   ```bash
   npm install
   ```
2. Start development server:
   ```bash
   npm start
   ```
3. Open `http://localhost:4200` in your web browser. Requests to `/api/*` are automatically forwarded to `http://localhost:8080/*` via `proxy.conf.json`.

---

## 7. How to Run with Docker

The project includes a multi-stage `Dockerfile`:
* Stage 1: Compiles the Angular production application inside `node:24-alpine`.
* Stage 2: Serves static assets using high-performance `nginx:1.27-alpine` running as a non-root user.

### Build and Run Docker Container
```bash
# Build image
docker build -t helpflow-web:latest .

# Run container on port 80 (or port 4200)
docker run -d --name helpflow-web -p 80:80 helpflow-web:latest
```

When deployed in the repository's Docker Compose network, Nginx routes `/api/` to `http://support-ticket-service:8080/`.

---

## 8. Unit Testing

Unit tests are written with **Vitest** and executed using the official `@angular/build:unit-test` builder.

### Run Tests
```bash
npm test
```

### Test Coverage Highlights
* `AuthService`: Token extraction, expiry checks, `sessionStorage` persistence, auto-logout scheduling.
* `authInterceptor`: Bearer token attachment and `Accept-Language` propagation.
* `errorInterceptor`: RFC 7807 ProblemDetail conversion, 401 redirection preserving intended URL, 403/5xx alert snackbars with `traceId`.
* `authGuard` & `roleGuard`: Route access control and redirection trees.
* `ticketStatusTransitions`: State machine matrix validation and 15-character cancellation rules.
* `I18nService`: Dynamic dictionary switching, persistence, and date formatting.
* `LoginComponent`: Reactive form validation and problem detail error binding.
* `TicketListComponent`: Filtering query param sync and table data binding.
* `StatusChangeDialogComponent`: Allowed transitions by role and observation constraint enforcement.

---

## 9. Styling (Tailwind CSS 4 & Angular Material)

The UI visual layer is powered by **Tailwind CSS 4** using CSS-first configuration and atomic utility classes, paired selectively with Angular Material 3.

### CSS-First `@theme` Tokens
Design tokens are declared in `src/tailwind.css` within the `@theme` block:
* **Brand palette:** `--color-brand-50` through `--color-brand-700`.
* **Neutrals & Surfaces:** `--color-ink`, `--color-surface`, `--color-canvas`.
* **Typography:** `--font-sans` configured with Roboto and system fallback sans-serif fonts.

### Angular Material 3 Integration & Scope
Angular Material is intentionally retained only for behavior-heavy widgets where reimplementing complex accessibility, keyboard navigation, or overlays would introduce risk:
* `mat-form-field` (inputs, textareas, selects, hints, and validation errors)
* `mat-dialog` (modal backdrops, focus trapping, and animations)
* `mat-snack-bar` (transient notifications)
* `mat-paginator` (accessible pagination controls)
* `mat-menu` (dropdown menus)
* `mat-tab-group` and `mat-sort` (tab transitions and table sorting indicators)

All container layouts, cards, badges, grids, metrics, timelines, buttons, and navigation chrome are styled exclusively with Tailwind utility classes. `src/styles.scss` is kept minimal, containing only the Material theme definition (primary color aligned with `--color-brand-500`), basic HTML/body resets, and preflight harmony rules.

### Dark Mode
Dark mode is driven by the operating system / browser preference via `prefers-color-scheme` (Tailwind 4's default `dark:` variant).
* Angular Material mirrors this automatically through `@media (prefers-color-scheme: dark)` in `src/styles.scss`, applying `$dark-theme`.
* All templates employ `dark:` modifiers (e.g., `bg-white dark:bg-slate-900`, `text-slate-900 dark:text-slate-100`, `border-slate-200 dark:border-slate-800`).

### How to Add a Token or Component Class
* **Adding a Design Token:** Declare it in `src/tailwind.css` under the `@theme` block:
  ```css
  @theme {
    --color-brand-accent: #2563eb;
  }
  ```
  It immediately becomes available as utility classes (e.g., `bg-brand-accent`, `text-brand-accent`).
* **Adding a Component Class:** When a specific UI pattern repeats across components (such as the base `.badge`), declare it in `src/tailwind.css` under `@layer components`:
  ```css
  @layer components {
    .badge {
      @apply inline-flex items-center justify-center px-2.5 py-0.5 rounded-full border text-xs font-semibold tracking-wide uppercase whitespace-nowrap transition-colors;
    }
  }
  ```

