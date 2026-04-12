package com.arcadia.lootbox.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base item class for all lootbox keys.
 * Adds tooltip info and enchantment glint for epic+ rarities.
 *
 * @author vyrriox
 */
public class LootboxKeyItem extends Item {

    public LootboxKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Use on a Lootbox to open it"));
        tooltip.add(Component.literal("§8Arcadia Lootbox Key"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Enchantment glint for epic+ rarity items
        return stack.getRarity() == net.minecraft.world.item.Rarity.EPIC;
    }
}
