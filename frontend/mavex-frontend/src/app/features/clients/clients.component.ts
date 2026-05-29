import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { LayoutService } from '../../core/services/layout.service';

@Component({
  selector: 'app-clients',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="card">
      <div class="card-body text-center py-5 text-muted">
        <h4 class="font-syne">Clients</h4>
        <p>Module clients — à implémenter.</p>
      </div>
    </div>
  `,
})
export class ClientsComponent implements OnInit {
  private readonly layout = inject(LayoutService);
  ngOnInit(): void { this.layout.setPage('Clients'); }
}
