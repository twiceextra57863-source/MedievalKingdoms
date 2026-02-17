package com.medieval.listeners;

import com.medieval.MedievalKingdoms;
import com.medieval.models.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final MedievalKingdoms plugin;
    
    public PlayerListener(MedievalKingdoms plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        data.updateLastActive();
        
        // Send welcome message
        if (data.isInKingdom()) {
            event.setJoinMessage("§8[§a+§8] §7" + player.getName() + " joined the realm");
        } else {
            event.setJoinMessage("§8[§a+§8] §7" + player.getName() + " arrived in the lands");
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerManager().savePlayerData(player.getUniqueId());
        
        event.setQuitMessage("§8[§c-§8] §7" + player.getName() + " left the realm");
    }
}
