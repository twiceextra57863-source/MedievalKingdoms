package com.medieval.models;

public enum Rank {
    KING("§6§lKing", "§6[King] §r", 9, "Ruler of kingdom"),
    NOBLE("§5§lNoble", "§5[Noble] §r", 5, "Aristocrat"),
    KNIGHT("§7§lKnight", "§7[Knight] §r", 4, "Warrior"),
    SQUIRE("§fSquire", "§f[Squire] §r", 2, "Knight in training"),
    PEASANT("§7Peasant", "§7[Peasant] §r", 1, "Common worker"),
    
    // For noble birth generation
    DUKE("§5§lDuke", "§5[Duke] §r", 8, "High noble"),
    COUNT("§3§lCount", "§3[Count] §r", 7, "Noble"),
    LORD("§2§lLord", "§2[Lord] §r", 6, "Land owner");
    
    private final String displayName;
    private final String prefix;
    private final int power;
    private final String description;
    
    Rank(String displayName, String prefix, int power, String description) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.power = power;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getPrefix() { return prefix; }
    public int getPower() { return power; }
    public String getDescription() { return description; }
    
    public static Rank getByName(String name) {
        for (Rank rank : values()) {
            if (rank.name().equalsIgnoreCase(name)) {
                return rank;
            }
        }
        return PEASANT;
    }
}
