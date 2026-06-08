package com.arcadia.lootbox.manager;

import com.arcadia.lootbox.config.LootboxConfig;
import com.arcadia.lootbox.data.LootboxDefinition;
import com.arcadia.lootbox.util.PermissionHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages free timed lootbox claims per player.
 * Persists claim timestamps to JSON file for server restart survival.
 *
 * <p>Each lootbox can define:
 * <ul>
 *   <li>{@code freeEnabled} — whether this lootbox can be claimed for free</li>
 *   <li>{@code freeCooldownHours} — default cooldown (e.g., 72h)</li>
 *   <li>{@code freePermission} — permission node for free claim (empty = everyone)</li>
 *   <li>{@code freeReducedCooldownHours} — reduced cooldown with special permission</li>
 *   <li>{@code freeReducedPermission} — permission node for reduced cooldown</li>
 * </ul>
 *
 * @author vyrriox
 */
public final class FreeLootboxManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArcadiaLootbox");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Key: "uuid:lootboxId" -> Value: epoch millis of last free claim
    private static final Map<String, Long> CLAIM_TIMESTAMPS = new ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicBoolean dirty = new java.util.concurrent.atomic.AtomicBoolean(false);

    private FreeLootboxManager() {}

    // --- Core API ---

    /**
     * Checks if a player can claim a specific free lootbox right now.
     *
     * @return true if the player can claim
     */
    public static boolean canClaim(ServerPlayer player, String lootboxId, LootboxDefinition def) {
        if (!LootboxConfig.FREE_LOOTBOX_ENABLED.get()) return false;
        if (!def.freeEnabled()) return false;

        // Check permission to claim free lootboxes
        if (!def.freePermission().isEmpty()) {
            if (!PermissionHelper.hasPermissionOrDefault(player, def.freePermission(), true)) {
                return false;
            }
        }

        long cooldownMs = getEffectiveCooldownMs(player, def);
        long lastClaim = getLastClaimTime(player.getUUID(), lootboxId);

        if (lastClaim == 0) return true; // Never claimed
        return System.currentTimeMillis() - lastClaim >= cooldownMs;
    }

    /**
     * Records a free claim for a player.
     */
    public static void recordClaim(UUID playerUuid, String lootboxId) {
        CLAIM_TIMESTAMPS.put(key(playerUuid, lootboxId), System.currentTimeMillis());
        dirty.set(true);
    }

    /**
     * Atomically checks and claims a free lootbox. Prevents double-claim race conditions.
     * @return true if the claim was successful, false if still on cooldown
     */
    public static boolean tryAtomicClaim(ServerPlayer player, String lootboxId, LootboxDefinition def) {
        if (!LootboxConfig.FREE_LOOTBOX_ENABLED.get()) return false;
        if (!def.freeEnabled()) return false;
        if (!def.freePermission().isEmpty()) {
            if (!PermissionHelper.hasPermissionOrDefault(player, def.freePermission(), true)) return false;
        }
        long cooldownMs = getEffectiveCooldownMs(player, def);
        String k = key(player.getUUID(), lootboxId);
        long now = System.currentTimeMillis();

        // Atomic check-and-set using compute
        boolean[] success = {false};
        CLAIM_TIMESTAMPS.compute(k, (key, lastClaim) -> {
            if (lastClaim != null && now - lastClaim < cooldownMs) return lastClaim; // Reject
            success[0] = true;
            return now; // Accept + record
        });
        if (success[0]) dirty.set(true);
        return success[0];
    }

    /**
     * Gets the remaining cooldown in milliseconds, or 0 if ready.
     */
    public static long getRemainingCooldownMs(ServerPlayer player, String lootboxId, LootboxDefinition def) {
        if (!def.freeEnabled()) return -1;

        long cooldownMs = getEffectiveCooldownMs(player, def);
        long lastClaim = getLastClaimTime(player.getUUID(), lootboxId);

        if (lastClaim == 0) return 0;
        long remaining = cooldownMs - (System.currentTimeMillis() - lastClaim);
        return Math.max(0, remaining);
    }

    /**
     * Returns a human-readable remaining time string (e.g., "2h 30m", "47h 15m").
     */
    public static String getRemainingFormatted(ServerPlayer player, String lootboxId, LootboxDefinition def) {
        long remainingMs = getRemainingCooldownMs(player, lootboxId, def);
        boolean fr = com.arcadia.lootbox.util.LanguageHelper.isFrench(player);
        if (remainingMs < 0) return fr ? "Indisponible" : "Unavailable"; // free not enabled (-1 sentinel)
        if (remainingMs == 0) return fr ? "Prêt !" : "Ready!";
        return formatDuration(remainingMs);
    }

    /**
     * Resets a player's free claim timer for a specific lootbox (admin command).
     */
    public static void resetClaim(UUID playerUuid, String lootboxId) {
        CLAIM_TIMESTAMPS.remove(key(playerUuid, lootboxId));
        dirty.set(true);
    }

    /**
     * Resets ALL free claim timers for a player.
     */
    public static void resetAllClaims(UUID playerUuid) {
        String prefix = playerUuid.toString() + ":";
        CLAIM_TIMESTAMPS.entrySet().removeIf(e -> e.getKey().startsWith(prefix));
        dirty.set(true);
    }

    // --- Cooldown calculation ---

    /**
     * Gets the effective cooldown for a player, taking permissions into account.
     * - Base cooldown from the lootbox definition
     * - Reduced cooldown if the player has the freeReducedPermission
     */
    private static long getEffectiveCooldownMs(ServerPlayer player, LootboxDefinition def) {
        int baseHours = def.freeCooldownHours() > 0 ? def.freeCooldownHours() : LootboxConfig.FREE_DEFAULT_COOLDOWN_HOURS.get();

        // Check for reduced cooldown permission
        if (def.freeReducedCooldownHours() > 0 && !def.freeReducedPermission().isEmpty()) {
            if (PermissionHelper.hasPermissionOrDefault(player, def.freeReducedPermission(), false)) {
                return def.freeReducedCooldownHours() * 3600_000L;
            }
        }

        // Global reduced cooldown permission
        String globalReducedPerm = LootboxConfig.FREE_REDUCED_PERMISSION.get();
        if (!globalReducedPerm.isEmpty() && PermissionHelper.hasPermissionOrDefault(player, globalReducedPerm, false)) {
            int reducedHours = LootboxConfig.FREE_REDUCED_COOLDOWN_HOURS.get();
            return reducedHours * 3600_000L;
        }

        return baseHours * 3600_000L;
    }

    private static long getLastClaimTime(UUID uuid, String lootboxId) {
        Long ts = CLAIM_TIMESTAMPS.get(key(uuid, lootboxId));
        return ts != null ? ts : 0;
    }

    private static String key(UUID uuid, String lootboxId) {
        return uuid.toString() + ":" + lootboxId;
    }

    // --- Persistence ---

    private static Path getDataFile() {
        return FMLPaths.CONFIGDIR.get().resolve("arcadia/arcadialootbox/free_claims.json");
    }

    /**
     * Loads claim timestamps from disk. Called on server start.
     */
    public static void load() {
        Path file = getDataFile();
        if (!Files.exists(file)) return;

        try (FileReader reader = new FileReader(file.toFile())) {
            Map<String, Long> loaded = GSON.fromJson(reader, new TypeToken<Map<String, Long>>(){}.getType());
            if (loaded != null) {
                CLAIM_TIMESTAMPS.clear();
                CLAIM_TIMESTAMPS.putAll(loaded);
                LOGGER.info("[ArcadiaLootbox] Loaded {} free claim records", loaded.size());
            }
        } catch (Exception e) {
            LOGGER.error("[ArcadiaLootbox] Failed to load free claims data", e);
        }
    }

    /**
     * Synchronously saves claim timestamps to disk. Used at shutdown where we must
     * block until the data is persisted. Periodic saves go through {@link #autoSave()}.
     */
    public static void save() {
        if (!dirty.compareAndSet(true, false)) return;
        // Snapshot under no lock — ConcurrentHashMap copy is safe — then write.
        writeSnapshot(new HashMap<>(CLAIM_TIMESTAMPS));
    }

    /**
     * Saves if dirty, OFF the calling (server tick) thread. Called periodically via the
     * scheduler. The dirty flag is cleared and a snapshot taken synchronously (cheap),
     * then GSON serialization + file IO run on the common pool so the tick loop never
     * blocks on disk.
     */
    public static void autoSave() {
        if (!dirty.compareAndSet(true, false)) return;
        Map<String, Long> snapshot = new HashMap<>(CLAIM_TIMESTAMPS);
        CompletableFuture.runAsync(() -> writeSnapshot(snapshot));
    }

    /**
     * Atomically writes a snapshot to disk (temp file + move). On failure the dirty
     * flag is re-armed so the next save retries.
     */
    private static void writeSnapshot(Map<String, Long> snapshot) {
        Path file = getDataFile();
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (FileWriter writer = new FileWriter(tmp.toFile())) {
                GSON.toJson(snapshot, writer);
            }
            try {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            dirty.set(true); // Mark dirty again so next save retries
            LOGGER.error("[ArcadiaLootbox] Failed to save free claims data", e);
        }
    }

    /**
     * Clears all data (server shutdown cleanup).
     */
    public static void clearAll() {
        save(); // Persist synchronously before clearing
        CLAIM_TIMESTAMPS.clear();
    }

    // --- Formatting ---

    public static String formatDuration(long ms) {
        long totalSeconds = ms / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0 || days > 0) sb.append(hours).append("h ");
        sb.append(minutes).append("m");
        return sb.toString().trim();
    }
}
