import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusChangeDialogComponent, StatusChangeDialogData } from './status-change-dialog.component';
import { TicketService } from '../../../../core/services/ticket.service';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('StatusChangeDialogComponent', () => {
  let component: StatusChangeDialogComponent;
  let fixture: ComponentFixture<StatusChangeDialogComponent>;
  let ticketServiceMock: { changeStatus: any };
  let dialogRefMock: { close: any };

  const dialogData: StatusChangeDialogData = {
    ticket: {
      id: 55,
      title: 'Server fan failed',
      description: 'Fan noisy',
      category: 'HARDWARE',
      priority: 'ALTA',
      site: 'MEDELLIN',
      affectedAsset: 'SRV-02',
      status: 'ASIGNADO',
      createdAt: '2026-10-27T10:00:00Z',
      slaDueAt: '2026-10-27T18:00:00Z',
      slaBreached: false,
      escalated: false,
      escalatedAt: null,
      firstResponseAt: null,
      resolvedAt: null,
      requester: { id: 1, fullName: 'Laura', email: 'laura@example.com' },
      technician: { id: 2, code: 'TEC-001', fullName: 'Carlos', specialty: 'HARDWARE' }
    },
    userRole: 'ROLE_ADMIN'
  };

  beforeEach(async () => {
    ticketServiceMock = {
      changeStatus: vi.fn().mockReturnValue(of({ ...dialogData.ticket, status: 'EN_PROGRESO' }))
    };
    dialogRefMock = {
      close: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [StatusChangeDialogComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TicketService, useValue: ticketServiceMock },
        { provide: MatDialogRef, useValue: dialogRefMock },
        { provide: MAT_DIALOG_DATA, useValue: dialogData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StatusChangeDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize with valid next transitions for admin from ASIGNADO', () => {
    expect(component.availableStatuses).toEqual(['EN_PROGRESO', 'CANCELADO']);
  });

  it('should require at least 15 characters when target status is CANCELADO', () => {
    component.form.patchValue({
      status: 'CANCELADO',
      observations: 'Too short'
    });

    expect(component.form.get('observations')?.hasError('minlength')).toBe(true);

    component.form.patchValue({
      observations: 'Ticket cancelled due to duplicate report submitted'
    });

    expect(component.form.get('observations')?.valid).toBe(true);
  });

  it('should submit change request and close dialog with response', () => {
    component.form.patchValue({
      status: 'EN_PROGRESO',
      observations: 'Investigating issue on site'
    });

    component.onSubmit();

    expect(ticketServiceMock.changeStatus).toHaveBeenCalledWith(55, {
      status: 'EN_PROGRESO',
      observations: 'Investigating issue on site'
    });
    expect(dialogRefMock.close).toHaveBeenCalledWith(expect.objectContaining({ status: 'EN_PROGRESO' }));
  });
});
