package net.rezolv.obsidanum.block.enum_blocks;

import net.minecraft.util.StringRepresentable;

public enum ScrollType implements StringRepresentable {
    NONE,
    UPDATE_NETHER,
    UPDATE_CATACOMBS,
    UPDATE_ORDER,
    REPAIR,
    NETHER,
    ORDER,
    DESTRUCTION,
    CATACOMBS;

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase();
    }
}