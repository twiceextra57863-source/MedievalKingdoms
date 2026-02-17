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
        // Check if name already exists
        if (getKingdomByName(name) != null) {
            leader.sendMessage("§cA kingdom with that name already exists!");
            return null;
        }
        
        // Check if player is already in a kingdom
        if (getPlayerKingdom(leader.getUniqueId()) != null) {
            leader.sendMessage("§cYou are already in a kingdom!");
            return null;
        }
        
        // Check create cost
        double createCost = plugin.getConfig().getDouble("kingdom.create-cost", 1000);
        PlayerData leaderData = plugin.getPlayerManager().getPlayerData(leader);
        
        if (leaderData.getGold() < createCost) {
            leader.sendMessage("§cYou need §6" + createCost + " gold §cto create a kingdom!");
            return null;
        }
        
        // Take gold
        leaderData.setGold(leaderData.getGold() - createCost);
        
        // Create new kingdom
        Kingdom kingdom = new Kingdom(name, leader.getUniqueId());
        kingdom.setId(nextKingdomId++);
        
        // Save to map
        kingdoms.put(kingdom.getId(), kingdom);
        playerKingdomMap.put(leader.getUniqueId(), kingdom.getId());
        
        // Update player data
        leaderData.setKingdomId(kingdom.getId());
        leaderData.setRank(Rank.KING);
        
        // Save to database
        plugin.getDatabaseManager().executeUpdate(
            "INSERT INTO kingdoms (id, name, leader_uuid, color, treasury) VALUES (?, ?, ?, ?, ?)",
            kingdom.getId(),
            kingdom.getName(),
            leader.getUniqueId().toString(),
            kingdom.getColor(),
            kingdom.getTreasury()
        );
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET kingdom_id = ?, rank = ? WHERE uuid = ?",
            kingdom.getId(),
            Rank.KING.name(),
            leader.getUniqueId().toString()
        );
        
        // Announce
        leader.sendMessage("§a§lKingdom created successfully!");
        leader.sendMessage("§7You are now the King of §e" + name);
        
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
        
        // Add player to kingdom
        kingdom.addMember(player.getUniqueId(), Rank.PEASANT);
        playerKingdomMap.put(player.getUniqueId(), kingdom.getId());
        
        // Update player data
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        playerData.setKingdomId(kingdom.getId());
        playerData.setRank(Rank.PEASANT);
        
        // Update database
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET kingdom_id = ?, rank = ? WHERE uuid = ?",
            kingdom.getId(),
            Rank.PEASANT.name(),
            player.getUniqueId().toString()
        );
        
        // Announce
        kingdom.broadcastToKingdom("§e" + player.getName() + " §ahas joined the kingdom!");
        player.sendMessage("§aYou joined §e" + kingdomName);
        
        return true;
    }
    
    public boolean leaveKingdom(Player player) {
        Kingdom kingdom = getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return false;
        }
        
        // Check if player is king
        if (kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cYou cannot leave as the King! Either transfer leadership or disband the kingdom.");
            return false;
        }
        
        // Remove from kingdom
        kingdom.removeMember(player.getUniqueId());
        playerKingdomMap.remove(player.getUniqueId());
        
        // Update player data
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        playerData.setKingdomId(-1);
        playerData.setRank(Rank.PEASANT);
        
        // Update database
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET kingdom_id = -1, rank = 'PEASANT' WHERE uuid = ?",
            player.getUniqueId().toString()
        );
        
        // Announce
        kingdom.broadcastToKingdom("§e" + player.getName() + " §chas left the kingdom!");
        player.sendMessage("§aYou left the kingdom.");
        
        return true;
    }
    
    public boolean disbandKingdom(Player player) {
        Kingdom kingdom = getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return false;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can disband the kingdom!");
            return false;
        }
        
        // Announce to all members
        kingdom.broadcastToKingdom("§c§l" + kingdom.getName() + " has been disbanded by King " + player.getName());
        
        // Update all members' player data
        for (UUID uuid : kingdom.getMembers()) {
            playerKingdomMap.remove(uuid);
            PlayerData data = plugin.getPlayerManager().getPlayerData(uuid);
            if (data != null) {
                data.setKingdomId(-1);
                data.setRank(Rank.PEASANT);
            }
        }
        
        // Update database
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET kingdom_id = -1, rank = 'PEASANT' WHERE kingdom_id = ?",
            kingdom.getId()
        );
        
        plugin.getDatabaseManager().executeUpdate(
            "DELETE FROM kingdoms WHERE id = ?",
            kingdom.getId()
        );
        
        // Remove from map
        kingdoms.remove(kingdom.getId());
        
        player.sendMessage("§aKingdom disbanded successfully!");
        
        return true;
    }
    
    public boolean transferLeadership(Player currentKing, Player newKing) {
        Kingdom kingdom = getPlayerKingdom(currentKing.getUniqueId());
        if (kingdom == null || !kingdom.isLeader(currentKing.getUniqueId())) {
            currentKing.sendMessage("§cYou are not the king!");
            return false;
        }
        
        if (!kingdom.getMembers().contains(newKing.getUniqueId())) {
            currentKing.sendMessage("§cThat player is not in your kingdom!");
            return false;
        }
        
        // Transfer leadership
        UUID oldLeader = kingdom.getLeaderUuid();
        kingdom.setLeaderUuid(newKing.getUniqueId());
        
        // Update ranks
        kingdom.promoteMember(oldLeader, Rank.NOBLE);
        kingdom.promoteMember(newKing.getUniqueId(), Rank.KING);
        
        // Update database
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE kingdoms SET leader_uuid = ? WHERE id = ?",
            newKing.getUniqueId().toString(),
            kingdom.getId()
        );
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET rank = 'NOBLE' WHERE uuid = ?",
            oldLeader.toString()
        );
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET rank = 'KING' WHERE uuid = ?",
            newKing.getUniqueId().toString()
        );
        
        // Announce
        kingdom.broadcastToKingdom("§6§l" + currentKing.getName() + " has abdicated the throne!");
        kingdom.broadcastToKingdom("§6§l" + newKing.getName() + " is now the new King!");
        
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
    
    public List<String> getKingdomList() {
        List<String> list = new ArrayList<>();
        for (Kingdom kingdom : kingdoms.values()) {
            list.add(String.format(
                "§e%s §7[§a%d§7/§c%d§7] §8- §fLeader: §b%s",
                kingdom.getColoredName(),
                kingdom.getOnlineCount(),
                kingdom.getMemberCount(),
                kingdom.getLeaderName()
            ));
        }
        return list;
    }
    
    private void loadKingdoms() {
        plugin.getDatabaseManager().executeQuery("SELECT * FROM kingdoms").thenAccept(resultSet -> {
            try {
                while (resultSet != null && resultSet.next()) {
                    Kingdom kingdom = new Kingdom(
                        resultSet.getString("name"),
                        UUID.fromString(resultSet.getString("leader_uuid"))
                    );
                    kingdom.setId(resultSet.getInt("id"));
                    kingdom.setColor(resultSet.getString("color"));
                    kingdom.setTreasury(resultSet.getDouble("treasury"));
                    
                    // Load capital if exists
                    String world = resultSet.getString("capital_world");
                    if (world != null) {
                        int x = resultSet.getInt("capital_x");
                        int y = resultSet.getInt("capital_y");
                        int z = resultSet.getInt("capital_z");
                        if (Bukkit.getWorld(world) != null) {
                            kingdom.setCapital(new Location(Bukkit.getWorld(world), x, y, z));
                        }
                    }
                    
                    kingdoms.put(kingdom.getId(), kingdom);
                    playerKingdomMap.put(kingdom.getLeaderUuid(), kingdom.getId());
                    
                    if (kingdom.getId() >= nextKingdomId) {
                        nextKingdomId = kingdom.getId() + 1;
                    }
                }
                plugin.getLogger().info("§aLoaded " + kingdoms.size() + " kingdoms from database!");
            } catch (SQLException e) {
                plugin.getLogger().severe("Error loading kingdoms: " + e.getMessage());
                e.printStackTrace();
            }
        });
        
        // Load members for each kingdom
        for (Kingdom kingdom : kingdoms.values()) {
            plugin.getDatabaseManager().executeQuery(
                "SELECT * FROM players WHERE kingdom_id = ?",
                kingdom.getId()
            ).thenAccept(resultSet -> {
                try {
                    while (resultSet != null && resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        Rank rank = Rank.valueOf(resultSet.getString("rank"));
                        kingdom.addMember(uuid, rank);
                        playerKingdomMap.put(uuid, kingdom.getId());
                    }
                } catch (SQLException e) {
                    plugin.getLogger().severe("Error loading members for kingdom " + kingdom.getName() + ": " + e.getMessage());
                }
            });
        }
    }
    
    public void saveAllKingdoms() {
        for (Kingdom kingdom : kingdoms.values()) {
            Location capital = kingdom.getCapital();
            String world = capital != null ? capital.getWorld().getName() : null;
            Integer x = capital != null ? capital.getBlockX() : null;
            Integer y = capital != null ? capital.getBlockY() : null;
            Integer z = capital != null ? capital.getBlockZ() : null;
            
            plugin.getDatabaseManager().executeUpdate(
                "UPDATE kingdoms SET treasury = ?, color = ?, capital_world = ?, capital_x = ?, capital_y = ?, capital_z = ? WHERE id = ?",
                kingdom.getTreasury(),
                kingdom.getColor(),
                world,
                x,
                y,
                z,
                kingdom.getId()
            );
        }
    }
          }
