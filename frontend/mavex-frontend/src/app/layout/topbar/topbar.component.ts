import {
  ChangeDetectionStrategy, Component, DestroyRef,
  OnInit, inject, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { startWith, switchMap } from 'rxjs/operators';
import { DatePipe } from '@angular/common';
import { LayoutService }       from '../../core/services/layout.service';
import { AuthService }         from '../../core/auth/auth.service';
import { NotificationService } from '../../core/services/notification.service';
import { NotificationItem }    from '../../core/models/notification.model';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, DatePipe],
  templateUrl: './topbar.component.html',
  styleUrl:    './topbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent implements OnInit {
  protected readonly layout     = inject(LayoutService);
  protected readonly authSvc    = inject(AuthService);
  private   readonly notifSvc   = inject(NotificationService);
  private   readonly destroyRef = inject(DestroyRef);

  protected readonly isOpen  = signal(false);
  protected readonly count   = signal(0);
  protected readonly notifs  = signal<NotificationItem[]>([]);
  protected readonly marking = signal(false);

  ngOnInit(): void {
    interval(30_000).pipe(
      startWith(0),
      switchMap(() => this.notifSvc.getUnreadCount()),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(res => this.count.set(res.count));
  }

  toggleDropdown(): void {
    const opening = !this.isOpen();
    this.isOpen.set(opening);
    if (opening) {
      this.notifSvc.getUnread()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(list => this.notifs.set(list));
    }
  }

  closeDropdown(): void { this.isOpen.set(false); }

  markAllRead(): void {
    if (this.marking()) return;
    this.marking.set(true);
    this.notifSvc.markAllAsRead()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.count.set(0);
          this.notifs.update(list => list.map(n => ({ ...n, read: true })));
          this.marking.set(false);
        },
        error: () => this.marking.set(false),
      });
  }
}
