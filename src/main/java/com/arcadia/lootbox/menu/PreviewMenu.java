package com.arcadia.lootbox.menu;

import com.arcadia.lib.item.ItemBuilder;
import com.arcadia.lootbox.data.LootboxDefinition;
import com.arcadia.lootbox.manager.FreeLootboxManager;
import com.arcadia.lootbox.util.LootHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

/**
 * Server-side lootbox preview GUI.
 * Right-click on lootbox opens this. Slot 49 = "Draw!" button.
 * Fully bilingual EN/FR based on player language.
 *
 * @author vyrriox
 */
public class PreviewMenu extends ChestMenu {

    private static final int DRAW_BUTTON_SLOT = 49;
    private final String lootboxId;
    private final BlockPos targetPos;

    public PreviewMenu(int containerId, Inventory playerInv, String id, BlockPos pos,
                       LootboxDefinition def, String language) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, createContainer(def, language, playerInv.player, id), 6);
        this.lootboxId = id;
        this.targetPos = pos;
    }

    private static SimpleContainer createContainer(LootboxDefinition def, String language, Player viewer, String lootboxId) {
        SimpleContainer c = new SimpleContainer(54);
        boolean fr = language != null && language.startsWith("fr");

        // Display name (FR if available)
        String name = (fr && def.displayNameFR() != null && !def.displayNameFR().isEmpty())
                ? def.displayNameFR() : def.displayName();

        // ── Border ─────────────────────────────────────────────────────
        ItemStack border = ItemBuilder.of(Items.ORANGE_STAINED_GLASS_PANE).name(Component.literal(" ")).build();
        for (int i = 0; i < 9; i++) c.setItem(i, border.copy());
        for (int i = 45; i < 54; i++) c.setItem(i, border.copy());
        for (int row = 1; row < 5; row++) {
            c.setItem(row * 9, border.copy());
            c.setItem(row * 9 + 8, border.copy());
        }

        // ── Info (slot 4) ──────────────────────────────────────────────
        String typeStr = def.isGuaranteedType()
                ? (fr ? "§a\u2714 Garanti §7(1 objet + garanti)" : "§a\u2714 Guaranteed §7(1 item + guaranteed)")
                : (fr ? "§e\u2696 Pondéré §7(% par objet)" : "§e\u2696 Weighted §7(% per item)");

        ItemBuilder info = ItemBuilder.of(Items.NETHER_STAR)
                .name(Component.literal(def.rarityColor() + "§l\u2B50 " + name))
                .addLore("")
                .addLore("§8\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550")
                .addLore("  §7" + (fr ? "Rareté" : "Rarity") + " : " + def.rarityColor() + (fr ? frRarity(def.rarity()) : def.rarityDisplayName()))
                .addLore("  §7" + (fr ? "Clé requise" : "Required Key") + " : §f" + shortItem(def.keyItem()))
                .addLore("  §7Type : " + typeStr)
                .addLore("  §7" + (fr ? "Récompenses" : "Rewards") + " : §f" + def.lootTable().size() + (fr ? " objets" : " items"))
                .addLore("§8\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");

        if (def.isGuaranteedType() && def.guaranteedItem() != null && !def.guaranteedItem().isEmpty()) {
            info.addLore("  §a\u2714 " + (fr ? "Garanti" : "Guaranteed") + " : §f" + shortItem(def.guaranteedItem()) +
                    " §7(" + def.guaranteedMinCount() + "-" + def.guaranteedMaxCount() + ")");
        }

        if (def.freeEnabled() && viewer instanceof ServerPlayer sp) {
            info.addLore("");
            if (FreeLootboxManager.canClaim(sp, lootboxId, def)) {
                info.addLore(fr ? "  §a§l\u2605 Réclamation GRATUITE !" : "  §a§l\u2605 FREE claim available!");
            } else {
                String remaining = FreeLootboxManager.getRemainingFormatted(sp, lootboxId, def);
                info.addLore("  §7" + (fr ? "Gratuit dans" : "Free in") + " : §e" + remaining);
            }
        }

        info.addLore("");
        info.addLore(fr ? "  §e\u25B6 Clic droit avec la clé pour tirer !" : "  §e\u25B6 Right-click with key to draw!");
        c.setItem(4, info.enchanted().build());

        // ── Draw button (slot 49) ──────────────────────────────────────
        c.setItem(DRAW_BUTTON_SLOT, ItemBuilder.of(Items.TRIPWIRE_HOOK)
                .name(Component.literal("§6§l\u2699 " + (fr ? "TIRER !" : "DRAW!")))
                .addLore("")
                .addLore(fr ? "  §7Cliquez pour ouvrir la lootbox" : "  §7Click to open the lootbox")
                .addLore(fr ? "  §7La clé doit être en main" : "  §7Key must be in your hand")
                .addLore("")
                .addLore(fr ? "  §e\u25B6 Clic pour lancer le tirage !" : "  §e\u25B6 Click to roll!")
                .enchanted()
                .build());

        // ── Loot entries (sorted rarest first) ─────────────────────────
        List<LootboxDefinition.LootEntry> sorted = def.lootTable().stream()
                .sorted(Comparator.comparingDouble(LootboxDefinition.LootEntry::chance))
                .toList();

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int idx = 0;
        for (LootboxDefinition.LootEntry entry : sorted) {
            if (idx >= slots.length) break;
            ResourceLocation res = ResourceLocation.tryParse(entry.item());
            if (res == null) continue;
            var item = BuiltInRegistries.ITEM.get(res);
            if (item == Items.AIR) continue;

            String eRarity = entry.rarity() != null ? entry.rarity() : "common";
            String eColor = rarityColor(eRarity);
            String eName = entry.displayName() != null ? entry.displayName() : shortItem(entry.item());
            String eRarityName = fr ? frRarity(eRarity) : capitalize(eRarity);

            String chanceStr = def.isGuaranteedType()
                    ? String.format("%.2f", entry.chance()) + " " + (fr ? "poids" : "weight")
                    : String.format("%.1f%%", entry.chance() * 100);

            ItemStack display = ItemBuilder.of(item)
                    .name(Component.literal(eColor + "§l" + eName))
                    .addLore("")
                    .addLore("  " + eColor + "\u25C6 " + eRarityName)
                    .addLore("  §7" + (fr ? "Probabilité" : "Chance") + " : §e" + chanceStr)
                    .addLore("  §7" + (fr ? "Quantité" : "Quantity") + " : §f" + entry.minCount() + " - " + entry.maxCount())
                    .addLore("")
                    .build();

            if ("legendary".equals(eRarity) || "mythic".equals(eRarity) || "epic".equals(eRarity)) {
                display = ItemBuilder.of(display).enchanted().build();
            }
            c.setItem(slots[idx++], display);
        }
        return c;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == DRAW_BUTTON_SLOT && player instanceof ServerPlayer sp) {
            sp.closeContainer();
            LootHelper.handleLootboxAttempt(sp.level(), targetPos, sp, lootboxId);
            return;
        }
        if (slotId >= 0 && slotId < 54) return;
        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) {
        double maxDist = 8.0;
        try { maxDist = com.arcadia.lootbox.config.LootboxConfig.MAX_INTERACTION_DISTANCE.get(); } catch (Exception ignored) {}
        double maxDistSq = maxDist * maxDist;
        return player.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5) <= maxDistSq;
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static String shortItem(String fullId) {
        if (fullId == null) return "???";
        int colon = fullId.indexOf(':');
        return colon >= 0 ? fullId.substring(colon + 1).replace('_', ' ') : fullId;
    }

    private static String rarityColor(String r) {
        return switch (r.toLowerCase()) {
            case "uncommon" -> "§a"; case "rare" -> "§9"; case "epic" -> "§5";
            case "legendary" -> "§6"; case "mythic" -> "§d"; default -> "§f";
        };
    }

    private static String capitalize(String s) {
        return s == null || s.isEmpty() ? "Common" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private static String frRarity(String r) {
        if (r == null) return "Commune";
        return switch (r.toLowerCase()) {
            case "uncommon" -> "Peu commune"; case "rare" -> "Rare"; case "epic" -> "Épique";
            case "legendary" -> "Légendaire"; case "mythic" -> "Mythique"; default -> "Commune";
        };
    }
}
