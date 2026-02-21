# DrakesTech

Plugin tecnico/gameplay extraido del modulo `drakestech` del antiguo `DrakesCore`.

## Objetivo
Ser el motor de progresion tech de DrakesCraft: maquinas, energia, guia modular, contenido custom y API para expansiones externas.

## Que hace hoy
- Maquinas base registradas por API (`solar_generator`, `electric_furnace`).
- Gestion de estado de maquinas con persistencia en `drakestech-machines.yml`.
- Tick loop + transferencia de energia adyacente.
- Guia in-game (`DrakesTech Guide`) con modulos y detalle de recetas.
- Guide v2 con paginacion, busqueda global y navegacion de retorno.
- Research/unlocks por jugador (`drakestech-research.yml`) con bloqueo visual en guide.
- Entrega automatica del libro al primer join (configurable).
- Ciclo de vida de addons (`DrakesTechAddon`) con auto-load y auto-unload.
- Registro data-driven desde `tech-content.yml` (modulos, entries, enchantments, maquinas por template).
- Motor de recetas `TechRecipeEngine` para smelting custom + fallback vanilla.
- Comando admin avanzado:
  - `/drakestech give <player> <machine_id>`
  - `/drakestech guide [player]`
  - `/drakestech search [player] <query>`
  - `/drakestech research <unlock|lock|module|status|list> ...`
  - `/drakestech list <machines|modules|entries|enchantments|addons> [module_id]`
  - `/drakestech diagnostics`
  - `/drakestech reload`
- API publica para addons externos via Bukkit ServicesManager.

## API de extensiones
- Documento: `EXPANSIONS.md`
- Servicio expuesto: `DrakesTechApi`
- Interfaz recomendada: `DrakesTechAddon`
- Permite registrar:
  - Maquinas
  - Modulos de guia
  - Entradas (armas/armaduras/objetos)
  - Encantamientos custom (metadata)

## Arquitectura
- `api/`: contrato publico para plugins externos.
- `bootstrap/`: carga de contenido builtin.
- `guide/`: GUI y libro de guia.
- `machines/`: implementaciones de maquinas.
- `manager/`: ciclo de vida y guardado/carga.
- `nbt/`: identidad de items por PDC.

## Configuracion
- `src/main/resources/drakestech.yml`
- `src/main/resources/tech-content.yml`
- Persistencia runtime:
  - `drakestech-machines.yml`
  - `drakestech-research.yml`
- Opciones actuales: libro guia, auto-entrega y apertura por click derecho.

## Dependencias
- Paper 1.20.6
- Java 21
- PlaceholderAPI (opcional)
- Vault (opcional)

## Siguiente nivel recomendado
- Registrar sistema real de armas/armaduras con stats y cooldowns.
- Encantamientos custom con efecto runtime (no solo metadata).
- Red de energia por cables/multiblock en vez de solo adyacencia directa.
- Persistencia SQL opcional para red multi-servidor.
