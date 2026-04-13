package com.arcadia.lootbox.manager;

import com.arcadia.lib.config.ArcadiaConfigPaths;
import com.arcadia.lootbox.data.LootboxDefinition;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Thread-safe lootbox definition manager with validation and async reload.
 *
 * @author vyrriox
 */
public final class LootboxManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArcadiaLootbox");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile Map<String, LootboxDefinition> LOOTBOXES = new ConcurrentHashMap<>();
    private static volatile boolean initialized = false;
    private static volatile long lastReloadTime = 0;

    private LootboxManager() {}

    public static Path getConfigDir() {
        return ArcadiaConfigPaths.modRoot("arcadialootbox");
    }

    public static void init() {
        if (initialized) return;
        LOGGER.info("[ArcadiaLootbox] LootboxManager initializing...");
        Path dir = getConfigDir();
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            createExamples(dir);
            createTutorial(dir);
            reload();
            initialized = true;
        } catch (IOException e) {
            LOGGER.error("[ArcadiaLootbox] Failed to initialize config directory", e);
        }
    }

    public static int reload() {
        Map<String, LootboxDefinition> loaded = loadFromDisk();
        // Atomic swap — no window where readers see an empty map
        LOOTBOXES = new ConcurrentHashMap<>(loaded);
        lastReloadTime = System.currentTimeMillis();
        LOGGER.info("[ArcadiaLootbox] Loaded {} lootbox definitions", loaded.size());
        return loaded.size();
    }

    public static CompletableFuture<Integer> reloadAsync() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, LootboxDefinition> loaded = loadFromDisk();
            // Atomic swap — no window where readers see an empty map
            LOOTBOXES = new ConcurrentHashMap<>(loaded);
            lastReloadTime = System.currentTimeMillis();
            return loaded.size();
        });
    }

    private static Map<String, LootboxDefinition> loadFromDisk() {
        Map<String, LootboxDefinition> result = new HashMap<>();
        File dir = getConfigDir().toFile();
        if (!dir.exists() || !dir.isDirectory()) return result;

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) return result;

        for (File file : files) {
            try (FileReader reader = new FileReader(file)) {
                LootboxDefinition raw = GSON.fromJson(reader, LootboxDefinition.class);
                if (raw == null) continue;
                LootboxDefinition def = raw.withDefaults();
                String id = file.getName().replace(".json", "");
                if (validate(id, def)) {
                    result.put(id, def);
                }
            } catch (Exception e) {
                LOGGER.error("[ArcadiaLootbox] Failed to load: {}", file.getName(), e);
            }
        }
        return result;
    }

    private static boolean validate(String id, LootboxDefinition def) {
        boolean valid = true;
        for (int i = 0; i < def.lootTable().size(); i++) {
            LootboxDefinition.LootEntry entry = def.lootTable().get(i);
            if (entry.minCount() > entry.maxCount()) {
                LOGGER.error("[ArcadiaLootbox] '{}' entry {} has minCount > maxCount", id, i);
                valid = false;
            }
            if (entry.chance() < 0 || entry.chance() > 1.0) {
                LOGGER.error("[ArcadiaLootbox] '{}' entry {} has invalid chance: {}", id, i, entry.chance());
                valid = false;
            }
        }
        if (def.isGuaranteedType() && (def.guaranteedItem() == null || def.guaranteedItem().isEmpty())) {
            LOGGER.warn("[ArcadiaLootbox] '{}' is guaranteed type but has no guaranteedItem", id);
        }
        return valid;
    }

    public static LootboxDefinition get(String id) { return LOOTBOXES.get(id); }
    public static boolean exists(String id) { return LOOTBOXES.containsKey(id); }
    public static Set<String> getAllIds() { return Collections.unmodifiableSet(LOOTBOXES.keySet()); }
    public static Collection<LootboxDefinition> getAll() { return Collections.unmodifiableCollection(LOOTBOXES.values()); }
    public static Map<String, LootboxDefinition> getAllMap() { return Collections.unmodifiableMap(LOOTBOXES); }
    public static int count() { return LOOTBOXES.size(); }
    public static long getLastReloadTime() { return lastReloadTime; }

    private static boolean isValidId(String id) {
        return id != null && id.matches("[A-Za-z0-9_\\-]+");
    }

    public static boolean createDefinition(String id, LootboxDefinition def) {
        if (!isValidId(id)) return false;
        Path file = getConfigDir().resolve(id + ".json");
        if (!file.toAbsolutePath().startsWith(getConfigDir().toAbsolutePath())) return false;
        if (Files.exists(file)) return false;
        try (FileWriter writer = new FileWriter(file.toFile())) {
            GSON.toJson(def, writer);
            LOOTBOXES.put(id, def.withDefaults());
            return true;
        } catch (IOException e) {
            LOGGER.error("[ArcadiaLootbox] Failed to create: {}", id, e);
            return false;
        }
    }

    public static boolean deleteDefinition(String id) {
        if (!isValidId(id)) return false;
        Path file = getConfigDir().resolve(id + ".json");
        if (!file.toAbsolutePath().startsWith(getConfigDir().toAbsolutePath())) return false;
        if (!Files.exists(file)) return false;
        try {
            Files.delete(file);
            LOOTBOXES.remove(id);
            return true;
        } catch (IOException e) {
            LOGGER.error("[ArcadiaLootbox] Failed to delete: {}", id, e);
            return false;
        }
    }

    // --- Example configs ---

    private static void createExamples(Path dir) {
        // Weighted example
        createIfAbsent(dir, "example_weighted.json", new LootboxDefinition(
                "Treasure Chest", "yellow", "arcadialootbox:shop_key_rare",
                "minecraft:block.chest.open", "§aLootbox opened!",
                List.of(
                        new LootboxDefinition.LootEntry("minecraft:diamond", 1, 3, 0.3, "rare", "Diamond", true),
                        new LootboxDefinition.LootEntry("minecraft:gold_ingot", 2, 5, 0.6, "uncommon", "Gold Ingot", false),
                        new LootboxDefinition.LootEntry("minecraft:iron_ingot", 5, 10, 1.0, "common", "Iron Ingot", false)
                ),
                List.of("minecraft:flame", "minecraft:happy_villager"),
                "weighted", "", 0, 0,
                "rare", true, "", false, -1, "",
                LootboxDefinition.AnimationConfig.defaults(),
                false, 20, "", false, "§6✦ Treasure Chest ✦", "§eGood luck!",
                List.of(), 0, "", 0, true,
                1, "Coffre au Tresor", "§aLootbox ouverte !", "§6✦ Coffre au Tresor ✦", "§eBonne chance !",
                false, 72, "", 48, ""
        ));

        // Guaranteed example (with free lootbox enabled)
        createIfAbsent(dir, "example_guaranteed.json", new LootboxDefinition(
                "Lucky Box", "lime", "arcadialootbox:vote_key_common",
                "minecraft:block.chest.open", "§aYou got something!",
                List.of(
                        new LootboxDefinition.LootEntry("minecraft:diamond", 1, 1, 0.05, "legendary", "Diamond", true),
                        new LootboxDefinition.LootEntry("minecraft:emerald", 1, 3, 0.15, "rare", "Emerald", false),
                        new LootboxDefinition.LootEntry("minecraft:gold_ingot", 2, 5, 0.30, "uncommon", "Gold Ingot", false),
                        new LootboxDefinition.LootEntry("minecraft:iron_ingot", 3, 8, 0.50, "common", "Iron Ingot", false)
                ),
                List.of("minecraft:happy_villager"),
                "guaranteed", "minecraft:bread", 1, 3,
                "uncommon", false, "", false, -1, "",
                LootboxDefinition.AnimationConfig.defaults(),
                false, 20, "", false, "§a✦ Lucky Box ✦", "§7You always get bread + one lucky item!",
                List.of(), 5, "", 0, true,
                2, "Boite Chanceuse", "§aVous avez obtenu quelque chose !", "§a✦ Boite Chanceuse ✦", "§7Vous obtenez toujours du pain + un objet chanceux !",
                true, 72, "", 48, "arcadialootbox.free.reduced"
        ));
    }

    private static void createIfAbsent(Path dir, String filename, LootboxDefinition def) {
        Path file = dir.resolve(filename);
        if (Files.exists(file)) return;
        try (FileWriter writer = new FileWriter(file.toFile())) {
            GSON.toJson(def, writer);
        } catch (IOException e) {
            LOGGER.error("[ArcadiaLootbox] Failed to create {}", filename, e);
        }
    }

    private static void createTutorial(Path dir) {
        Path file = dir.resolve("README.txt");
        if (Files.exists(file)) return;
        String content = """
            ================================================================
                      ARCADIA LOOTBOX v1.2.0 - CONFIG GUIDE
            ================================================================

            TWO LOOTBOX TYPES:
            ===================
            1. "weighted" (default): Each item rolls independently.
               Every item has its own chance %. Multiple items can drop.

            2. "guaranteed": ONE item is picked from the pool (weighted),
               PLUS a guaranteed item always drops. Set fields:
               - "type": "guaranteed"
               - "guaranteedItem": "minecraft:bread"
               - "guaranteedMinCount": 1
               - "guaranteedMaxCount": 3
               In the lootTable, "chance" acts as WEIGHT (higher = more likely).

            KEY ITEMS:
            ==========
            The mod registers 50 key items. Use their IDs in "keyItem":
            - arcadialootbox:dungeon_key_<tier>
            - arcadialootbox:shop_key_<tier>
            - arcadialootbox:vote_key_<tier>
            - arcadialootbox:lootable_key_<tier>
            - arcadialootbox:event_key_<tier>
            - arcadialootbox:boss_key_<tier>

            Standard tiers: common, uncommon, rare, superior, epic,
                           legendary, mythic, divine, celestial, transcendent
            Event tiers: bronze, silver, gold, platinum, diamond
            Boss tiers: minor, major, elite, supreme, overlord

            COMMANDS:
            =========
            /arcadia_lootbox give <player> <id> [amount]
            /arcadia_lootbox giveall <id> [amount]
            /arcadia_lootbox givekey <player> <key_id> [amount]
            /arcadia_lootbox reload
            /arcadia_lootbox list
            /arcadia_lootbox listkeys
            /arcadia_lootbox info <id>
            /arcadia_lootbox preview <player> <id>
            /arcadia_lootbox history <player>
            /arcadia_lootbox clearhistory <player>
            /arcadia_lootbox create <id> <displayName>
            /arcadia_lootbox delete <id>
            /arcadia_lootbox setuses <pos> <uses>
            /arcadia_lootbox resetcooldown <player>
            /arcadia_lootbox stats
            /arcadia_lootbox hub
            """;
        try (FileWriter writer = new FileWriter(file.toFile())) {
            writer.write(content);
        } catch (IOException e) {
            LOGGER.error("[ArcadiaLootbox] Failed to create tutorial", e);
        }
    }
}
