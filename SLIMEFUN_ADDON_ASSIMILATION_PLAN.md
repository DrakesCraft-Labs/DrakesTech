# DrakesTech - Slimefun Addon Assimilation Plan (2026-02-21)

## Objetivo
Convertir ideas clave de Slimefun + addons en sistemas nativos de DrakesTech sin acoplarse a Slimefun.

## Regla de implementacion
- No copiar codigo literal de addons.
- Reimplementar mecanicas con arquitectura DrakesTech y API propia.
- Mantener progreso crafteable de extremo a extremo (sin callejones sin salida).

## Fuentes tecnicas analizadas (local)
- `AdvancedTech-dev`: herramientas explosivas 4x4 / 5x5 / 7x7.
- `LiteXpansion`: mining drills y consumo de carga.
- `SlimeTinker`: traits de mineria 3x3 y herramientas explosivas.
- `SlimefunWarfare`: armas de fuego y sintetizador de explosivos.
- `MissileWarfare`: misiles, control y eventos de explosion.
- `Networks`: controladores, bridges, buses y grid.
- `Quaptics`: storage/ticker/cache para conexiones complejas.
- `InfinityExpansion`: storage units avanzadas e infinita.

## Lanes de producto para DrakesTech

### 1) Mining Arsenal
Objetivo: reemplazar pico vanilla late-game por linea industrial.
- `drill_mk1_3x3` (energia baja, precision alta).
- `drill_mk2_5x5` (energia media, desgaste moderado).
- `drill_mk3_7x7` (energia alta, cooldown de seguridad).
- `forest_harvester` (tala de arboles por estructura detectada).

Notas:
- Bloquear uso si no hay energia.
- Respetar protecciones de region/claims.
- Integrar con sistema de research XP.

### 2) Ballistics & Explosives
Objetivo: combate tecnico con riesgo/control.
- `impact_charge` (explosivo direccionado, radio pequeno).
- `cluster_charge` (fragmentacion, costo alto).
- `rail_launcher` (arma de energia, sin dano de bloque por defecto).
- `missile_bench` + `guidance_core` (misiles configurables por tier).

Notas:
- Config `block-damage` global y por mundo.
- Integracion futura con WorldGuard/GriefPrevention hooks.

### 3) Network & Storage V2
Objetivo: automatizacion comparable/superior a addons de red.
- `network_controller_t2` (mas nodos, mas throughput).
- `storage_matrix_unit` (almacenamiento por celdas).
- `quantum_bridge` (enlace de larga distancia con costo energetico).
- `crafting_coprocessor` (cola de crafteos automaticos).

Notas:
- Cache invalida por eventos de bloque.
- Telemetria interna de tick cost y nodos activos.

## Backlog priorizado

### P0 (siguiente sprint)
1. Tool framework energizado para herramientas activas (drills).
2. Primer item funcional `drill_mk1_3x3`.
3. Sistema de costo energetico por bloque minado.
4. Guardas de seguridad: region checks + blacklist de bloques.
5. Bench de pruebas de rendimiento en servidor local (TPS impact).

### P1
1. `drill_mk2_5x5` y `drill_mk3_7x7` con curvas de balance.
2. `impact_charge` y `cluster_charge` con perfiles de dano.
3. `network_controller_t2` y `storage_matrix_unit`.
4. UI de recipes estilo grid (consistente con guia tech).

### P2
1. `missile_bench` + `guidance_core`.
2. `quantum_bridge` entre chunks/distancias grandes.
3. `crafting_coprocessor` con jobs y prioridades.
4. Integraciones opcionales de claims y anti-grief avanzadas.

## Criterios de calidad (definition of done)
- Feature tiene receta alcanzable desde cadenas existentes.
- Feature tiene costo energetico y balance configurable.
- Feature tiene flags de seguridad en config.
- Feature tiene test minimo (unit o integration) + prueba en dev-server.
- Feature tiene documentacion en README/changelog.

## Proximo objetivo recomendado
Implementar `drill_mk1_3x3` y dejarlo listo para release como primera pieza de la lane `Mining Arsenal`.

