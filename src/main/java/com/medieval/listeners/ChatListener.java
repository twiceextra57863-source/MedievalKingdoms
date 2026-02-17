package com.medieval.listeners;

import com.medieval.MedievalKingdoms;
import com.medieval.models.Kingdom;
import com.medieval.models.PlayerData;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {
    private final MedievalKingdoms plugin;
    
    public ChatListener(MedievalKingdoms plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        
        // Check for kingdom chat prefix
        if (message.startsWith("!")) {
            event.setCancelled(true);
            handleKingdomChat(player, message.substring(1));
            return;
        }
        
        // Format normal chat with rank
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        String rankPrefix = data.getRank().getPrefix();
        
        event.message(Component.text(rankPrefix + player.getName() + "§f: " + message));
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
