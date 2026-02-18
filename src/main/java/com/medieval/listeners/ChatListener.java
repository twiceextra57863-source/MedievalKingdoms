package com.medieval.listeners;

import com.medieval.MedievalKingdoms;
import com.medieval.models.Kingdom;
import com.medieval.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final MedievalKingdoms plugin;
    
    public ChatListener(MedievalKingdoms plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        // Check for kingdom chat prefix "!"
        if (message.startsWith("!")) {
            event.setCancelled(true);
            handleKingdomChat(player, message.substring(1));
            return;
        }
        
        // Format normal chat with rank
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        String rankPrefix = data.getRank().getPrefix();
        
        event.setFormat(rankPrefix + "%s: §f%s");
    }
    
    private void handleKingdomChat(Player player, String message) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        String rankPrefix = data.getRank().getPrefix();
        
        kingdom.broadcastToKingdom(String.format(
            "%s§7[%s§7] %s§f: %s",
            rankPrefix,
            kingdom.getColoredName(),
            player.getName(),
            message
        ));
    }
}
