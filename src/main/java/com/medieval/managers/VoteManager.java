package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.*;
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
        
        // Check if starter is noble or king
        if (!kingdom.isNoble(starter.getUniqueId()) && !kingdom.isLeader(starter.getUniqueId())) {
            starter.sendMessage("§cOnly nobles and the king can start elections!");
            return;
        }
        
        // Check minimum voters
        if (kingdom.getMemberCount() < plugin.getConfig().getInt("election.min-voters", 5)) {
            starter.sendMessage("§cNeed at least " + plugin.getConfig().getInt("election.min-voters", 5) + " members to start an election!");
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
        
        // Play sound
        if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
            for (UUID uuid : kingdom.getMembers()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                }
            }
        }
        
        // Announce election
        kingdom.broadcastToKingdom("§6§l=================================");
        kingdom.broadcastToKingdom("§6§l      ELECTION STARTED!");
        kingdom.broadcastToKingdom("§6§l=================================");
        kingdom.broadcastToKingdom("§e" + starter.getName() + " §ahas called for a new election!");
        kingdom.broadcastToKingdom("§aUse §e/vote cast <player> §ato cast your vote!");
        kingdom.broadcastToKingdom("§7Election ends in " + (plugin.getConfig().getInt("election.duration", 600) / 60) + " minutes!");
        
        // Start election timer
        int duration = plugin.getConfig().getInt("election.duration", 600); // seconds
        new BukkitRunnable() {
            int timeLeft = duration;
            
            @Override
            public void run() {
                if (!activeElections.containsKey(kingdom.getId())) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }
                
                timeLeft--;
                
                // Update boss bar
                bossBar.setProgress((double) timeLeft / duration);
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
    
    public boolean castVote(Player voter, String candidateName) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(voter.getUniqueId());
        if (kingdom == null) {
            voter.sendMessage("§cYou are not in a kingdom!");
            return false;
        }
        
        if (!isElectionActive(kingdom.getId())) {
            voter.sendMessage("§cNo active election in your kingdom!");
            return false;
        }
        
        // Find candidate
        Player candidate = Bukkit.getPlayer(candidateName);
        if (candidate == null) {
            voter.sendMessage("§cPlayer not found or offline!");
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
        
        // Check vote cooldown in database
        plugin.getDatabaseManager().executeQuery(
            "SELECT vote_time FROM votes WHERE kingdom_id = ? AND voter_uuid = ? ORDER BY vote_time DESC LIMIT 1",
            kingdom.getId(),
            voter.getUniqueId().toString()
        ).thenAccept(resultSet -> {
            try {
                if (resultSet != null && resultSet.next()) {
                    long lastVote = resultSet.getTimestamp("vote_time").getTime();
                    long now = System.currentTimeMillis();
                    long cooldown = plugin.getConfig().getLong("election.vote-cooldown", 86400) * 1000; // 24 hours in ms
                    
                    if (now - lastVote < cooldown) {
                        long hoursLeft = (cooldown - (now - lastVote)) / 3600000;
                        voter.sendMessage("§cYou can only vote once every 24 hours! §e" + hoursLeft + " hours remaining.");
                        return;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error checking vote cooldown: " + e.getMessage());
            }
            
            // Process vote on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                processVote(voter, candidate, kingdom);
            });
        });
        
        return true;
    }
    
    private void processVote(Player voter, Player candidate, Kingdom kingdom) {
        ActiveElection election = activeElections.get(kingdom.getId());
        
        // Check if voter already voted
        if (playerVotes.containsKey(voter.getUniqueId())) {
            UUID previousVote = playerVotes.get(voter.getUniqueId());
            if (previousVote.equals(candidate.getUniqueId())) {
                voter.sendMessage("§cYou already voted for this player!");
                return;
            }
            
            // Change vote
            playerVotes.put(voter.getUniqueId(), candidate.getUniqueId());
            election.changeVote(voter.getUniqueId(), candidate.getUniqueId());
            voter.sendMessage("§aYour vote has been changed to §e" + candidate.getName());
            
            // Update database
            plugin.getDatabaseManager().executeUpdate(
                "UPDATE votes SET candidate_uuid = ?, vote_time = CURRENT_TIMESTAMP WHERE kingdom_id = ? AND voter_uuid = ?",
                candidate.getUniqueId().toString(),
                kingdom.getId(),
                voter.getUniqueId().toString()
            );
            
            // Announce vote change
            if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
                voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        } else {
            // New vote
            playerVotes.put(voter.getUniqueId(), candidate.getUniqueId());
            election.addVote(voter.getUniqueId(), candidate.getUniqueId());
            voter.sendMessage("§aYou voted for §e" + candidate.getName());
            
            // Insert into database
            plugin.getDatabaseManager().executeUpdate(
                "INSERT INTO votes (kingdom_id, voter_uuid, candidate_uuid) VALUES (?, ?, ?)",
                kingdom.getId(),
                voter.getUniqueId().toString(),
                candidate.getUniqueId().toString()
            );
            
            // Update candidate's vote count
            plugin.getDatabaseManager().executeUpdate(
                "UPDATE players SET votecount = votecount + 1 WHERE uuid = ?",
                candidate.getUniqueId().toString()
            );
            
            // Play sound
            if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
                voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }
        }
        
        // Show updated standings
        showCurrentStandings(voter, kingdom);
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
        
        // Update boss bar
        BossBar bossBar = electionBossBars.get(kingdom.getId());
        if (bossBar != null) {
            bossBar.removeAll();
        }
        
        if (winner == null || maxVotes == 0 || totalVotes == 0) {
            // No votes
            kingdom.broadcastToKingdom("§c§lElection ended with no votes! The current leader remains.");
        } else {
            // Declare winner
            Player winnerPlayer = Bukkit.getPlayer(winner);
            String winnerName = winnerPlayer != null ? winnerPlayer.getName() : Bukkit.getOfflinePlayer(winner).getName();
            
            // Update kingdom leader
            UUID oldLeader = kingdom.getLeaderUuid();
            kingdom.setLeaderUuid(winner);
            
            // Update ranks
            if (oldLeader != null) {
                kingdom.promoteMember(oldLeader, Rank.NOBLE);
                plugin.getDatabaseManager().executeUpdate(
                    "UPDATE players SET rank = 'NOBLE' WHERE uuid = ?",
                    oldLeader.toString()
                );
            }
            kingdom.promoteMember(winner, Rank.KING);
            plugin.getDatabaseManager().executeUpdate(
                "UPDATE players SET rank = 'KING' WHERE uuid = ?",
                winner.toString()
            );
            
            // Update kingdom in database
            plugin.getDatabaseManager().executeUpdate(
                "UPDATE kingdoms SET leader_uuid = ? WHERE id = ?",
                winner.toString(),
                kingdom.getId()
            );
            
            // Announce winner
            kingdom.broadcastToKingdom("§6§l=================================");
            kingdom.broadcastToKingdom("§6§l      ELECTION RESULTS!");
            kingdom.broadcastToKingdom("§6§l=================================");
            kingdom.broadcastToKingdom("§e§l" + winnerName + " §6§lhas been elected as the new King!");
            kingdom.broadcastToKingdom("");
            
            // Show all results
            for (Map.Entry<UUID, Integer> entry : results.entrySet()) {
                String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                int votes = entry.getValue();
                double percentage = totalVotes > 0 ? (votes * 100.0 / totalVotes) : 0;
                String bar = getVoteBar(votes, maxVotes);
                kingdom.broadcastToKingdom(String.format(
                    "§7%s: §f%s §e(%d votes, %.1f%%) %s",
                    bar,
                    playerName,
                    votes,
                    percentage,
                    winner == entry.getKey() ? "§6§l👑" : ""
                ));
            }
            
            // Special effects for winner
            if (winnerPlayer != null && winnerPlayer.isOnline()) {
                // Play sound
                if (plugin.getConfig().getBoolean("sounds.enabled", true)) {
                    winnerPlayer.playSound(winnerPlayer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
                }
                
                // Show title
                winnerPlayer.sendTitle(
                    "§6§lYOU ARE NOW KING!",
                    "§eRule wisely, " + winnerName,
                    20,
                    100,
                    20
                );
                
                // Give crown (optional)
                // winnerPlayer.getInventory().addItem(createCrown());
            }
            
            // Log to database
            plugin.getDatabaseManager().executeUpdate(
                "INSERT INTO election_history (kingdom_id, winner_uuid, votes, date) VALUES (?, ?, ?, NOW())",
                kingdom.getId(),
                winner.toString(),
                maxVotes
            );
        }
        
        // Clean up
        activeElections.remove(kingdom.getId());
        electionBossBars.remove(kingdom.getId());
        
        // Clear votes for this kingdom
        playerVotes.entrySet().removeIf(entry -> 
            kingdom.getMembers().contains(entry.getKey())
        );
    }
    
    private String getVoteBar(int votes, int maxVotes) {
        int barLength = 20;
        int filled = maxVotes > 0 ? (int) ((double) votes / maxVotes * barLength) : 0;
        
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("§a■");
            } else {
                bar.append("§7■");
            }
        }
        bar.append("§8]");
        
        return bar.toString();
    }
    
    private void showCurrentStandings(Player player, Kingdom kingdom) {
        if (!isElectionActive(kingdom.getId())) return;
        
        ActiveElection election = activeElections.get(kingdom.getId());
        Map<UUID, Integer> results = election.getResults();
        int maxVotes = results.values().stream().max(Integer::compareTo).orElse(1);
        int totalVotes = results.values().stream().mapToInt(Integer::intValue).sum();
        
        player.sendMessage("§6§lCurrent Election Standings:");
        for (Map.Entry<UUID, Integer> entry : results.entrySet()) {
            if (entry.getValue() == 0) continue; // Skip zero votes
            
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            int votes = entry.getValue();
            double percentage = totalVotes > 0 ? (votes * 100.0 / totalVotes) : 0;
            String bar = getVoteBar(votes, maxVotes);
            boolean hasVoted = playerVotes.containsKey(player.getUniqueId()) && 
                              playerVotes.get(player.getUniqueId()).equals(entry.getKey());
            
            player.sendMessage(String.format(
                "%s §f%s §7(%d votes, %.1f%%) %s",
                bar,
                playerName,
                votes,
                percentage,
                hasVoted ? "§a✓" : ""
            ));
        }
        player.sendMessage("§7Total Votes: §a" + totalVotes);
    }
    
    public boolean isElectionActive(int kingdomId) {
        return activeElections.containsKey(kingdomId);
    }
    
    public ActiveElection getActiveElection(int kingdomId) {
        return activeElections.get(kingdomId);
    }
    
    public void cancelElection(int kingdomId, Player canceller) {
        Kingdom kingdom = plugin.getKingdomManager().getKingdomById(kingdomId);
        if (kingdom == null) return;
        
        if (!kingdom.isLeader(canceller.getUniqueId())) {
            canceller.sendMessage("§cOnly the King can cancel elections!");
            return;
        }
        
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

class ActiveElection {
    private final Kingdom kingdom;
    private final Map<UUID, Integer> votes;
    private final Map<UUID, UUID> voterMap;
    private final Date startTime;
    
    public ActiveElection(Kingdom kingdom) {
        this.kingdom = kingdom;
        this.votes = new HashMap<>();
        this.voterMap = new HashMap<>();
        this.startTime = new Date();
        
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
    
    public Date getStartTime() {
        return startTime;
    }
                }
