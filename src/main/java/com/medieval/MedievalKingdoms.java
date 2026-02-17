package com.medieval;

import com.medieval.managers.*;
import com.medieval.commands.*;
import com.medieval.listeners.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.sql.SQLException;

public class MedievalKingdoms extends JavaPlugin {
    private static MedievalKingdoms instance;
    
    // Managers
    private DatabaseManager databaseManager;
    private KingdomManager kingdomManager;
    private PlayerManager playerManager;
    private VoteManager voteManager;
    private ElectionManager electionManager;
    private RoleAssignmentManager roleManager;
    private TerritoryManager territoryManager;
    private AllianceManager allianceManager;
    private WarManager warManager;
    private EconomyManager economyManager;
    private CastleManager castleManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default configs
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("database.yml", false);
        
        // Initialize database first
        try {
            databaseManager = new DatabaseManager(this);
            databaseManager.initializeDatabase();
            getLogger().info("§aDatabase connected successfully!");
        } catch (SQLException e) {
            getLogger().severe("§cFailed to connect to database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize all managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start scheduled tasks
        startScheduledTasks();
        
        getLogger().info("§6=================================");
        getLogger().info("§6 Medieval Kingdoms Plugin v2.0");
        getLogger().info("§6 Created by: YourName");
        getLogger().info("§6=================================");
    }
    
    private void initializeManagers() {
        playerManager = new PlayerManager(this);
        kingdomManager = new KingdomManager(this);
        voteManager = new VoteManager(this);
        electionManager = new ElectionManager(this);
        roleManager = new RoleAssignmentManager(this);
        territoryManager = new TerritoryManager(this);
        allianceManager = new AllianceManager(this);
        warManager = new WarManager(this);
        economyManager = new EconomyManager(this);
        castleManager = new CastleManager(this);
    }
    
    private void registerCommands() {
        getCommand("kingdom").setExecutor(new KingdomCommand(this));
        getCommand("vote").setExecutor(new VoteCommand(this));
        getCommand("election").setExecutor(new ElectionCommand(this));
        getCommand("role").setExecutor(new RoleCommand(this));
        getCommand("kadmin").setExecutor(new AdminCommand(this));
        getCommand("territory").setExecutor(new TerritoryCommand(this));
        getCommand("war").setExecutor(new WarCommand(this));
        getCommand("alliance").setExecutor(new AllianceCommand(this));
    }
    
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new VotingListener(this), this);
        getServer().getPluginManager().registerEvents(new KingdomListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new TerritoryListener(this), this);
        getServer().getPluginManager().registerEvents(new WarListener(this), this);
    }
    
    private void startScheduledTasks() {
        // Auto-save every 10 minutes
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            playerManager.saveAllPlayers();
            kingdomManager.saveAllKingdoms();
        }, 12000L, 12000L);
        
        // Check for active elections every minute
        getServer().getScheduler().runTaskTimer(this, () -> {
            electionManager.checkActiveElections();
        }, 1200L, 1200L);
        
        // Tax collection every 24 hours
        getServer().getScheduler().runTaskTimer(this, () -> {
            economyManager.collectTaxes();
        }, 1728000L, 1728000L);
    }
    
    @Override
    public void onDisable() {
        // Save all data
        playerManager.saveAllPlayers();
        kingdomManager.saveAllKingdoms();
        databaseManager.closeConnection();
        
        getLogger().info("§cMedieval Kingdoms Plugin Disabled!");
    }
    
    public static MedievalKingdoms getInstance() {
        return instance;
    }
    
    // Manager getters
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public KingdomManager getKingdomManager() { return kingdomManager; }
    public PlayerManager getPlayerManager() { return playerManager; }
    public VoteManager getVoteManager() { return voteManager; }
    public ElectionManager getElectionManager() { return electionManager; }
    public RoleAssignmentManager getRoleManager() { return roleManager; }
    public TerritoryManager getTerritoryManager() { return territoryManager; }
    public AllianceManager getAllianceManager() { return allianceManager; }
    public WarManager getWarManager() { return warManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public CastleManager getCastleManager() { return castleManager; }
}
