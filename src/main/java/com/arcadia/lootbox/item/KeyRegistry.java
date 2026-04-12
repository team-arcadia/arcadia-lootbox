package com.arcadia.lootbox.item;

import com.arcadia.lootbox.ArcadiaLootbox;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.Supplier;

/**
 * Registers all lootbox key items directly into the mod.
 * 50 keys total: Dungeon(10), Shop(10), Vote(10), Lootable(10), Event(5), Boss(5).
 *
 * @author vyrriox
 */
public final class KeyRegistry {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ArcadiaLootbox.MODID);

    // Store all key IDs for lookup
    private static final List<String> ALL_KEY_IDS = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> KEY_ITEMS = new LinkedHashMap<>();

    // ── Tier definitions ────────────────────────────────────────────────────
    private static final String[] TIER_10_NAMES = {
            "common", "uncommon", "rare", "superior", "epic",
            "legendary", "mythic", "divine", "celestial", "transcendent"
    };
    private static final Rarity[] TIER_10_RARITIES = {
            Rarity.COMMON, Rarity.COMMON, Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.RARE,
            Rarity.RARE, Rarity.EPIC, Rarity.EPIC, Rarity.EPIC, Rarity.EPIC
    };

    private static final String[] EVENT_NAMES = { "bronze", "silver", "gold", "platinum", "diamond" };
    private static final Rarity[] EVENT_RARITIES = { Rarity.COMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.EPIC, Rarity.EPIC };

    private static final String[] BOSS_NAMES = { "minor", "major", "elite", "supreme", "overlord" };
    private static final Rarity[] BOSS_RARITIES = { Rarity.UNCOMMON, Rarity.RARE, Rarity.RARE, Rarity.EPIC, Rarity.EPIC };

    static {
        // Dungeon Keys (10 tiers)
        registerCategory("dungeon", TIER_10_NAMES, TIER_10_RARITIES);
        // Shop Keys (10 tiers)
        registerCategory("shop", TIER_10_NAMES, TIER_10_RARITIES);
        // Vote Keys (10 tiers)
        registerCategory("vote", TIER_10_NAMES, TIER_10_RARITIES);
        // Lootable Keys (10 tiers)
        registerCategory("lootable", TIER_10_NAMES, TIER_10_RARITIES);
        // Event Keys (5 tiers)
        registerCategory("event", EVENT_NAMES, EVENT_RARITIES);
        // Boss Keys (5 tiers)
        registerCategory("boss", BOSS_NAMES, BOSS_RARITIES);
    }

    private static void registerCategory(String category, String[] names, Rarity[] rarities) {
        for (int i = 0; i < names.length; i++) {
            String id = category + "_key_" + names[i];
            Rarity rarity = rarities[i];
            DeferredItem<Item> item = ITEMS.register(id,
                    () -> new LootboxKeyItem(new Item.Properties().stacksTo(64).rarity(rarity)));
            KEY_ITEMS.put(id, item);
            ALL_KEY_IDS.add(id);
        }
    }

    private KeyRegistry() {}

    /**
     * Returns all registered key item IDs.
     */
    public static List<String> getAllKeyIds() {
        return Collections.unmodifiableList(ALL_KEY_IDS);
    }

    /**
     * Returns the total number of registered keys.
     */
    public static int getKeyCount() {
        return ALL_KEY_IDS.size();
    }

    /**
     * Gets a key item by its ID (e.g., "dungeon_key_rare").
     */
    public static Item getKeyItem(String id) {
        DeferredItem<Item> deferred = KEY_ITEMS.get(id);
        return deferred != null ? deferred.get() : null;
    }

    /**
     * Checks if an ID corresponds to a registered key.
     */
    public static boolean isKey(String id) {
        return KEY_ITEMS.containsKey(id);
    }

    /**
     * Returns the full ResourceLocation string for a key (e.g., "arcadialootbox:dungeon_key_rare").
     */
    public static String getKeyResourceLocation(String keyId) {
        return ArcadiaLootbox.MODID + ":" + keyId;
    }
}
