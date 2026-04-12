package com.arcadia.lootbox.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Global TOML configuration for Arcadia Lootbox.
 *
 * @author vyrriox
 */
public final class LootboxConfig {

    public static final ModConfigSpec SPEC;

    // General
    public static final ModConfigSpec.IntValue DEFAULT_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue MAX_LOOTBOXES_PER_CHUNK;
    public static final ModConfigSpec.BooleanValue DESTROY_ON_OPEN_DEFAULT;
    public static final ModConfigSpec.BooleanValue LOG_ALL_OPENINGS;
    public static final ModConfigSpec.BooleanValue REQUIRE_SNEAK_DEFAULT;

    // Broadcast
    public static final ModConfigSpec.BooleanValue BROADCAST_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_FORMAT;
    public static final ModConfigSpec.ConfigValue<String> BROADCAST_RARE_FORMAT;
    public static final ModConfigSpec.IntValue BROADCAST_MIN_RARITY;

    // Hub / Shop
    public static final ModConfigSpec.BooleanValue HUB_ENABLED;
    public static final ModConfigSpec.ConfigValue<String> SHOP_URL;
    public static final ModConfigSpec.ConfigValue<String> SHOP_DISPLAY_NAME;

    // Performance
    public static final ModConfigSpec.IntValue PARTICLE_LIMIT;
    public static final ModConfigSpec.IntValue MAX_DROPS_PER_OPEN;
    public static final ModConfigSpec.BooleanValue ASYNC_CONFIG_RELOAD;
    public static final ModConfigSpec.IntValue HISTORY_MAX_ENTRIES;

    // Security
    public static final ModConfigSpec.DoubleValue MAX_INTERACTION_DISTANCE;
    public static final ModConfigSpec.BooleanValue ANTI_AUTOCLICKER;
    public static final ModConfigSpec.IntValue ANTI_AUTOCLICKER_THRESHOLD;

    // Animation
    public static final ModConfigSpec.BooleanValue ANIMATIONS_ENABLED;
    public static final ModConfigSpec.BooleanValue TITLE_ON_OPEN;
    public static final ModConfigSpec.IntValue TITLE_FADE_IN;
    public static final ModConfigSpec.IntValue TITLE_STAY;
    public static final ModConfigSpec.IntValue TITLE_FADE_OUT;

    // Sounds
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_OPEN_SOUND;
    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CLOSE_SOUND;
    public static final ModConfigSpec.DoubleValue SOUND_VOLUME;
    public static final ModConfigSpec.DoubleValue SOUND_PITCH;

    // Free Lootbox
    public static final ModConfigSpec.BooleanValue FREE_LOOTBOX_ENABLED;
    public static final ModConfigSpec.IntValue FREE_DEFAULT_COOLDOWN_HOURS;
    public static final ModConfigSpec.IntValue FREE_REDUCED_COOLDOWN_HOURS;
    public static final ModConfigSpec.ConfigValue<String> FREE_REDUCED_PERMISSION;
    public static final ModConfigSpec.IntValue FREE_AUTOSAVE_MINUTES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Arcadia Lootbox v1.2.0 - Global Configuration").push("general");
        DEFAULT_COOLDOWN_TICKS = builder.comment("Default cooldown in ticks (20 = 1s)").defineInRange("defaultCooldownTicks", 20, 1, 6000);
        MAX_LOOTBOXES_PER_CHUNK = builder.comment("Max lootboxes per chunk (0 = unlimited)").defineInRange("maxLootboxesPerChunk", 0, 0, 256);
        DESTROY_ON_OPEN_DEFAULT = builder.comment("Destroy lootbox block after opening by default").define("destroyOnOpenDefault", false);
        LOG_ALL_OPENINGS = builder.comment("Log all openings to server log").define("logAllOpenings", true);
        REQUIRE_SNEAK_DEFAULT = builder.comment("Require sneaking to open by default").define("requireSneakDefault", false);
        builder.pop();

