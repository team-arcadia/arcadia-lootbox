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
 * Server-side lootbox preview GUI with pagination.
 * Right-click on lootbox opens this. Slot 49 = "Draw!" button.
 * Slots 46/52 = prev/next page buttons when items > 28.
 *
 * @author vyrriox
 */
public class PreviewMenu extends ChestMenu {

    private static final int DRAW_BUTTON_SLOT = 49;
    private static final int PREV_PAGE_SLOT = 46;
    private static final int NEXT_PAGE_SLOT = 52;
    private static final int[] ITEM_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.length; // 28

    private final String lootboxId;
    private final BlockPos targetPos;
    private final LootboxDefinition def;
    private final String language;
    private final List<LootboxDefinition.LootEntry> sortedLoot;
    private int currentPage = 0;

    public PreviewMenu(int containerId, Inventory playerInv, String id, BlockPos pos,
                       LootboxDefinition def, String language) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, new SimpleContainer(54), 6);
        this.lootboxId = id;
        this.targetPos = pos;
        this.def = def;
        this.language = language;
        this.sortedLoot = def.lootTable().stream()
                .sorted(Comparator.comparingDouble(LootboxDefinition.LootEntry::chance))
                .toList();
        buildPage(playerInv.player);
    }

    private int totalPages() {
        return Math.max(1, (sortedLoot.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    }

    private void buildPage(Player viewer) {
        var c = this.getContainer();
        boolean fr = language != null && language.startsWith("fr");

        // Clear all slots
        for (int i = 0; i < 54; i++) c.setItem(i, ItemStack.EMPTY);

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
        Component typeLine = Component.translatable(def.isGuaranteedType()
                ? "arcadialootbox.gui.preview.guaranteed_lore"
                : "arcadialootbox.gui.preview.weighted_lore");

        ItemBuilder info = ItemBuilder.of(com.arcadia.lib.LibModItems.ARCADIA_STAR.get())
                .name(Component.literal(def.rarityColor() + "§l\u2B50 " + name))
                .addLore("")
                .addLore("§8\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550")
                .addLore(Component.translatable("arcadialootbox.gui.preview.rarity").copy()
                        .append(Component.literal(def.rarityColor() + (fr ? frRarity(def.rarity()) : def.rarityDisplayName()))))
                .addLore(Component.translatable("arcadialootbox.gui.preview.key_required").copy()
                        .append(Component.literal(shortItem(def.keyItem()))))
                .addLore(Component.translatable("arcadialootbox.lore.type", typeLine))
                .addLore(Component.translatable("arcadialootbox.gui.preview.rewards", def.lootTable().size()))
                .addLore("§8\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550");

        if (def.isGuaranteedType() && def.guaranteedItem() != null && !def.guaranteedItem().isEmpty()) {
            info.addLore("  §a\u2714 §f" + shortItem(def.guaranteedItem())
                    + " §7(" + def.guaranteedMinCount() + "-" + def.guaranteedMaxCount() + ")");
        }
        if (def.freeEnabled() && viewer instanceof ServerPlayer sp) {
            info.addLore("");
            if (FreeLootboxManager.canClaim(sp, lootboxId, def)) {
                info.addLore(Component.translatable("arcadialootbox.gui.preview.free_claim"));
            } else {
                String remaining = FreeLootboxManager.getRemainingFormatted(sp, lootboxId, def);
                info.addLore(Component.translatable("arcadialootbox.gui.preview.free_in", remaining));
            }
        }
        if (totalPages() > 1) {
            info.addLore("");
            info.addLore("  §7Page §e" + (currentPage + 1) + "§7/" + totalPages());
        }
        info.addLore("");
        info.addLore(Component.translatable("arcadialootbox.gui.preview.click_open"));
        c.setItem(4, info.enchanted().build());

        // ── Draw button (slot 49) ──────────────────────────────────────
        c.setItem(DRAW_BUTTON_SLOT, ItemBuilder.of(Items.TRIPWIRE_HOOK)
                .name(Component.translatable("arcadialootbox.gui.preview.draw"))
                .addLore("")
                .addLore(Component.translatable("arcadialootbox.gui.preview.click_instructions"))
                .addLore(Component.translatable("arcadialootbox.gui.preview.key_deducted"))
                .addLore("")
                .addLore(Component.translatable("arcadialootbox.gui.preview.click_roll"))
                .enchanted()
                .build());

        // ── Pagination buttons ─────────────────────────────────────────
        if (totalPages() > 1) {
            if (currentPage > 0) {
                c.setItem(PREV_PAGE_SLOT, ItemBuilder.of(Items.ARROW)
                        .name(Component.translatable("arcadialootbox.gui.preview.prev"))
                        .addLore("§7Page " + currentPage + "/" + totalPages())
                        .build());
            }
            if (currentPage < totalPages() - 1) {
                c.setItem(NEXT_PAGE_SLOT, ItemBuilder.of(Items.ARROW)
                        .name(Component.translatable("arcadialootbox.gui.preview.next"))
                        .addLore("§7Page " + (currentPage + 2) + "/" + totalPages())
                        .build());
            }
        }

        // ── Loot entries for current page ──────────────────────────────
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, sortedLoot.size());

        for (int i = startIdx; i < endIdx; i++) {
            int slotIdx = i - startIdx;
            if (slotIdx >= ITEM_SLOTS.length) break;

            LootboxDefinition.LootEntry entry = sortedLoot.get(i);
            ResourceLocation res = ResourceLocation.tryParse(entry.item());
            if (res == null) continue;
            var item = BuiltInRegistries.ITEM.get(res);
            if (item == Items.AIR) continue;

            String eRarity = entry.rarity() != null ? entry.rarity() : "common";
            String eColor = rarityColor(eRarity);
            String eName = entry.displayName() != null ? entry.displayName() : shortItem(entry.item());
            String eRarityName = fr ? frRarity(eRarity) : capitalize(eRarity);

            String chanceRaw = def.isGuaranteedType()
                    ? String.format("%.2f", entry.chance())
                    : String.format("%.1f%%", entry.chance() * 100);

            ItemStack display = ItemBuilder.of(item)
                    .name(Component.literal(eColor + "§l" + eName))
                    .addLore("")
                    .addLore("  " + eColor + "\u25C6 " + eRarityName)
                    .addLore(Component.literal("  §7").append(
                            Component.translatable("arcadialootbox.gui.preview.chance"))
                            .append(Component.literal(" : §e" + chanceRaw
                                    + (def.isGuaranteedType() ? " " : ""))))
                    .addLore(Component.literal("  §7").append(
                            Component.translatable("arcadialootbox.gui.preview.quantity"))
                            .append(Component.literal(" : §f" + entry.minCount() + " - " + entry.maxCount())))
                    .addLore("")
                    .build();

            if ("legendary".equals(eRarity) || "mythic".equals(eRarity) || "epic".equals(eRarity)) {
                display = ItemBuilder.of(display).enchanted().build();
            }
            c.setItem(ITEM_SLOTS[slotIdx], display);
        }
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Draw button
        if (slotId == DRAW_BUTTON_SLOT && player instanceof ServerPlayer sp) {
            sp.closeContainer();
            LootHelper.handleLootboxAttempt(sp.level(), targetPos, sp, lootboxId);
            return;
        }
        // Pagination
        if (slotId == PREV_PAGE_SLOT && currentPage > 0) {
            currentPage--;
            buildPage(player);
            return;
        }
        if (slotId == NEXT_PAGE_SLOT && currentPage < totalPages() - 1) {
            currentPage++;
            buildPage(player);
            return;
        }
        // Block all other clicks in chest area
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
