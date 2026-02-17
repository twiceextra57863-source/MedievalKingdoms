package com.medieval.models;

import java.util.*;

public enum Rank {
    EMPEROR("§6§lEmperor", 10, "rules all kingdoms"),
    KING("§e§lKing", 9, "rules a kingdom"),
    DUKE("§5§lDuke", 8, "rules a duchy"),
    COUNT("§3§lCount", 7, "rules a county"),
    LORD("§2§lLord", 6, "rules a village"),
    KNIGHT("§7§lKnight", 5, "military commander"),
    SQUIRE("§fSquire", 3, "knight in training"),
    PEASANT("§7Peasant", 1, "common worker");
    
    private String displayName;
    private int power;
    private String description;
    
    Rank(String displayName, int power, String description) {
        this.displayName = displayName;
        this.power = power;
        this.description = description;
    }
}

public class Emperor {
    private UUID playerId;
    private String empireName;
    private List<Kingdom> kingdoms;
    private Location throneRoom;
    private double treasury;
    private Date crownedDate;
    private List<UUID> heirs;
    
    public void crownKing(Player king, Kingdom kingdom) {
        // Crown a new king
        // Give them a crown item
        // Announce to all kingdoms
    }
    
    public void declareWar(Emperor enemy) {
        // Empire vs Empire war
        // All kingdoms join
        // Winner takes territories
    }
}

public class Kingdom {
    private UUID kingId;
    private String kingdomName;
    private String banner; // Banner pattern/color
    private List<Noble> nobles;
    private List<Castle> castles;
    private List<Territory> territories;
    private double treasury;
    private int militaryStrength;
    private List<UUID> allies;
    private List<UUID> enemies;
    private Religion stateReligion;
    
    public void addTerritory(Chunk chunk) {
        // Claim new territory
        // Build border markers
        // Add to kingdom map
    }
    
    public void collectTaxes() {
        // Collect from peasants
        // Store in treasury
        // Distribute to nobles
    }
}
