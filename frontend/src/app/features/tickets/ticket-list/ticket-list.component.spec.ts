import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TicketListComponent } from './ticket-list.component';
import { TicketService } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('TicketListComponent', () => {
  let component: TicketListComponent;
  let fixture: ComponentFixture<TicketListComponent>;
  let ticketServiceMock: { list: any };
  let routerMock: { navigate: any };

  beforeEach(async () => {
    ticketServiceMock = {
      list: vi.fn().mockReturnValue(of({
        content: [
          {
            id: 101,
            title: 'Network outage',
            category: 'RED',
            priority: 'ALTA',
            status: 'ABIERTO',
            site: 'BOGOTA',
            affectedAsset: 'Router-01',
            createdAt: '2026-10-27T10:00:00Z',
            slaDueAt: '2026-10-27T18:00:00Z',
            slaBreached: false,
            escalated: false,
            requester: { id: 1, fullName: 'Laura Gomez', email: 'laura@helpflow.example' },
            technician: null
          }
        ],
        page: 0,
        size: 10,
        totalElements: 1,
        totalPages: 1
      }))
    };

    routerMock = {
      navigate: vi.fn()
    };

    const authServiceMock = {
      isUser: vi.fn().mockReturnValue(false),
      isAdmin: vi.fn().mockReturnValue(true)
    };

    await TestBed.configureTestingModule({
      imports: [TicketListComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TicketService, useValue: ticketServiceMock },
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ status: 'ABIERTO' })
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TicketListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load tickets based on initial query params', () => {
    expect(component).toBeTruthy();
    expect(ticketServiceMock.list).toHaveBeenCalled();
    expect(component.tickets().length).toBe(1);
    expect(component.tickets()[0].id).toBe(101);
  });

  it('should apply filters and update URL query params', () => {
    component.filterForm.patchValue({
      status: 'EN_PROGRESO',
      priority: 'CRITICA',
      overdue: true
    });

    component.applyFilters();

    expect(routerMock.navigate).toHaveBeenCalledWith([], expect.objectContaining({
      queryParams: expect.objectContaining({
        status: 'EN_PROGRESO',
        priority: 'CRITICA',
        overdue: 'true'
      })
    }));
  });

  it('should reset filters to default values', () => {
    component.filterForm.patchValue({ status: 'CANCELADO' });
    component.resetFilters();

    expect(component.filterForm.get('status')?.value).toBe('');
    expect(component.filterForm.get('overdue')?.value).toBe(false);
  });
});
