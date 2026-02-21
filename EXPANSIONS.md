# DrakesTech Expansion API

DrakesTech soporta addons externos para ampliar maquinas, armas, armaduras y encantamientos sin tocar el core.

## 1) Requisitos del addon
- `softdepend: [DrakesTech]` en tu `plugin.yml`.
- Compilar contra la API publica de DrakesTech.

## 2) Metodo recomendado: interfaz `DrakesTechAddon`
Haz que tu clase principal implemente:

```java
import me.jackstar.drakestech.addon.DrakesTechAddon;
import me.jackstar.drakestech.api.DrakesTechApi;

public final class MyTechAddonPlugin extends JavaPlugin implements DrakesTechAddon {

    @Override
    public void onDrakesTechLoad(DrakesTechApi api) {
        // Registrar contenido aqui.
    }

    @Override
    public void onDrakesTechUnload(DrakesTechApi api) {
        // Optional: limpieza propia.
    }
}
```

DrakesTech detecta automaticamente este contrato en `PluginEnableEvent`.

## 3) Metodo alternativo: Bukkit ServicesManager
Si no quieres implementar la interfaz:

```java
DrakesTechApi api = Bukkit.getServicesManager().load(DrakesTechApi.class);
if (api == null) {
    getLogger().warning("DrakesTech API not available");
    return;
}
```

## 4) Registrar modulo de guia
```java
api.registerGuideModule(this, new TechGuideModule(
    "my_weapons",
    "<red><b>My Weapons</b></red>",
    Material.NETHERITE_SWORD,
    List.of("<gray>Custom combat content.</gray>")
));
```

## 5) Registrar maquina custom
```java
MachineDefinition crusher = new MachineDefinition(
    "addon_crusher",
    "machines",
    "<gold><b>Addon Crusher</b></gold>",
    List.of("<gray>Crushes ores faster.</gray>"),
    List.of("<gray>Recipe:</gray> <yellow>Iron + Piston + Redstone</yellow>"),
    new ItemBuilder(Material.PISTON)
        .name("<gold><b>Addon Crusher</b></gold>")
        .lore("<gray>Expansion machine.</gray>")
        .build(),
    location -> new AddonCrusherMachine(location)
);
api.registerMachine(this, crusher);
```

## 6) Registrar entrada de guia (armas/armaduras/items)
```java
api.registerGuideEntry(this, new TechGuideEntry(
    "inferno_blade",
    "my_weapons",
    "<red><b>Inferno Blade</b></red>",
    Material.BLAZE_ROD,
    List.of("<gray>Burn stacks on hit.</gray>"),
    List.of("<gray>Recipe:</gray> <yellow>Blaze Rod + Netherite Sword</yellow>"),
    new ItemBuilder(Material.NETHERITE_SWORD)
        .name("<red><b>Inferno Blade</b></red>")
        .build()
));
```

## 7) Registrar encantamiento custom (metadata)
```java
api.registerEnchantment(this, new TechEnchantmentDefinition(
    "inferno_core",
    "<gradient:red:gold><b>Inferno Core</b></gradient>",
    3,
    List.of("<gray>Increases fire damage for tech weapons.</gray>")
));
```

## 8) Abrir la guia para jugador
```java
api.openGuide(player);
api.openGuideSearch(player, "energy");
```

Consulta rapida de entradas:

```java
List<TechGuideEntry> hits = api.searchGuideEntries("blade");
Optional<TechGuideEntry> exact = api.findGuideEntry("weapons", "inferno_blade");
```

Research API:

```java
boolean unlocked = api.hasUnlockedGuideEntry(player, "weapons", "inferno_blade");
api.unlockGuideEntry(player, "weapons", "inferno_blade");
api.lockGuideEntry(player, "weapons", "inferno_blade");
api.unlockGuideModule(player, "weapons");
Collection<String> keys = api.getUnlockedGuideKeys(player);
```

## 9) Limpieza automatica al desactivar addon
DrakesTech elimina automaticamente el contenido registrado por `owner` cuando un plugin se desactiva.
Esto evita registros huerfanos tras `/reload` o recargas parciales.

## 10) Buenas practicas
- Usa IDs estables y con prefijo: `myaddon_machine_*`, `myaddon_weapon_*`.
- Registra tu contenido en `onDrakesTechLoad`.
- Si cambias IDs, maneja migraciones de persistencia en tu addon.
- Usa `/drakestech diagnostics` y `/drakestech list addons` para validar integracion.
