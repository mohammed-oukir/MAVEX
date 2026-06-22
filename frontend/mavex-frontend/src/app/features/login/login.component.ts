import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

type Tab  = 'login' | 'forgot';
type Step = 1 | 2 | 3;

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  private readonly fb     = inject(FormBuilder);
  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);

  tab          = signal<Tab>('login');
  step         = signal<Step>(1);
  loading      = signal(false);
  error        = signal('');
  success      = signal('');
  showPwd      = signal(false);
  showPwdReset = signal(false);

  loginForm = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  forgotEmailForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  otpForm = this.fb.group({
    otp: ['', [Validators.required, Validators.minLength(4)]],
  });

  resetForm = this.fb.group({
    password:  ['', [Validators.required, Validators.minLength(8)]],
    password2: ['', Validators.required],
  });

  private forgotEmail = '';

  setTab(t: Tab): void {
    this.tab.set(t);
    this.step.set(1);
    this.error.set('');
    this.success.set('');
  }

  submitLogin(): void {
    if (this.loginForm.invalid) { this.loginForm.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    const { email, password } = this.loginForm.value;
    this.auth.login(email!, password!).subscribe({
      next: () => {
        const role = this.auth.currentUser().role;
        const dest = role === 'COMPTABLE' ? '/exchange-rates' : '/dashboard';
        this.router.navigate([dest]);
      },
      error: err => {
        this.loading.set(false);
        this.error.set(err?.error?.message || 'Email ou mot de passe incorrect.');
      },
    });
  }

  submitForgotEmail(): void {
    if (this.forgotEmailForm.invalid) { this.forgotEmailForm.markAllAsTouched(); return; }
    this.loading.set(true);
    this.error.set('');
    this.forgotEmail = this.forgotEmailForm.value.email!;
    this.auth.forgotPassword(this.forgotEmail).subscribe({
      next: () => { this.loading.set(false); this.step.set(2); },
      error: err => { this.loading.set(false); this.error.set(err?.error?.message || 'Erreur lors de l\'envoi.'); },
    });
  }

  submitOtp(): void {
    if (this.otpForm.invalid) { this.otpForm.markAllAsTouched(); return; }
    this.step.set(3);
  }

  submitReset(): void {
    if (this.resetForm.invalid) { this.resetForm.markAllAsTouched(); return; }
    const { password, password2 } = this.resetForm.value;
    if (password !== password2) { this.error.set('Les mots de passe ne correspondent pas.'); return; }
    this.loading.set(true);
    this.error.set('');
    this.auth.resetPassword(this.forgotEmail, this.otpForm.value.otp!, password!).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set('Mot de passe réinitialisé. Vous pouvez vous connecter.');
        this.setTab('login');
      },
      error: err => { this.loading.set(false); this.error.set(err?.error?.message || 'Code OTP invalide.'); },
    });
  }

  fieldInvalid(form: ReturnType<typeof this.fb.group>, name: string): boolean {
    const c = form.get(name);
    return !!(c?.invalid && c?.touched);
  }
}
