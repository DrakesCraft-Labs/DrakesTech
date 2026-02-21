# DrakesTech Content Generator v2

Generador masivo de `tech-content.yml` con balance configurable por JSON.

## Archivos
- `generate_massive_tech_content.py`: script principal.
- `generator-config.json`: perfil base editable.
- `generator-config.hardcore.json`: ejemplo de perfil dificil.

## Que puedes configurar
- `max_tier`: cantidad de tiers.
- `enabled_metals`: metales activos para la generacion.
- `tier_catalysts`: catalizadores por tier (si faltan, rota en ciclo).
- `module_unlock_levels`: costo de desbloqueo de modulo en niveles.
- `xp_model`: formula de costo XP por modulo (`base`, `per_tier`, `power`).
- `difficulty_by_module`: multiplicador de dificultad por modulo.
- `output_multiplier_by_family`: multiplicador de outputs por familia de item.

## Incluye por defecto
- Lineas de metales por tier (dust/ingot/plate/wire/coil/gear).
- `redstone_alloy_ingot_t*`.
- `hardened_metal_t*`.
- Power cores, plasma cells, armas y armaduras por tier.
- Maquinas vanilla automation:
  - `cobblestone_generator`
  - `iron_generator`
  - `redstone_generator`
  - `tech_storage_chest`

## Uso
Desde `Plugins/DrakesTech`:

```powershell
python .\tools\content-gen\generate_massive_tech_content.py
```

Con perfil custom:

```powershell
python .\tools\content-gen\generate_massive_tech_content.py --config .\tools\content-gen\generator-config.hardcore.json
```

Guardar el config efectivo fusionado:

```powershell
python .\tools\content-gen\generate_massive_tech_content.py --write-effective-config .\tmp\effective-config.json
```

Salida por defecto:
- `src/main/resources/tech-content.yml`

## Garantias del generador
- Cada item custom tiene receta de salida.
- Cada item custom es alcanzable desde materiales vanilla (validacion de progresion incluida).
