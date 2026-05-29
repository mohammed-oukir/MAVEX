import { Injectable, signal } from '@angular/core';

export interface TopbarAction {
  label:        string;
  icon?:        string;
  routerLink?:  string;
  onClick?:     () => void;
}

@Injectable({ providedIn: 'root' })
export class LayoutService {
  title  = signal('');
  action = signal<TopbarAction | null>(null);

  setPage(title: string, action?: TopbarAction): void {
    this.title.set(title);
    this.action.set(action ?? null);
  }
}
