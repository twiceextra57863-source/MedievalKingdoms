package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.PlayerData;
import com.medieval.models.Rank;
import org.bukkit.Bukkit;
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
                plugin.getDatabaseManager().executeUpdate(
                    "INSERT INTO players (uuid, name, kingdom_id, rank, gold, reputation, votecount) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    uuid.toString(), Bukkit.getOfflinePlayer(uuid).getName(), -1, 
                    Rank.PEASANT.name(), 100.0, 50, 0
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
            ResultSet rs = plugin.getDatabaseManager().executeQuery(
                "SELECT * FROM players WHERE uuid = ?", uuid.toString()
            );
            
            if (rs.next()) {
                PlayerData data = new PlayerData(uuid);
                data.setKingdomId(rs.getInt("kingdom_id"));
                data.setRank(Rank.valueOf(rs.getString("rank")));
                data.setGold(rs.getDouble("gold"));
                data.setReputation(rs.getInt("reputation"));
                data.setVoteCount(rs.getInt("votecount"));
                return data;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading player: " + e.getMessage());
        }
        return null;
    }
    
    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return;
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET name = ?, kingdom_id = ?, rank = ?, gold = ?, reputation = ?, votecount = ? WHERE uuid = ?",
            data.getName(), data.getKingdomId(), data.getRank().name(), 
            data.getGold(), data.getReputation(), data.getVoteCount(), uuid.toString()
        );
    }
    
    public void saveAllPlayers() {
        for (UUID uuid : playerDataMap.keySet()) {
            savePlayerData(uuid);
        }
    }
    
    private void loadAllPlayers() {
        try {
            ResultSet rs = plugin.getDatabaseManager().executeQuery("SELECT uuid FROM players");
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                loadPlayerFromDatabase(uuid);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading players: " + e.getMessage());
        }
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
