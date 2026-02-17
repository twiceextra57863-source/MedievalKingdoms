package com.medieval;

import org.bukkit.plugin.java.JavaPlugin;
import com.medieval.managers.*;

public class MedievalKingdoms extends JavaPlugin {
    private static MedievalKingdoms instance;
    
    // Core Managers
    private EmpireManager empireManager;
    private KingdomManager kingdomManager;
    private NobleManager nobleManager;
    private CastleManager castleManager;
    private WarManager warManager;
    private TaxManager taxManager;
    private SuccessionManager successionManager;
    private ArmyManager armyManager;
    private EconomyManager economyManager;
    private DiplomacyManager diplomacyManager;
    private ReligionManager religionManager;
    private TournamentManager tournamentManager;
    private TradeManager tradeManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize all systems
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        getLogger().info("§6=================================");
        getLogger().info("§6 Medieval Kingdoms Plugin v1.0");
        getLogger().info("§6 Created by: YourName");
        getLogger().info("§6=================================");
        
        // Check for Emperor on startup
        if (empireManager.getEmperor() == null) {
            getLogger().info("§cNo Emperor found! Use /kingdom createempire to establish an empire!");
        }
    }
    
    private void initializeManagers() {
        empireManager = new EmpireManager(this);
        kingdomManager = new KingdomManager(this);
        nobleManager = new NobleManager(this);
        castleManager = new CastleManager(this);
        warManager = new WarManager(this);
        taxManager = new TaxManager(this);
        successionManager = new SuccessionManager(this);
        armyManager = new ArmyManager(this);
        economyManager = new EconomyManager(this);
        diplomacyManager = new DiplomacyManager(this);
        religionManager = new ReligionManager(this);
        tournamentManager = new TournamentManager(this);
        tradeManager = new TradeManager(this);
    }
}
