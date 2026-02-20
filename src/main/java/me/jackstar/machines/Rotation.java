package me.jackstar.drakestech.machines;

public enum Rotation {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    public Rotation rotateClockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }
}
