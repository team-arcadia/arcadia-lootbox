package com.vyrriox.arcadialootbox.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vyrriox.arcadialootbox.data.LootboxDefinition;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages loading and retrieving Lootbox definitions from JSON files.
 * 
 * @author vyrriox
 */
public class LootboxManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LootboxManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, LootboxDefinition> LOOTBOXES = new HashMap<>();

    private static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get().resolve("arcadia/arcadialootbox");
    }

    public static void init() {
        LOGGER.info("LootboxManager: Initializing...");
        Path dir = getConfigDir();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                LOGGER.info("LootboxManager: Created config directory at " + dir);
            } catch (IOException e) {
                LOGGER.error("Failed to create config directory", e);
            }
        }
        
        // Ensure defaults exist
        createExample(dir);
        createTutorial(dir);
        
        reload();
    }

    private static void createExample(Path dir) {
        Path exampleFile = dir.resolve("example.json");
        if (Files.exists(exampleFile)) return;

        try {
            LootboxDefinition example = new LootboxDefinition(
                    "Coffre au Trésor",
                    "yellow",
                    "minecraft:tripwire_hook",
                    "minecraft:block.chest.open",
                    "§aLootbox ouverte !",
                    java.util.List.of(
                            new LootboxDefinition.LootEntry("minecraft:diamond", 1, 3, 0.5),
                            new LootboxDefinition.LootEntry("minecraft:gold_ingot", 2, 5, 0.8),
                            new LootboxDefinition.LootEntry("minecraft:iron_ingot", 5, 10, 1.0)
                    ),
                    java.util.List.of("minecraft:flame", "minecraft:happy_villager")
            );

            try (FileWriter writer = new FileWriter(exampleFile.toFile())) {
                GSON.toJson(example, writer);
                LOGGER.info("LootboxManager: Created example.json");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to create example config", e);
        }
    }

    private static void createTutorial(Path dir) {
        Path tutorialFile = dir.resolve("README.txt");
        if (Files.exists(tutorialFile)) return;

        String content = """
            ================================================================
                              ARCADIA LOOTBOX - CONFIG GUIDE
                                 [ENGLISH & FRANÇAIS]
            ================================================================
            
            [FR] GUIDE FRANÇAIS
            ===================
            Comment créer une nouvelle Lootbox :
            1. Créez un nouveau fichier .json dans ce dossier (ex: `legendaire.json`).
            2. Le nom du fichier sera l'ID unique de la lootbox (ex: `legendaire`).
            3. Copiez la structure JSON ci-dessous et modifiez les valeurs.
            
            Structure du JSON :
            -------------------
            {
              "displayName": "Nom Visible du Joueur",
              "color": "yellow",  <-- Couleurs disponibles : white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black
              "keyItem": "minecraft:tripwire_hook",  <-- ID de l'item nécessaire pour ouvrir (Main Hand)
              "openSound": "minecraft:block.chest.open", <-- Son joué à l'ouverture (Optionnel)
              "openMessage": "§aBravo !", <-- Message envoyé dans le chat (Codes couleurs § supportés)
              "lootTable": [
                {
                  "item": "minecraft:diamond",
                  "minCount": 1,
                  "maxCount": 3,
                  "chance": 0.5  <-- 0.5 = 50%, 1.0 = 100%, 0.01 = 1%
                }
              ],
              "particles": [
                "minecraft:flame",
                "minecraft:happy_villager"
              ]
            }
            
            Commandes (Admin) :
            -------------------
            - /arcadialoot give <joueur> <id>  : Donne la lootbox (Block Shulker) au joueur.
            - /arcadialoot reload            : Recharge la configuration sans redémarrer le serveur.
            
            Astuces :
            - Vous pouvez utiliser n'importe quel item comme clé (même des items moddés).
            - Si "openMessage" est vide, un message par défaut sera utilisé.
            
            
            ================================================================
            
            [EN] ENGLISH GUIDE
            ==================
            How to create a new Lootbox:
            1. Create a new .json file in this folder (e.g., `legendary.json`).
            2. The filename becomes the unique lootbox ID (e.g., `legendary`).
            3. Copy the JSON structure below and edit the values.
            
            JSON Structure:
            ---------------
            {
              "displayName": "Display Name",
              "color": "yellow",  <-- Available colors: white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black
              "keyItem": "minecraft:tripwire_hook",  <-- Item ID required to open (Main Hand)
              "openSound": "minecraft:block.chest.open", <-- Sound played on open (Optional)
              "openMessage": "§aCongrats!", <-- Chat message on open (Supports § color codes)
              "lootTable": [
                {
                  "item": "minecraft:diamond",
                  "minCount": 1,
                  "maxCount": 3,
                  "chance": 0.5  <-- 0.5 = 50%, 1.0 = 100%, 0.01 = 1%
                }
              ],
              "particles": [
                "minecraft:flame",
                "minecraft:happy_villager"
              ]
            }
            
            Commands (Admin):
            -----------------
            - /arcadialoot give <player> <id>  : Gives the lootbox (Shulker Block) to the player.
            - /arcadialoot reload            : Reloads the configuration without restarting.
            
            Tips:
            - You can use any item specific key (even modded items).
            - If "openMessage" is empty, a default message is used.
            """;

        try (FileWriter writer = new FileWriter(tutorialFile.toFile())) {
            writer.write(content);
            LOGGER.info("LootboxManager: Created README.txt tutorial");
        } catch (IOException e) {
            LOGGER.error("Failed to create tutorial file", e);
        }
    }

    public static void reload() {
        LOOTBOXES.clear();
        File dir = getConfigDir().toFile();
        LOGGER.info("LootboxManager: Reloading from " + dir.getAbsolutePath());
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                if (files.length == 0) LOGGER.info("LootboxManager: No JSON files found.");
                for (File file : files) {
                    try (FileReader reader = new FileReader(file)) {
                        LootboxDefinition def = GSON.fromJson(reader, LootboxDefinition.class);
                        String id = file.getName().replace(".json", "");
                        LOOTBOXES.put(id, def);
                        LOGGER.info("Loaded lootbox: " + id);
                    } catch (Exception e) {
                        LOGGER.error("Failed to load lootbox config: " + file.getName(), e);
                    }
                }
            }
        } else {
             LOGGER.warn("LootboxManager: Config directory missing or invalid.");
        }
    }

    public static LootboxDefinition get(String id) {
        return LOOTBOXES.get(id);
    }

    public static boolean exists(String id) {
        return LOOTBOXES.containsKey(id);
    }
}
