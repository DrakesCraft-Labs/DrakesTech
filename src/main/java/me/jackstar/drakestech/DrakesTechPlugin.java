package me.jackstar.drakestech;

import me.jackstar.drakestech.commands.DrakesTechCommand;
import me.jackstar.drakestech.listeners.DrakesTechBlockListener;
import me.jackstar.drakestech.manager.MachineManager;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import me.jackstar.drakestech.nbt.NbtItemHandler;
import me.jackstar.drakestech.nbt.PdcNbtItemHandler;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class DrakesTechPlugin extends JavaPlugin {

    private MachineManager machineManager;

    @Override
    public void onEnable() {
        NbtItemHandler nbtItemHandler = new PdcNbtItemHandler(this);
        MachineFactory machineFactory = new MachineFactory(nbtItemHandler);
        machineManager = new MachineManager(this, machineFactory);
        machineManager.start();

        PluginCommand drakesTechCommand = getCommand("drakestech");
        if (drakesTechCommand != null) {
            drakesTechCommand.setExecutor(new DrakesTechCommand(machineFactory));
        } else {
            getLogger().warning("Command 'drakestech' not found in plugin.yml.");
        }

        getServer().getPluginManager().registerEvents(
                new DrakesTechBlockListener(machineManager, machineFactory),
                this);
    }

    @Override
    public void onDisable() {
        if (machineManager != null) {
            machineManager.stop();
        }
    }
}
