package com.medieval;

import com.medieval.managers.*;
import com.medieval.commands.*;
import com.medieval.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.SQLException;

public class MedievalKingdoms extends JavaPlugin {
    private static MedievalKingdoms instance;
    
    // Only essential managers
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
            getLogger().info("Database connected!");
        } catch (SQLException e) {
            getLogger().severe("Database failed! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
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
        
        getLogger().info("MedievalKingdoms Enabled!");
    }
    
    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }
    
    public static MedievalKingdoms getInstance() { return instance; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public KingdomManager getKingdomManager() { return kingdomManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public VoteManager getVoteManager() { return voteManager; }
    public RoleAssignmentManager getRoleManager() { return roleManager; }
}
