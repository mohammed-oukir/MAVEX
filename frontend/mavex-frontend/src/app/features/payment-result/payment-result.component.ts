import {
  ChangeDetectionStrategy, Component, OnInit, inject, signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

type ResultType = 'success' | 'error' | 'cancelled';

interface ResultConfig {
  type:    ResultType;
  icon:    string;
  title:   string;
  message: string;
  color:   string;
  bg:      string;
}

@Component({
  selector: 'app-payment-result',
  imports: [RouterLink],
  templateUrl: './payment-result.component.html',
  styleUrl: './payment-result.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentResultComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  config = signal<ResultConfig>(this.successConfig());

  ngOnInit(): void {
    const type = this.route.snapshot.data['type'] as ResultType ?? 'success';
    this.config.set(this.getConfig(type));
  }

  private getConfig(type: ResultType): ResultConfig {
    if (type === 'error')     return this.errorConfig();
    if (type === 'cancelled') return this.cancelledConfig();
    return this.successConfig();
  }

  private successConfig(): ResultConfig {
    return {
      type:    'success',
      icon:    'success',
      title:   'Paiement confirmé !',
      message: 'Votre paiement a été enregistré avec succès. Votre colis sera traité et livré prochainement. Vous recevrez une confirmation par email.',
      color:   '#16A34A',
      bg:      '#DCFCE7',
    };
  }

  private errorConfig(): ResultConfig {
    return {
      type:    'error',
      icon:    'error',
      title:   'Erreur de paiement',
      message: 'Une erreur est survenue lors du traitement de votre paiement. Veuillez réessayer en utilisant le lien reçu par email ou contacter notre service client.',
      color:   '#DC2626',
      bg:      '#FEE2E2',
    };
  }

  private cancelledConfig(): ResultConfig {
    return {
      type:    'cancelled',
      icon:    'cancelled',
      title:   'Paiement annulé',
      message: 'Vous avez annulé le paiement. Votre colis reste en attente. Vous pouvez réessayer à tout moment via le lien reçu dans votre email.',
      color:   '#D97706',
      bg:      '#FEF3C7',
    };
  }
}
