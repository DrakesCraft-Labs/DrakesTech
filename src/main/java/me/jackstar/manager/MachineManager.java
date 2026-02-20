package me.jackstar.drakestech.manager;

import me.jackstar.drakestech.energy.EnergyNode;
import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MachineManager {

    private static final double MAX_TRANSFER_PER_TICK = 20.0D;
    private static final long AUTOSAVE_INTERVAL_TICKS = 200L;

    private final JavaPlugin plugin;
    private final MachineFactory machineFactory;
    private final Map<Location, AbstractMachine> machines = new ConcurrentHashMap<>();
    private final File dataFile;
    private BukkitTask tickTask;
    private long ticksSinceLastSave;

    public MachineManager(JavaPlugin plugin, MachineFactory machineFactory) {
        this.plugin = plugin;
        this.machineFactory = machineFactory;
        this.dataFile = new File(plugin.getDataFolder(), "drakestech-machines.yml");
    }

    public void start() {
        if (tickTask != null) {
            return;
        }

        loadMachines();
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            tickMachines();
            transferEnergyAdjacent();
            ticksSinceLastSave++;
            if (ticksSinceLastSave >= AUTOSAVE_INTERVAL_TICKS) {
                saveMachines();
                ticksSinceLastSave = 0L;
            }
        }, 1L, 1L);
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        saveMachines();
    }

    public void registerMachine(AbstractMachine machine) {
        registerMachine(machine, true);
    }

    private void registerMachine(AbstractMachine machine, boolean persist) {
        if (machine == null || machine.getLocation() == null || machine.getLocation().getWorld() == null) {
            return;
        }
        machines.put(normalize(machine.getLocation()), machine);
        if (persist) {
            saveMachines();
        }
    }

    public Optional<AbstractMachine> getMachineAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(machines.get(normalize(location)));
    }

    public Optional<AbstractMachine> removeMachineAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        AbstractMachine removed = machines.remove(normalize(location));
        if (removed != null) {
            saveMachines();
        }
        return Optional.ofNullable(removed);
    }

    public Collection<AbstractMachine> getMachines() {
        return Collections.unmodifiableCollection(machines.values());
    }

    private void tickMachines() {
        for (AbstractMachine machine : machines.values()) {
            try {
                machine.tick();
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING,
                        "Machine tick failed at " + machine.getLocation() + " (" + machine.getId() + ").", ex);
            }
        }
    }

    private void transferEnergyAdjacent() {
        for (AbstractMachine sourceMachine : machines.values()) {
            if (!(sourceMachine instanceof EnergyNode source) || !source.canExtract() || source.getStoredEnergy() <= 0) {
                continue;
            }

            for (BlockFace face : new BlockFace[] {
                    BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
            }) {
                Location adjacent = sourceMachine.getLocation().getBlock().getRelative(face).getLocation();
                AbstractMachine sinkMachine = machines.get(normalize(adjacent));
                if (!(sinkMachine instanceof EnergyNode sink) || sinkMachine == sourceMachine || !sink.canReceive()) {
                    continue;
                }

                double space = sink.getMaxEnergy() - sink.getStoredEnergy();
                if (space <= 0) {
                    continue;
                }

                double transferRequest = Math.min(MAX_TRANSFER_PER_TICK, Math.min(space, source.getStoredEnergy()));
                if (transferRequest <= 0) {
                    continue;
                }

                double extracted = source.extractEnergy(transferRequest);
                if (extracted <= 0) {
                    continue;
                }

                double before = sink.getStoredEnergy();
                sink.receiveEnergy(extracted);
                double accepted = Math.max(0, sink.getStoredEnergy() - before);
                if (accepted < extracted) {
                    source.receiveEnergy(extracted - accepted);
                }
            }
        }
    }

    private Location normalize(Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private void loadMachines() {
        machines.clear();

        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection machinesSection = config.getConfigurationSection("machines");
        if (machinesSection == null) {
            return;
        }

        for (String key : machinesSection.getKeys(false)) {
            try {
                ConfigurationSection section = machinesSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }

                String machineId = section.getString("id");
                String worldName = section.getString("world");
                World world = worldName == null ? null : plugin.getServer().getWorld(worldName);
                if (machineId == null || world == null) {
                    continue;
                }

                int x = section.getInt("x");
                int y = section.getInt("y");
                int z = section.getInt("z");
                Location location = new Location(world, x, y, z);

                AbstractMachine machine = machineFactory.createMachine(machineId, location).orElse(null);
                if (machine == null) {
                    continue;
                }

                if (machine instanceof EnergyNode energyNode) {
                    double savedEnergy = Math.max(0.0D, section.getDouble("energy", 0.0D));
                    energyNode.receiveEnergy(Math.min(savedEnergy, energyNode.getMaxEnergy()));
                }

                Inventory inventory = machine.getInventory();
                ConfigurationSection invSection = section.getConfigurationSection("inventory");
                if (inventory != null && invSection != null) {
                    for (String slotKey : invSection.getKeys(false)) {
                        int slot;
                        try {
                            slot = Integer.parseInt(slotKey);
                        } catch (NumberFormatException ignored) {
                            continue;
                        }
                        if (slot < 0 || slot >= inventory.getSize()) {
                            continue;
                        }
                        ItemStack stack = invSection.getItemStack(slotKey);
                        if (stack != null && !stack.getType().isAir()) {
                            inventory.setItem(slot, stack);
                        }
                    }
                }

                registerMachine(machine, false);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Failed to load machine entry '" + key + "'.", ex);
            }
        }
    }

    public void saveMachines() {
        try {
            if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder for DrakesTech persistence.");
                return;
            }

            YamlConfiguration config = new YamlConfiguration();
            int index = 0;
            for (AbstractMachine machine : machines.values()) {
                String path = "machines." + index;
                Location location = machine.getLocation();
                if (location == null || location.getWorld() == null) {
                    continue;
                }

                config.set(path + ".id", machine.getId());
                config.set(path + ".world", location.getWorld().getName());
                config.set(path + ".x", location.getBlockX());
                config.set(path + ".y", location.getBlockY());
                config.set(path + ".z", location.getBlockZ());

                if (machine instanceof EnergyNode energyNode) {
                    config.set(path + ".energy", energyNode.getStoredEnergy());
                }

                Inventory inventory = machine.getInventory();
                if (inventory != null) {
                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        ItemStack stack = inventory.getItem(slot);
                        if (stack == null || stack.getType().isAir()) {
                            continue;
                        }
                        config.set(path + ".inventory." + slot, stack);
                    }
                }
                index++;
            }

            config.save(dataFile);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save DrakesTech machines.", ex);
        }
    }
}
