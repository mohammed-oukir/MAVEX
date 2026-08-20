import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }         from '../../core/services/layout.service';
import { UserService }           from '../../core/services/user.service';
import { ToastService }          from '../../core/services/toast.service';
import { AgentPermissionService } from '../../core/services/agent-permission.service';
import { UserResponse }          from '../../core/models/user.model';
import {
  PermissionCatalogItem,
  PermissionAuditLogItem,
} from '../../core/models/agent-permission.model';

interface ModuleGroup {
  module:       string;
  label:        string;
  icon:         string;   // clé utilisée dans le @switch de l'icône du template
  permissions:  PermissionCatalogItem[];
}

/** Libellé FR + icône par module — ordre d'affichage volontaire. */
const MODULE_META: Record<string, { label: string; icon: string }> = {
  DASHBOARD:           { label: 'Tableau de bord',      icon: 'home' },
  SHIPMENTS:           { label: 'Expéditions',          icon: 'truck' },
  ORDERS:              { label: 'Commandes',            icon: 'package' },
  CLIENTS:             { label: 'Clients',               icon: 'users' },
  SHIPPERS:            { label: 'Expéditeurs',           icon: 'send' },
  AIRLINES:            { label: 'Compagnies aériennes',  icon: 'plane' },
  IMPORTS:             { label: 'Imports',                icon: 'upload' },
  PAYMENTS:            { label: 'Facturation',            icon: 'file-text' },
  EMAIL_SETTINGS:      { label: 'Paramètres email',      icon: 'mail' },
  DASHBOARD_ANALYTICS: { label: 'Statistiques',          icon: 'bar-chart' },
};
const MODULE_ORDER = Object.keys(MODULE_META);

