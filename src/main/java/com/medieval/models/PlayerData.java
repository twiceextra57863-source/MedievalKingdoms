package com.medieval.models;

import org.bukkit.Bukkit;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private String name;
    private int kingdomId;
    private Rank rank;
    private double gold;
    private int reputation;
    private int voteCount;
    
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.name = Bukkit.getOfflinePlayer(uuid).getName();
        this.kingdomId = -1;
        this.rank = Rank.PEASANT;
        this.gold = 100;
        this.reputation = 50;
        this.voteCount = 0;
    }
    
    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public int getKingdomId() { return kingdomId; }
    public void setKingdomId(int kingdomId) { this.kingdomId = kingdomId; }
    
    public Rank getRank() { return rank; }
    public void setRank(Rank rank) { this.rank = rank; }
    
    public double getGold() { return gold; }
    public void setGold(double gold) { this.gold = gold; }
    
    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }
    
    public int getVoteCount() { return voteCount; }
    public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    public void incrementVoteCount() { this.voteCount++; }
}
