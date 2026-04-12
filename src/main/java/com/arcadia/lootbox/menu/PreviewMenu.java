package com.arcadia.lootbox.menu;

import com.arcadia.lib.item.ItemBuilder;
import com.arcadia.lootbox.data.LootboxDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * Enhanced server-side preview GUI with border, rarity sorting, and dual-type display.
 *
 * @author vyrriox
 */
public class PreviewMenu extends ChestMenu {

    private final String lootboxId;
    private final BlockPos targetPos;

    public PreviewMenu(int containerId, Inventory playerInv, String id, BlockPos pos,
                       LootboxDefinition def, String language) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, createContainer(def, language), 6);
        this.lootboxId = id;
        this.targetPos = pos;
    }

    private static SimpleContainer createContainer(LootboxDefinition def, String language) {
        SimpleContainer container = new SimpleContainer(54);
        boolean isFrench = language != null && language.startsWith("fr");

        // Border
        ItemStack border = ItemBuilder.of(Items.ORANGE_STAINED_GLASS_PANE).name(Component.literal(" ")).build();
        for (int i = 0; i < 9; i++) container.setItem(i, border.copy());
        for (int i = 45; i < 54; i++) container.setItem(i, border.copy());
        for (int row = 1; row < 5; row++) {
            container.setItem(row * 9, border.copy());
            container.setItem(row * 9 + 8, border.copy());
        }

        // Info item
        String typeLabel = def.isGuaranteedType()
                ? (isFrench ? "§aType: Garanti (1 item + garanti)" : "§aType: Guaranteed (1 item + guaranteed)")
                : (isFrench ? "§eType: Pondéré (% par item)" : "§eType: Weighted (% per item)");

        var infoBuilder = ItemBuilder.of(Items.NETHER_STAR)
                .name(Component.literal(def.rarityColor() + "§l" + def.displayName()))
                .addLore("§7" + (isFrench ? "Rareté" : "Rarity") + ": " + def.rarityColor() + def.rarityDisplayName())
                .addLore("§7" + (isFrench ? "Clé" : "Key") + ": §f" + def.keyItem())
                .addLore(typeLabel)
                .addLore("§7" + (isFrench ? "Objets" : "Items") + ": §f" + def.lootTable().size());

        if (def.isGuaranteedType() && def.guaranteedItem() != null && !def.guaranteedItem().isEmpty()) {
            infoBuilder.addLore("§7" + (isFrench ? "Garanti" : "Guaranteed") + ": §a" + def.guaranteedItem() +
                    " (" + def.guaranteedMinCount() + "-" + def.guaranteedMaxCount() + ")");
        }
        infoBuilder.addLore("").addLore(isFrench ? "§eClic gauche avec la clé pour ouvrir" : "§eLeft-click with key to open");
        container.setItem(4, infoBuilder.enchanted().build());

        // Sort loot
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
                    ? String.format("%.2f", entry.chance()) + " " + (isFrench ? "poids" : "weight")
                    : String.format("%.1f%%", entry.chance() * 100);

            ItemStack display = ItemBuilder.of(item)
                    .name(Component.literal(eColor + eName))
                    .addLore("§7" + (isFrench ? "Chance" : "Chance") + ": §e" + chanceStr)
                    .addLore("§7" + (isFrench ? "Quantité" : "Qty") + ": §f" + entry.minCount() + "-" + entry.maxCount())
                    .addLore("§7" + (isFrench ? "Rareté" : "Rarity") + ": " + eColor + capitalize(eRarity))
                    .build();

            if ("legendary".equals(eRarity) || "mythic".equals(eRarity) || "epic".equals(eRarity)) {
                display = ItemBuilder.of(display).enchanted().build();
            }
            container.setItem(slots[idx++], display);
        }
        return container;
    }

    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
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
}
