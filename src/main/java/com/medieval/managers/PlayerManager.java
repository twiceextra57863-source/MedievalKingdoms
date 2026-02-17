package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.PlayerData;
import com.medieval.models.Rank;
import org.bukkit.entity.Player;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private final MedievalKingdoms plugin;
    private final Map<UUID, PlayerData> playerDataMap;
    
    public PlayerManager(MedievalKingdoms plugin) {
        this.plugin = plugin;
        this.playerDataMap = new ConcurrentHashMap<>();
        loadAllPlayers();
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, k -> {
            PlayerData data = loadPlayerFromDatabase(uuid);
            if (data == null) {
                data = new PlayerData(uuid);
                // Save new player to database
                plugin.getDatabaseManager().executeUpdate(
                    "INSERT INTO players (uuid, name, kingdom_id, rank, gold, reputation, votecount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    uuid.toString(),
                    Bukkit.getOfflinePlayer(uuid).getName(),
                    -1,
                    Rank.PEASANT.name(),
                    100.0,
                    50,
                    0
                );
            }
            return data;
        });
    }
    
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    private PlayerData loadPlayerFromDatabase(UUID uuid) {
        try {
            ResultSet resultSet = plugin.getDatabaseManager().executeQuery(
                "SELECT * FROM players WHERE uuid = ?",
                uuid.toString()
            ).get();
            
            if (resultSet != null && resultSet.next()) {
                PlayerData data = new PlayerData(uuid);
                data.setName(resultSet.getString("name"));
                data.setKingdomId(resultSet.getInt("kingdom_id"));
                data.setRank(Rank.valueOf(resultSet.getString("rank")));
                data.setGold(resultSet.getDouble("gold"));
                data.setReputation(resultSet.getInt("reputation"));
                data.setVoteCount(resultSet.getInt("votecount"));
                return data;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading player data for " + uuid + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET name = ?, kingdom_id = ?, rank = ?, gold = ?, reputation = ?, votecount = ? WHERE uuid = ?",
            data.getName(),
            data.getKingdomId(),
            data.getRank().name(),
            data.getGold(),
            data.getReputation(),
            data.getVoteCount(),
            uuid.toString()
        );
    }
    
    public void saveAllPlayers() {
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
        plugin.getLogger().info("§aSaved " + playerDataMap.size() + " players to database!");
    }
    
    private void loadAllPlayers() {
        plugin.getDatabaseManager().executeQuery("SELECT uuid FROM players").thenAccept(resultSet -> {
            try {
                int count = 0;
                while (resultSet != null && resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    PlayerData data = loadPlayerFromDatabase(uuid);
                    if (data != null) {
                        playerDataMap.put(uuid, data);
                        count++;
                    }
                }
                plugin.getLogger().info("§aLoaded " + count + " players from database!");
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading players: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    public boolean hasEnoughGold(UUID uuid, double amount) {
        PlayerData data = getPlayerData(uuid);
        return data != null && data.getGold() >= amount;
    }
    
    public boolean takeGold(UUID uuid, double amount) {
        PlayerData data = getPlayerData(uuid);
        if (data != null && data.getGold() >= amount) {
            data.setGold(data.getGold() - amount);
            savePlayerData(uuid);
            return true;
        }
        return false;
    }
    
    public void addGold(UUID uuid, double amount) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            data.setGold(data.getGold() + amount);
            savePlayerData(uuid);
        }
    }
    
    public void addReputation(UUID uuid, int amount) {
        PlayerData data = getPlayerData(uuid);
        if (data != null) {
            int newRep = Math.min(100, Math.max(0, data.getReputation() + amount));
            data.setReputation(newRep);
            savePlayerData(uuid);
        }
    }
}
