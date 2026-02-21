# Slimefun Compatibility Plan for DrakesTech

## Goal
Run DrakesTech as main tech ecosystem while supporting migration/interop with Slimefun and selected Slimefun addons.

## Recommended strategy
1. Do not hard-couple DrakesTech to Slimefun internals.
2. Build compatibility as optional bridge plugins that implement `DrakesTechAddon`.
3. Keep DrakesTech API as the stable surface (`DrakesTechApi`).

## Compatibility modes

### Mode A: Coexistence (safe default)
- Keep Slimefun enabled.
- DrakesTech adds its own machines/items/progression.
- No recipe overwrite.
- Add bridge buses where needed (import/export/storage).

### Mode B: Gradual migration
- Mirror important Slimefun item lines into DrakesTech IDs.
- Provide conversion recipes: `slimefun_item -> drakestech_item`.
- Lock new progression in DrakesTech research modules.

### Mode C: Full replacement
- Disable Slimefun content in gameplay.
- Keep optional bridge plugin only for save-data conversion and migration commands.

## Adapter architecture

### Plugin layout
- `DrakesTech-SlimefunBridge` (separate plugin)
- `softdepend: [DrakesTech, Slimefun]`

### Adapter responsibilities
1. Read Slimefun registry items/recipes.
2. Map target items to DrakesTech IDs.
3. Register equivalent DrakesTech items/machines/modules through API.
4. Expose migration commands (`/dt migrate ...`).

### Mapping file (required)
- `slimefun-map.yml`
- Fields:
  - `slimefun-id`
  - `drakestech-id`
  - `mode` (`alias`, `convert`, `replace`)
  - `keep-lore` (`true/false`)

## Recipes and progression
- Use deterministic IDs in DrakesTech (`machine_id`, `item_id`) and never rename after release.
- Keep progression compatible by assigning module unlock costs per tier.
- For mass migration, generate mapping from `docs/SLIMEFUN_ITEM_INDEX.csv`.

## Data safety
- Always backup world and playerdata before migration.
- Dry-run mode first (`/dt migrate dryrun`).
- Write migration logs per player with counters.

## Current status in this branch
- DrakesTech now includes network Phase 1 nodes:
  - `network_controller`
  - `network_bridge`
  - `network_import_bus`
  - `network_export_bus`
  - `network_storage_bus`
- This helps coexistence and migration pipelines with external storage/cargo flows.
