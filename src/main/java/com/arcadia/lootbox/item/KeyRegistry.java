package com.arcadia.lootbox.item;

import com.arcadia.lootbox.ArcadiaLootbox;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.*;
import java.util.function.Supplier;

/**
 * Registers all 50 lootbox key items and a creative tab.
 *
 * @author vyrriox
 */
public final class KeyRegistry {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ArcadiaLootbox.MODID);

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArcadiaLootbox.MODID);

    private static final List<String> ALL_KEY_IDS = new ArrayList<>();
    private static final Map<String, DeferredItem<Item>> KEY_ITEMS = new LinkedHashMap<>();

    // ── Tier definitions ────────────────────────────────────────────────
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
        registerCategory("dungeon", TIER_10_NAMES, TIER_10_RARITIES);
        registerCategory("shop", TIER_10_NAMES, TIER_10_RARITIES);
        registerCategory("vote", TIER_10_NAMES, TIER_10_RARITIES);
        registerCategory("lootable", TIER_10_NAMES, TIER_10_RARITIES);
        registerCategory("event", EVENT_NAMES, EVENT_RARITIES);
        registerCategory("boss", BOSS_NAMES, BOSS_RARITIES);
    }

    // ── Creative Tab ────────────────────────────────────────────────────
    public static final Supplier<CreativeModeTab> LOOTBOX_TAB = CREATIVE_TABS.register("lootbox_keys",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Arcadia Lootbox Keys"))
                    .icon(() -> {
                        DeferredItem<Item> icon = KEY_ITEMS.get("dungeon_key_legendary");
                        return icon != null ? new ItemStack(icon.get()) : new ItemStack(Items.TRIPWIRE_HOOK);
                    })
                    .displayItems((params, output) -> {
                        for (DeferredItem<Item> item : KEY_ITEMS.values()) {
                            output.accept(item.get());
                        }
                    })
                    .build());

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

    public static List<String> getAllKeyIds() { return Collections.unmodifiableList(ALL_KEY_IDS); }
    public static int getKeyCount() { return ALL_KEY_IDS.size(); }

    public static Item getKeyItem(String id) {
        DeferredItem<Item> deferred = KEY_ITEMS.get(id);
        return deferred != null ? deferred.get() : null;
    }

    public static boolean isKey(String id) { return KEY_ITEMS.containsKey(id); }

    public static String getKeyResourceLocation(String keyId) {
        return ArcadiaLootbox.MODID + ":" + keyId;
    }
}
