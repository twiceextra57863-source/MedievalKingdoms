package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.*;
import org.bukkit.entity.Player;
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
            }
            return data;
        });
    }
    
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    private PlayerData loadPlayerFromDatabase(UUID uuid) {
        try {
            var future = plugin.getDatabaseManager().executeQuery(
                "SELECT * FROM players WHERE uuid = ?",
                uuid.toString()
            );
            
            var resultSet = future.get();
            if (resultSet != null && resultSet.next()) {
                PlayerData data = new PlayerData(uuid);
                data.setKingdomId(resultSet.getInt("kingdom_id"));
                data.setRank(Rank.valueOf(resultSet.getString("rank")));
                data.setGold(resultSet.getDouble("gold"));
                data.setReputation(resultSet.getInt("reputation"));
                data.setVoteCount(resultSet.getInt("votecount"));
                return data;
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading player data: " + e.getMessage());
        }
        return null;
    }
    
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;
        
        plugin.getDatabaseManager().executeUpdate(
            "INSERT INTO players (uuid, name, kingdom_id, rank, gold, reputation, votecount) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE " +
            "name = VALUES(name), kingdom_id = VALUES(kingdom_id), rank = VALUES(rank), " +
            "gold = VALUES(gold), reputation = VALUES(reputation), votecount = VALUES(votecount)",
            uuid.toString(),
            data.getName(),
            data.getKingdomId(),
            data.getRank().name(),
            data.getGold(),
            data.getReputation(),
            data.getVoteCount()
        );
    }
    
    public void saveAllPlayers() {
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
    }
    
    private void loadAllPlayers() {
        plugin.getDatabaseManager().executeQuery("SELECT uuid FROM players").thenAccept(resultSet -> {
            try {
                while (resultSet != null && resultSet.next()) {
                    UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                    loadPlayerFromDatabase(uuid);
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading players: " + e.getMessage());
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
}
