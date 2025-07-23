// Part.java (должен быть в том же пакете)
package net.rezolv.obsidanum.block.custom;

import net.minecraft.util.StringRepresentable;

public enum Part implements StringRepresentable {
    PART_000("000"),
    PART_001("001"),
    PART_010("010"),
    PART_011("011"),
    PART_100("100"),
    PART_101("101"),
    PART_110("110"),
    PART_111("111");

    private final String code;

    Part(String code) {
        this.code = code;
    }

    @Override
    public String getSerializedName() {
        return "part_" + code;
    }
    public static Part fromCoords(int x, int y, int z) {
        for (Part part : values()) {
            if (part.getX() == x && part.getY() == y && part.getZ() == z) {
                return part;
            }
        }
        throw new IllegalArgumentException("Invalid part coordinates: " + x + y + z);
    }
    public int getX() { return code.charAt(0) - '0'; }
    public int getY() { return code.charAt(1) - '0'; }
    public int getZ() { return code.charAt(2) - '0'; }
}