package com.medieval.commands;

import com.medieval.MedievalKingdoms;
import com.medieval.models.Kingdom;
import com.medieval.models.PlayerData;
import com.medieval.models.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class VoteCommand implements CommandExecutor, TabCompleter {
    private final MedievalKingdoms plugin;
    
    public VoteCommand(MedievalKingdoms plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "start":
                handleStartElection(player);
                break;
                
            case "cast":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /vote cast <player>");
                    return true;
                }
                handleCastVote(player, args[1]);
                break;
                
            case "status":
                handleVoteStatus(player);
                break;
                
            case "cancel":
                handleCancelElection(player);
                break;
                
            case "results":
                handleViewResults(player);
                break;
                
            default:
                sendHelp(player);
        }
        
        return true;
    }
    
    private void handleStartElection(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        // Check if player is noble or king
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        if (data.getRank().getPower() < Rank.NOBLE.getPower()) {
            player.sendMessage("§cOnly nobles and the king can start elections!");
            return;
        }
        
        // Check if election already running
        if (plugin.getVoteManager().isElectionActive(kingdom.getId())) {
            player.sendMessage("§cAn election is already running in your kingdom!");
            return;
        }
        
        // Check minimum members
        if (kingdom.getMemberCount() < 3) {
            player.sendMessage("§cNeed at least 3 members to start an election!");
            return;
        }
        
        plugin.getVoteManager().startElection(kingdom, player);
    }
    
    private void handleCastVote(Player player, String candidateName) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!plugin.getVoteManager().isElectionActive(kingdom.getId())) {
            player.sendMessage("§cNo active election in your kingdom!");
            return;
        }
        
        // Find candidate
        Player candidate = Bukkit.getPlayer(candidateName);
        if (candidate == null) {
            player.sendMessage("§cPlayer not found or offline!");
            return;
        }
        
        // Check if candidate is in same kingdom
        if (!kingdom.getMembers().contains(candidate.getUniqueId())) {
            player.sendMessage("§cThat player is not in your kingdom!");
            return;
        }
        
        // Check if voting for self
        if (player.getUniqueId().equals(candidate.getUniqueId())) {
            player.sendMessage("§cYou cannot vote for yourself!");
            return;
        }
        
        // Cast vote
        boolean success = plugin.getVoteManager().castVote(player, candidate);
        if (success) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.sendMessage("§aYour vote has been cast for §e" + candidate.getName());
            
            // Show current standings
            showCurrentStandings(player, kingdom);
        }
    }
    
    private void handleVoteStatus(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!plugin.getVoteManager().isElectionActive(kingdom.getId())) {
            player.sendMessage("§cNo active election in your kingdom!");
            return;
        }
        
        showCurrentStandings(player, kingdom);
    }
    
    private void handleCancelElection(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can cancel elections!");
            return;
        }
        
        plugin.getVoteManager().cancelElection(kingdom.getId(), player);
    }
    
    private void handleViewResults(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        // Check if there was a recent election
        player.sendMessage("§6§l=== Last Election Results ===");
        player.sendMessage("§7Check kingdom history for details.");
    }
    
    private void showCurrentStandings(Player player, Kingdom kingdom) {
        Map<UUID, Integer> votes = plugin.getVoteManager().getCurrentVotes(kingdom.getId());
        if (votes == null || votes.isEmpty()) {
            player.sendMessage("§7No votes cast yet.");
            return;
        }
        
        int totalVotes = votes.values().stream().mapToInt(Integer::intValue).sum();
        int maxVotes = votes.values().stream().max(Integer::compareTo).orElse(1);
        
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║    CURRENT VOTE STANDINGS   ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        
        // Sort by votes (highest first)
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(votes.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        for (Map.Entry<UUID, Integer> entry : sorted) {
            if (entry.getValue() == 0) continue;
            
            String playerName = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            int voteCount = entry.getValue();
            double percentage = (voteCount * 100.0) / totalVotes;
            String bar = getVoteBar(voteCount, maxVotes);
            
            player.sendMessage(String.format("§6§l║ %s §f%s", bar, playerName));
            player.sendMessage(String.format("§6§l║    §7%d votes §8(§e%.1f%%§8)", voteCount, percentage));
        }
        
        player.sendMessage("§6§l║");
        player.sendMessage("§6§l║ §fTotal Votes: §a" + totalVotes);
        player.sendMessage("§6§l╚══════════════════════════════╝");
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
    
    private void sendHelp(Player player) {
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║      VOTE COMMANDS          ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        player.sendMessage("§6§l║ §e/vote start §7- Start election");
        player.sendMessage("§6§l║ §e/vote cast <player> §7- Cast vote");
        player.sendMessage("§6§l║ §e/vote status §7- Check standings");
        player.sendMessage("§6§l║ §e/vote cancel §7- Cancel election");
        player.sendMessage("§6§l║ §e/vote results §7- View results");
        player.sendMessage("§6§l╚══════════════════════════════╝");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("start", "cast", "status", "cancel", "results"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("cast")) {
            Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
            if (kingdom != null) {
                for (UUID uuid : kingdom.getMembers()) {
                    if (!uuid.equals(player.getUniqueId())) {
                        Player member = Bukkit.getPlayer(uuid);
                        if (member != null) {
                            completions.add(member.getName());
                        }
                    }
                }
            }
        }
        
        return completions;
    }
          }
