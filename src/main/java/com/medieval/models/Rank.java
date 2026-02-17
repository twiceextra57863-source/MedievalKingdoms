package com.medieval.models;

public enum Rank {
    KING("§6§lKing", "§6[King] §r", 9, "Ruler of kingdom"),
    NOBLE("§5§lNoble", "§5[Noble] §r", 5, "Aristocrat"),
    KNIGHT("§7§lKnight", "§7[Knight] §r", 4, "Warrior"),
    PEASANT("§7Peasant", "§7[Peasant] §r", 1, "Common worker");
    
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
}