@Component({
  selector: 'app-agent-permissions',
  imports: [DatePipe],
  templateUrl: './agent-permissions.component.html',
  styleUrl: './agent-permissions.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AgentPermissionsComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly userSvc    = inject(UserService);
  private readonly permSvc    = inject(AgentPermissionService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Colonne gauche — liste des agents ───────────────────── */
  allUsers      = signal<UserResponse[]>([]);
  loadingAgents = signal(true);
  searchQ       = signal('');

  agents = computed(() => this.allUsers().filter(u => u.role === 'AGENT'));

  filteredAgents = computed(() => {
    const q = this.searchQ().trim().toLowerCase();
    const list = this.agents();
    if (!q) return list;
    return list.filter(a => a.fullName.toLowerCase().includes(q));
  });

  selectedAgent = signal<UserResponse | null>(null);

  /* ── Catalogue (chargé une fois, partagé entre agents) ───── */
  catalog        = signal<PermissionCatalogItem[]>([]);
  catalogLoading = signal(false);
  private catalogLoaded = false;

  moduleGroups = computed<ModuleGroup[]>(() => {
    const byModule = new Map<string, PermissionCatalogItem[]>();
    for (const p of this.catalog()) {
      const arr = byModule.get(p.module) ?? [];
      arr.push(p);
      byModule.set(p.module, arr);
    }
    return MODULE_ORDER
      .filter(m => byModule.has(m))
      .map(m => ({
        module: m,
        label:  MODULE_META[m].label,
        icon:   MODULE_META[m].icon,
        permissions: byModule.get(m)!,
      }));
  });

  totalPermissionsCount = computed(() => this.catalog().length);

  /* ── État des permissions de l'agent sélectionné ─────────── */
  permissionsLoading = signal(false);
  /** Référence = dernier état confirmé par le backend (après GET ou après un save réussi). */
  savedIds  = signal<Set<number>>(new Set());
  /** État local en mémoire, modifié par les cases à cocher, pas encore sauvegardé. */
  localIds  = signal<Set<number>>(new Set());

  hasUnsavedChanges = computed(() => !this.setsEqual(this.savedIds(), this.localIds()));
  activeCount        = computed(() => this.localIds().size);

  saving = signal(false);

  /* ── Accordéon : modules ouverts ─────────────────────────── */
  openModules = signal<Set<string>>(new Set());

  /* ── Historique ───────────────────────────────────────────── */
  showHistory     = signal(false);
  historyLoading  = signal(false);
  history         = signal<PermissionAuditLogItem[]>([]);

  /* ── Confirmation de changement d'agent avec modifs en attente ── */
  pendingAgentSwitch = signal<UserResponse | null>(null);

  ngOnInit(): void {
    this.layout.setPage('Permissions');
    this.loadAgents();
  }

  private loadAgents(): void {
    this.loadingAgents.set(true);
    this.userSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: users => { this.allUsers.set(users); this.loadingAgents.set(false); },
      error: () => {
        this.loadingAgents.set(false);
        this.toast.error('Impossible de charger la liste des agents.');
      },
    });
  }

  private loadCatalogIfNeeded(): void {
    if (this.catalogLoaded) return;
    this.catalogLoading.set(true);
    this.permSvc.getCatalog().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => {
        this.catalog.set(items);
        this.catalogLoaded = true;
        this.catalogLoading.set(false);
      },
      error: () => {
        this.catalogLoading.set(false);
        this.toast.error('Impossible de charger le catalogue de permissions.');
      },
    });
  }

  selectAgent(agent: UserResponse): void {
    if (this.selectedAgent()?.id === agent.id) return;

    if (this.hasUnsavedChanges()) {
      this.pendingAgentSwitch.set(agent);
      return;
    }
    this.doSelectAgent(agent);
  }

  confirmAgentSwitch(): void {
    const next = this.pendingAgentSwitch();
    this.pendingAgentSwitch.set(null);
    if (next) this.doSelectAgent(next);
  }

  cancelAgentSwitch(): void {
    this.pendingAgentSwitch.set(null);
  }

  private doSelectAgent(agent: UserResponse): void {
    this.selectedAgent.set(agent);
    this.showHistory.set(false);
    this.loadCatalogIfNeeded();

    this.permissionsLoading.set(true);
    this.savedIds.set(new Set());
    this.localIds.set(new Set());

    this.permSvc.getAgentPermissions(agent.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        const ids = new Set(res.permissionIds);
        this.savedIds.set(ids);
        this.localIds.set(new Set(ids));
        this.permissionsLoading.set(false);
        this.openModules.set(new Set(this.moduleGroups()[0] ? [this.moduleGroups()[0].module] : []));
      },
      error: () => {
        this.permissionsLoading.set(false);
        this.toast.error("Impossible de charger les permissions de cet agent.");
      },
    });
  }

  toggleModule(module: string): void {
    this.openModules.update(set => {
      const next = new Set(set);
      next.has(module) ? next.delete(module) : next.add(module);
      return next;
    });
  }

  isModuleOpen(module: string): boolean {
    return this.openModules().has(module);
  }

  isChecked(permissionId: number): boolean {
    return this.localIds().has(permissionId);
  }

  togglePermission(permissionId: number): void {
    this.localIds.update(set => {
      const next = new Set(set);
      next.has(permissionId) ? next.delete(permissionId) : next.add(permissionId);
      return next;
    });
  }

  moduleActiveCount(group: ModuleGroup): number {
    const ids = this.localIds();
    return group.permissions.filter(p => ids.has(p.id)).length;
  }

  isModuleFullyChecked(group: ModuleGroup): boolean {
    return group.permissions.length > 0 && this.moduleActiveCount(group) === group.permissions.length;
  }

  toggleAllInModule(group: ModuleGroup): void {
    const allChecked = this.isModuleFullyChecked(group);
    this.localIds.update(set => {
      const next = new Set(set);
      for (const p of group.permissions) {
        allChecked ? next.delete(p.id) : next.add(p.id);
      }
      return next;
    });
  }

  save(): void {
    const agent = this.selectedAgent();
    if (!agent || !this.hasUnsavedChanges() || this.saving()) return;

    this.saving.set(true);
    const permissionIds = Array.from(this.localIds());

    this.permSvc.updateAgentPermissions(agent.id, permissionIds)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          const ids = new Set(res.permissionIds);
          this.savedIds.set(ids);
          this.localIds.set(new Set(ids));
          this.saving.set(false);
          this.toast.success('Permissions enregistrées avec succès.');
        },
        error: () => {
          this.saving.set(false);
          this.toast.error("Erreur lors de l'enregistrement — vos modifications n'ont pas été perdues.");
        },
      });
  }

  openHistory(): void {
    const agent = this.selectedAgent();
    if (!agent) return;

    this.showHistory.set(true);
    this.historyLoading.set(true);
    this.permSvc.getAgentHistory(agent.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: items => { this.history.set(items); this.historyLoading.set(false); },
      error: () => {
        this.historyLoading.set(false);
        this.toast.error("Impossible de charger l'historique.");
      },
    });
  }

  closeHistory(): void {
    this.showHistory.set(false);
  }

  initials(name: string): string {
    return name.split(' ').slice(0, 2).map(w => w[0] ?? '').join('').toUpperCase();
  }

  private setsEqual(a: Set<number>, b: Set<number>): boolean {
    if (a.size !== b.size) return false;
    for (const id of a) if (!b.has(id)) return false;
    return true;
  }
}
