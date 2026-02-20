package me.jackstar.drakestech.commands;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DrakesTechCommand implements CommandExecutor {

    private final MachineFactory machineFactory;

    public DrakesTechCommand(MachineFactory machineFactory) {
        this.machineFactory = machineFactory;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!sender.hasPermission("drakestech.admin")) {
            MessageUtils.send(sender, "<red>You do not have permission to use this command.</red>");
            return true;
        }

        if (args.length < 1 || !"give".equalsIgnoreCase(args[0])) {
            sendUsage(sender, label);
            return true;
        }

        if (args.length < 3) {
            MessageUtils.send(sender, "<red>Usage: /" + label + " give <player> <machine_id></red>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline()) {
            MessageUtils.send(sender, "<red>Player not found or offline.</red>");
            return true;
        }

        String machineId = args[2];
        Optional<ItemStack> machineItem = machineFactory.createMachineItem(machineId);
        if (machineItem.isEmpty()) {
            MessageUtils.send(sender, "<red>Unknown machine id: <gray>" + machineId + "</gray></red>");
            MessageUtils.send(sender, "<yellow>Available: <gray>" + String.join(", ", machineFactory.getSupportedMachineIds()) + "</gray></yellow>");
            return true;
        }

        target.getInventory().addItem(machineItem.get());
        MessageUtils.send(sender, "<green>Given <yellow>" + machineId + "</yellow> to <aqua>" + target.getName() + "</aqua>.</green>");
        MessageUtils.send(target, "<green>You received machine: <yellow>" + machineId + "</yellow>.</green>");
        return true;
    }

    private void sendUsage(CommandSender sender, String label) {
        MessageUtils.send(sender, "<yellow>Usage:</yellow> <gray>/" + label + " give <player> <machine_id></gray>");
        MessageUtils.send(sender, "<yellow>Available:</yellow> <gray>" + String.join(", ", machineFactory.getSupportedMachineIds()) + "</gray>");
    }
}
