# DrakesTech

Plugin tecnico/gameplay extraido del modulo `drakestech` del antiguo `DrakesCore`.

## Objetivo
Agregar maquinas y logica de energia estilo tech sin mods de cliente.

## Que hace hoy
- Comando admin `/drakestech` para items/maquinas.
- `MachineManager` para ciclo de vida y tick de maquinas.
- `MachineFactory` para construir maquinas por ID.
- NBT/PDC para identificar items custom.
- Listener de bloques para interaccion base.

## Arquitectura
- `machines/`: maquinas abstractas e implementaciones.
- `energy/`: nodos y red energetica.
- `multiblock/`: deteccion base de estructuras.
- `nbt/`: capa de metadatos en items.

## Dependencias
- Paper 1.20.6
- Java 21

## Pendiente real
- Persistencia robusta de estado de maquinas.
- Interfaces GUI de maquinas.
- Balance de energia, recetas y progresion de juego.
- Telemetria para debugging en produccion.
