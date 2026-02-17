package com.medieval.managers;

import com.medieval.MedievalKingdoms;
import com.medieval.models.*;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RoleAssignmentManager implements Listener {
    private final MedievalKingdoms plugin;
    private final Map<UUID, RoleAssignment> pendingAssignments;
    
    private final double NOBLE_CHANCE = 0.05;
    private final double KNIGHT_CHANCE = 0.10;
    private final double BASTARD_CHANCE = 0.02;
    private final double SPECIAL_BIRTH_CHANCE = 0.01;
    
    public RoleAssignmentManager(MedievalKingdoms plugin) {
        this.plugin = plugin;
        this.pendingAssignments = new HashMap<>();
    }
    
    @EventHandler
    public void onFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPlayedBefore()) {
            assignInitialRole(player);
        }
    }
    
    private void assignInitialRole(Player player) {
        UUID playerId = player.getUniqueId();
        RoleAssignment assignment = generateBirthRole(player);
        pendingAssignments.put(playerId, assignment);
        
        startRoleCeremony(player, assignment);
    }
    
    private RoleAssignment generateBirthRole(Player player) {
        RoleAssignment assignment = new RoleAssignment(player);
        double roll = ThreadLocalRandom.current().nextDouble();
        
        if (roll < SPECIAL_BIRTH_CHANCE) {
            assignment.setSpecialBirth(true);
            assignment.setSpecialType(getRandomSpecialBirth());
            assignment.setProposedRank(Rank.NOBLE);
        }
        else if (roll < NOBLE_CHANCE + SPECIAL_BIRTH_CHANCE) {
            assignment.setNobleBirth(true);
            assignment.setProposedRank(generateNobleRank());
        }
        else if (roll < NOBLE_CHANCE + SPECIAL_BIRTH_CHANCE + KNIGHT_CHANCE) {
            assignment.setKnightFamily(true);
            assignment.setProposedRank(Rank.KNIGHT);
        }
        else if (roll < NOBLE_CHANCE + SPECIAL_BIRTH_CHANCE + KNIGHT_CHANCE + BASTARD_CHANCE) {
            assignment.setBastard(true);
            assignment.setProposedRank(Rank.SQUIRE);
        }
        else {
            assignment.setProposedRank(Rank.PEASANT);
        }
        
        assignment.setBackstory(generateBackstory(assignment));
        return assignment;
    }
    
    private Rank generateNobleRank() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.01) {
            return Rank.DUKE;
        } else if (roll < 0.05) {
            return Rank.COUNT;
        } else if (roll < 0.15) {
            return Rank.LORD;
        } else {
            return Rank.NOBLE;
        }
    }
    
    private String getRandomSpecialBirth() {
        String[] specials = {
            "§6Dragon Blood", "§5Ancient Heritage", "§bProphecy Child",
            "§dFairy Touched", "§cWarrior Spirit", "§aNature's Blessing"
        };
        return specials[ThreadLocalRandom.current().nextInt(specials.length)];
    }
    
    private String generateBackstory(RoleAssignment assignment) {
        StringBuilder backstory = new StringBuilder();
        
        if (assignment.isSpecialBirth()) {
            backstory.append("§6You were born under a rare celestial alignment!\n");
            backstory.append("§eLegends speak of your ").append(assignment.getSpecialType()).append("§e.\n");
            backstory.append("§6Great things are expected of you.");
        } else if (assignment.isNobleBirth()) {
            backstory.append("§6You were born into nobility! Your family has served the kingdom for generations.\n");
            backstory.append("§eYou start with a small estate and some family gold.");
        } else if (assignment.isKnightFamily()) {
            backstory.append("§7Your father was a brave knight who died in battle.\n");
            backstory.append("§eYou begin your journey as a ").append(assignment.getProposedRank().getDisplayName());
        } else if (assignment.isBastard()) {
            backstory.append("§5You are the bastard child of a powerful noble.\n");
            backstory.append("§dYou start as a ").append(assignment.getProposedRank().getDisplayName()).append(" with a chance to prove yourself.");
        } else {
            backstory.append("§7You were born to poor peasants in a small village.\n");
            backstory.append("§7You start with nothing but your determination.");
        }
        
        return backstory.toString();
    }
    
    private void startRoleCeremony(Player player, RoleAssignment assignment) {
        player.sendMessage("§6§l=====================================");
        player.sendMessage("§6§l     WELCOME TO THE KINGDOM!");
        player.sendMessage("§6§l=====================================");
        
        new BukkitRunnable() {
            int countdown = 5;
            
            @Override
            public void run() {
                if (countdown > 0) {
                    player.sendMessage("§eExamining your lineage... §7(" + countdown + ")");
                    countdown--;
                } else {
                    presentRoleChoice(player, assignment);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }
    
    private void presentRoleChoice(Player player, RoleAssignment assignment) {
        player.sendMessage("");
        player.sendMessage("§6§lYOUR BIRTHRIGHT:");
        player.sendMessage(assignment.getBackstory());
        player.sendMessage("");
        
        player.sendMessage("§eType §6/role accept §eto accept your fate!");
        player.sendMessage("§eType §6/role reroll §eto try again (cost: 100 gold)");
        
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }
    
    public void acceptRole(Player player) {
        RoleAssignment assignment = pendingAssignments.remove(player.getUniqueId());
        if (assignment == null) {
            player.sendMessage("§cNo pending role assignment!");
            return;
        }
        
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        data.setRank(assignment.getProposedRank());
        
        if (assignment.isNobleBirth()) {
            data.addGold(5000);
        } else if (assignment.isKnightFamily()) {
            data.addGold(1000);
        } else if (assignment.isBastard()) {
            data.addGold(500);
        } else if (assignment.isSpecialBirth()) {
            data.addGold(10000);
            data.addReputation(20);
        }
        
        showRoleCard(player, data);
    }
    
    private void showRoleCard(Player player, PlayerData data) {
        player.sendMessage("");
        player.sendMessage("§6§l╔══════════════════════════════╗");
        player.sendMessage("§6§l║      YOUR ROLE CARD         ║");
        player.sendMessage("§6§l╠══════════════════════════════╣");
        player.sendMessage("§6§l║ §fName: §e" + player.getName());
        player.sendMessage("§6§l║ §fRank: " + data.getRank().getDisplayName());
        player.sendMessage("§6§l║ §fGold: §6" + data.getGold());
        player.sendMessage("§6§l║ §fReputation: §a" + data.getReputation() + "/100");
        player.sendMessage("§6§l╚══════════════════════════════╝");
        
        if (data.getRank().getPower() >= 8) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        } else if (data.getRank().getPower() >= 5) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }
    
    public void rerollRole(Player player) {
        PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        
        if (data.getGold() < 100) {
            player.sendMessage("§cYou need 100 gold to reroll your role!");
            return;
        }
        
        data.removeGold(100);
        RoleAssignment newAssignment = generateBirthRole(player);
        pendingAssignments.put(player.getUniqueId(), newAssignment);
        
        player.sendMessage("§aYour fate has been rewritten!");
        presentRoleChoice(player, newAssignment);
    }
}

class RoleAssignment {
    private final Player player;
    private Rank proposedRank;
    private boolean nobleBirth;
    private boolean knightFamily;
    private boolean bastard;
    private boolean specialBirth;
    private String specialType;
    private String backstory;
    
    public RoleAssignment(Player player) {
        this.player = player;
        this.nobleBirth = false;
        this.knightFamily = false;
        this.bastard = false;
        this.specialBirth = false;
    }
    
    public Player getPlayer() { return player; }
    
    public Rank getProposedRank() { return proposedRank; }
    public void setProposedRank(Rank rank) { this.proposedRank = rank; }
    
    public boolean isNobleBirth() { return nobleBirth; }
    public void setNobleBirth(boolean nobleBirth) { this.nobleBirth = nobleBirth; }
    
    public boolean isKnightFamily() { return knightFamily; }
    public void setKnightFamily(boolean knightFamily) { this.knightFamily = knightFamily; }
    
    public boolean isBastard() { return bastard; }
    public void setBastard(boolean bastard) { this.bastard = bastard; }
    
    public boolean isSpecialBirth() { return specialBirth; }
    public void setSpecialBirth(boolean specialBirth) { this.specialBirth = specialBirth; }
    
    public String getSpecialType() { return specialType; }
    public void setSpecialType(String specialType) { this.specialType = specialType; }
    
    public String getBackstory() { return backstory; }
    public void setBackstory(String backstory) { this.backstory = backstory; }
}
