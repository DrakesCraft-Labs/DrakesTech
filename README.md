# DrakesTech

Plugin tecnico/gameplay extraido del modulo `drakestech` del antiguo `DrakesCore`.

## Objetivo
Ser el motor de progresion tech de DrakesCraft: maquinas, energia, automatizacion, guia modular, contenido custom masivo y API para expansiones externas.

## Estado actual
### Core tecnico
- Registro de contenido data-driven desde `tech-content.yml`.
- API publica (`DrakesTechApi`) para addons externos.
- Persistencia de maquinas en `drakestech-machines.yml`.
- Persistencia de research por jugador en `drakestech-research.yml`.
- Tick loop de maquinas + red de energia adyacente.
- Red de transferencia de items entre maquinas adyacentes (inputs/outputs).

### Maquinas incluidas
- `solar_generator`
- `electric_furnace`
- `cobblestone_generator` (vanilla)
- `iron_generator` (vanilla)
- `redstone_generator` (vanilla)
- `tech_storage_chest` (cofre enlazable a maquinas)
- `network_controller`
- `network_bridge`
- `network_import_bus`
- `network_export_bus`
- `network_storage_bus`

### Automatizacion
- Generadores de recursos vanilla con consumo energetico.
- Cofre de almacenamiento tech conectado por adyacencia a maquinas.
- Filtro configurable para que el cofre acepte solo items de DrakesTech o cualquier item.
- Red de nodos con topologia por adyacencia (controller + bridges + buses).
- Import bus: inserta items del bus al storage de red.
- Export bus: retira por template desde la red.
- Storage bus: expone inventario vanilla adyacente a la red.

### Guia / UX
- Libro `DrakesTech Guide` con modulos, busqueda y detalle de recetas.
- Sistema de desbloqueo por XP de modulos/entries.
- Entrega automatica del libro al primer join (configurable).

### Contenido masivo (v2)
- Generador configurable en `tools/content-gen`.
- Soporta perfiles de balance por JSON (`generator-config*.json`).
- Incluye nuevas lineas de progresion:
  - `redstone_alloy_ingot_t*`
  - `hardened_metal_t*`
- Garantia del generador:
  - todo item custom tiene receta de salida
  - todo item custom es alcanzable desde materiales vanilla

## Comandos admin
- `/drakestech give <player> <machine|item> <id> [amount]`
- `/drakestech guide [player]`
- `/drakestech search [player] <query>`
- `/drakestech research <unlock|lock|module|status|list> ...`
- `/drakestech list <machines|items|modules|entries|enchantments|addons> [module_id]`
- `/drakestech diagnostics`
- `/drakestech reload`

## Configuracion
- `src/main/resources/drakestech.yml`
  - guia
  - research
  - transferencia de items
  - comportamiento de Tech Storage Chest
  - settings de red (`network.*`)
- `src/main/resources/tech-content.yml`
  - items, recipes, modules, machines, entries

## Tools de contenido
- `tools/content-gen/generate_massive_tech_content.py`
- `tools/content-gen/generator-config.json`
- `tools/content-gen/generator-config.hardcore.json`

Uso:
```powershell
cd Plugins\DrakesTech
python .\tools\content-gen\generate_massive_tech_content.py
```

## Dependencias
- Paper 1.20.6+
- Java 21
- Slimefun (opcional, para coexistencia/bridge)
- PlaceholderAPI (opcional)
- Vault (opcional)

## Compatibilidad Slimefun
- Plan tecnico: `SLIMEFUN_COMPAT_PLAN.md`
- Asimilacion por addons (investigacion + backlog): `SLIMEFUN_ADDON_ASSIMILATION_PLAN.md`
- Scanner local de features por addon: `tools/slimefun-research/scan_addon_features.ps1`
