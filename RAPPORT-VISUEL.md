# Rapport d'analyse visuelle — MAVEX Angular
> Généré le 2026-06-23. Lecture seule — aucune modification effectuée.

---

## Résumé exécutif

| Priorité | Problème | Pages concernées |
|----------|----------|-----------------|
| **Haute** | Fonds colorés permanents sur boutons d'action | Orders |
| **Haute** | Taille / border boutons incorrects | Orders, Airlines, Exchange-rates, Imports |
| **Haute** | Badges inline au lieu de `<app-badge>` | Clients, Shippers, Orders, Users, Exchange-rates |
| **Haute** | Gradient + border 2px sur header table | Orders, Airlines |
| **Moyenne** | KPI cards non uniformes (icône, chiffre, label) | Clients, Shippers, Orders, Users, Dashboard |
| **Moyenne** | Modals avec système de styles local | Exchange-rates, Email-template, Imports |
| **Basse** | ~30 classes orphelines (ancienne vue carte) | Users |
| **Basse** | Double définition locale des classes nm-* | Imports |
| **Basse** | `.pf-ico-purple` défini avec les couleurs orange | Profile |

---

## 1. Boutons d'action

### Standard de référence
```
width: 28px; height: 28px
border: 0.5px solid #E5E7EB; border-radius: 6px
background: transparent; color: #6B7280
hover défaut  → background #F3F4F6
hover delete  → background #FEF2F2; color #DC2626; border #FECACA
PAS de fond coloré permanent
```

### Conformes
- `clients.component.css` — `.cl-act` ✓
- `shippers.component.css` — `.sh-act` ✓
- `shipments.component.css` — `.sp-act` ✓ (standard de référence)
- `shipment-detail.component.css` — `.sd-act` ✓

### Non conformes

#### Orders — `.or-act-btn`
- `width: 30px; height: 30px` → doit être 28px
- `border: none` → doit être `0.5px solid #E5E7EB`
- `border-radius: 8px` → doit être 6px
- `.or-act-edit` : **fond orange permanent** `background: #FFF7ED; color: #F97316` — interdit
- `.or-act-del` : **fond rouge permanent** `background: #FEF2F2; color: #EF4444` — interdit

#### Airlines — `.al-act`
- `width: 32px; height: 32px` → doit être 28px
- `border: 1.5px solid #E5E7EB` → doit être 0.5px
- `border-radius: 9px` → doit être 6px
- `transform: scale(1.1)` au hover → non standard

#### Exchange-rates — `.er-icon-btn`
- `width: 30px; height: 30px` → doit être 28px
- `border: none` → doit être `0.5px solid #E5E7EB`
- `border-radius: 7px` → doit être 6px

#### Imports — `.im-act-btn`
- `width: 30px; height: 30px` → doit être 28px
- `border: 1px solid #E5E7EB` → doit être 0.5px
- `border-radius: 7px` → doit être 6px

#### Users — `.us-act-icon` (vue table)
- Conforme dans la vue table active
- Contient des classes carte orphelines avec `34×34px` / `border: 1.5px` / `border-radius: 9px` — à supprimer (voir section 7)

---

## 2. Badges de statut

### Standard de référence
Utiliser `<app-badge [status]="..." />` partout.
PAS de spans inline avec styles badge maison.

### Conformes (utilisent `<app-badge>`)
- `dashboard.component.html` ✓
- `shipment-detail.component.html` ✓
- `imports.component.html` ✓
- `shipments.component.html` ✓

### Non conformes (spans inline avec styles locaux)

#### Clients
```html
<span [class]="c.active ? 'cl-badge cl-badge-active' : 'cl-badge cl-badge-inactive'">
```
Classes locales : `.cl-badge`, `.cl-badge-active`, `.cl-badge-inactive`, `.cl-os-badge`, `.cl-os-created`, `.cl-os-pending`…

#### Shippers
```html
<span [class]="s.active ? 'sh-badge sh-badge-active' : 'sh-badge sh-badge-inactive'">
```
Classes locales : `.sh-badge`, `.sh-badge-active`, `.sh-badge-inactive`

#### Orders
```html
<span [class]="'or-status-badge ' + statusClass(o.status)">
```
Méthode `statusClass()` en TypeScript. Classes locales : `.or-status-badge`, `.or-s-created`, `.or-s-pending`, `.or-s-paid`, `.or-s-cancelled`…

#### Users
```html
<span [class]="u.active ? 'us-status us-status-on' : 'us-status us-status-off'">
```
Classes locales : `.us-status`, `.us-status-on`, `.us-status-off`

#### Exchange-rates
Classes locales : `.er-badge`, `.er-badge--active`, `.er-badge--inactive`

> **Note :** les badges active/inactive (booléen) ne correspondent pas à des statuts couverts par `BadgeComponent`. Une migration nécessite soit d'étendre `BadgeComponent` avec `ACTIVE`/`INACTIVE`, soit de garder un span simple unifié.

---

## 3. KPI Cards

