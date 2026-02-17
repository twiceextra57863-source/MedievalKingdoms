package com.medieval.models;

import org.bukkit.*;

public class Territory {
    private final int id;
    private final int kingdomId;
    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private final long claimedTime;
    
    public Territory(int id, int kingdomId, String world, int chunkX, int chunkZ, long claimedTime) {
        this.id = id;
        this.kingdomId = kingdomId;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.claimedTime = claimedTime;
    }
    
    public Chunk getChunk() {
        World w = Bukkit.getWorld(world);
        if (w != null) {
            return w.getChunkAt(chunkX, chunkZ);
        }
        return null;
    }
    
    public Location getCenter() {
        Chunk chunk = getChunk();
        if (chunk != null) {
            return new Location(chunk.getWorld(), chunk.getX() * 16 + 8, 64, chunk.getZ() * 16 + 8);
        }
        return null;
    }
    
    public boolean contains(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        int x = loc.getBlockX() >> 4;
        int z = loc.getBlockZ() >> 4;
        return x == chunkX && z == chunkZ;
    }
    
    // Getters
    public int getId() { return id; }
    public int getKingdomId() { return kingdomId; }
    public String getWorld() { return world; }
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public long getClaimedTime() { return claimedTime; }
}
