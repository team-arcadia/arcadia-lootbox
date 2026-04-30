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

import java.util.*;

/**
 * Server-side lootbox preview GUI with collapsible rarity categories.
 *
 * <p>Two views:
 * <ul>
 *   <li>OVERVIEW — slot 22 = info, rarity buckets in slot row 2/3, Draw button slot 49</li>
 *   <li>CATEGORY — items of a single rarity, with Back button slot 45</li>
 * </ul>
 *
 * <p>Draw click semantics:
 * <ul>
 *   <li>Left-click = open 1 lootbox</li>
 *   <li>Right-click = open up to all keys held (capped at {@link LootHelper#BULK_OPEN_LIMIT})</li>
 *   <li>Shift-click = open up to {@link LootHelper#BULK_OPEN_LIMIT}</li>
 * </ul>
 *
 * @author vyrriox
 */
public class PreviewMenu extends ChestMenu {

    private static final int DRAW_BUTTON_SLOT = 49;
    private static final int BACK_BUTTON_SLOT = 45;
    private static final int INFO_SLOT = 4;

    // Slot grid for items inside a category (4 rows of 7 slots = 28)
    private static final int[] CATEGORY_ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private static final int CATEGORY_ITEMS_PER_PAGE = CATEGORY_ITEM_SLOTS.length;

    // Rarities in display order (low → high)
    private static final List<String> RARITY_ORDER = List.of(
            "common", "uncommon", "rare", "epic", "legendary", "mythic"
    );

    private final String lootboxId;
    private final BlockPos targetPos;
    private final LootboxDefinition def;
    private final boolean fr;

    /** Loot grouped by rarity, in render order. */
    private final LinkedHashMap<String, List<LootboxDefinition.LootEntry>> byRarity;

    /** When non-null, we're showing items of that rarity instead of the overview. */
    private String openCategory = null;
    private int categoryPage = 0;

