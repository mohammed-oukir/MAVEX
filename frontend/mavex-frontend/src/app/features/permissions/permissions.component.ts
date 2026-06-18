import {
  ChangeDetectionStrategy, Component, OnInit, computed, inject, signal,
} from '@angular/core';
import { PermissionApiService } from '../../core/services/permission-api.service';
import {
  ALL_ACTIONS, ALL_MODULES, MODULE_LABELS, ACTION_LABELS,
  AgentPermissions, AgentSummary, VIEW_ONLY_MODULES, PermissionEntry,
} from '../../core/models/permission.model';
import { PermissionAction, PermissionModule } from '../../core/models/auth.model';
import { LayoutService } from '../../core/services/layout.service';

type EditorState = 'list' | 'edit';

@Component({
  selector: 'app-permissions',
  imports: [],
  templateUrl: './permissions.component.html',
  styleUrl: './permissions.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PermissionsComponent implements OnInit {
  private readonly api    = inject(PermissionApiService);
  private readonly layout = inject(LayoutService);

  // ── Constantes exposées au template ────────────────────
  protected readonly ALL_MODULES    = ALL_MODULES;
  protected readonly ALL_ACTIONS    = ALL_ACTIONS;
  protected readonly MODULE_LABELS  = MODULE_LABELS;
  protected readonly ACTION_LABELS  = ACTION_LABELS;
  protected readonly VIEW_ONLY      = VIEW_ONLY_MODULES;

  // ── État de l'écran ─────────────────────────────────────
  protected readonly view           = signal<EditorState>('list');
  protected readonly agents         = signal<AgentSummary[]>([]);
  protected readonly loadingAgents  = signal(true);
  protected readonly errorAgents    = signal<string | null>(null);

  // ── Éditeur ─────────────────────────────────────────────
  protected readonly selectedAgent  = signal<AgentSummary | null>(null);
  protected readonly editorData     = signal<AgentPermissions | null>(null);
  protected readonly loadingEditor  = signal(false);
  protected readonly saving         = signal(false);
  protected readonly saveError      = signal<string | null>(null);
  protected readonly saveSuccess    = signal(false);
  protected readonly expandedModules= signal<Set<PermissionModule>>(new Set(ALL_MODULES));

  /** true si les données locales diffèrent de celles chargées depuis l'API */
  private _originalByModule: Record<PermissionModule, PermissionAction[]> | null = null;
  protected readonly hasUnsaved = signal(false);

  // ── Filtrage de la liste ─────────────────────────────────
  protected readonly search = signal('');
  protected readonly filteredAgents = computed(() => {
    const list = this.agents() ?? [];
    const q = this.search().toLowerCase().trim();
    if (!q) return list;
    return list.filter(a =>
      a.fullName.toLowerCase().includes(q) || a.email.toLowerCase().includes(q)
    );
  });

  // ── Matrice locale (module → actions cochées) ────────────
  protected readonly localMatrix = signal<Record<PermissionModule, Set<PermissionAction>>>(
    this.emptyMatrix()
  );

  ngOnInit(): void {
    this.layout.setPage('Permissions');
  }

  constructor() { this.loadAgents(); }

  // ── Chargement liste ─────────────────────────────────────

  private loadAgents(): void {
    this.loadingAgents.set(true);
    this.errorAgents.set(null);
    this.api.getAgents().subscribe({
      next: list => { this.agents.set(list ?? []); this.loadingAgents.set(false); },
      error: ()   => { this.errorAgents.set('Impossible de charger les agents.'); this.agents.set([]); this.loadingAgents.set(false); },
    });
  }

  // ── Sélection d'un agent ─────────────────────────────────

  protected openEditor(agent: AgentSummary): void {
    this.selectedAgent.set(agent);
    this.view.set('edit');
    this.loadingEditor.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);
    this.hasUnsaved.set(false);

    this.api.getAgentPermissions(agent.userId).subscribe({
      next: data => {
        this.editorData.set(data);
        const matrix = this.buildMatrix(data.byModule);
        this.localMatrix.set(matrix);
        this._originalByModule = data.byModule;
        this.loadingEditor.set(false);
      },
      error: () => {
        this.loadingEditor.set(false);
        this.saveError.set('Impossible de charger les permissions de cet agent.');
      },
    });
  }

  protected backToList(): void {
    this.view.set('list');
    this.selectedAgent.set(null);
    this.editorData.set(null);
    this._originalByModule = null;
    this.hasUnsaved.set(false);
  }

  // ── Gestion des cases à cocher ───────────────────────────

  protected isChecked(module: PermissionModule, action: PermissionAction): boolean {
    return this.localMatrix()[module]?.has(action) ?? false;
  }

  protected toggle(module: PermissionModule, action: PermissionAction): void {
    const matrix  = this.localMatrix();
    const set     = new Set(matrix[module]);
    const current = set.has(action);

    if (current) {
      set.delete(action);
      // Décocher VIEW → décocher tout
      if (action === 'VIEW') {
        set.clear();
      }
    } else {
      set.add(action);
      // Cocher CREATE/EDIT/DELETE → cocher VIEW aussi
      if (action !== 'VIEW') {
        set.add('VIEW');
      }
    }

    this.localMatrix.set({ ...matrix, [module]: set });
    this.hasUnsaved.set(this.detectChanges());
  }

  protected isViewOnly(module: PermissionModule): boolean {
    return this.VIEW_ONLY.includes(module);
  }

  protected toggleModule(module: PermissionModule, grantAll: boolean): void {
    const matrix = this.localMatrix();
    const actions: PermissionAction[] = grantAll
      ? (this.isViewOnly(module) ? ['VIEW'] : [...ALL_ACTIONS])
      : [];
    this.localMatrix.set({ ...matrix, [module]: new Set(actions) });
    this.hasUnsaved.set(this.detectChanges());
  }

  protected isModuleFullyGranted(module: PermissionModule): boolean {
    const set = this.localMatrix()[module];
    const expected = this.isViewOnly(module) ? ['VIEW'] : ALL_ACTIONS;
    return expected.every(a => set.has(a as PermissionAction));
  }

  protected moduleActionCount(module: PermissionModule): number {
    return this.localMatrix()[module]?.size ?? 0;
  }

  protected toggleExpanded(module: PermissionModule): void {
    const s = new Set(this.expandedModules());
    s.has(module) ? s.delete(module) : s.add(module);
    this.expandedModules.set(s);
  }

  protected isExpanded(module: PermissionModule): boolean {
    return this.expandedModules().has(module);
  }

  // ── Sauvegarde ───────────────────────────────────────────

  protected save(): void {
    const agent = this.selectedAgent();
    if (!agent) return;

    const entries: PermissionEntry[] = [];
    for (const mod of ALL_MODULES) {
      for (const act of ALL_ACTIONS) {
        entries.push({ module: mod, action: act, granted: this.isChecked(mod, act) });
      }
    }

    this.saving.set(true);
    this.saveError.set(null);
    this.saveSuccess.set(false);

    this.api.saveAgentPermissions(agent.userId, { permissions: entries }).subscribe({
      next: updated => {
        this._originalByModule = updated.byModule;
        this.editorData.set(updated);
        this.localMatrix.set(this.buildMatrix(updated.byModule));
        this.hasUnsaved.set(false);
        this.saving.set(false);
        this.saveSuccess.set(true);
        // Mise à jour du compteur dans la liste
        const updatedCount = Object.values(updated.byModule).reduce((acc, arr) => acc + arr.length, 0);
        this.agents.update(list =>
          list.map(a => a.userId === agent.userId ? { ...a, totalGranted: updatedCount } : a)
        );
        setTimeout(() => this.saveSuccess.set(false), 3000);
      },
      error: () => {
        this.saveError.set('Erreur lors de la sauvegarde. Réessayez.');
        this.saving.set(false);
      },
    });
  }

  // ── Helpers ──────────────────────────────────────────────

  private emptyMatrix(): Record<PermissionModule, Set<PermissionAction>> {
    return ALL_MODULES.reduce((acc, m) => ({ ...acc, [m]: new Set<PermissionAction>() }), {} as any);
  }

  private buildMatrix(byModule: Record<PermissionModule, PermissionAction[]>): Record<PermissionModule, Set<PermissionAction>> {
    return ALL_MODULES.reduce((acc, m) => ({
      ...acc,
      [m]: new Set(byModule[m] ?? []),
    }), {} as any);
  }

  private detectChanges(): boolean {
    if (!this._originalByModule) return false;
    for (const mod of ALL_MODULES) {
      const orig = new Set(this._originalByModule[mod] ?? []);
      const curr = this.localMatrix()[mod];
      if (orig.size !== curr.size) return true;
      for (const a of curr) { if (!orig.has(a)) return true; }
    }
    return false;
  }

  protected grantAll(): void {
    const matrix = this.emptyMatrix();
    for (const mod of ALL_MODULES) {
      matrix[mod] = new Set(this.isViewOnly(mod) ? ['VIEW'] : ALL_ACTIONS as PermissionAction[]);
    }
    this.localMatrix.set(matrix);
    this.hasUnsaved.set(this.detectChanges());
  }

  protected revokeAll(): void {
    this.localMatrix.set(this.emptyMatrix());
    this.hasUnsaved.set(this.detectChanges());
  }

  protected totalGranted = computed(() => {
    let count = 0;
    for (const mod of ALL_MODULES) {
      count += this.localMatrix()[mod]?.size ?? 0;
    }
    return count;
  });

  protected maxPermissions = computed(() => {
    let total = 0;
    for (const mod of ALL_MODULES) {
      total += this.isViewOnly(mod) ? 1 : ALL_ACTIONS.length;
    }
    return total;
  });
}
