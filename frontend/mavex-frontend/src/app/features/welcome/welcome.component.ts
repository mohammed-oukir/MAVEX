import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { LayoutService } from '../../core/services/layout.service';

@Component({
  selector: 'app-welcome',
  templateUrl: './welcome.component.html',
  styleUrl: './welcome.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WelcomeComponent implements OnInit {
  private readonly layout = inject(LayoutService);

  ngOnInit(): void {
    this.layout.setPage('Accueil');
  }
}
