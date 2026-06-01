import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { LayoutService } from '../../core/services/layout.service';

@Component({
  selector: 'app-payments',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card border-0 shadow-sm">
      <div class="card-body text-center py-5">
        <div class="mb-3" style="color:#9CA3AF">
          <svg width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.25" viewBox="0 0 24 24" aria-hidden="true">
            <rect x="1" y="4" width="22" height="16" rx="2" ry="2"/>
            <line x1="1" y1="10" x2="23" y2="10"/>
          </svg>
        </div>
        <h4 class="fw-bold mb-1" style="font-family:'Syne',sans-serif">Paiements</h4>
        <p class="text-muted mb-0" style="font-size:14px">Module en cours de développement.</p>
      </div>
    </div>
  `,
})
export class PaymentsComponent implements OnInit {
  private readonly layout = inject(LayoutService);
  ngOnInit(): void { this.layout.setPage('Paiements'); }
}
