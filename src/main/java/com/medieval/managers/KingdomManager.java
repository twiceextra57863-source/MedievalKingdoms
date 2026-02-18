package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class KingdomManager {
    private final MedievalKingdoms plugin;
    private final Map<Integer, Kingdom> kingdoms;
    private final Map<UUID, Integer> playerKingdomMap;
    private int nextKingdomId;
    
    public KingdomManager(MedievalKingdoms plugin) {
        this.plugin = plugin;
        this.kingdoms = new ConcurrentHashMap<>();
        this.playerKingdomMap = new ConcurrentHashMap<>();
        this.nextKingdomId = 1;
        loadKingdoms();
    }
    
    public Kingdom createKingdom(String name, Player leader) {
        if (getKingdomByName(name) != null) {
            leader.sendMessage("§cA kingdom with that name already exists!");
            return null;
        }
        
        if (getPlayerKingdom(leader.getUniqueId()) != null) {
            leader.sendMessage("§cYou are already in a kingdom!");
            return null;
        }
        
        Kingdom kingdom = new Kingdom(name, leader.getUniqueId());
        kingdom.setId(nextKingdomId++);
        
        kingdoms.put(kingdom.getId(), kingdom);
        playerKingdomMap.put(leader.getUniqueId(), kingdom.getId());
        
        plugin.getDatabaseManager().executeUpdate(
            "INSERT INTO kingdoms (id, name, leader_uuid, color, treasury) VALUES (?, ?, ?, ?, ?)",
            kingdom.getId(), kingdom.getName(), leader.getUniqueId().toString(), 
            kingdom.getColor(), kingdom.getTreasury()
        );
        
        leader.sendMessage("§a§lKingdom created successfully!");
        return kingdom;
    }
    
    public boolean joinKingdom(Player player, String kingdomName) {
        Kingdom kingdom = getKingdomByName(kingdomName);
        if (kingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return false;
        }
        
        if (getPlayerKingdom(player.getUniqueId()) != null) {
            player.sendMessage("§cYou are already in a kingdom!");
            return false;
        }
        
        kingdom.addMember(player.getUniqueId(), Rank.PEASANT);
        playerKingdomMap.put(player.getUniqueId(), kingdom.getId());
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET kingdom_id = ?, rank = ? WHERE uuid = ?",
            kingdom.getId(), Rank.PEASANT.name(), player.getUniqueId().toString()
        );
        
        kingdom.broadcastToKingdom("§e" + player.getName() + " §ahas joined the kingdom!");
        return true;
    }
    
    public boolean leaveKingdom(Player player) {
        Kingdom kingdom = getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return false;
        }
        
        if (kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cYou cannot leave as the King!");
            return false;
        }
        
        kingdom.removeMember(player.getUniqueId());
        playerKingdomMap.remove(player.getUniqueId());
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET kingdom_id = -1, rank = 'PEASANT' WHERE uuid = ?",
            player.getUniqueId().toString()
        );
        
        player.sendMessage("§aYou left the kingdom.");
        return true;
    }
    
    public Kingdom getPlayerKingdom(UUID playerUuid) {
        Integer kingdomId = playerKingdomMap.get(playerUuid);
        if (kingdomId == null) return null;
        return kingdoms.get(kingdomId);
    }
    
    public Kingdom getKingdomByName(String name) {
        for (Kingdom kingdom : kingdoms.values()) {
            if (kingdom.getName().equalsIgnoreCase(name)) {
                return kingdom;
            }
        }
        return null;
    }
    
    public Kingdom getKingdomById(int id) {
        return kingdoms.get(id);
    }
    
    public List<Kingdom> getAllKingdoms() {
        return new ArrayList<>(kingdoms.values());
    }
    
    private void loadKingdoms() {
        try {
            ResultSet rs = plugin.getDatabaseManager().executeQuery("SELECT * FROM kingdoms");
            while (rs.next()) {
                Kingdom kingdom = new Kingdom(
                    rs.getString("name"),
                    UUID.fromString(rs.getString("leader_uuid"))
                );
                kingdom.setId(rs.getInt("id"));
                kingdom.setColor(rs.getString("color"));
                kingdom.setTreasury(rs.getDouble("treasury"));
                
                String world = rs.getString("capital_world");
                if (world != null) {
                    kingdom.setCapital(new Location(
                        Bukkit.getWorld(world),
                        rs.getInt("capital_x"),
                        rs.getInt("capital_y"),
                        rs.getInt("capital_z")
                    ));
                }
                
                kingdoms.put(kingdom.getId(), kingdom);
                playerKingdomMap.put(kingdom.getLeaderUuid(), kingdom.getId());
                
                if (kingdom.getId() >= nextKingdomId) {
                    nextKingdomId = kingdom.getId() + 1;
                }
            }
            
            // Load members
            for (Kingdom kingdom : kingdoms.values()) {
                ResultSet memberRs = plugin.getDatabaseManager().executeQuery(
                    "SELECT * FROM players WHERE kingdom_id = ?", kingdom.getId()
                );
                while (memberRs.next()) {
                    UUID uuid = UUID.fromString(memberRs.getString("uuid"));
                    Rank rank = Rank.valueOf(memberRs.getString("rank"));
                    kingdom.addMember(uuid, rank);
                    playerKingdomMap.put(uuid, kingdom.getId());
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading kingdoms: " + e.getMessage());
        }
    }
}
