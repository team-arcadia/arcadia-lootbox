package com.arcadia.lootbox.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-block lootbox usage counts. Persists in BlockEntity NBT.
 *
 * @author vyrriox
 */
public final class UsageTracker {

    private static final String NBT_KEY = "ArcadiaLootUses";
    private static final Map<Long, Integer> CACHE = new ConcurrentHashMap<>();

    private UsageTracker() {}

    public static int getRemainingUses(BlockEntity be, int maxUses) {
        if (maxUses <= 0) return -1;
        return Math.max(0, maxUses - getUsageCount(be));
    }

    public static int incrementUsage(BlockEntity be) {
        int next = getUsageCount(be) + 1;
        be.getPersistentData().putInt(NBT_KEY, next);
        be.setChanged();
        CACHE.put(be.getBlockPos().asLong(), next);
        return next;
    }

    public static int getUsageCount(BlockEntity be) {
        long key = be.getBlockPos().asLong();
        Integer cached = CACHE.get(key);
        if (cached != null) return cached;
        int count = be.getPersistentData().contains(NBT_KEY) ? be.getPersistentData().getInt(NBT_KEY) : 0;
        CACHE.put(key, count);
        return count;
    }

    public static void setUsageCount(BlockEntity be, int count) {
        be.getPersistentData().putInt(NBT_KEY, count);
        be.setChanged();
        CACHE.put(be.getBlockPos().asLong(), count);
    }

    public static void removeFromCache(BlockPos pos) { CACHE.remove(pos.asLong()); }
    public static void clearCache() { CACHE.clear(); }

    public static boolean hasUsesRemaining(BlockEntity be, int maxUses) {
        return maxUses <= 0 || getUsageCount(be) < maxUses;
    }
}
