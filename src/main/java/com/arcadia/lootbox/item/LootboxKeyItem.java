package com.arcadia.lootbox.item;

import com.arcadia.lootbox.data.LootboxDefinition;
import com.arcadia.lootbox.manager.LootboxManager;
import com.arcadia.lootbox.network.LootboxNet;
import com.arcadia.lootbox.util.LootHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Base item class for all lootbox keys.
 * Tooltip is bilingual via translation keys.
 * <p>Right-click in air opens the matching lootbox preview:
 * <ul>
 *   <li>0 matches → no-op</li>
 *   <li>1 match → opens that lootbox preview directly</li>
 *   <li>2+ matches → opens the Hub so the player can pick</li>
 * </ul>
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

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.success(stack);
        }

        ResourceLocation thisKeyId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (thisKeyId == null) return InteractionResultHolder.pass(stack);

        List<String> matches = new ArrayList<>();
        for (var e : LootboxManager.getAllMap().entrySet()) {
            LootboxDefinition def = e.getValue();
            ResourceLocation defKey = ResourceLocation.tryParse(def.keyItem());
            if (defKey != null && defKey.equals(thisKeyId)) {
                matches.add(e.getKey());
            }
        }

        if (matches.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        if (matches.size() == 1) {
            LootHelper.openPreviewGui(sp, matches.get(0), sp.blockPosition());
            return InteractionResultHolder.success(stack);
        }
        // Multiple lootboxes share this key — open the Hub
        LootboxNet.sendOpenHub(sp);
        return InteractionResultHolder.success(stack);
    }
}
