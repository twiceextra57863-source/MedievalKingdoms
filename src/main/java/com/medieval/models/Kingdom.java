package com.medieval.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;

public class Kingdom {
    private int id;
    private final String name;
    private UUID leaderUuid;
    private String leaderName;
    private final List<UUID> members;
    private final List<UUID> nobles;
    private final List<UUID> knights;
    private final Map<UUID, Rank> playerRanks;
    private String color;
    private String banner;
    private double treasury;
    private Location capital;
    private final List<Territory> territories;
    private final List<Alliance> alliances;
    private final KingdomStats stats;
    private final Date created;
    
    public Kingdom(String name, UUID leaderUuid) {
        this.id = -1;
        this.name = name;
        this.leaderUuid = leaderUuid;
        this.leaderName = Bukkit.getOfflinePlayer(leaderUuid).getName();
        this.members = new ArrayList<>();
        this.nobles = new ArrayList<>();
        this.knights = new ArrayList<>();
        this.playerRanks = new HashMap<>();
        this.color = "§6";
        this.banner = "default";
        this.treasury = 1000.0;
        this.territories = new ArrayList<>();
        this.alliances = new ArrayList<>();
        this.stats = new KingdomStats();
        this.created = new Date();
        
        // Add leader as first member with King rank
        members.add(leaderUuid);
        playerRanks.put(leaderUuid, Rank.KING);
    }
    
    public void addMember(UUID playerUuid, Rank rank) {
        if (!members.contains(playerUuid)) {
            members.add(playerUuid);
            playerRanks.put(playerUuid, rank);
            
            if (rank == Rank.NOBLE || rank == Rank.DUKE || rank == Rank.COUNT || rank == Rank.LORD) {
                nobles.add(playerUuid);
            } else if (rank == Rank.KNIGHT) {
                knights.add(playerUuid);
            }
        }
    }
    
    public void removeMember(UUID playerUuid) {
        members.remove(playerUuid);
        nobles.remove(playerUuid);
        knights.remove(playerUuid);
        playerRanks.remove(playerUuid);
    }
    
    public void promoteMember(UUID playerUuid, Rank newRank) {
        if (members.contains(playerUuid)) {
            Rank oldRank = playerRanks.get(playerUuid);
            playerRanks.put(playerUuid, newRank);
            
            // Update noble/knight lists
            if (oldRank == Rank.NOBLE || oldRank == Rank.DUKE || oldRank == Rank.COUNT || oldRank == Rank.LORD) {
                nobles.remove(playerUuid);
            }
            if (oldRank == Rank.KNIGHT) {
                knights.remove(playerUuid);
            }
            
            if (newRank == Rank.NOBLE || newRank == Rank.DUKE || newRank == Rank.COUNT || newRank == Rank.LORD) {
                nobles.add(playerUuid);
            } else if (newRank == Rank.KNIGHT) {
                knights.add(playerUuid);
            }
            
            // Announce promotion
            String message = String.format(
                "§6[Kingdom] §e%s §ahas been promoted to %s",
                Bukkit.getOfflinePlayer(playerUuid).getName(),
                newRank.getDisplayName()
            );
            broadcastToKingdom(message);
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
    
    public boolean isLeader(UUID playerUuid) {
        return leaderUuid.equals(playerUuid);
    }
    
    public boolean isNoble(UUID playerUuid) {
        return nobles.contains(playerUuid) || isLeader(playerUuid);
    }
    
    public boolean isKnight(UUID playerUuid) {
        return knights.contains(playerUuid);
    }
    
    public Rank getPlayerRank(UUID playerUuid) {
        return playerRanks.getOrDefault(playerUuid, Rank.PEASANT);
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    public int getOnlineCount() {
        int online = 0;
        for (UUID uuid : members) {
            if (Bukkit.getPlayer(uuid) != null && Bukkit.getPlayer(uuid).isOnline()) {
                online++;
            }
        }
        return online;
    }
    
    public String getColoredName() {
        return color + name;
    }
    
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
    
    public List<UUID> getMembers() { return new ArrayList<>(members); }
    public List<UUID> getNobles() { return new ArrayList<>(nobles); }
    public List<UUID> getKnights() { return new ArrayList<>(knights); }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public String getBanner() { return banner; }
    public void setBanner(String banner) { this.banner = banner; }
    
    public double getTreasury() { return treasury; }
    public void setTreasury(double treasury) { this.treasury = treasury; }
    public void addToTreasury(double amount) { this.treasury += amount; }
    public boolean removeFromTreasury(double amount) {
        if (treasury >= amount) {
            treasury -= amount;
            return true;
        }
        return false;
    }
    
    public Location getCapital() { return capital; }
    public void setCapital(Location capital) { this.capital = capital; }
    
    public List<Territory> getTerritories() { return new ArrayList<>(territories); }
    public void addTerritory(Territory territory) { territories.add(territory); }
    public void removeTerritory(Territory territory) { territories.remove(territory); }
    
    public List<Alliance> getAlliances() { return new ArrayList<>(alliances); }
    public void addAlliance(Alliance alliance) { alliances.add(alliance); }
    public void removeAlliance(Alliance alliance) { alliances.remove(alliance); }
    
    public KingdomStats getStats() { return stats; }
    
    public Date getCreated() { return created; }
}

class KingdomStats {
    private int warsWon;
    private int warsLost;
    private int territoriesConquered;
    private int territoriesLost;
    private double totalTaxCollected;
    private Date lastWarDate;
    
    public KingdomStats() {
        this.warsWon = 0;
        this.warsLost = 0;
        this.territoriesConquered = 0;
        this.territoriesLost = 0;
        this.totalTaxCollected = 0;
    }
    
    public int getWarsWon() { return warsWon; }
    public void addWarWon() { warsWon++; }
    
    public int getWarsLost() { return warsLost; }
    public void addWarLost() { warsLost++; }
    
    public int getTerritoriesConquered() { return territoriesConquered; }
    public void addTerritoryConquered() { territoriesConquered++; }
    
    public int getTerritoriesLost() { return territoriesLost; }
    public void addTerritoryLost() { territoriesLost++; }
    
    public double getTotalTaxCollected() { return totalTaxCollected; }
    public void addTaxCollected(double amount) { totalTaxCollected += amount; }
    
    public Date getLastWarDate() { return lastWarDate; }
    public void setLastWarDate(Date date) { this.lastWarDate = date; }
}
