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
 * Server-side lootbox preview GUI.
 *
 * <p>Layout (54 slots, 9x6, in three clean zones):
 * <pre>
 *   Row 0 (0-8)   ░░░░ ⭐INFO ░░░░       — top border + info pane (slot 4)
 *   Row 1 (9-17)  ░ items 10-16  ░       — content
 *   Row 2 (18-26) ░ items 19-25  ░
 *   Row 3 (27-35) ░ items 28-34  ░
 *   Row 4 (36-44) ░ F0 F1 .. F6  ░       — rarity filter chips (only present rarities)
 *   Row 5 (45-53) ░ ◀ . ★DRAW . ▶ ░     — action bar: prev (47), Draw (49), next (51)
 * </pre>
 *
 * @author vyrriox
 */
public class PreviewMenu extends ChestMenu {

    // Action bar slots
    private static final int INFO_SLOT = 4;
    private static final int PREV_PAGE_SLOT = 47;
    private static final int DRAW_BUTTON_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 51;

    /** 3 rows × 7 columns = 21 items per page. */
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };
    private static final int ITEMS_PER_PAGE = ITEM_SLOTS.length; // 21

    /** Filter row, centered horizontally (slot 36 = "All", 37-43 = up to 7 rarities). */
    private static final int FILTER_ALL_SLOT = 36;
    private static final int[] FILTER_RARITY_SLOTS = {37, 38, 39, 40, 41, 42, 43};

    /** Rarity render order, highest first. */
    private static final List<String> RARITY_ORDER = List.of(
            "mythic", "legendary", "epic", "rare", "uncommon", "common"
    );

    private final String lootboxId;
    private final BlockPos targetPos;
    private final LootboxDefinition def;
    private final boolean fr;

    private final List<LootboxDefinition.LootEntry> allSorted;
    private final List<String> presentRarities;

    private String filter = null;
    private int page = 0;

    public PreviewMenu(int containerId, Inventory playerInv, String id, BlockPos pos,
                       LootboxDefinition def, String language) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, new SimpleContainer(54), 6);
        this.lootboxId = id;
        this.targetPos = pos;
        this.def = def;
        this.fr = language != null && language.startsWith("fr");

        Comparator<LootboxDefinition.LootEntry> byRarity = Comparator.comparingInt(
                e -> rarityRank(e.rarity()));
        Comparator<LootboxDefinition.LootEntry> byChance = Comparator.comparingDouble(
                LootboxDefinition.LootEntry::chance);
        this.allSorted = def.lootTable().stream()
                .sorted(byRarity.thenComparing(byChance))
                .toList();

        Set<String> seen = new LinkedHashSet<>();
        for (String r : RARITY_ORDER) {
            for (var e : def.lootTable()) {
                if (r.equalsIgnoreCase(e.rarity() != null ? e.rarity() : "common")) {
                    seen.add(r);
                    break;
                }
            }
        }
        this.presentRarities = new ArrayList<>(seen);

        rebuild(playerInv.player);
    }

    private static int rarityRank(String r) {
        if (r == null) return RARITY_ORDER.indexOf("common");
        int idx = RARITY_ORDER.indexOf(r.toLowerCase());
        return idx < 0 ? RARITY_ORDER.size() : idx;
    }

    // ── Build ───────────────────────────────────────────────────────────────

    private void rebuild(Player viewer) {
        var c = this.getContainer();
        clearAll(c);
        drawFrame(c);

        List<LootboxDefinition.LootEntry> visible = filteredItems();
        int totalPages = Math.max(1, (visible.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;

        c.setItem(INFO_SLOT, buildInfo(viewer, visible.size(), totalPages));

        // Items
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, visible.size());
        for (int i = start; i < end; i++) {
            int slotIdx = i - start;
            if (slotIdx >= ITEM_SLOTS.length) break;
            ItemStack icon = buildItemIcon(visible.get(i));
            if (icon != null) c.setItem(ITEM_SLOTS[slotIdx], icon);
        }

        // Filter row — only when more than one rarity exists
        if (presentRarities.size() > 1) {
            c.setItem(FILTER_ALL_SLOT, buildFilterAllChip());
            int chips = Math.min(presentRarities.size(), FILTER_RARITY_SLOTS.length);
            for (int i = 0; i < chips; i++) {
                c.setItem(FILTER_RARITY_SLOTS[i], buildFilterChip(presentRarities.get(i)));
            }
        }

        // Action bar — Draw centered, prev/next on its sides
        c.setItem(DRAW_BUTTON_SLOT, buildDrawButton(viewer));
        if (totalPages > 1) {
            if (page > 0) {
                c.setItem(PREV_PAGE_SLOT, ItemBuilder.of(Items.ARROW)
                        .name(Component.translatable("arcadialootbox.gui.preview.prev"))
                        .addLore("§7Page " + page + "/" + totalPages)
                        .build());
            }
            if (page < totalPages - 1) {
                c.setItem(NEXT_PAGE_SLOT, ItemBuilder.of(Items.ARROW)
                        .name(Component.translatable("arcadialootbox.gui.preview.next"))
                        .addLore("§7Page " + (page + 2) + "/" + totalPages)
                        .build());
            }
        }
    }

    private List<LootboxDefinition.LootEntry> filteredItems() {
        if (filter == null) return allSorted;
        List<LootboxDefinition.LootEntry> out = new ArrayList<>();
        for (var e : allSorted) {
            String r = e.rarity() != null ? e.rarity().toLowerCase() : "common";
            if (r.equals(filter)) out.add(e);
        }
        return out;
    }

    private ItemStack buildInfo(Player viewer, int visibleCount, int totalPages) {
        String name = (fr && def.displayNameFR() != null && !def.displayNameFR().isEmpty())
                ? def.displayNameFR() : def.displayName();

        Component typeLine = Component.translatable(def.isGuaranteedType()
                ? "arcadialootbox.gui.preview.guaranteed_lore"
                : "arcadialootbox.gui.preview.weighted_lore");

        ItemBuilder info = ItemBuilder.of(com.arcadia.lib.LibModItems.ARCADIA_STAR.get())
                .name(Component.literal(def.rarityColor() + "§l⭐ " + name))
                .addLore("")
                .addLore("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
                .addLore(Component.translatable("arcadialootbox.gui.preview.rarity").copy()
                        .append(Component.literal(def.rarityColor()
                                + (fr ? frRarity(def.rarity()) : def.rarityDisplayName()))))
                .addLore(Component.translatable("arcadialootbox.gui.preview.key_required").copy()
                        .append(Component.literal(shortItem(def.keyItem()))))
                .addLore(Component.translatable("arcadialootbox.lore.type", typeLine))
                .addLore(Component.translatable("arcadialootbox.gui.preview.rewards", visibleCount));

        if (def.isGuaranteedType() && def.guaranteedItem() != null && !def.guaranteedItem().isEmpty()) {
            info.addLore("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            info.addLore(Component.translatable("arcadialootbox.gui.preview.guaranteed_section"));
            info.addLore("  §a✔ §f" + shortItem(def.guaranteedItem())
                    + " §7(" + def.guaranteedMinCount() + "-" + def.guaranteedMaxCount() + ")");
        }

        if (filter != null) {
            info.addLore("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            String color = rarityColor(filter);
            String rarityName = fr ? frRarity(filter) : capitalize(filter);
            info.addLore("§7" + (fr ? "Filtre :" : "Filter:") + " " + color + rarityName);
        }

        if (viewer instanceof ServerPlayer sp) {
            info.addLore("§8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            int keysHeld = LootHelper.countKeysInInventory(sp, lootboxId);
            info.addLore(Component.translatable("arcadialootbox.gui.preview.keys_held",
                    String.valueOf(keysHeld)));
            if (def.freeEnabled()) {
                if (FreeLootboxManager.canClaim(sp, lootboxId, def)) {
                    info.addLore(Component.translatable("arcadialootbox.gui.preview.free_claim"));
                } else {
                    String remaining = FreeLootboxManager.getRemainingFormatted(sp, lootboxId, def);
                    info.addLore(Component.translatable("arcadialootbox.gui.preview.free_in", remaining));
                }
            }
        }

        if (totalPages > 1) {
            info.addLore("");
            info.addLore("  §8» §7Page §e" + (page + 1) + "§7/" + totalPages);
        }
        return info.enchanted().build();
    }

    private ItemStack buildItemIcon(LootboxDefinition.LootEntry entry) {
        ResourceLocation res = ResourceLocation.tryParse(entry.item());
        if (res == null) return null;
        var item = BuiltInRegistries.ITEM.get(res);
        if (item == Items.AIR) return null;

        String eRarity = entry.rarity() != null ? entry.rarity() : "common";
        String eColor = rarityColor(eRarity);
        String eName = entry.displayName() != null ? entry.displayName() : shortItem(entry.item());
        String eRarityName = fr ? frRarity(eRarity) : capitalize(eRarity);

        String chanceRaw = def.isGuaranteedType()
                ? String.format(Locale.ROOT, "%.2f", entry.chance())
                : String.format(Locale.ROOT, "%.1f%%", entry.chance() * 100);

        ItemStack display = ItemBuilder.of(item)
                .name(Component.literal(eColor + "§l" + eName))
                .addLore("")
                .addLore("  " + eColor + "◆ §r" + eColor + eRarityName)
                .addLore(Component.literal("  §7").append(
                        Component.translatable("arcadialootbox.gui.preview.chance"))
                        .append(Component.literal(" §8: §e" + chanceRaw)))
                .addLore(Component.literal("  §7").append(
                        Component.translatable("arcadialootbox.gui.preview.quantity"))
                        .append(Component.literal(" §8: §f" + entry.minCount() + "§8-§f" + entry.maxCount())))
                .build();

        if ("legendary".equals(eRarity) || "mythic".equals(eRarity) || "epic".equals(eRarity)) {
            display = ItemBuilder.of(display).enchanted().build();
        }
        return display;
    }

    private ItemStack buildFilterAllChip() {
        boolean active = filter == null;
        ItemBuilder b = ItemBuilder.of(active ? Items.NETHER_STAR : Items.BOOK)
                .name(Component.literal((active ? "§e§l✦ " : "§7") + (fr ? "Toutes les raretés" : "All rarities")))
                .addLore("")
                .addLore("§8▸ §7" + allSorted.size() + (fr ? " objets au total" : " items total"));
        if (active) {
            b.addLore("§8» §a" + (fr ? "Filtre actif" : "Active filter"));
            b.enchanted();
        } else {
            b.addLore("§8» §6" + (fr ? "Clique pour tout afficher" : "Click to show all"));
        }
        return b.build();
    }

    private ItemStack buildFilterChip(String rarity) {
        boolean active = rarity.equals(filter);
        String color = rarityColor(rarity);
        String rarityName = fr ? frRarity(rarity) : capitalize(rarity);
        int count = 0;
        for (var e : allSorted) {
            String r = e.rarity() != null ? e.rarity().toLowerCase() : "common";
            if (r.equals(rarity)) count++;
        }
        ItemBuilder b = ItemBuilder.of(filterIconFor(rarity))
                .name(Component.literal(color + (active ? "§l◆ " : "") + rarityName))
                .addLore("")
                .addLore("§8▸ §7" + count + (fr ? " objet(s)" : " item(s)"));
        if (active) {
            b.addLore("§8» §a" + (fr ? "Filtre actif" : "Active filter"));
            b.enchanted();
        } else {
            b.addLore("§8» §6" + (fr ? "Clique pour filtrer" : "Click to filter"));
        }
        return b.build();
    }

    private ItemStack buildDrawButton(Player viewer) {
        ItemBuilder draw = ItemBuilder.of(Items.TRIPWIRE_HOOK)
                .name(Component.translatable("arcadialootbox.gui.preview.draw"))
                .addLore("")
                .addLore(Component.translatable("arcadialootbox.gui.preview.click_instructions"))
                .addLore(Component.translatable("arcadialootbox.gui.preview.key_deducted"))
                .addLore("");

        if (viewer instanceof ServerPlayer sp) {
            int keysHeld = LootHelper.countKeysInInventory(sp, lootboxId);
            int bulkCap = Math.min(keysHeld, LootHelper.BULK_OPEN_LIMIT);

            if (keysHeld <= 0) {
                draw.addLore(Component.translatable("arcadialootbox.gui.preview.no_keys"));
            } else {
                draw.addLore(Component.translatable("arcadialootbox.gui.preview.draw_left"));
                if (keysHeld > 1) {
                    draw.addLore(Component.translatable("arcadialootbox.gui.preview.draw_right",
                            String.valueOf(bulkCap)));
                    draw.addLore(Component.translatable("arcadialootbox.gui.preview.draw_shift",
                            String.valueOf(bulkCap)));
                }
            }
            draw.addLore("");
            draw.addLore(Component.translatable("arcadialootbox.gui.preview.keys_held",
                    String.valueOf(keysHeld)));
        }
        return draw.enchanted().build();
    }

    // ── Click handling ──────────────────────────────────────────────────────

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.QUICK_MOVE
                || clickType == ClickType.SWAP
                || clickType == ClickType.QUICK_CRAFT
                || clickType == ClickType.THROW
                || clickType == ClickType.CLONE
                || clickType == ClickType.PICKUP_ALL) {
            if (slotId == DRAW_BUTTON_SLOT && clickType == ClickType.QUICK_MOVE
                    && player instanceof ServerPlayer sp) {
                int held = LootHelper.countKeysInInventory(sp, lootboxId);
                int requested = Math.min(held, LootHelper.BULK_OPEN_LIMIT);
                if (requested <= 0) return;
                sp.closeContainer();
                LootHelper.handleBulkLootboxAttempt(sp.level(), targetPos, sp, lootboxId, requested);
            }
            return;
        }

        if (slotId == DRAW_BUTTON_SLOT && player instanceof ServerPlayer sp) {
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

        if (slotId == PREV_PAGE_SLOT && page > 0) {
            page--;
            rebuild(player);
            return;
        }
        int totalPages = Math.max(1, (filteredItems().size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (slotId == NEXT_PAGE_SLOT && page < totalPages - 1) {
            page++;
            rebuild(player);
            return;
        }

        if (slotId == FILTER_ALL_SLOT && presentRarities.size() > 1) {
            filter = null;
            page = 0;
            rebuild(player);
            return;
        }
        if (presentRarities.size() > 1) {
            for (int i = 0; i < FILTER_RARITY_SLOTS.length && i < presentRarities.size(); i++) {
                if (slotId == FILTER_RARITY_SLOTS[i]) {
                    String clicked = presentRarities.get(i);
                    filter = clicked.equals(filter) ? null : clicked;
                    page = 0;
                    rebuild(player);
                    return;
                }
            }
        }

        if (slotId >= 0 && slotId < 54) return;
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

    /**
     * Three-band frame:
     *   - top/sides/middle separator: orange glass (warm tone matches Arcadia theme)
     *   - filter row corners: yellow glass (visual cue: "you can click here")
     *   - action row corners: black glass (anchors the Draw button)
     */
    private void drawFrame(net.minecraft.world.Container c) {
        ItemStack warm = pane(Items.ORANGE_STAINED_GLASS_PANE);
        ItemStack accent = pane(Items.YELLOW_STAINED_GLASS_PANE);
        ItemStack base = pane(Items.BLACK_STAINED_GLASS_PANE);

        // Top row (info bar)
        for (int i = 0; i < 9; i++) {
            if (i != INFO_SLOT) c.setItem(i, warm.copy());
        }
        // Side columns for content rows 1-3
        for (int row = 1; row <= 3; row++) {
            c.setItem(row * 9, warm.copy());
            c.setItem(row * 9 + 8, warm.copy());
        }
        // Filter row: side accents only (chips fill the middle)
        c.setItem(36, accent.copy()); // overwritten if filters shown — placeholder behind
        c.setItem(44, accent.copy());
        // Action row: black base everywhere except prev/draw/next
        for (int i = 45; i < 54; i++) {
            if (i != PREV_PAGE_SLOT && i != DRAW_BUTTON_SLOT && i != NEXT_PAGE_SLOT) {
                c.setItem(i, base.copy());
            }
        }
    }

    private static ItemStack pane(net.minecraft.world.item.Item paneItem) {
        return ItemBuilder.of(paneItem).name(Component.literal(" ")).build();
    }

    private static net.minecraft.world.item.Item filterIconFor(String rarity) {
        return switch (rarity == null ? "common" : rarity.toLowerCase()) {
            case "uncommon" -> Items.LIME_DYE;
            case "rare" -> Items.LAPIS_LAZULI;
            case "epic" -> Items.AMETHYST_SHARD;
            case "legendary" -> Items.GOLD_INGOT;
            case "mythic" -> Items.NETHER_STAR;
            default -> Items.BONE_MEAL;
        };
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
