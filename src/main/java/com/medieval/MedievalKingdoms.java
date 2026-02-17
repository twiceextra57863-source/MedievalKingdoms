package com.medieval;

import com.medieval.managers.*;
import com.medieval.commands.*;
import com.medieval.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.SQLException;

public class MedievalKingdoms extends JavaPlugin {
    private static MedievalKingdoms instance;
    
    // Only these 5 managers - NO TerritoryManager, AllianceManager, etc.
    private DatabaseManager databaseManager;
    private KingdomManager kingdomManager;
    private PlayerManager playerManager;
    private VoteManager voteManager;
    private RoleAssignmentManager roleManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initializeDatabase();
            getLogger().info("§aDatabase connected!");
        } catch (SQLException e) {
            getLogger().severe("§cDatabase failed! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize only these 5 managers
        playerManager = new PlayerManager(this);
        kingdomManager = new KingdomManager(this);
        voteManager = new VoteManager(this);
        roleManager = new RoleAssignmentManager(this);
        
        // Register commands
        getCommand("kingdom").setExecutor(new KingdomCommand(this));
        getCommand("vote").setExecutor(new VoteCommand(this));
        getCommand("role").setExecutor(new RoleCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(roleManager, this);
        
        getLogger().info("§a=================================");
        getLogger().info("§a MedievalKingdoms v2.0 Enabled!");
        getLogger().info("§a=================================");
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        getLogger().info("§cMedievalKingdoms Disabled!");
    }
    
    public static MedievalKingdoms getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public KingdomManager getKingdomManager() { return kingdomManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public VoteManager getVoteManager() { return voteManager; }
    public RoleAssignmentManager getRoleManager() { return roleManager; }
}
