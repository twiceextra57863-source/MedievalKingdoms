package com.medieval.models;

public enum Rank {
    EMPEROR("§6§lEmperor", "§6[Emperor] §r", 10, "§6", "Can rule all kingdoms"),
    KING("§e§lKing", "§e[King] §r", 9, "§e", "Can manage kingdom, start wars"),
    DUKE("§5§lDuke", "§5[Duke] §r", 8, "§5", "Can manage territories"),
    COUNT("§3§lCount", "§3[Count] §r", 7, "§3", "Can manage castles"),
    LORD("§2§lLord", "§2[Lord] §r", 6, "§2", "Can manage villages"),
    NOBLE("§dNoble", "§d[Noble] §r", 5, "§d", "Can start elections"),
    KNIGHT("§7§lKnight", "§7[Knight] §r", 4, "§7", "Can participate in wars"),
    SQUIRE("§fSquire", "§f[Squire] §r", 2, "§f", "In training"),
    PEASANT("§7Peasant", "§7[Peasant] §r", 1, "§7", "Can work and earn");
    
    private final String displayName;
    private final String prefix;
    private final int power;
    private final String color;
    private final String description;
    
    Rank(String displayName, String prefix, int power, String color, String description) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.power = power;
        this.color = color;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public int getPower() {
        return power;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static Rank getByName(String name) {
        for (Rank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return PEASANT;
    }
}
