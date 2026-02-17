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
