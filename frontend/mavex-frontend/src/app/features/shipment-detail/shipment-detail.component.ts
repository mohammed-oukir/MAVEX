import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, input, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { OrderService } from '../../core/services/order.service';
import { EmailService } from '../../core/services/email.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { ShipmentResponse } from '../../core/models/shipment.model';
import { OrderResponse } from '../../core/models/order.model';

@Component({
  selector: 'app-shipment-detail',
  imports: [RouterLink, FormsModule, BadgeComponent],
  templateUrl: './shipment-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShipmentDetailComponent implements OnInit {
  id = input.required<string>();

  private readonly layout     = inject(LayoutService);
  private readonly shipSvc    = inject(ShipmentService);
  private readonly orderSvc   = inject(OrderService);
  private readonly emailSvc   = inject(EmailService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  shipment = signal<ShipmentResponse | null>(null);
  orders   = signal<OrderResponse[]>([]);
  loading  = signal(true);
  search   = signal('');

  editingDuty   = signal(false);
  dutyRateInput = signal(0);
  savingDuty    = signal(false);

  sendingId  = signal<number | null>(null);
  sendingAll = signal(false);

  filteredOrders = computed(() => {
    const q = this.search().toLowerCase();
    if (!q) return this.orders();
    return this.orders().filter(o =>
      o.hawb?.toLowerCase().includes(q) ||
      o.clientFullName?.toLowerCase().includes(q) ||
      o.goodsDescription?.toLowerCase().includes(q) ||
      o.clientEmail?.toLowerCase().includes(q)
    );
  });

  totalOrders  = computed(() => this.orders().length);
  paidCount    = computed(() => this.orders().filter(o => o.status === 'PAID').length);
  pendingCount = computed(() => this.orders().filter(o => o.status === 'PENDING_PAYMENT').length);
  emailCount   = computed(() => this.orders().filter(o => o.status === 'EMAIL_SENT').length);

  ngOnInit(): void {
    this.layout.setPage('Shipment Detail');
    this.loadData();
  }

  private loadData(): void {
    const sid = Number(this.id());
    this.loading.set(true);
    this.shipSvc.getById(sid).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: s => {
        this.shipment.set(s);
        this.layout.setPage(s.mawb);
        this.dutyRateInput.set(s.dutyRate ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.orderSvc.getByShipment(sid).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => this.orders.set(list),
    });
  }

  startEditDuty(): void   { this.editingDuty.set(true); }
  cancelEditDuty(): void  { this.editingDuty.set(false); this.dutyRateInput.set(this.shipment()?.dutyRate ?? 0); }

  saveDuty(): void {
    const s = this.shipment();
    if (!s) return;
    this.savingDuty.set(true);
    this.shipSvc.updateDutyRate(s.id, { dutyRate: this.dutyRateInput() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.shipment.set(updated);
          this.savingDuty.set(false);
          this.editingDuty.set(false);
          this.toast.success('Duty rate mis à jour.');
          this.orderSvc.getByShipment(s.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: list => this.orders.set(list) });
        },
        error: err => { this.savingDuty.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
      });
  }

  sendEmail(id: number): void {
    this.sendingId.set(id);
    this.emailSvc.sendToOrder(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.sendingId.set(null); this.toast.success('Email envoyé.'); this.reloadOrders(); },
      error: err => { this.sendingId.set(null); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
  }

  sendAll(): void {
    const s = this.shipment();
    if (!s) return;
    this.sendingAll.set(true);
    this.emailSvc.sendToShipment(s.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.sendingAll.set(false); this.toast.success('Emails envoyés.'); this.reloadOrders(); },
      error: err => { this.sendingAll.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
  }

  private reloadOrders(): void {
    const s = this.shipment();
    if (!s) return;
    this.orderSvc.getByShipment(s.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: list => this.orders.set(list) });
  }
}
