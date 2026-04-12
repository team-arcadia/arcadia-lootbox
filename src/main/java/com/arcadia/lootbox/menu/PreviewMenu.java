package com.arcadia.lootbox.menu;

import com.arcadia.lib.item.ItemBuilder;
import com.arcadia.lootbox.data.LootboxDefinition;
import com.arcadia.lootbox.manager.FreeLootboxManager;
import com.arcadia.lootbox.util.LanguageHelper;
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
 * Server-side preview GUI with ArcadiaTheme styling.
 * Right-click on lootbox opens this. Slot 49 = "Draw!" button to open the lootbox.
 * No more left-click interaction needed — everything happens through this menu.
 *
 * @author vyrriox
 */
public class PreviewMenu extends ChestMenu {

    private static final int DRAW_BUTTON_SLOT = 49;

    private final String lootboxId;
    private final BlockPos targetPos;
    private final String language;

    public PreviewMenu(int containerId, Inventory playerInv, String id, BlockPos pos,
                       LootboxDefinition def, String language) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, createContainer(def, language, playerInv.player, id), 6);
        this.lootboxId = id;
        this.targetPos = pos;
        this.language = language;
    }

    private static SimpleContainer createContainer(LootboxDefinition def, String language, Player viewer, String lootboxId) {
        SimpleContainer container = new SimpleContainer(54);
        boolean fr = language != null && language.startsWith("fr");

        // ── Border (copper glass panes) ────────────────────────────────
        ItemStack border = ItemBuilder.of(Items.ORANGE_STAINED_GLASS_PANE).name(Component.literal(" ")).build();
        for (int i = 0; i < 9; i++) container.setItem(i, border.copy());
        for (int i = 45; i < 54; i++) container.setItem(i, border.copy());
        for (int row = 1; row < 5; row++) {
            container.setItem(row * 9, border.copy());
            container.setItem(row * 9 + 8, border.copy());
        }

        // ── Info item (slot 4 — top center) ────────────────────────────
        String typeLabel = def.isGuaranteedType()
                ? (fr ? "§aType: Garanti (1 objet + garanti)" : "§aType: Guaranteed (1 item + guaranteed)")
                : (fr ? "§eType: Pondere (% par objet)" : "§eType: Weighted (% per item)");

        ItemBuilder infoBuilder = ItemBuilder.of(Items.NETHER_STAR)
                .name(Component.literal(def.rarityColor() + "§l" + def.displayName()))
                .addLore("§7" + (fr ? "Rarete" : "Rarity") + ": " + def.rarityColor() + (fr ? frRarity(def.rarity()) : def.rarityDisplayName()))
                .addLore("§7" + (fr ? "Cle" : "Key") + ": §f" + def.keyItem())
                .addLore(typeLabel)
                .addLore("§7" + (fr ? "Objets" : "Items") + ": §f" + def.lootTable().size());

        if (def.isGuaranteedType() && def.guaranteedItem() != null && !def.guaranteedItem().isEmpty()) {
            infoBuilder.addLore("§7" + (fr ? "Garanti" : "Guaranteed") + ": §a" + def.guaranteedItem() +
                    " (" + def.guaranteedMinCount() + "-" + def.guaranteedMaxCount() + ")");
        }

        // Free lootbox info
        if (def.freeEnabled() && viewer instanceof ServerPlayer sp) {
            infoBuilder.addLore("");
            if (FreeLootboxManager.canClaim(sp, lootboxId, def)) {
                infoBuilder.addLore(fr ? "§a§lReclamation GRATUITE disponible !" : "§a§lFREE claim available!");
            } else {
                String remaining = FreeLootboxManager.getRemainingFormatted(sp, lootboxId, def);
                infoBuilder.addLore("§7" + (fr ? "Gratuit dans : §e" : "Free in: §e") + remaining);
            }
        }

        container.setItem(4, infoBuilder.enchanted().build());

        // ── Draw button (slot 49 — bottom center) ──────────────────────
        container.setItem(DRAW_BUTTON_SLOT, ItemBuilder.of(Items.TRIPWIRE_HOOK)
                .name(Component.literal("§6§l" + (fr ? "Tirer !" : "Draw!")))
                .addLore(fr ? "§7Cliquez pour ouvrir cette lootbox" : "§7Click to open this lootbox")
                .addLore(fr ? "§7Necessite la cle en main" : "§7Requires key in hand")
                .addLore("")
                .addLore(fr ? "§eClic pour lancer le tirage !" : "§eClick to roll!")
                .enchanted()
                .build());

        // ── Loot items (inner slots, sorted by rarity) ─────────────────
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
            String eName = entry.displayName() != null ? entry.displayName() : entry.item();

            String chanceStr = def.isGuaranteedType()
                    ? String.format("%.2f", entry.chance()) + " " + (fr ? "poids" : "weight")
                    : String.format("%.1f%%", entry.chance() * 100);

            ItemStack display = ItemBuilder.of(item)
                    .name(Component.literal(eColor + eName))
                    .addLore("§7" + (fr ? "Chance" : "Chance") + ": §e" + chanceStr)
                    .addLore("§7" + (fr ? "Quantite" : "Qty") + ": §f" + entry.minCount() + "-" + entry.maxCount())
                    .addLore("§7" + (fr ? "Rarete" : "Rarity") + ": " + eColor + (fr ? frRarity(eRarity) : capitalize(eRarity)))
                    .build();

            if ("legendary".equals(eRarity) || "mythic".equals(eRarity) || "epic".equals(eRarity)) {
                display = ItemBuilder.of(display).enchanted().build();
            }
            container.setItem(slots[idx++], display);
        }
        return container;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Draw button clicked!
        if (slotId == DRAW_BUTTON_SLOT && player instanceof ServerPlayer sp) {
            sp.closeContainer();
            // Trigger lootbox opening via LootHelper
            LootHelper.handleLootboxAttempt(sp.level(), targetPos, sp, lootboxId);
            return;
        }

        // Block all other clicks in the chest area
        if (slotId >= 0 && slotId < 54) return;
        if (clickType == ClickType.QUICK_MOVE) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) {
        return player.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5) <= 64.0;
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
            case "uncommon" -> "Peu commune"; case "rare" -> "Rare"; case "epic" -> "Epique";
            case "legendary" -> "Legendaire"; case "mythic" -> "Mythique"; default -> "Commune";
        };
    }
}
