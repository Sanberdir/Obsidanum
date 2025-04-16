package net.rezolv.obsidanum.item.upgrade;

public enum ObsidanumToolUpgrades {
    // укрепление
    STRENGTH("strength"),
    // балансировка
    BALANCING("balancing"),
    // заточка
    SHARPENING("sharpening"),
    // длинная рукоять
    LONG_HANDLE("long_handle"),
    // СОБИРАТЕЛЬ
    HARVESTER("harvester"),
    // археолог
    ARCHAEOLOGIST("archaeologist"),
    // богатый урожай
    RICH_HARVEST("rich_harvest"),
    // ДРОВОСЕК
    WOODCUTTER("woodcutter"),
    // Камнелом
    STONE_BREAKER("stone_breaker");
    private final String name;
    ObsidanumToolUpgrades(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
}
