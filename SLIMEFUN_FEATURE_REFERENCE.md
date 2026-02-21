# DrakesTech - Slimefun Feature Reference (Curated)

Referencia de clases/rutas detectadas para inspirar implementaciones en DrakesTech.

## Mining 3x3 / 5x5 / 7x7
- `AdvancedTech-dev/.../handheld_digger_1.java` (`ExplosiveTool4x4`)
- `AdvancedTech-dev/.../handheld_digger_2.java` (`ExplosiveTool5x5`)
- `AdvancedTech-dev/.../handheld_digger_3.java` (`ExplosiveTool7x7`)
- `AdvancedTech-dev/.../ExplosiveTool5x5.java`
- `LiteXpansion-master/.../Events.java` (`MiningDrill`, `diamondDrill`)
- `SlimeTinker-master/.../Traits.java` (menciones de mineria 3x3)

## Armas y explosivos
- `SlimefunWarfare-master/.../Setup.java` (`Gun`, `setupExplosives`, `ExplosiveSynthesizer`)
- `MissileWarfare-master/.../MissileWarfare.java` (`MissileController`, `ExplosionEventListener`)
- `SlimeTinker-master/.../Guide.java` (`ToolTemplateExplosive` para varias herramientas)

## Network y storage
- `Networks-master/.../Networks.java` (registro de network controllers/grids)
- `Quaptics-master/.../QuapticStorage.java` + `QuapticTicker.java` (storage/ticker/cache)
- `InfinityExpansion-master/.../items/storage/` (linea de storage units)

## Observaciones clave para DrakesTech
- La mayoria de addons mezcla logica de gameplay con acoplamiento Slimefun interno.
- En DrakesTech conviene separar:
  - `domain`: reglas (energia, alcance, costo)
  - `application`: casos de uso (romper area, disparar, transferir)
  - `infrastructure`: hooks Paper/world/protecciones
  - `presentation`: comandos, GUI, lore/feedback

## Riesgos
- Balance: herramientas en area rompen economia si no hay consumo energetico serio.
- Seguridad: explosivos requieren controles por mundo/region y flags.
- Performance: area-break y projectiles masivos deben limitarse por tick.

