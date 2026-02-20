package me.jackstar.drakestech.listeners;

import me.jackstar.drakescraft.utils.MessageUtils;
import me.jackstar.drakestech.manager.MachineManager;
import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.factory.MachineFactory;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DrakesTechBlockListener implements Listener {

    private final MachineManager machineManager;
    private final MachineFactory machineFactory;

    public DrakesTechBlockListener(MachineManager machineManager, MachineFactory machineFactory) {
        this.machineManager = machineManager;
        this.machineFactory = machineFactory;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null) {
            return;
        }

        String machineId = machineFactory.readMachineId(itemInHand).orElse(null);
        if (machineId == null) {
            return;
        }

        Location location = event.getBlockPlaced().getLocation();
        machineFactory.createMachine(machineId, location).ifPresent(machine -> {
            machineManager.registerMachine(machine);
            MessageUtils.send(event.getPlayer(), "<green>Placed machine: <yellow>" + machineId + "</yellow>.</green>");
        });
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        AbstractMachine machine = machineManager.removeMachineAt(location).orElse(null);
        if (machine == null) {
            return;
        }

        event.setDropItems(false);
        event.setExpToDrop(0);

        Inventory machineInventory = machine.getInventory();
        if (machineInventory != null) {
            for (ItemStack stack : machineInventory.getContents()) {
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                event.getBlock().getWorld().dropItemNaturally(location.clone().add(0.5, 0.5, 0.5), stack.clone());
            }
        }

        machineFactory.createMachineItem(machine.getId())
                .ifPresent(drop -> event.getBlock().getWorld().dropItemNaturally(location.clone().add(0.5, 0.5, 0.5), drop));

        MessageUtils.send(event.getPlayer(), "<red>Machine removed: <yellow>" + machine.getId() + "</yellow>.</red>");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Location location = event.getClickedBlock().getLocation();
        AbstractMachine machine = machineManager.getMachineAt(location).orElse(null);
        if (machine == null) {
            return;
        }

        Inventory inventory = machine.getInventory();
        if (inventory == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        player.openInventory(inventory);
    }
}
