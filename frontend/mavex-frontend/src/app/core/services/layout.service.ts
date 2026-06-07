import { inject, Injectable, signal } from '@angular/core';
import { DOCUMENT } from '@angular/common';

export interface TopbarAction {
  label:        string;
  icon?:        string;
  routerLink?:  string;
  onClick?:     () => void;
}

@Injectable({ providedIn: 'root' })
export class LayoutService {
  private readonly doc = inject(DOCUMENT);

  title       = signal('');
  action      = signal<TopbarAction | null>(null);
  sidebarOpen = signal(this.doc.defaultView ? this.doc.defaultView.innerWidth > 768 : true);

  setPage(title: string, action?: TopbarAction): void {
    this.title.set(title);
    this.action.set(action ?? null);
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(v => !v);
  }
}
