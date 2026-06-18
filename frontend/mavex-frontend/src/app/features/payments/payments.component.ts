import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { LayoutService } from '../../core/services/layout.service';

@Component({
  selector: 'app-payments',
  imports: [],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentsComponent implements OnInit {
  private readonly layout = inject(LayoutService);

  ngOnInit(): void {
    this.layout.setPage('Paiements');
  }
}
