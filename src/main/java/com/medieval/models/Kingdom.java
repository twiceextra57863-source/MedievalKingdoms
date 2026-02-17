package com.medieval.models;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.*;

public class Kingdom {
    private int id;
    private String name;
    private UUID leaderUuid;
    private String leaderName;
    private List<UUID> members;
    private Map<UUID, Rank> playerRanks;
    private String color;
    private double treasury;
    
    public Kingdom(String name, UUID leaderUuid) {
        this.id = -1;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.leaderName = Bukkit.getOfflinePlayer(leaderUuid).getName();
        this.members = new ArrayList<>();
        this.playerRanks = new HashMap<>();
        this.color = "§6";
        this.treasury = 1000;
        
        members.add(leaderUuid);
        playerRanks.put(leaderUuid, Rank.KING);
    }
    
    public void addMember(UUID playerUuid, Rank rank) {
        if (!members.contains(playerUuid)) {
            members.add(playerUuid);
            playerRanks.put(playerUuid, rank);
        }
    }
    
    public void removeMember(UUID playerUuid) {
        members.remove(playerUuid);
        playerRanks.remove(playerUuid);
    }
    
    public void promoteMember(UUID playerUuid, Rank newRank) {
        if (members.contains(playerUuid)) {
            playerRanks.put(playerUuid, newRank);
            broadcastToKingdom("§e" + Bukkit.getOfflinePlayer(playerUuid).getName() + 
                " §ahas been promoted to " + newRank.getDisplayName());
        }
    }
    
    public void broadcastToKingdom(String message) {
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
    
    public boolean isLeader(UUID playerUuid) { return leaderUuid.equals(playerUuid); }
    
    public Rank getPlayerRank(UUID playerUuid) {
        return playerRanks.getOrDefault(playerUuid, Rank.PEASANT);
    }
    
    public int getMemberCount() { return members.size(); }
    
    public int getOnlineCount() {
        int online = 0;
        for (UUID uuid : members) {
            if (Bukkit.getPlayer(uuid) != null) online++;
        }
        return online;
    }
    
    public String getColoredName() { return color + name; }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public UUID getLeaderUuid() { return leaderUuid; }
    public void setLeaderUuid(UUID leaderUuid) { 
        this.leaderUuid = leaderUuid;
        this.leaderName = Bukkit.getOfflinePlayer(leaderUuid).getName();
    }
    public String getLeaderName() { return leaderName; }
    public List<UUID> getMembers() { return members; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public double getTreasury() { return treasury; }
    public void setTreasury(double treasury) { this.treasury = treasury; }
}