### Standard de référence
```
Icône  : 44×44px, border-radius 10px, fond léger coloré
Chiffre: font-size 26px, font-weight 800
Label  : font-size 12px, text-transform uppercase, color #9CA3AF
```

### Conformes
- `shipments.component.css` — `.sp-stat-icon` 44px / `.sp-stat-num` 26px ✓ (standard de référence)
- `exchange-rates.component.css` — `.er-kpi-icon` 44px / `.er-kpi-value` 26px ✓ (sauf label color #6B7280 au lieu de #9CA3AF — écart mineur)

### Non conformes

| Composant | Classe icône | Taille icône | Taille chiffre | Label |
|-----------|-------------|--------------|----------------|-------|
| **Clients** | `.cl-kpi-icon` | 32×32px, radius 8px | 32px | pas d'uppercase, color #6B7280 |
| **Shippers** | `.sh-kpi-icon` | 32×32px, radius 8px | 32px | pas d'uppercase, color #6B7280 |
| **Orders** | `.or-kpi-icon` | 52×52px, radius 14px | 30px | pas d'uppercase, color #6B7280 |
| **Users** | `.us-stat-icon` | 36×36px, radius 8px | 22px | pas d'uppercase |
| **Dashboard** | `.dk-kpi-icon` | 36×36px, radius 10px | 28px | 11px (doit être 12px) |

---

## 4. Headers de table

### Standard de référence
```css
font-size: 11px; font-weight: 700; color: #6B7280;
text-transform: uppercase;
background: #F9FAFB;
border-bottom: 1px solid #E5E7EB;
/* PAS de gradient, PAS de border-bottom 2px */
```

### Conformes
- `shipments.component.css` — `.sp-table > thead > tr > th` ✓ (standard de référence)
- `imports.component.css` — `.im-table-head th` ✓
- `exchange-rates.component.css` — `.er-table th` ✓ (border-bottom color #F3F4F6 — écart mineur)
- `dashboard.component.css` — `.dk-table > thead > tr > th` quasi-conforme (font-weight 600, color #9CA3AF, fond #FAFAFA — écarts mineurs)

### Non conformes

#### Orders — `.or-thead th`
- `background: linear-gradient(to bottom, #F9FAFB, #F3F4F6)` → **gradient interdit**
- `border-bottom: 2px solid #E5E7EB` → **2px interdit** (doit être 1px)
- `font-size: 10.5px` → doit être 11px
- Première colonne : `border-left: 4px solid #F97316` → non standard

#### Airlines — `.al-thead th`
- `background: linear-gradient(to bottom, #FAFAFA, #fff)` → **gradient interdit**
- `border-bottom: 2px solid #F3F4F6` → **2px interdit**
- `font-size: 10.5px` → doit être 11px
- `font-weight: 800` → doit être 700

#### Clients — `.cl-thead th`
- `font-weight: 500` → doit être 700
- `border-bottom: 1px solid #F3F4F6` → couleur trop claire (doit être #E5E7EB)
- `text-transform: uppercase` absent de la règle CSS

#### Shippers — `.sh-thead th`
- `font-weight: 500` → doit être 700
- Mêmes écarts que clients

#### Users — `.us-table thead th`
- `font-size: 12px` → doit être 11px
- `font-weight: 600` → doit être 700
- `border-bottom: 0.5px solid #E5E7EB` → doit être 1px

---

## 5. Modals

### Standard de référence
Utiliser les classes nm-* globales définies dans `styles.css` :
`nm-header`, `nm-body`, `nm-footer`, `nm-btn-primary`, `nm-btn-cancel`, `nm-btn-danger`, `nm-close`, `nm-icon`, `nm-alert`, `nm-title`, `nm-sub`, `nm-label`, `nm-input`, `nm-select`
`border-radius: 16px` — `box-shadow: 0 25px 60px rgba(0,0,0,.22)`

### Conformes (utilisent nm-* globaux)
- `clients.component.html` ✓
- `shippers.component.html` ✓
- `airlines.component.html` ✓
- `orders.component.html` ✓
- `users.component.html` ✓
- `shipments.component.html` ✓

### Non conformes

#### Exchange-rates
Système entièrement local et incompatible avec nm-* :
- Classes propres : `.er-overlay`, `.er-modal`, `.er-modal-header`, `.er-modal-title`, `.er-modal-close`, `.er-modal-body`, `.er-modal-footer`
- Boutons propres : `.er-btn`, `.er-btn--ghost`, `.er-btn--danger`, `.er-btn--primary`
- `border-radius: 14px` → doit être 16px
- `box-shadow: 0 20px 60px rgba(0,0,0,.18)` → légèrement différent

#### Email-template
- Modal de prévisualisation `.et-preview-modal` avec système local
- **Redéfinit `.nm-close` localement** avec un style différent (fond #f8fafc, border, outline bleu #007bff) — collision avec le global `styles.css`

#### Imports
- **Redéfinit localement** nm-header, nm-body, nm-footer, nm-btn-cancel, nm-btn-primary, nm-btn-danger, nm-icon, nm-close, nm-alert, nm-title, nm-sub, nm-opt (12 classes)
- Double définition silencieuse — les valeurs locales peuvent diverger du global sans avertissement

---

## 6. Skeleton loading

### Toutes les pages avec table ont un skeleton ✓

| Page | Classe skeleton | Signal utilisé |
|------|----------------|---------------|
| Shipments | `.sk-row` | `loading()` |
| Shipment-detail | bloc global | `loading()` |
| Orders | `.or-sk` | `loading()` |
| Clients | `.sk-row` | `loading()` |
| Shippers | `.sk-row` | `loading()` |
| Airlines | `.al-sk` | `loading()` |
| Users | `.us-card-skeleton` | `loading()` |
| Exchange-rates | `.er-skel` | `loading()` |
| Dashboard | `.dk-sk-val`, `.dk-sk-row` | `loading()` |
| Imports (historique) | `.im-sk-row` | `historyLoading()` |

### Cas non bloquant
- `imports.component.html` : la zone de **prévisualisation** (après upload) n'a pas de skeleton — mais elle s'affiche uniquement après action utilisateur, pas au chargement initial.

---

## 7. Classes CSS orphelines et collisions

### Users — ~30 classes orphelines (ancienne vue carte)
Le composant est passé en vue tableau mais le CSS conserve toutes les règles de l'ancienne vue carte :
`.us-card`, `.us-card-top`, `.us-top-admin`, `.us-top-agent`, `.us-dot`, `.us-dot-on`, `.us-dot-off`,
`.us-role-chip`, `.us-card-actions`, `.us-act`, `.us-act-profile`, `.us-act-edit`,
`.us-act-deactivate`, `.us-act-activate`, `.us-act-icon` (version carte), `.us-avatar-wrap`,
`.us-card-info`, `.us-card-name`, `.us-card-email`, `.us-card-date`…
**→ Bloc mort à supprimer entièrement.**

### Imports — double définition nm-*
12 classes nm-* redéfinies localement dans `imports.component.css`.
Les définitions locales peuvent diverger silencieusement du global `styles.css`.
**→ Supprimer les redéfinitions locales et utiliser uniquement le global.**

### Profile — copier-coller raté
`.pf-ico-purple` défini avec les couleurs d'orange :
```css
.pf-ico-purple { background: #FFF7ED; color: #F97316; } /* devrait être violet */
```
**→ Corriger vers `background: #F5F3FF; color: #7C3AED`.**

### Clients — classes HTML sans règle CSS
`.cl-act-view`, `.cl-act-edit`, `.cl-act-activate`, `.cl-act-deactivate` présentes dans le HTML
mais **aucune règle CSS** définie pour ces modificateurs.
La classe de base `.cl-act` s'applique correctement, mais ces classes ne servent à rien.
**→ Soit les définir (pour des hover différenciés), soit les retirer du HTML.**

### Dashboard / Shipment-detail — collision de noms
`dashboard.component.css` définit `.ico-orange`, `.ico-blue`, `.ico-green`, `.ico-amber`.
`shipment-detail.component.css` définit aussi `.ico-blue`, `.ico-purple`, `.ico-orange`, `.ico-green` avec des couleurs différentes.
Angular scoped CSS évite normalement les collisions, mais les composants standalone doivent vérifier que ces classes ne fuient pas.

### Email-template — redéfinition `.nm-close`
`.nm-close` redéfini localement avec `background: #f8fafc; border: 1px solid #e2e8f0; outline: 2px solid #007bff`.
En conflit avec le `.nm-close` global `styles.css` qui est `background: none; border: none`.
**→ Renommer la classe locale (ex. `.et-preview-close`) pour éviter la collision.**

---

## Tableau de travail — par fichier à modifier

| Fichier | Actions |
|---------|---------|
| `orders.component.css` | Boutons (taille + fonds permanents) + header table (gradient → flat, 2px → 1px) + KPI cards |
| `orders.component.html` | Remplacer spans badge inline par `<app-badge>` |
| `airlines.component.css` | Boutons (taille + border + transform) + header table (gradient → flat, 2px → 1px) |
| `exchange-rates.component.css` | Boutons + migrer modal vers nm-* |
| `exchange-rates.component.html` | Remplacer spans badge + migrer modal |
| `imports.component.css` | Boutons + supprimer redéfinitions nm-* locales |
| `clients.component.css` | KPI cards (32→44px) + header table (font-weight 500→700, couleur border) |
| `clients.component.html` | Remplacer spans badge inline par `<app-badge>` étendu |
| `shippers.component.css` | Idem clients |
| `shippers.component.html` | Remplacer spans badge inline |
| `users.component.css` | Supprimer ~30 classes carte orphelines + header table |
| `users.component.html` | Évaluer migration badge active/inactive |
| `dashboard.component.css` | KPI cards (36→44px) |
| `email-template.component.css` | Renommer `.nm-close` local → `.et-preview-close` |
| `profile.component.css` | Corriger `.pf-ico-purple` |
