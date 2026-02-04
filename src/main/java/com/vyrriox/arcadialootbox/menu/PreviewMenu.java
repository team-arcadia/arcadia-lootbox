package com.vyrriox.arcadialootbox.menu;

import com.vyrriox.arcadialootbox.data.LootboxDefinition;
import net.minecraft.ChatFormatting;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Server-Side Preview GUI for Lootboxes.
 * 
 * @author vyrriox
 */
public class PreviewMenu extends ChestMenu {
    private final String lootboxId;
    private final BlockPos targetPos;

    // Constructor now accepts language
    public PreviewMenu(int containerId, Inventory playerInv, String id, BlockPos pos, LootboxDefinition def, String language) {
        super(MenuType.GENERIC_9x6, containerId, playerInv, createContainer(def, language), 6);
        this.lootboxId = id;
        this.targetPos = pos;
    }

    private static SimpleContainer createContainer(LootboxDefinition def, String language) {
        SimpleContainer container = new SimpleContainer(54);
        boolean isFrench = language != null && language.startsWith("fr");
        
        // Fill items
        if (def.lootTable() != null) {
            int slot = 0;
            for (LootboxDefinition.LootEntry entry : def.lootTable()) {
                ResourceLocation res = ResourceLocation.tryParse(entry.item());
                if (res != null) {
                     ItemStack displayStack = new ItemStack(BuiltInRegistries.ITEM.get(res));
                     List<Component> lore = new ArrayList<>();
                     
                     // Localized Lore
                     if (isFrench) {
                         lore.add(Component.literal(String.format("§7Probabilité: §e%.1f%%", entry.chance() * 100)).withStyle(ChatFormatting.GRAY));
                         lore.add(Component.literal("§7Quantité: " + entry.minCount() + "-" + entry.maxCount()).withStyle(ChatFormatting.GRAY));
                     } else {
                         lore.add(Component.literal(String.format("§7Chance: §e%.1f%%", entry.chance() * 100)).withStyle(ChatFormatting.GRAY));
                         lore.add(Component.literal("§7Quantity: " + entry.minCount() + "-" + entry.maxCount()).withStyle(ChatFormatting.GRAY));
                     }
                     
                     displayStack.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(lore));
                     
                     if (slot < 54) container.setItem(slot++, displayStack);
                }
            }
        }
        return container;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Cancel any click in the chest part (0-53)
        if (slotId >= 0 && slotId < 54) {
             return;
        }
        // Block shift-clicks into chest
        if (clickType == ClickType.QUICK_MOVE) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; 
    }
    
    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5) <= 64.0;
    }
}
