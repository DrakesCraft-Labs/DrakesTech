package me.jackstar.drakestech.multiblock;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import java.util.Map;

public class MultiblockDetector {

    public boolean matches(Location center, Map<Vector, String> structureMap) {
        for (Map.Entry<Vector, String> entry : structureMap.entrySet()) {
            Vector offset = entry.getKey();
            String expectedMaterial = entry.getValue();

            Block block = center.clone().add(offset).getBlock();
            if (!block.getType().name().equalsIgnoreCase(expectedMaterial)) {
                return false;
            }
        }
        return true;
    }
}
