package me.jackstar.drakestech.addon;

import me.jackstar.drakestech.api.DrakesTechApi;

public interface DrakesTechAddon {

    void onDrakesTechLoad(DrakesTechApi api);

    default void onDrakesTechUnload(DrakesTechApi api) {
        // Optional hook.
    }
}