    public PreviewMenu(int containerId, Inventory playerInv, String id, BlockPos pos,
                       LootboxDefinition def, String language) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, new SimpleContainer(54), 6);
        this.lootboxId = id;
        this.targetPos = pos;
        this.def = def;
        this.fr = language != null && language.startsWith("fr");
        this.byRarity = groupByRarity(def.lootTable());
        rebuild(playerInv.player);
    }

    private static LinkedHashMap<String, List<LootboxDefinition.LootEntry>> groupByRarity(
            List<LootboxDefinition.LootEntry> loot) {
        Map<String, List<LootboxDefinition.LootEntry>> raw = new HashMap<>();
        for (var e : loot) {
            String r = e.rarity() != null ? e.rarity().toLowerCase() : "common";
            raw.computeIfAbsent(r, k -> new ArrayList<>()).add(e);
        }
        LinkedHashMap<String, List<LootboxDefinition.LootEntry>> ordered = new LinkedHashMap<>();
        for (String r : RARITY_ORDER) {
            if (raw.containsKey(r) && !raw.get(r).isEmpty()) {
                List<LootboxDefinition.LootEntry> list = raw.get(r);
                list.sort(Comparator.comparingDouble(LootboxDefinition.LootEntry::chance));
                ordered.put(r, list);
            }
        }
        for (var e : raw.entrySet()) {
            if (!ordered.containsKey(e.getKey())) {
                e.getValue().sort(Comparator.comparingDouble(LootboxDefinition.LootEntry::chance));
                ordered.put(e.getKey(), e.getValue());
            }
        }
        return ordered;
    }

    private void rebuild(Player viewer) {
        if (openCategory != null) buildCategoryView(viewer);
        else buildOverviewView(viewer);
    }

    // ── Overview ────────────────────────────────────────────────────────────

    private void buildOverviewView(Player viewer) {
        var c = this.getContainer();
        clearAll(c);
        drawBorder(c);

        String name = (fr && def.displayNameFR() != null && !def.displayNameFR().isEmpty())
                ? def.displayNameFR() : def.displayName();

        Component typeLine = Component.translatable(def.isGuaranteedType()
                ? "arcadialootbox.gui.preview.guaranteed_lore"
                : "arcadialootbox.gui.preview.weighted_lore");

        ItemBuilder info = ItemBuilder.of(com.arcadia.lib.LibModItems.ARCADIA_STAR.get())
                .name(Component.literal(def.rarityColor() + "§l⭐ " + name))
                .addLore("")
                .addLore("§8════════════════════")
                .addLore(Component.translatable("arcadialootbox.gui.preview.rarity").copy()
                        .append(Component.literal(def.rarityColor() + (fr ? frRarity(def.rarity()) : def.rarityDisplayName()))))
                .addLore(Component.translatable("arcadialootbox.gui.preview.key_required").copy()
                        .append(Component.literal(shortItem(def.keyItem()))))
                .addLore(Component.translatable("arcadialootbox.lore.type", typeLine))
                .addLore(Component.translatable("arcadialootbox.gui.preview.rewards", def.lootTable().size()))
                .addLore("§8════════════════════");

        if (viewer instanceof ServerPlayer sp) {
            int keysHeld = LootHelper.countKeysInInventory(sp, lootboxId);
            info.addLore(Component.translatable("arcadialootbox.gui.preview.keys_held", String.valueOf(keysHeld)));
            if (def.freeEnabled()) {
                if (FreeLootboxManager.canClaim(sp, lootboxId, def)) {
                    info.addLore(Component.translatable("arcadialootbox.gui.preview.free_claim"));
                } else {
                    String remaining = FreeLootboxManager.getRemainingFormatted(sp, lootboxId, def);
                    info.addLore(Component.translatable("arcadialootbox.gui.preview.free_in", remaining));
                }
            }
        }
        info.addLore("");
        info.addLore(Component.translatable("arcadialootbox.gui.preview.click_open"));
        c.setItem(INFO_SLOT, info.enchanted().build());

        // ── Rarity category buckets (row 3, slots 28-34) ─────────────
        List<String> rarities = new ArrayList<>(byRarity.keySet());
        // Center across row 3 (slots 28-34)
        int total = rarities.size();
        int startIdx = Math.max(0, (7 - total) / 2);
        for (int i = 0; i < total && i < 7; i++) {
            String rarity = rarities.get(i);
            List<LootboxDefinition.LootEntry> items = byRarity.get(rarity);
            int slot = 28 + startIdx + i;
            c.setItem(slot, buildBucketIcon(rarity, items));
        }

        // ── Guaranteed item (if any) ─────────────────────────────────
        if (def.isGuaranteedType() && def.guaranteedItem() != null && !def.guaranteedItem().isEmpty()) {
            ResourceLocation gRes = ResourceLocation.tryParse(def.guaranteedItem());
            if (gRes != null) {
                var gItem = BuiltInRegistries.ITEM.get(gRes);
                if (gItem != Items.AIR) {
                    ItemStack guaranteed = ItemBuilder.of(gItem)
                            .name(Component.translatable("arcadialootbox.gui.preview.guaranteed_section"))
                            .addLore("")
                            .addLore("  §a✔ §f" + shortItem(def.guaranteedItem()))
                            .addLore("  §7" + def.guaranteedMinCount() + " - " + def.guaranteedMaxCount())
                            .addLore("")
                            .enchanted()
                            .build();
                    c.setItem(16, guaranteed);
                }
            }
        }

        // ── Draw button ──────────────────────────────────────────────
        if (viewer instanceof ServerPlayer sp) {
            int keysHeld = LootHelper.countKeysInInventory(sp, lootboxId);
            int bulkCap = Math.min(keysHeld, LootHelper.BULK_OPEN_LIMIT);

            ItemBuilder draw = ItemBuilder.of(Items.TRIPWIRE_HOOK)
                    .name(Component.translatable("arcadialootbox.gui.preview.draw"))
                    .addLore("")
                    .addLore(Component.translatable("arcadialootbox.gui.preview.click_instructions"))
                    .addLore(Component.translatable("arcadialootbox.gui.preview.key_deducted"))
                    .addLore("");

            if (keysHeld <= 0) {
                draw.addLore(Component.translatable("arcadialootbox.gui.preview.no_keys"));
            } else {
                draw.addLore(Component.translatable("arcadialootbox.gui.preview.draw_left"));
                if (keysHeld > 1) {
                    draw.addLore(Component.translatable("arcadialootbox.gui.preview.draw_right",
                            String.valueOf(bulkCap)));
                    draw.addLore(Component.translatable("arcadialootbox.gui.preview.draw_shift",
                            String.valueOf(Math.min(keysHeld, LootHelper.BULK_OPEN_LIMIT))));
                }
            }
            draw.addLore("");
            draw.addLore(Component.translatable("arcadialootbox.gui.preview.keys_held", String.valueOf(keysHeld)));

            c.setItem(DRAW_BUTTON_SLOT, draw.enchanted().build());
        } else {
            c.setItem(DRAW_BUTTON_SLOT, ItemBuilder.of(Items.TRIPWIRE_HOOK)
                    .name(Component.translatable("arcadialootbox.gui.preview.draw"))
                    .enchanted().build());
        }
    }

    private ItemStack buildBucketIcon(String rarity, List<LootboxDefinition.LootEntry> items) {
        var iconItem = bucketIconFor(rarity);
        String color = rarityColor(rarity);
        String rarityName = fr ? frRarity(rarity) : capitalize(rarity);

        ItemBuilder b = ItemBuilder.of(iconItem)
                .name(Component.translatable("arcadialootbox.gui.preview.category_title",
                        color + "§l" + rarityName))
                .addLore("")
                .addLore(Component.translatable("arcadialootbox.gui.preview.category_count",
                        String.valueOf(items.size())))
                .addLore("");

        // Preview up to 3 sample item names
        int preview = Math.min(3, items.size());
        for (int i = 0; i < preview; i++) {
            var e = items.get(i);
            String n = e.displayName() != null ? e.displayName() : shortItem(e.item());
            b.addLore("  §7• §f" + n);
        }
        if (items.size() > preview) {
            b.addLore("  §8... +" + (items.size() - preview));
        }
        b.addLore("");
        b.addLore(Component.translatable("arcadialootbox.gui.preview.category_click"));

        if ("legendary".equals(rarity) || "mythic".equals(rarity) || "epic".equals(rarity)) {
            b.enchanted();
        }
        b.count(Math.min(64, Math.max(1, items.size())));
        return b.build();
    }

    private static net.minecraft.world.item.Item bucketIconFor(String rarity) {
        return switch (rarity == null ? "common" : rarity.toLowerCase()) {
            case "uncommon" -> Items.LIME_DYE;
            case "rare" -> Items.LAPIS_LAZULI;
            case "epic" -> Items.AMETHYST_SHARD;
            case "legendary" -> Items.GOLD_INGOT;
            case "mythic" -> Items.NETHER_STAR;
            default -> Items.WHITE_DYE;
        };
    }

    // ── Category view ───────────────────────────────────────────────────────

    private void buildCategoryView(Player viewer) {
        var c = this.getContainer();
        clearAll(c);
        drawBorder(c);

        List<LootboxDefinition.LootEntry> items = byRarity.getOrDefault(openCategory, List.of());
        int totalPages = Math.max(1, (items.size() + CATEGORY_ITEMS_PER_PAGE - 1) / CATEGORY_ITEMS_PER_PAGE);
        if (categoryPage >= totalPages) categoryPage = totalPages - 1;

        String color = rarityColor(openCategory);
        String rarityName = fr ? frRarity(openCategory) : capitalize(openCategory);

        // Info slot — current category title
        ItemBuilder info = ItemBuilder.of(bucketIconFor(openCategory))
                .name(Component.translatable("arcadialootbox.gui.preview.category_title",
                        color + "§l" + rarityName))
                .addLore("")
                .addLore(Component.translatable("arcadialootbox.gui.preview.category_count",
                        String.valueOf(items.size())));
        if (totalPages > 1) {
            info.addLore("  §7Page §e" + (categoryPage + 1) + "§7/" + totalPages);
        }
        c.setItem(INFO_SLOT, info.enchanted().build());

        // Back button
        c.setItem(BACK_BUTTON_SLOT, ItemBuilder.of(Items.ARROW)
                .name(Component.translatable("arcadialootbox.gui.preview.back"))
                .addLore(Component.translatable("arcadialootbox.gui.preview.back_lore"))
                .build());

        // Pagination (slot 53 = next, slot 51 = prev) when needed
        if (totalPages > 1) {
            if (categoryPage > 0) {
                c.setItem(48, ItemBuilder.of(Items.ARROW)
                        .name(Component.translatable("arcadialootbox.gui.preview.prev"))
                        .addLore("§7Page " + categoryPage + "/" + totalPages)
                        .build());
            }
            if (categoryPage < totalPages - 1) {
                c.setItem(50, ItemBuilder.of(Items.ARROW)
                        .name(Component.translatable("arcadialootbox.gui.preview.next"))
                        .addLore("§7Page " + (categoryPage + 2) + "/" + totalPages)
                        .build());
            }
        }

        // Draw button still available from category view
        c.setItem(DRAW_BUTTON_SLOT, ItemBuilder.of(Items.TRIPWIRE_HOOK)
                .name(Component.translatable("arcadialootbox.gui.preview.draw"))
                .addLore("")
                .addLore(Component.translatable("arcadialootbox.gui.preview.click_roll"))
                .enchanted().build());

        // Items for current page
        int startIdx = categoryPage * CATEGORY_ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + CATEGORY_ITEMS_PER_PAGE, items.size());
        for (int i = startIdx; i < endIdx; i++) {
            int slotIdx = i - startIdx;
            if (slotIdx >= CATEGORY_ITEM_SLOTS.length) break;

            LootboxDefinition.LootEntry entry = items.get(i);
            ResourceLocation res = ResourceLocation.tryParse(entry.item());
            if (res == null) continue;
            var item = BuiltInRegistries.ITEM.get(res);
            if (item == Items.AIR) continue;

            String eRarity = entry.rarity() != null ? entry.rarity() : "common";
            String eColor = rarityColor(eRarity);
            String eName = entry.displayName() != null ? entry.displayName() : shortItem(entry.item());

            String chanceRaw = def.isGuaranteedType()
                    ? String.format(Locale.ROOT, "%.2f", entry.chance())
                    : String.format(Locale.ROOT, "%.1f%%", entry.chance() * 100);

            ItemStack display = ItemBuilder.of(item)
                    .name(Component.literal(eColor + "§l" + eName))
                    .addLore("")
                    .addLore(Component.literal("  §7").append(
                            Component.translatable("arcadialootbox.gui.preview.chance"))
                            .append(Component.literal(" : §e" + chanceRaw)))
                    .addLore(Component.literal("  §7").append(
                            Component.translatable("arcadialootbox.gui.preview.quantity"))
                            .append(Component.literal(" : §f" + entry.minCount() + " - " + entry.maxCount())))
                    .addLore("")
                    .build();

            if ("legendary".equals(eRarity) || "mythic".equals(eRarity) || "epic".equals(eRarity)) {
                display = ItemBuilder.of(display).enchanted().build();
            }
            c.setItem(CATEGORY_ITEM_SLOTS[slotIdx], display);
        }
    }

    // ── Click handling ──────────────────────────────────────────────────────

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // BLOCK any item movement out of the GUI: shift-click, double-click, swap, drop, etc.
        if (clickType == ClickType.QUICK_MOVE
                || clickType == ClickType.SWAP
                || clickType == ClickType.QUICK_CRAFT
                || clickType == ClickType.THROW
                || clickType == ClickType.CLONE
                || clickType == ClickType.PICKUP_ALL) {
            // For the Draw button, treat shift-click as a bulk-open instead of a quick-move.
            if (slotId == DRAW_BUTTON_SLOT && clickType == ClickType.QUICK_MOVE
                    && openCategory == null && player instanceof ServerPlayer sp) {
                int held = LootHelper.countKeysInInventory(sp, lootboxId);
                int requested = Math.min(held, LootHelper.BULK_OPEN_LIMIT);
                if (requested <= 0) return;
                sp.closeContainer();
                LootHelper.handleBulkLootboxAttempt(sp.level(), targetPos, sp, lootboxId, requested);
            }
            return;
        }

        // Draw button (overview only — in category view we keep it too for convenience)
        if (slotId == DRAW_BUTTON_SLOT && player instanceof ServerPlayer sp) {
            // button: 0 = left, 1 = right
            if (button == 1) {
                int held = LootHelper.countKeysInInventory(sp, lootboxId);
                int requested = Math.min(held, LootHelper.BULK_OPEN_LIMIT);
                if (requested <= 0) return;
                sp.closeContainer();
                LootHelper.handleBulkLootboxAttempt(sp.level(), targetPos, sp, lootboxId, requested);
            } else {
                sp.closeContainer();
                LootHelper.handleLootboxAttempt(sp.level(), targetPos, sp, lootboxId);
            }
            return;
        }

        // Back button (category view only)
        if (slotId == BACK_BUTTON_SLOT && openCategory != null) {
            openCategory = null;
            categoryPage = 0;
            rebuild(player);
            return;
        }

        // Pagination in category view
        if (openCategory != null) {
            if (slotId == 48 && categoryPage > 0) {
                categoryPage--;
                rebuild(player);
                return;
            }
            int totalPages = Math.max(1, (byRarity.getOrDefault(openCategory, List.of()).size()
                    + CATEGORY_ITEMS_PER_PAGE - 1) / CATEGORY_ITEMS_PER_PAGE);
            if (slotId == 50 && categoryPage < totalPages - 1) {
                categoryPage++;
                rebuild(player);
                return;
            }
        }

        // Bucket click in overview → open category
        if (openCategory == null && slotId >= 28 && slotId <= 34) {
            int idx = slotId - 28;
            List<String> rarities = new ArrayList<>(byRarity.keySet());
            int total = rarities.size();
            int startIdx = Math.max(0, (7 - total) / 2);
            int rarityIdx = idx - startIdx;
            if (rarityIdx >= 0 && rarityIdx < total) {
                openCategory = rarities.get(rarityIdx);
                categoryPage = 0;
                rebuild(player);
                return;
            }
        }

        // Block all other clicks in the chest area
        if (slotId >= 0 && slotId < 54) return;

        // Player-inventory clicks: block them entirely so nothing is ever moved/swapped here
        // (super.clicked would handle drops outside the GUI; we disable that too)
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) {
        double maxDist = 8.0;
        try { maxDist = com.arcadia.lootbox.config.LootboxConfig.MAX_INTERACTION_DISTANCE.get(); } catch (Exception ignored) {}
        double maxDistSq = maxDist * maxDist;
        return player.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5) <= maxDistSq;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private static void clearAll(net.minecraft.world.Container c) {
        for (int i = 0; i < 54; i++) c.setItem(i, ItemStack.EMPTY);
    }

    private void drawBorder(net.minecraft.world.Container c) {
        ItemStack border = ItemBuilder.of(Items.ORANGE_STAINED_GLASS_PANE).name(Component.literal(" ")).build();
        for (int i = 0; i < 9; i++) c.setItem(i, border.copy());
        for (int i = 45; i < 54; i++) c.setItem(i, border.copy());
        for (int row = 1; row < 5; row++) {
            c.setItem(row * 9, border.copy());
            c.setItem(row * 9 + 8, border.copy());
        }
    }

    private static String shortItem(String fullId) {
        if (fullId == null) return "???";
        int colon = fullId.indexOf(':');
        return colon >= 0 ? fullId.substring(colon + 1).replace('_', ' ') : fullId;
    }

    private static String rarityColor(String r) {
        if (r == null) return "§f";
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
