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

public class KingdomCommand implements CommandExecutor, TabCompleter {
    private final MedievalKingdoms plugin;
    
    public KingdomCommand(MedievalKingdoms plugin) {
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
            // Kingdom Management
            case "create":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom create <name>");
                    return true;
                }
                handleCreate(player, args[1]);
                break;
                
            case "join":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom join <name>");
                    return true;
                }
                handleJoin(player, args[1]);
                break;
                
            case "leave":
                handleLeave(player);
                break;
                
            case "disband":
                handleDisband(player);
                break;
                
            // Information
            case "info":
                if (args.length > 1) {
                    handleInfo(player, args[1]);
                } else {
                    handleInfo(player, null);
                }
                break;
                
            case "list":
                handleList(player);
                break;
                
            case "members":
                handleMembers(player);
                break;
                
            // Management
            case "promote":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /kingdom promote <player> <rank>");
                    player.sendMessage("§7Ranks: noble, knight");
                    return true;
                }
                handlePromote(player, args[1], args[2]);
                break;
                
            case "demote":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom demote <player>");
                    return true;
                }
                handleDemote(player, args[1]);
                break;
                
            case "kick":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom kick <player>");
                    return true;
                }
                handleKick(player, args[1]);
                break;
                
            case "transfer":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom transfer <player>");
                    return true;
                }
                handleTransfer(player, args[1]);
                break;
                
            // Chat and Communication
            case "chat":
            case "c":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom chat <message>");
                    return true;
                }
                handleKingdomChat(player, args);
                break;
                
            // Customization
            case "setcolor":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom setcolor <color>");
                    player.sendMessage("§7Colors: red, blue, green, yellow, purple, aqua");
                    return true;
                }
                handleSetColor(player, args[1]);
                break;
                
            case "setcapital":
                handleSetCapital(player);
                break;
                
            // Treasury
            case "balance":
                handleBalance(player);
                break;
                
            case "deposit":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom deposit <amount>");
                    return true;
                }
                handleDeposit(player, args[1]);
                break;
                
            case "withdraw":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /kingdom withdraw <amount>");
                    return true;
                }
                handleWithdraw(player, args[1]);
                break;
                
            default:
                sendHelp(player);
        }
        
        return true;
    }
    
    // ==================== Kingdom Management ====================
    
    private void handleCreate(Player player, String name) {
        // Check name length
        if (name.length() < 3 || name.length() > 16) {
            player.sendMessage("§cKingdom name must be 3-16 characters long!");
            return;
        }
        
        // Check if name is alphanumeric
        if (!name.matches("[a-zA-Z0-9]+")) {
            player.sendMessage("§cKingdom name can only contain letters and numbers!");
            return;
        }
        
        Kingdom kingdom = plugin.getKingdomManager().createKingdom(name, player);
        if (kingdom != null) {
            player.sendMessage("§a§lKingdom created successfully!");
            player.sendMessage("§7You are now the King of §e" + name);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
            
            // Broadcast to server
            Bukkit.broadcastMessage("§6§l" + player.getName() + " §ehas founded a new kingdom: §6" + name);
        }
    }
    
    private void handleJoin(Player player, String name) {
        Kingdom kingdom = plugin.getKingdomManager().getKingdomByName(name);
        if (kingdom == null) {
            player.sendMessage("§cKingdom not found!");
            return;
        }
        
        if (plugin.getKingdomManager().joinKingdom(player, name)) {
            player.sendMessage("§aYou joined §e" + name);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
    
    private void handleLeave(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cYou cannot leave as the King! Use §e/kingdom transfer §cor §e/kingdom disband");
            return;
        }
        
        plugin.getKingdomManager().leaveKingdom(player);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
    }
    
    private void handleDisband(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can disband the kingdom!");
            return;
        }
        
        // Confirmation
        player.sendMessage("§c§lAre you sure you want to disband your kingdom?");
        player.sendMessage("§cThis action cannot be undone!");
        player.sendMessage("§eType §6/kingdom disband confirm §eto confirm.");
        
        // Store confirmation in metadata or handle separately
    }
    
    // ==================== Information ====================
    
    private void handleInfo(Player player, String name) {
        Kingdom kingdom;
        if (name == null) {
            kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
            if (kingdom == null) {
                player.sendMessage("§cYou are not in a kingdom! Use §e/kingdom list §cto see all kingdoms.");
                return;
            }
        } else {
            kingdom = plugin.getKingdomManager().getKingdomByName(name);
            if (kingdom == null) {
                player.sendMessage("§cKingdom not found!");
                return;
            }
        }
        
        PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
        Rank playerRank = kingdom.getPlayerRank(player.getUniqueId());
        
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║      KINGDOM INFORMATION    ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        player.sendMessage("§6§l║ §fName: " + kingdom.getColoredName());
        player.sendMessage("§6§l║ §fKing: §e" + kingdom.getLeaderName());
        player.sendMessage("§6§l║ §fMembers: §a" + kingdom.getMemberCount() + " §7(§a" + kingdom.getOnlineCount() + " online§7)");
        
        // Count ranks
        int nobles = 0, knights = 0, peasants = 0;
        for (UUID uuid : kingdom.getMembers()) {
            Rank r = kingdom.getPlayerRank(uuid);
            if (r == Rank.NOBLE) nobles++;
            else if (r == Rank.KNIGHT) knights++;
            else if (r == Rank.PEASANT) peasants++;
        }
        
        player.sendMessage("§6§l║ §fNobles: §5" + nobles);
        player.sendMessage("§6§l║ §fKnights: §7" + knights);
        player.sendMessage("§6§l║ §fPeasants: §7" + peasants);
        player.sendMessage("§6§l║ §fTreasury: §6" + kingdom.getTreasury() + " gold");
        player.sendMessage("§6§l║");
        player.sendMessage("§6§l║ §fYour Rank: " + playerRank.getDisplayName());
        player.sendMessage("§6§l╚══════════════════════════════╝");
    }
    
    private void handleList(Player player) {
        List<Kingdom> kingdoms = plugin.getKingdomManager().getAllKingdoms();
        
        if (kingdoms.isEmpty()) {
            player.sendMessage("§7No kingdoms have been created yet.");
            return;
        }
        
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║        KINGDOM LIST         ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        
        for (Kingdom kingdom : kingdoms) {
            String status = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId()) == kingdom ? 
                " §a✔" : "";
            player.sendMessage(String.format("§6§l║ %s §7[§a%d§7/§c%d§7] §8- §f%s%s",
                kingdom.getColoredName(),
                kingdom.getOnlineCount(),
                kingdom.getMemberCount(),
                kingdom.getLeaderName(),
                status
            ));
        }
        
        player.sendMessage("§6§l╚══════════════════════════════╝");
    }
    
    private void handleMembers(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║      KINGDOM MEMBERS        ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        
        // Group by rank
        List<String> kingList = new ArrayList<>();
        List<String> nobleList = new ArrayList<>();
        List<String> knightList = new ArrayList<>();
        List<String> peasantList = new ArrayList<>();
        
        for (UUID uuid : kingdom.getMembers()) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            boolean online = Bukkit.getPlayer(uuid) != null;
            String status = online ? "§a●" : "§7○";
            
            Rank rank = kingdom.getPlayerRank(uuid);
            String entry = " §7- " + status + " §f" + name;
            
            if (uuid.equals(kingdom.getLeaderUuid())) {
                kingList.add(rank.getPrefix() + "§lKing" + entry);
            } else if (rank == Rank.NOBLE) {
                nobleList.add(rank.getPrefix() + "Noble" + entry);
            } else if (rank == Rank.KNIGHT) {
                knightList.add(rank.getPrefix() + "Knight" + entry);
            } else {
                peasantList.add(rank.getPrefix() + "Peasant" + entry);
            }
        }
        
        // Display
        for (String line : kingList) player.sendMessage("§6§l║" + line);
        for (String line : nobleList) player.sendMessage("§6§l║" + line);
        for (String line : knightList) player.sendMessage("§6§l║" + line);
        for (String line : peasantList) player.sendMessage("§6§l║" + line);
        
        player.sendMessage("§6§l╚══════════════════════════════╝");
    }
    
    // ==================== Management ====================
    
    private void handlePromote(Player player, String targetName, String rankStr) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can promote members!");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }
        
        if (!kingdom.getMembers().contains(target.getUniqueId())) {
            player.sendMessage("§cThat player is not in your kingdom!");
            return;
        }
        
        Rank currentRank = kingdom.getPlayerRank(target.getUniqueId());
        Rank newRank;
        
        switch (rankStr.toLowerCase()) {
            case "noble":
                if (currentRank == Rank.NOBLE) {
                    player.sendMessage("§cThat player is already a noble!");
                    return;
                }
                newRank = Rank.NOBLE;
                break;
            case "knight":
                if (currentRank == Rank.KNIGHT) {
                    player.sendMessage("§cThat player is already a knight!");
                    return;
                }
                newRank = Rank.KNIGHT;
                break;
            default:
                player.sendMessage("§cInvalid rank! Use: noble, knight");
                return;
        }
        
        kingdom.promoteMember(target.getUniqueId(), newRank);
        plugin.getPlayerManager().getPlayerData(target).setRank(newRank);
        
        player.sendMessage("§aYou promoted §e" + target.getName() + " §ato " + newRank.getDisplayName());
        target.sendMessage("§aYou have been promoted to " + newRank.getDisplayName() + " §aby King " + player.getName());
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    private void handleDemote(Player player, String targetName) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can demote members!");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }
        
        if (target.getUniqueId().equals(kingdom.getLeaderUuid())) {
            player.sendMessage("§cYou cannot demote yourself!");
            return;
        }
        
        Rank currentRank = kingdom.getPlayerRank(target.getUniqueId());
        Rank newRank;
        
        if (currentRank == Rank.NOBLE) {
            newRank = Rank.KNIGHT;
        } else if (currentRank == Rank.KNIGHT) {
            newRank = Rank.PEASANT;
        } else {
            player.sendMessage("§cThat player cannot be demoted further!");
            return;
        }
        
        kingdom.promoteMember(target.getUniqueId(), newRank);
        plugin.getPlayerManager().getPlayerData(target).setRank(newRank);
        
        player.sendMessage("§cYou demoted §e" + target.getName() + " §cto " + newRank.getDisplayName());
        target.sendMessage("§cYou have been demoted to " + newRank.getDisplayName() + " §cby King " + player.getName());
    }
    
    private void handleKick(Player player, String targetName) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can kick members!");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }
        
        if (target.getUniqueId().equals(kingdom.getLeaderUuid())) {
            player.sendMessage("§cYou cannot kick yourself!");
            return;
        }
        
        kingdom.removeMember(target.getUniqueId());
        plugin.getPlayerManager().getPlayerData(target).setKingdomId(-1);
        plugin.getPlayerManager().getPlayerData(target).setRank(Rank.PEASANT);
        
        kingdom.broadcastToKingdom("§c" + target.getName() + " has been kicked from the kingdom by King " + player.getName());
        target.sendMessage("§cYou have been kicked from the kingdom!");
    }
    
    private void handleTransfer(Player player, String targetName) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can transfer leadership!");
            return;
        }
        
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }
        
        if (!kingdom.getMembers().contains(target.getUniqueId())) {
            player.sendMessage("§cThat player is not in your kingdom!");
            return;
        }
        
        // Transfer leadership
        UUID oldLeader = kingdom.getLeaderUuid();
        kingdom.setLeaderUuid(target.getUniqueId());
        
        // Update ranks
        kingdom.promoteMember(oldLeader, Rank.NOBLE);
        kingdom.promoteMember(target.getUniqueId(), Rank.KING);
        
        plugin.getPlayerManager().getPlayerData(oldLeader).setRank(Rank.NOBLE);
        plugin.getPlayerManager().getPlayerData(target).setRank(Rank.KING);
        
        kingdom.broadcastToKingdom("§6§l" + player.getName() + " has abdicated the throne!");
        kingdom.broadcastToKingdom("§6§l" + target.getName() + " is now the new King!");
        
        target.playSound(target.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
    }
    
    // ==================== Chat ====================
    
    private void handleKingdomChat(Player player, String[] args) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Rank rank = kingdom.getPlayerRank(player.getUniqueId());
        
        kingdom.broadcastToKingdom(String.format(
            "%s§7[%s§7] %s§f: %s",
            rank.getPrefix(),
            kingdom.getColoredName(),
            player.getName(),
            message
        ));
    }
    
    // ==================== Customization ====================
    
    private void handleSetColor(Player player, String colorName) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can change kingdom color!");
            return;
        }
        
        String color = getColorCode(colorName);
        if (color == null) {
            player.sendMessage("§cInvalid color! Use: red, blue, green, yellow, purple, aqua");
            return;
        }
        
        kingdom.setColor(color);
        kingdom.broadcastToKingdom("§aKingdom color changed to " + color + colorName);
    }
    
    private void handleSetCapital(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can set the capital!");
            return;
        }
        
        kingdom.setCapital(player.getLocation());
        player.sendMessage("§aCapital set to your current location!");
        kingdom.broadcastToKingdom("§e" + player.getName() + " §ahas set the kingdom capital!");
    }
    
    private String getColorCode(String color) {
        switch (color.toLowerCase()) {
            case "red": return "§c";
            case "blue": return "§9";
            case "green": return "§a";
            case "yellow": return "§e";
            case "purple": return "§5";
            case "aqua": return "§b";
            default: return null;
        }
    }
    
    // ==================== Treasury ====================
    
    private void handleBalance(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        player.sendMessage("§6Kingdom Treasury: §e" + kingdom.getTreasury() + " gold");
    }
    
    private void handleDeposit(Player player, String amountStr) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                player.sendMessage("§cAmount must be positive!");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return;
        }
        
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        if (data.getGold() < amount) {
            player.sendMessage("§cYou don't have enough gold! You have: §6" + data.getGold());
            return;
        }
        
        data.setGold(data.getGold() - amount);
        kingdom.addToTreasury(amount);
        
        player.sendMessage("§aYou deposited §6" + amount + " gold §ato the kingdom treasury.");
        player.sendMessage("§7Kingdom treasury now has: §6" + kingdom.getTreasury() + " gold");
    }
    
    private void handleWithdraw(Player player, String amountStr) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        if (kingdom == null) {
            player.sendMessage("§cYou are not in a kingdom!");
            return;
        }
        
        if (!kingdom.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the King can withdraw from the treasury!");
            return;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                player.sendMessage("§cAmount must be positive!");
                return;
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid amount!");
            return;
        }
        
        if (kingdom.getTreasury() < amount) {
            player.sendMessage("§cKingdom doesn't have enough gold! Treasury: §6" + kingdom.getTreasury());
            return;
        }
        
        kingdom.removeFromTreasury(amount);
        plugin.getPlayerManager().getPlayerData(player).addGold(amount);
        
        player.sendMessage("§aYou withdrew §6" + amount + " gold §afrom the kingdom treasury.");
        player.sendMessage("§7Kingdom treasury now has: §6" + kingdom.getTreasury() + " gold");
    }
    
    // ==================== Help ====================
    
    private void sendHelp(Player player) {
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        boolean isKing = kingdom != null && kingdom.isLeader(player.getUniqueId());
        boolean isInKingdom = kingdom != null;
        
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║     KINGDOM COMMANDS        ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        
        // Basic commands (always available)
        player.sendMessage("§6§l║ §e/kingdom list §7- List all kingdoms");
        player.sendMessage("§6§l║ §e/kingdom info [name] §7- View kingdom info");
        
        if (!isInKingdom) {
            // Commands for players without kingdom
            player.sendMessage("§6§l║ §e/kingdom create <name> §7- Create a kingdom");
            player.sendMessage("§6§l║ §e/kingdom join <name> §7- Join a kingdom");
        } else {
            // Commands for kingdom members
            player.sendMessage("§6§l║ §e/kingdom members §7- List members");
            player.sendMessage("§6§l║ §e/kingdom chat <msg> §7- Kingdom chat");
            player.sendMessage("§6§l║ §e/kingdom leave §7- Leave kingdom");
            player.sendMessage("§6§l║ §e/kingdom balance §7- Check treasury");
            player.sendMessage("§6§l║ §e/kingdom deposit <amt> §7- Deposit gold");
            
            if (isKing) {
                // King only commands
                player.sendMessage("§6§l║ §c===== KING COMMANDS =====");
                player.sendMessage("§6§l║ §e/kingdom promote <p> <rank> §7- Promote");
                player.sendMessage("§6§l║ §e/kingdom demote <p> §7- Demote");
                player.sendMessage("§6§l║ §e/kingdom kick <p> §7- Kick member");
                player.sendMessage("§6§l║ §e/kingdom transfer <p> §7- Transfer king");
                player.sendMessage("§6§l║ §e/kingdom disband §7- Delete kingdom");
                player.sendMessage("§6§l║ §e/kingdom setcolor <c> §7- Set color");
                player.sendMessage("§6§l║ §e/kingdom setcapital §7- Set capital");
                player.sendMessage("§6§l║ §e/kingdom withdraw <amt> §7- Withdraw");
            }
        }
        
        player.sendMessage("§6§l╚══════════════════════════════╝");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player)) {
            return completions;
        }
        
        Player player = (Player) sender;
        Kingdom kingdom = plugin.getKingdomManager().getPlayerKingdom(player.getUniqueId());
        boolean isKing = kingdom != null && kingdom.isLeader(player.getUniqueId());
        
        if (args.length == 1) {
            // Main commands
            List<String> commands = new ArrayList<>(Arrays.asList(
                "create", "join", "list", "info", "chat"
            ));
            
            if (kingdom != null) {
                commands.add("members");
                commands.add("leave");
                commands.add("balance");
                commands.add("deposit");
                
                if (isKing) {
                    commands.addAll(Arrays.asList(
                        "promote", "demote", "kick", "transfer", 
                        "disband", "setcolor", "setcapital", "withdraw"
                    ));
                }
            }
            
            for (String cmd : commands) {
                if (cmd.startsWith(args[0].toLowerCase())) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join":
                case "info":
                    // List all kingdoms
                    for (Kingdom k : plugin.getKingdomManager().getAllKingdoms()) {
                        if (k.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(k.getName());
                        }
                    }
                    break;
                    
                case "promote":
                case "demote":
                case "kick":
                case "transfer":
                    if (kingdom != null) {
                        for (UUID uuid : kingdom.getMembers()) {
                            if (!uuid.equals(player.getUniqueId())) {
                                Player member = Bukkit.getPlayer(uuid);
                                if (member != null && member.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                    completions.add(member.getName());
                                }
                            }
                        }
                    }
                    break;
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("promote")) {
            // Rank suggestions
            for (String rank : Arrays.asList("noble", "knight")) {
                if (rank.startsWith(args[2].toLowerCase())) {
                    completions.add(rank);
                }
            }
        }
        
        return completions;
    }
}
