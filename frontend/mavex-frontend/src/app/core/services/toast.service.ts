import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id:      string;
  type:    ToastType;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  show(message: string, type: ToastType = 'info'): void {
    const id = Date.now().toString();
    this.toasts.update(t => [...t, { id, type, message }]);
    setTimeout(() => this.dismiss(id), 4500);
  }

  success(message: string): void { this.show(message, 'success'); }
  error(message: string):   void { this.show(message, 'error'); }
  info(message: string):    void { this.show(message, 'info'); }

  dismiss(id: string): void {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}
