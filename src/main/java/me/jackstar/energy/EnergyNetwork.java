package me.jackstar.drakestech.energy;

public interface EnergyNetwork {
    void addSource(EnergyNode node);

    void addSink(EnergyNode node);

    double getTotalStored();

    double getCapacity();

    void tick(); // Distribute energy
}
