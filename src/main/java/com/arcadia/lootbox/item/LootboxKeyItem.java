package com.arcadia.lootbox.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base item class for all lootbox keys.
 * Tooltip is bilingual via translation keys.
 *
 * @author vyrriox
 */
public class LootboxKeyItem extends Item {

    public LootboxKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.arcadialootbox.key.use"));
        tooltip.add(Component.translatable("tooltip.arcadialootbox.key.mod"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getRarity() == net.minecraft.world.item.Rarity.EPIC;
    }
}