        builder.comment("Broadcast settings").push("broadcast");
        BROADCAST_ENABLED = builder.define("enabled", true);
        BROADCAST_FORMAT = builder.define("format", "§6⚙ §e{player} §7opened §e{lootbox} §7and got §f{count}x §e{item}§7!");
        BROADCAST_RARE_FORMAT = builder.define("rareFormat", "§6⚙ §d✦ §e{player} §7found {rarity_color}{rarity} §7item: §f{count}x §e{item} §7from §e{lootbox}§7! §d✦");
        BROADCAST_MIN_RARITY = builder.comment("Min rarity for broadcast (0=common..5=mythic)").defineInRange("minRarity", 2, 0, 5);
        builder.pop();

        builder.comment("Arcadia Hub & Shop").push("hub");
        HUB_ENABLED = builder.define("enabled", true);
        SHOP_URL = builder.define("shopUrl", "https://store.yourserver.com/lootbox");
        SHOP_DISPLAY_NAME = builder.define("shopDisplayName", "Buy Lootbox Keys");
        builder.pop();

        builder.comment("Performance").push("performance");
        PARTICLE_LIMIT = builder.defineInRange("particleLimit", 15, 1, 100);
        MAX_DROPS_PER_OPEN = builder.defineInRange("maxDropsPerOpen", 10, 1, 54);
        ASYNC_CONFIG_RELOAD = builder.define("asyncConfigReload", true);
        HISTORY_MAX_ENTRIES = builder.defineInRange("historyMaxEntries", 100, 0, 10000);
        builder.pop();

        builder.comment("Security").push("security");
        MAX_INTERACTION_DISTANCE = builder.defineInRange("maxInteractionDistance", 8.0, 1.0, 64.0);
        ANTI_AUTOCLICKER = builder.define("antiAutoclicker", true);
        ANTI_AUTOCLICKER_THRESHOLD = builder.defineInRange("antiAutoclickerThreshold", 10, 1, 100);
        builder.pop();

        builder.comment("Animation").push("animation");
        ANIMATIONS_ENABLED = builder.define("enabled", true);
        TITLE_ON_OPEN = builder.define("titleOnOpen", true);
        TITLE_FADE_IN = builder.defineInRange("titleFadeIn", 5, 0, 100);
        TITLE_STAY = builder.defineInRange("titleStay", 30, 0, 200);
        TITLE_FADE_OUT = builder.defineInRange("titleFadeOut", 10, 0, 100);
        builder.pop();

        builder.comment("Sounds").push("sounds");
        DEFAULT_OPEN_SOUND = builder.define("defaultOpenSound", "minecraft:block.chest.open");
        DEFAULT_CLOSE_SOUND = builder.define("defaultCloseSound", "");
        SOUND_VOLUME = builder.defineInRange("volume", 1.0, 0.0, 2.0);
        SOUND_PITCH = builder.defineInRange("pitch", 1.0, 0.1, 2.0);
        builder.pop();

        builder.comment("Free timed lootbox system").push("free");
        FREE_LOOTBOX_ENABLED = builder.comment("Enable free timed lootboxes globally").define("enabled", true);
        FREE_DEFAULT_COOLDOWN_HOURS = builder.comment("Default cooldown in hours between free claims (72 = 3 days)").defineInRange("defaultCooldownHours", 72, 1, 8760);
        FREE_REDUCED_COOLDOWN_HOURS = builder.comment("Reduced cooldown for players with the reduced permission").defineInRange("reducedCooldownHours", 48, 1, 8760);
        FREE_REDUCED_PERMISSION = builder.comment("Permission node for reduced cooldown (empty = disabled)").define("reducedPermission", "arcadialootbox.free.reduced");
        FREE_AUTOSAVE_MINUTES = builder.comment("Auto-save free claim data interval in minutes").defineInRange("autosaveMinutes", 5, 1, 60);
        builder.pop();

        SPEC = builder.build();
    }

    private LootboxConfig() {}

    public static int rarityToLevel(String rarity) {
        if (rarity == null) return 0;
        return switch (rarity.toLowerCase()) {
            case "uncommon" -> 1;
            case "rare" -> 2;
            case "epic" -> 3;
            case "legendary" -> 4;
            case "mythic" -> 5;
            default -> 0;
        };
    }
}
