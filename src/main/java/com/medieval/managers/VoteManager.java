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
        if (isElectionActive(kingdom.getId())) {
            starter.sendMessage("§cAn election is already running!");
            return;
        }
        
        if (kingdom.getMemberCount() < 3) {
            starter.sendMessage("§cNeed at least 3 members!");
            return;
        }
        
        ActiveElection election = new ActiveElection(kingdom);
        activeElections.put(kingdom.getId(), election);
        
        // Fixed: BarColor.YELLOW instead of GOLD
        BossBar bossBar = Bukkit.createBossBar(
            "§6§lElection in " + kingdom.getColoredName(),
            BarColor.YELLOW,
            BarStyle.SEGMENTED_10
        );
        electionBossBars.put(kingdom.getId(), bossBar);
        
        for (UUID uuid : kingdom.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) bossBar.addPlayer(player);
        }
        
        kingdom.broadcastToKingdom("§6§l=================================");
        kingdom.broadcastToKingdom("§6§l      ELECTION STARTED!");
        kingdom.broadcastToKingdom("§aUse §e/vote cast <player> §ato vote!");
        
        new BukkitRunnable() {
            int timeLeft = 600;
            
            @Override
            public void run() {
                if (!activeElections.containsKey(kingdom.getId())) {
                    bossBar.removeAll();
                    cancel();
                    return;
                }
                
                timeLeft--;
                bossBar.setProgress((double) timeLeft / 600);
                bossBar.setTitle(String.format(
                    "§6§lElection in %s §7| §eTime: §6%d:%02d §7| §fVotes: §a%d",
                    kingdom.getName(), timeLeft / 60, timeLeft % 60, election.getTotalVotes()
                ));
                
                if (timeLeft <= 0) {
                    endElection(kingdom, election);
                    bossBar.removeAll();
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    
    public boolean castVote(Player voter, Player candidate) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(voter.getUniqueId());
        if (kingdom == null || !isElectionActive(kingdom.getId())) return false;
        
        if (!kingdom.getMembers().contains(candidate.getUniqueId())) {
            voter.sendMessage("§cCandidate not in your kingdom!");
            return false;
        }
        
        if (voter.getUniqueId().equals(candidate.getUniqueId())) {
            voter.sendMessage("§cYou cannot vote for yourself!");
            return false;
        }
        
        ActiveElection election = activeElections.get(kingdom.getId());
        
        if (playerVotes.containsKey(voter.getUniqueId())) {
            UUID oldVote = playerVotes.get(voter.getUniqueId());
            election.changeVote(voter.getUniqueId(), candidate.getUniqueId());
            playerVotes.put(voter.getUniqueId(), candidate.getUniqueId());
            voter.sendMessage("§aYour vote changed to §e" + candidate.getName());
        } else {
            election.addVote(voter.getUniqueId(), candidate.getUniqueId());
            playerVotes.put(voter.getUniqueId(), candidate.getUniqueId());
            voter.sendMessage("§aYou voted for §e" + candidate.getName());
        }
        
        voter.playSound(voter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        return true;
    }
    
    private void endElection(Kingdom kingdom, ActiveElection election) {
        Map<UUID, Integer> results = election.getResults();
        UUID winner = null;
        int maxVotes = -1;
        
        for (Map.Entry<UUID, Integer> entry : results.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winner = entry.getKey();
            }
        }
        
        if (winner == null || maxVotes == 0) {
            kingdom.broadcastToKingdom("§cElection ended with no votes!");
        } else {
            String winnerName = Bukkit.getOfflinePlayer(winner).getName();
            kingdom.setLeaderUuid(winner);
            kingdom.promoteMember(winner, Rank.KING);
            kingdom.broadcastToKingdom("§6§l" + winnerName + " is the new King!");
        }
        
        activeElections.remove(kingdom.getId());
        electionBossBars.remove(kingdom.getId());
        playerVotes.entrySet().removeIf(e -> kingdom.getMembers().contains(e.getKey()));
    }
    
    public boolean isElectionActive(int kingdomId) {
        return activeElections.containsKey(kingdomId);
    }
    
    public Map<UUID, Integer> getCurrentVotes(int kingdomId) {
        ActiveElection election = activeElections.get(kingdomId);
        return election != null ? election.getResults() : new HashMap<>();
    }
}

class ActiveElection {
    private final Kingdom kingdom;
    private final Map<UUID, Integer> votes;
    private final Map<UUID, UUID> voterMap;
    
    public ActiveElection(Kingdom kingdom) {
        this.kingdom = kingdom;
        this.votes = new HashMap<>();
        this.voterMap = new HashMap<>();
        for (UUID uuid : kingdom.getMembers()) votes.put(uuid, 0);
    }
    
    public void addVote(UUID voter, UUID candidate) {
        voterMap.put(voter, candidate);
        votes.put(candidate, votes.getOrDefault(candidate, 0) + 1);
    }
    
    public void changeVote(UUID voter, UUID newCandidate) {
        UUID old = voterMap.get(voter);
        if (old != null) votes.put(old, votes.get(old) - 1);
        voterMap.put(voter, newCandidate);
        votes.put(newCandidate, votes.getOrDefault(newCandidate, 0) + 1);
    }
    
    public Map<UUID, Integer> getResults() { return new HashMap<>(votes); }
    public int getTotalVotes() { return voterMap.size(); }
}
