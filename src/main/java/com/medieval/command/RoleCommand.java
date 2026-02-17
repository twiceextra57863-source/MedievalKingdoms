package com.medieval.commands;

import com.medieval.MedievalKingdoms;
import com.medieval.models.PlayerData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class RoleCommand implements CommandExecutor {
    private final MedievalKingdoms plugin;
    
    public RoleCommand(MedievalKingdoms plugin) {
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
            showRole(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "accept":
                plugin.getRoleManager().acceptRole(player);
                break;
                
            case "reroll":
                plugin.getRoleManager().rerollRole(player);
                break;
                
            case "info":
                if (args.length > 1) {
                    showRoleInfo(player, args[1]);
                } else {
                    showRoleInfo(player, null);
                }
                break;
                
            default:
                showRole(player);
        }
        
        return true;
    }
    
    private void showRole(Player player) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║         YOUR ROLE           ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        player.sendMessage("§6§l║ §fName: §e" + player.getName());
        player.sendMessage("§6§l║ §fRank: " + data.getRank().getDisplayName());
        player.sendMessage("§6§l║ §fDescription: §7" + data.getRank().getDescription());
        player.sendMessage("§6§l║ §fGold: §6" + data.getGold());
        player.sendMessage("§6§l║ §fReputation: §a" + data.getReputation() + "/100");
        player.sendMessage("§6§l║ §fVotes Received: §e" + data.getVoteCount());
        player.sendMessage("§6§l╚══════════════════════════════╝");
    }
    
    private void showRoleInfo(Player player, String rankName) {
        if (rankName == null) {
            player.sendMessage("§6Available Ranks:");
            player.sendMessage("§e- emperor §7(§6Ruler of all§7)");
            player.sendMessage("§e- king §7(§eRuler of kingdom§7)");
            player.sendMessage("§e- duke §7(§5High noble§7)");
            player.sendMessage("§e- count §7(§3Noble§7)");
            player.sendMessage("§e- lord §7(§2Land owner§7)");
            player.sendMessage("§e- noble §7(§dAristocrat§7)");
            player.sendMessage("§e- knight §7(§7Warrior§7)");
            player.sendMessage("§e- squire §7(§fIn training§7)");
            player.sendMessage("§e- peasant §7(§7Commoner§7)");
            return;
        }
        
        // Show specific rank info
        player.sendMessage("§6Use §e/role §6to see your own role.");
    }
}
