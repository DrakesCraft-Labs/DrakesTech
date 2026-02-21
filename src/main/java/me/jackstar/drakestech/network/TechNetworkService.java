package me.jackstar.drakestech.network;

import me.jackstar.drakestech.config.DrakesTechSettings;
import me.jackstar.drakestech.machines.AbstractMachine;
import me.jackstar.drakestech.machines.impl.NetworkBridgeMachine;
import me.jackstar.drakestech.machines.impl.NetworkControllerMachine;
import me.jackstar.drakestech.machines.impl.NetworkExportBusMachine;
import me.jackstar.drakestech.machines.impl.NetworkImportBusMachine;
import me.jackstar.drakestech.machines.impl.NetworkStorageBusMachine;
import me.jackstar.drakestech.machines.impl.TechStorageChestMachine;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TechNetworkService {

    private final DrakesTechSettings settings;

    private final Map<String, NetworkGraph> networks = new ConcurrentHashMap<>();
    private final Map<Location, String> locationToNetwork = new ConcurrentHashMap<>();

    public TechNetworkService(DrakesTechSettings settings) {
        this.settings = settings;
    }

    public void tick(Collection<AbstractMachine> machines) {
        if (!settings.isNetworkEnabled()) {
            clear();
            return;
        }

        if (machines == null || machines.isEmpty()) {
            clear();
            return;
        }

        Map<Location, AbstractMachine> byLocation = indexMachines(machines);
        rebuild(byLocation);
        processNetworks(byLocation);
    }

    public void clear() {
        networks.clear();
        locationToNetwork.clear();
    }

    public Optional<String> getNetworkId(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(locationToNetwork.get(normalize(location)));
    }

    public int getNetworkCount() {
        return networks.size();
    }

    public int getMappedNodeCount() {
        return locationToNetwork.size();
    }

    public Set<String> getNetworkIds() {
        return Collections.unmodifiableSet(new HashSet<>(networks.keySet()));
    }

    private Map<Location, AbstractMachine> indexMachines(Collection<AbstractMachine> machines) {
        Map<Location, AbstractMachine> byLocation = new HashMap<>();
        for (AbstractMachine machine : machines) {
            if (machine == null || machine.getLocation() == null || machine.getLocation().getWorld() == null) {
                continue;
            }
            byLocation.put(normalize(machine.getLocation()), machine);
        }
        return byLocation;
    }

    private void rebuild(Map<Location, AbstractMachine> byLocation) {
        networks.clear();
        locationToNetwork.clear();

        List<Location> controllers = new ArrayList<>();
        for (Map.Entry<Location, AbstractMachine> entry : byLocation.entrySet()) {
            if (entry.getValue() instanceof NetworkControllerMachine) {
                controllers.add(entry.getKey());
            }
        }

        controllers.sort(Comparator
                .comparing((Location location) -> location.getWorld() == null ? "" : location.getWorld().getName())
                .thenComparingInt(Location::getBlockX)
                .thenComparingInt(Location::getBlockY)
                .thenComparingInt(Location::getBlockZ));

        Set<Location> globallyAssigned = new HashSet<>();
        int maxNodes = Math.max(8, settings.getNetworkMaxNodesPerNetwork());

        for (Location controllerLocation : controllers) {
            if (globallyAssigned.contains(controllerLocation)) {
                continue;
            }

            String networkId = toNetworkId(controllerLocation);
            NetworkGraph graph = new NetworkGraph(networkId, controllerLocation);

            ArrayDeque<Location> queue = new ArrayDeque<>();
            queue.add(controllerLocation);

            while (!queue.isEmpty()) {
                Location current = queue.poll();
                if (graph.members.contains(current) || graph.members.size() >= maxNodes) {
                    continue;
                }

                AbstractMachine currentMachine = byLocation.get(current);
                if (!isNetworkMemberMachine(currentMachine)) {
                    continue;
                }

                // Do not merge two controllers into one graph.
                if (currentMachine instanceof NetworkControllerMachine && !current.equals(controllerLocation)) {
                    continue;
                }

                graph.members.add(current);
                globallyAssigned.add(current);
                locationToNetwork.put(current, networkId);

                classifyNode(currentMachine, current, graph);

                for (BlockFace face : cardinalFaces()) {
                    Location adjacent = normalize(current.getBlock().getRelative(face).getLocation());
                    if (!graph.members.contains(adjacent)) {
                        AbstractMachine adjacentMachine = byLocation.get(adjacent);
                        if (isNetworkMemberMachine(adjacentMachine)) {
                            queue.add(adjacent);
                        }
                    }
                }
            }

            networks.put(networkId, graph);
        }
    }

    private void processNetworks(Map<Location, AbstractMachine> byLocation) {
        for (NetworkGraph graph : networks.values()) {
            List<StorageProvider> providers = resolveProviders(graph, byLocation);
            processImportBuses(graph, byLocation, providers);
            processExportBuses(graph, byLocation, providers);
        }
    }

    private void processImportBuses(NetworkGraph graph,
            Map<Location, AbstractMachine> byLocation,
            List<StorageProvider> providers) {
        for (Location location : graph.importBusLocations) {
            AbstractMachine machine = byLocation.get(location);
            if (!(machine instanceof NetworkImportBusMachine importBus)) {
                continue;
            }

            Inventory inventory = importBus.getInventory();
            if (inventory == null) {
                continue;
            }

            for (int slot : importBus.getInputSlots()) {
                if (slot < 0 || slot >= inventory.getSize()) {
                    continue;
                }

                ItemStack stack = inventory.getItem(slot);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }

                pushIntoProviders(stack, providers);
                if (stack.getAmount() <= 0) {
                    inventory.setItem(slot, null);
                } else {
                    inventory.setItem(slot, stack);
                }
            }
        }
    }

    private void processExportBuses(NetworkGraph graph,
            Map<Location, AbstractMachine> byLocation,
            List<StorageProvider> providers) {
        for (Location location : graph.exportBusLocations) {
            AbstractMachine machine = byLocation.get(location);
            if (!(machine instanceof NetworkExportBusMachine exportBus)) {
                continue;
            }

            Inventory inventory = exportBus.getInventory();
            if (inventory == null) {
                continue;
            }

            ItemStack output = inventory.getItem(NetworkExportBusMachine.OUTPUT_SLOT);
            if (output != null && !output.getType().isAir()) {
                continue;
            }

            ItemStack template = inventory.getItem(NetworkExportBusMachine.TEMPLATE_SLOT);
            if (template == null || template.getType().isAir()) {
                continue;
            }

            int maxRequest = Math.min(template.getMaxStackSize(), exportBus.getMaxItemsPerCycle());
            if (maxRequest <= 0) {
                continue;
            }

            ItemStack fetched = pullFromProviders(template, maxRequest, providers);
            if (fetched == null || fetched.getType().isAir() || fetched.getAmount() <= 0) {
                continue;
            }

            inventory.setItem(NetworkExportBusMachine.OUTPUT_SLOT, fetched);
        }
    }

    private List<StorageProvider> resolveProviders(NetworkGraph graph, Map<Location, AbstractMachine> byLocation) {
        List<StorageProvider> providers = new ArrayList<>();
        Set<Location> machineLocations = byLocation.keySet();

        for (Location location : graph.techStorageLocations) {
            AbstractMachine machine = byLocation.get(location);
            if (!(machine instanceof TechStorageChestMachine storageChest)) {
                continue;
            }
            Inventory inventory = storageChest.getInventory();
            if (inventory == null) {
                continue;
            }
            providers.add(new StorageProvider(inventory,
                    storageChest.getInputSlots(),
                    storageChest.getOutputSlots(),
                    storageChest::canAcceptInput));
        }

        for (Location location : graph.storageBusLocations) {
            AbstractMachine machine = byLocation.get(location);
            if (!(machine instanceof NetworkStorageBusMachine storageBus)) {
                continue;
            }

            Inventory inventory = storageBus.resolveTargetInventory(machineLocations).orElse(null);
            if (inventory == null) {
                continue;
            }

            int[] allSlots = buildAllSlots(inventory.getSize());
            providers.add(new StorageProvider(inventory, allSlots, allSlots, (slot, stack) -> true));
        }

        return providers;
    }

    private void pushIntoProviders(ItemStack incoming, List<StorageProvider> providers) {
        if (incoming == null || incoming.getType().isAir() || incoming.getAmount() <= 0) {
            return;
        }

        // First pass: fill matching stacks.
        for (StorageProvider provider : providers) {
            mergeIntoProvider(incoming, provider, true);
            if (incoming.getAmount() <= 0) {
                return;
            }
        }

        // Second pass: use empty slots.
        for (StorageProvider provider : providers) {
            mergeIntoProvider(incoming, provider, false);
            if (incoming.getAmount() <= 0) {
                return;
            }
        }
    }

    private void mergeIntoProvider(ItemStack incoming, StorageProvider provider, boolean mergeOnly) {
        Inventory inventory = provider.inventory();
        for (int slot : provider.inputSlots()) {
            if (incoming.getAmount() <= 0) {
                return;
            }
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            if (!provider.filter().canInsert(slot, incoming.clone())) {
                continue;
            }

            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                if (mergeOnly) {
                    continue;
                }
                int move = Math.min(incoming.getAmount(), incoming.getMaxStackSize());
                if (move <= 0) {
                    continue;
                }
                ItemStack placed = incoming.clone();
                placed.setAmount(move);
                inventory.setItem(slot, placed);
                incoming.setAmount(incoming.getAmount() - move);
                continue;
            }

            if (!existing.isSimilar(incoming)) {
                continue;
            }

            int free = existing.getMaxStackSize() - existing.getAmount();
            if (free <= 0) {
                continue;
            }

            int move = Math.min(free, incoming.getAmount());
            existing.setAmount(existing.getAmount() + move);
            inventory.setItem(slot, existing);
            incoming.setAmount(incoming.getAmount() - move);
        }
    }

    private ItemStack pullFromProviders(ItemStack template, int amount, List<StorageProvider> providers) {
        if (template == null || template.getType().isAir() || amount <= 0) {
            return null;
        }

        ItemStack result = null;
        int remaining = amount;

        for (StorageProvider provider : providers) {
            Inventory inventory = provider.inventory();
            for (int slot : provider.outputSlots()) {
                if (remaining <= 0) {
                    break;
                }
                if (slot < 0 || slot >= inventory.getSize()) {
                    continue;
                }

                ItemStack existing = inventory.getItem(slot);
                if (existing == null || existing.getType().isAir() || !existing.isSimilar(template)) {
                    continue;
                }

                int take = Math.min(existing.getAmount(), remaining);
                if (take <= 0) {
                    continue;
                }

                if (result == null) {
                    result = existing.clone();
                    result.setAmount(0);
                }

                result.setAmount(result.getAmount() + take);
                existing.setAmount(existing.getAmount() - take);
                if (existing.getAmount() <= 0) {
                    inventory.setItem(slot, null);
                } else {
                    inventory.setItem(slot, existing);
                }

                remaining -= take;
            }

            if (remaining <= 0) {
                break;
            }
        }

        return result;
    }

    private boolean isNetworkMemberMachine(AbstractMachine machine) {
        return machine instanceof NetworkControllerMachine
                || machine instanceof NetworkBridgeMachine
                || machine instanceof NetworkImportBusMachine
                || machine instanceof NetworkExportBusMachine
                || machine instanceof NetworkStorageBusMachine
                || machine instanceof TechStorageChestMachine;
    }

    private void classifyNode(AbstractMachine machine, Location location, NetworkGraph graph) {
        if (machine instanceof NetworkImportBusMachine) {
            graph.importBusLocations.add(location);
        }
        if (machine instanceof NetworkExportBusMachine) {
            graph.exportBusLocations.add(location);
        }
        if (machine instanceof NetworkStorageBusMachine) {
            graph.storageBusLocations.add(location);
        }
        if (machine instanceof TechStorageChestMachine) {
            graph.techStorageLocations.add(location);
        }
    }

    private String toNetworkId(Location location) {
        return (location.getWorld() == null ? "world" : location.getWorld().getName())
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
    }

    private Location normalize(Location location) {
        return new Location(
                location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private int[] buildAllSlots(int size) {
        int[] slots = new int[size];
        for (int i = 0; i < size; i++) {
            slots[i] = i;
        }
        return slots;
    }

    private BlockFace[] cardinalFaces() {
        return new BlockFace[] {
                BlockFace.NORTH,
                BlockFace.EAST,
                BlockFace.SOUTH,
                BlockFace.WEST,
                BlockFace.UP,
                BlockFace.DOWN
        };
    }

    private static final class NetworkGraph {
        private final String id;
        private final Location controller;
        private final Set<Location> members = new HashSet<>();
        private final Set<Location> importBusLocations = new HashSet<>();
        private final Set<Location> exportBusLocations = new HashSet<>();
        private final Set<Location> storageBusLocations = new HashSet<>();
        private final Set<Location> techStorageLocations = new HashSet<>();

        private NetworkGraph(String id, Location controller) {
            this.id = id;
            this.controller = controller;
        }
    }

    private record StorageProvider(Inventory inventory,
            int[] inputSlots,
            int[] outputSlots,
            SlotInsertFilter filter) {
    }

    @FunctionalInterface
    private interface SlotInsertFilter {
        boolean canInsert(int slot, ItemStack stack);
    }
}

