import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { LayoutService } from '../../core/services/layout.service';

@Component({
  selector: 'app-shippers',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card border-0 shadow-sm">
      <div class="card-body text-center py-5">
        <div class="mb-3" style="color:#9CA3AF">
          <svg width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.25" viewBox="0 0 24 24" aria-hidden="true">
            <rect x="1" y="3" width="15" height="13" rx="1"/>
            <path d="M16 8h4l3 3v5h-7V8z"/>
            <circle cx="5.5" cy="18.5" r="2.5"/>
            <circle cx="18.5" cy="18.5" r="2.5"/>
          </svg>
        </div>
        <h4 class="fw-bold mb-1" style="font-family:'Syne',sans-serif">Shippers</h4>
        <p class="text-muted mb-0" style="font-size:14px">Module en cours de développement.</p>
      </div>
    </div>
  `,
})
export class ShippersComponent implements OnInit {
  private readonly layout = inject(LayoutService);
  ngOnInit(): void { this.layout.setPage('Shippers'); }
}
