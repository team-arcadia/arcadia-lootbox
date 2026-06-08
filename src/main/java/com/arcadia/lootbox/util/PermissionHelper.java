package com.arcadia.lootbox.util;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Soft LuckPerms integration — works gracefully without LuckPerms installed.
 * Falls back to vanilla OP level checks when LP is not present.
 *
 * @author vyrriox
 */
public final class PermissionHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArcadiaLootbox");
    private static Boolean luckPermsAvailable = null;

    private PermissionHelper() {}

    /**
     * Checks if LuckPerms is loaded. Caches the result for performance.
     */
    public static boolean isLuckPermsLoaded() {
        if (luckPermsAvailable == null) {
            luckPermsAvailable = ModList.get().isLoaded("luckperms");
            if (luckPermsAvailable) {
                LOGGER.info("[ArcadiaLootbox] LuckPerms detected — permission nodes enabled.");
            } else {
                LOGGER.info("[ArcadiaLootbox] LuckPerms not found — using vanilla OP fallback.");
            }
        }
        return luckPermsAvailable;
    }

    /**
     * Checks if a player has a specific permission node.
     * - With LuckPerms: checks the actual permission node
     * - Without LuckPerms: falls back to OP level 2 (admin)
     *
     * @param player the server player
     * @param permission the permission node (e.g., "arcadialootbox.open.rare")
     * @return true if the player has the permission
     */
    public static boolean hasPermission(ServerPlayer player, String permission) {
        if (permission == null || permission.isEmpty()) return true;

        if (isLuckPermsLoaded()) {
            return checkLuckPerms(player, permission);
        }

        // Fallback: OP level 2 grants all lootbox permissions
        return player.hasPermissions(2);
    }

    /**
     * Checks if a player has a specific permission, with a default value if LP is absent.
     * Useful for "everyone gets this unless denied" patterns.
     *
     * @param player the player
     * @param permission the permission node
     * @param defaultWithoutLP what to return if LuckPerms is NOT present
     * @return permission check result
     */
    public static boolean hasPermissionOrDefault(ServerPlayer player, String permission, boolean defaultWithoutLP) {
        if (permission == null || permission.isEmpty()) return true;

        if (isLuckPermsLoaded()) {
            return checkLuckPerms(player, permission);
        }

        return defaultWithoutLP;
    }

    /**
     * Actual LuckPerms check — isolated in its own method so the class loads
     * even when LuckPerms is not present (no static reference to LP classes).
     */
    private static boolean checkLuckPerms(ServerPlayer player, String permission) {
        try {
            // Use arcadia-lib's PermissionService which wraps LuckPerms API
            return com.arcadia.lib.permissions.PermissionService.hasPermission(player, permission);
        } catch (Exception e) {
            // Safe-deny: a transient LP failure must not silently grant the node to every OP.
            LOGGER.debug("[ArcadiaLootbox] LP permission check failed for '{}': {}", permission, e.getMessage());
            return false;
        }
    }

    /**
     * Resets the cached LuckPerms availability (for hot-reload scenarios).
     */
    public static void resetCache() {
        luckPermsAvailable = null;
    }
}
