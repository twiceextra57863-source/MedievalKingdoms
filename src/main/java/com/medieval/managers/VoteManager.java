package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.Kingdom;
import com.medieval.models.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class VoteManager {
    private final MedievalKingdoms plugin;
    private final Map<Integer, ActiveElection> activeElections;
    private final Map<UUID, UUID> playerVotes;
    private final Map<Integer, BossBar> electionBossBars;
    
    public VoteManager(MedievalKingdoms plugin) {
        this.plugin = plugin;
        this.activeElections = new ConcurrentHashMap<>();
        this.playerVotes = new ConcurrentHashMap<>();
        this.electionBossBars = new ConcurrentHashMap<>();
    }
    
    public void startElection(Kingdom kingdom, Player starter) {
        // Check if election already running
        if (isElectionActive(kingdom.getId())) {
            starter.sendMessage("§cAn election is already running in your kingdom!");
            return;
        }
        
        // Check minimum members
        if (kingdom.getMemberCount() < 3) {
            starter.sendMessage("§cNeed at least 3 members to start an election!");
            return;
        }
        
        // Create new election
        ActiveElection election = new ActiveElection(kingdom);
        activeElections.put(kingdom.getId(), election);
        
        // Create boss bar
        BossBar bossBar = Bukkit.createBossBar(
            "§6§lElection in " + kingdom.getColoredName(),
            BarColor.GOLD,
            BarStyle.SEGMENTED_10
        );
        electionBossBars.put(kingdom.getId(), bossBar);
        
        // Add all online kingdom members to boss bar
        for (UUID uuid : kingdom.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                bossBar.addPlayer(player);
            }
        }
        
        // Announce election
        kingdom.broadcastToKingdom("§6§l=================================");
        kingdom.broadcastToKingdom("§6§l      ELECTION STARTED!");
        kingdom.broadcastToKingdom("§6§l=================================");
        kingdom.broadcastToKingdom("§e" + starter.getName() + " §ahas called for a new election!");
        kingdom.broadcastToKingdom("§aUse §e/vote cast <player> §ato cast your vote!");
        kingdom.broadcastToKingdom("§7Election ends in 10 minutes!");
        
        // Start election timer
        new BukkitRunnable() {
            int timeLeft = 600; // 10 minutes in seconds
            
            @Override
            public void run() {
                if (!activeElections.containsKey(kingdom.getId())) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }
                
                timeLeft--;
                
                // Update boss bar
                bossBar.setProgress((double) timeLeft / 600);
                bossBar.setTitle(String.format(
                    "§6§lElection in %s §7| §eTime left: §6%d:%02d §7| §fVotes: §a%d",
                    kingdom.getName(),
                    timeLeft / 60,
                    timeLeft % 60,
                    election.getTotalVotes()
                ));
                
                // Send reminders at certain intervals
                if (timeLeft == 300) { // 5 minutes
                    kingdom.broadcastToKingdom("§e5 minutes remaining in the election! Cast your vote!");
                } else if (timeLeft == 60) { // 1 minute
                    kingdom.broadcastToKingdom("§c1 minute remaining! Last chance to vote!");
                } else if (timeLeft == 10) {
                    kingdom.broadcastToKingdom("§c§l10 seconds remaining!");
                } else if (timeLeft <= 0) {
                    // End election
                    endElection(kingdom, election);
                    bossBar.removeAll();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    public boolean castVote(Player voter, Player candidate) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(voter.getUniqueId());
        if (kingdom == null) {
            voter.sendMessage("§cYou are not in a kingdom!");
            return false;
        }
        
        if (!isElectionActive(kingdom.getId())) {
            voter.sendMessage("§cNo active election in your kingdom!");
            return false;
        }
        
        // Check if candidate is in same kingdom
        if (!kingdom.getMembers().contains(candidate.getUniqueId())) {
            voter.sendMessage("§cThat player is not in your kingdom!");
            return false;
        }
        
        // Check if voting for self
        if (voter.getUniqueId().equals(candidate.getUniqueId())) {
            voter.sendMessage("§cYou cannot vote for yourself!");
            return false;
        }
        
        ActiveElection election = activeElections.get(kingdom.getId());
        
        // Check if voter already voted
        if (playerVotes.containsKey(voter.getUniqueId())) {
            UUID previousVote = playerVotes.get(voter.getUniqueId());
            if (previousVote.equals(candidate.getUniqueId())) {
                voter.sendMessage("§cYou already voted for this player!");
                return false;
            }
            
            // Change vote
            playerVotes.put(voter.getUniqueId(), candidate.getUniqueId());
            election.changeVote(voter.getUniqueId(), candidate.getUniqueId());
            voter.sendMessage("§aYour vote has been changed to §e" + candidate.getName());
            
            // Play sound
            voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        } else {
            // New vote
            playerVotes.put(voter.getUniqueId(), candidate.getUniqueId());
            election.addVote(voter.getUniqueId(), candidate.getUniqueId());
            voter.sendMessage("§aYou voted for §e" + candidate.getName());
            
            // Play sound
            voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
        
        return true;
    }
    
    private void endElection(Kingdom kingdom, ActiveElection election) {
        // Calculate winner
        Map<UUID, Integer> results = election.getResults();
        UUID winner = null;
        int maxVotes = -1;
        int totalVotes = 0;
        
        for (Map.Entry<UUID, Integer> entry : results.entrySet()) {
            totalVotes += entry.getValue();
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }
        
        if (winner == null || maxVotes == 0 || totalVotes == 0) {
            // No votes
            kingdom.broadcastToKingdom("§c§lElection ended with no votes! The current leader remains.");
        } else {
            // Declare winner
            String winnerName = Bukkit.getOfflinePlayer(winner).getName();
            
            // Update kingdom leader
            UUID oldLeader = kingdom.getLeaderUuid();
            kingdom.setLeaderUuid(winner);
            
            // Update ranks
            if (oldLeader != null) {
                kingdom.promoteMember(oldLeader, Rank.NOBLE);
            }
            kingdom.promoteMember(winner, Rank.KING);
            
            // Announce winner
            kingdom.broadcastToKingdom("§6§l=================================");
            kingdom.broadcastToKingdom("§6§l      ELECTION RESULTS!");
            kingdom.broadcastToKingdom("§6§l=================================");
            kingdom.broadcastToKingdom("§e§l" + winnerName + " §6§lhas been elected as the new King!");
            
            // Show all results
            for (Map.Entry<UUID, Integer> entry : results.entrySet()) {
                if (entry.getValue() > 0) {
                    String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                    int votes = entry.getValue();
                    double percentage = (votes * 100.0) / totalVotes;
                    kingdom.broadcastToKingdom(String.format(
                        "§7- §f%s: §e%d votes §7(§e%.1f%%§7)",
                        playerName,
                        votes,
                        percentage
                    ));
                }
            }
            
            // Special effects for winner
            Player winnerPlayer = Bukkit.getPlayer(winner);
            if (winnerPlayer != null && winnerPlayer.isOnline()) {
                winnerPlayer.playSound(winnerPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
                winnerPlayer.sendTitle(
                    "§6§lYOU ARE NOW KING!",
                    "§eRule wisely!",
                    20,
                    100,
                    20
                );
            }
        }
        
        // Clean up
        activeElections.remove(kingdom.getId());
        electionBossBars.remove(kingdom.getId());
        
        // Clear votes for this kingdom
        playerVotes.entrySet().removeIf(entry -> 
            kingdom.getMembers().contains(entry.getKey())
        );
    }
    
    public boolean isElectionActive(int kingdomId) {
        return activeElections.containsKey(kingdomId);
    }
    
    public Map<UUID, Integer> getCurrentVotes(int kingdomId) {
        ActiveElection election = activeElections.get(kingdomId);
        if (election != null) {
            return election.getResults();
        }
        return new HashMap<>();
    }
    
    public void cancelElection(int kingdomId, Player canceller) {
        Kingdom kingdom = plugin.getKingdomManager().getKingdomById(kingdomId);
        if (kingdom == null) return;
        
        ActiveElection election = activeElections.remove(kingdomId);
        if (election != null) {
            BossBar bossBar = electionBossBars.remove(kingdomId);
            if (bossBar != null) {
                bossBar.removeAll();
            }
            
            kingdom.broadcastToKingdom("§c§lElection cancelled by King " + canceller.getName());
        }
    }
}

// Separate class for ActiveElection (must be in same file)
class ActiveElection {
    private final Kingdom kingdom;
    private final Map<UUID, Integer> votes;
    private final Map<UUID, UUID> voterMap;
    
    public ActiveElection(Kingdom kingdom) {
        this.kingdom = kingdom;
        this.votes = new HashMap<>();
        this.voterMap = new HashMap<>();
        
        // Initialize vote counts for all members
        for (UUID uuid : kingdom.getMembers()) {
            votes.put(uuid, 0);
        }
    }
    
    public void addVote(UUID voter, UUID candidate) {
        voterMap.put(voter, candidate);
        votes.put(candidate, votes.getOrDefault(candidate, 0) + 1);
    }
    
    public void changeVote(UUID voter, UUID newCandidate) {
        UUID oldCandidate = voterMap.get(voter);
        if (oldCandidate != null) {
            votes.put(oldCandidate, votes.get(oldCandidate) - 1);
        }
        voterMap.put(voter, newCandidate);
        votes.put(newCandidate, votes.getOrDefault(newCandidate, 0) + 1);
    }
    
    public Map<UUID, Integer> getResults() {
        return new HashMap<>(votes);
    }
    
    public int getTotalVotes() {
        return voterMap.size();
    }
    }
