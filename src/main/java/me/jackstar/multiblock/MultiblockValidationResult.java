package me.jackstar.drakestech.multiblock;

public record MultiblockValidationResult(boolean valid, String reason) {

    public static MultiblockValidationResult ok() {
        return new MultiblockValidationResult(true, "OK");
    }

    public static MultiblockValidationResult fail(String reason) {
        return new MultiblockValidationResult(false, reason);
    }
}
