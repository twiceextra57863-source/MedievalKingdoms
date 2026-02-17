package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.Kingdom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ElectionManager {
    private final MedievalKingdoms plugin;
    
    public ElectionManager(MedievalKingdoms plugin) {
        this.plugin = plugin;
    }
    
    public void checkActiveElections() {
        plugin.getDatabaseManager().executeQuery(
            "SELECT * FROM elections WHERE status = 'ACTIVE' AND end_time <= NOW()"
        ).thenAccept(resultSet -> {
            try {
                while (resultSet != null && resultSet.next()) {
                    int electionId = resultSet.getInt("id");
                    int kingdomId = resultSet.getInt("kingdom_id");
                    String candidateUuid = resultSet.getString("candidate_uuid");
                    int votes = resultSet.getInt("votes");
                    
                    // End election and process results
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        processElectionResults(electionId, kingdomId, UUID.fromString(candidateUuid), votes);
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking active elections: " + e.getMessage());
            }
        });
    }
    
    private void processElectionResults(int electionId, int kingdomId, UUID winnerUuid, int votes) {
        Kingdom kingdom = plugin.getKingdomManager().getKingdomById(kingdomId);
        if (kingdom == null) return;
        
        // Update election status
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE elections SET status = 'COMPLETED' WHERE id = ?",
            electionId
        );
        
        // Update kingdom leader
        UUID oldLeader = kingdom.getLeaderUuid();
        kingdom.setLeaderUuid(winnerUuid);
        
        // Update player ranks
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET rank = 'NOBLE' WHERE uuid = ?",
            oldLeader.toString()
        );
        
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE players SET rank = 'KING' WHERE uuid = ?",
            winnerUuid.toString()
        );
        
        // Update kingdom in database
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE kingdoms SET leader_uuid = ? WHERE id = ?",
            winnerUuid.toString(),
            kingdomId
        );
        
        // Log to history
        plugin.getDatabaseManager().executeUpdate(
            "INSERT INTO election_history (kingdom_id, winner_uuid, votes, date) VALUES (?, ?, ?, NOW())",
            kingdomId,
            winnerUuid.toString(),
            votes
        );
        
        // Announce
        Player winner = Bukkit.getPlayer(winnerUuid);
        String winnerName = winner != null ? winner.getName() : Bukkit.getOfflinePlayer(winnerUuid).getName();
        kingdom.broadcastToKingdom("§6§l" + winnerName + " has won the election and is now the King!");
    }
    
    public void startElection(Kingdom kingdom, Player starter, List<UUID> candidates) {
        // End any active elections
        plugin.getDatabaseManager().executeUpdate(
            "UPDATE elections SET status = 'CANCELLED' WHERE kingdom_id = ? AND status = 'ACTIVE'",
            kingdom.getId()
        );
        
        // Insert new election records
        long endTime = System.currentTimeMillis() + (plugin.getConfig().getLong("election.duration", 600) * 1000);
        
        for (UUID candidate : candidates) {
            plugin.getDatabaseManager().executeUpdate(
                "INSERT INTO elections (kingdom_id, candidate_uuid, votes, end_time, status) VALUES (?, ?, 0, FROM_UNIXTIME(?), 'ACTIVE')",
                kingdom.getId(),
                candidate.toString(),
                endTime / 1000
            );
        }
        
        kingdom.broadcastToKingdom("§6§lA new election has started! Use §e/vote cast <player> §6§lto vote!");
    }
    
    public List<UUID> getCandidates(int kingdomId) {
        List<UUID> candidates = new ArrayList<>();
        
        plugin.getDatabaseManager().executeQuery(
            "SELECT candidate_uuid FROM elections WHERE kingdom_id = ? AND status = 'ACTIVE'",
            kingdomId
        ).thenAccept(resultSet -> {
            try {
                while (resultSet != null && resultSet.next()) {
                    candidates.add(UUID.fromString(resultSet.getString("candidate_uuid")));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting candidates: " + e.getMessage());
            }
        });
        
        return candidates;
    }
    
    public int getVotesForCandidate(int kingdomId, UUID candidateUuid) {
        try {
            ResultSet resultSet = plugin.getDatabaseManager().executeQuery(
                "SELECT votes FROM elections WHERE kingdom_id = ? AND candidate_uuid = ? AND status = 'ACTIVE'",
                kingdomId,
                candidateUuid.toString()
            ).get();
            
            if (resultSet != null && resultSet.next()) {
                return resultSet.getInt("votes");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error getting votes: " + e.getMessage());
        }
        
        return 0;
    }
}
