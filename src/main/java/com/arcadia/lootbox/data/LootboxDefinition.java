package com.arcadia.lootbox.data;

import java.util.List;

/**
 * Enhanced lootbox definition loaded from JSON config files.
 * Supports two lootbox types:
 *   - "weighted": each item rolls independently with its own chance %
 *   - "guaranteed": picks ONE item from the pool (weighted random), plus a guaranteed item
 *
 * @author vyrriox
 */
public record LootboxDefinition(
        String displayName,
        String color,
        String keyItem,
        String openSound,
        String openMessage,
        List<LootEntry> lootTable,
        List<String> particles,
        // v1.2.0 — type system
        String type,
        String guaranteedItem,
        int guaranteedMinCount,
        int guaranteedMaxCount,
        // v1.2.0 — rarity & broadcast
        String rarity,
        boolean broadcastRare,
        String broadcastMessage,
        // v1.2.0 — usage & permissions
        boolean destroyOnOpen,
        int maxUses,
        String permission,
        // v1.2.0 — animation
        AnimationConfig animation,
        // v1.2.0 — behavior
        boolean requireSneakToOpen,
        int cooldownTicks,
        String closeSound,
        boolean giveToInventoryOnly,
        String openTitle,
        String openSubtitle,
        // v1.2.0 — advanced rewards
        List<CommandReward> commandRewards,
        double experienceReward,
        // v1.2.0 — conditions
        String requiredBiome,
        int requiredLevel,
        boolean logOpening,
        // v1.2.0 — French translations (optional, falls back to EN fields)
        String displayNameFR,
        String openMessageFR,
        String openTitleFR,
        String openSubtitleFR,
        // v1.2.0 — free lootbox timer
        boolean freeEnabled,
        int freeCooldownHours,
        String freePermission,
        int freeReducedCooldownHours,
        String freeReducedPermission
) {
    public record LootEntry(
            String item,
            int minCount,
            int maxCount,
            double chance,
            String rarity,
            String displayName,
            boolean broadcast
    ) {
        public LootEntry(String item, int minCount, int maxCount, double chance) {
            this(item, minCount, maxCount, chance, "common", null, false);
        }
    }

    public record AnimationConfig(
            String type,
            int durationTicks,
            int particleCount,
            float particleSpeed,
            double particleRadius,
            String particleType,
            boolean playTitleAnimation
    ) {
        public static AnimationConfig defaults() {
            return new AnimationConfig("burst", 20, 15, 0.1f, 0.5, "minecraft:flame", true);
        }
    }

    public record CommandReward(
            String command,
            double chance,
            boolean asConsole
    ) {}

    /**
     * Returns whether this is a "guaranteed" type lootbox (single drop + guaranteed item).
     */
    public boolean isGuaranteedType() {
        return "guaranteed".equalsIgnoreCase(type);
    }

    /**
     * Returns a definition with all null fields replaced by sensible defaults.
     */
    public LootboxDefinition withDefaults() {
        return new LootboxDefinition(
                displayName != null ? displayName : "Lootbox",
                color != null ? color : "white",
                keyItem != null ? keyItem : "minecraft:tripwire_hook",
                openSound != null ? openSound : "minecraft:block.chest.open",
                openMessage != null ? openMessage : "",
                lootTable != null ? lootTable : List.of(),
                particles != null ? particles : List.of("minecraft:flame"),
                type != null ? type : "weighted",
                guaranteedItem != null ? guaranteedItem : "",
                guaranteedMinCount > 0 ? guaranteedMinCount : 1,
                guaranteedMaxCount > 0 ? guaranteedMaxCount : 1,
                rarity != null ? rarity : "common",
                broadcastRare,
                broadcastMessage != null ? broadcastMessage : "",
                destroyOnOpen,
                maxUses > 0 ? maxUses : -1,
                permission != null ? permission : "",
                animation != null ? animation : AnimationConfig.defaults(),
                requireSneakToOpen,
                cooldownTicks > 0 ? cooldownTicks : 20,
                closeSound != null ? closeSound : "",
                giveToInventoryOnly,
                openTitle != null ? openTitle : "",
                openSubtitle != null ? openSubtitle : "",
                commandRewards != null ? commandRewards : List.of(),
                experienceReward,
                requiredBiome != null ? requiredBiome : "",
                requiredLevel,
                logOpening,
                displayNameFR != null ? displayNameFR : "",
                openMessageFR != null ? openMessageFR : "",
                openTitleFR != null ? openTitleFR : "",
                openSubtitleFR != null ? openSubtitleFR : "",
                freeEnabled,
                freeCooldownHours > 0 ? freeCooldownHours : 72,
                freePermission != null ? freePermission : "",
                freeReducedCooldownHours > 0 ? freeReducedCooldownHours : 48,
                freeReducedPermission != null ? freeReducedPermission : ""
        );
    }

    public String rarityColor() {
        if (rarity == null) return "§f";
        return switch (rarity.toLowerCase()) {
            case "common" -> "§f";
            case "uncommon" -> "§a";
            case "rare" -> "§9";
            case "epic" -> "§5";
            case "legendary" -> "§6";
            case "mythic" -> "§d";
            default -> "§f";
        };
    }

    public String rarityDisplayName() {
        if (rarity == null) return "Common";
        return switch (rarity.toLowerCase()) {
            case "common" -> "Common";
            case "uncommon" -> "Uncommon";
            case "rare" -> "Rare";
            case "epic" -> "Epic";
            case "legendary" -> "Legendary";
            case "mythic" -> "Mythic";
            default -> "Common";
        };
    }
}
