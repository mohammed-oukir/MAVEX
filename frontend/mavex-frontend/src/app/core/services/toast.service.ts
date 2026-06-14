import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'info';

export interface Toast {
  id:         string;
  type:       ToastType;
  message:    string;
  dismissing: boolean;
}

const DISPLAY_MS   = 4000;
const FADEOUT_MS   = 350;

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);

  show(message: string, type: ToastType = 'info'): void {
    const id = Date.now().toString();
    this.toasts.update(t => [...t, { id, type, message, dismissing: false }]);

    // Start fade-out before actual removal
    setTimeout(() => this._startDismiss(id), DISPLAY_MS);
    setTimeout(() => this._remove(id),        DISPLAY_MS + FADEOUT_MS);
  }

  success(message: string): void { this.show(message, 'success'); }
  error(message: string):   void { this.show(message, 'error'); }
  info(message: string):    void { this.show(message, 'info'); }

  dismiss(id: string): void {
    this._startDismiss(id);
    setTimeout(() => this._remove(id), FADEOUT_MS);
  }

  private _startDismiss(id: string): void {
    this.toasts.update(t =>
      t.map(toast => toast.id === id ? { ...toast, dismissing: true } : toast)
    );
  }

  private _remove(id: string): void {
    this.toasts.update(t => t.filter(toast => toast.id !== id));
  }
}
