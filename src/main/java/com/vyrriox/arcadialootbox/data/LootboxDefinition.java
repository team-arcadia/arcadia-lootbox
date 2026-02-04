package com.vyrriox.arcadialootbox.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.item.DyeColor;
import java.util.List;

/**
 * Defines the structure of a Lootbox Config.
 * 
 * @author vyrriox
 */
public record LootboxDefinition(
        String displayName,
        String color, // Matches DyeColor names or "random"
        String keyItem, // ResourceLocation string of the key item
        String openSound, // Sound event ID
        String openMessage,
        List<LootEntry> lootTable,
        List<String> particles) {
    public record LootEntry(
            String item,
            int minCount,
            int maxCount,
            double chance // 0.0 to 1.0
    ) {
    }
}
