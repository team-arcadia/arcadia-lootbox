package com.vyrriox.arcadialootbox.util;

import com.vyrriox.arcadialootbox.data.LootboxDefinition;
import com.vyrriox.arcadialootbox.manager.LootboxManager;
import com.vyrriox.arcadialootbox.menu.PreviewMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.Random;

/**
 * Helper class for Lootbox operations.
 * 
 * @author vyrriox
 */
public class LootHelper {

    public static String getLootboxIdFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        net.minecraft.world.item.component.CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("LootboxID")) {
                return tag.getString("LootboxID");
            }
        }
        return null;
    }

    public static void openPreviewGui(ServerPlayer player, String id, BlockPos pos) {
        LootboxDefinition def = LootboxManager.get(id);
        if (def == null) return;
        
        // Detect Language
        String lang = player.clientInformation() != null ? player.clientInformation().language() : "en_us";
        boolean isFrench = lang.startsWith("fr");
        
        // Localized Title
        Component title = isFrench 
                ? Component.literal("Contenu de la Lootbox: " + def.displayName()) 
                : Component.literal("Lootbox Content: " + def.displayName());

        player.openMenu(new SimpleMenuProvider((winId, inv, p) -> 
            new PreviewMenu(winId, inv, id, pos, def, lang), 
            title
        ));
    }

    public static boolean handleLootboxAttempt(net.minecraft.world.level.Level level, BlockPos pos, ServerPlayer player, String id) {
        if (id == null || id.isEmpty()) return false;

        LootboxDefinition def = LootboxManager.get(id);
        if (def == null) return false; 

        // Check Key
        ItemStack stack = player.getMainHandItem();
        ResourceLocation keyRes = ResourceLocation.tryParse(def.keyItem());
        
        if (keyRes != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(keyRes)) {
             // Correct Key
             if (player.getCooldowns().isOnCooldown(stack.getItem())) return true;
             
             // Open it
             player.getCooldowns().addCooldown(stack.getItem(), 20);
             openLootboxLogic((ServerLevel) level, pos, player, def, stack);
             return true; // Cancel break
        }
        
        return false; 
    }

    private static void openLootboxLogic(ServerLevel level, BlockPos pos, ServerPlayer player, LootboxDefinition def, ItemStack keyStack) {
         // Verify existence
         if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock)) {
             return;
         }
         
         Random random = new Random();

        // Sound Logic
        ResourceLocation soundRes = ResourceLocation.tryParse(def.openSound());
        net.minecraft.sounds.SoundEvent sound = SoundEvents.SHULKER_BOX_OPEN;
        if (soundRes != null) {
             net.minecraft.sounds.SoundEvent s = BuiltInRegistries.SOUND_EVENT.get(soundRes);
             if (s != null) sound = s;
        }
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);

        // Drops -> Inventory
        if (def.lootTable() != null) {
            java.util.Map<net.minecraft.world.item.Item, Integer> drops = new java.util.HashMap<>();
            
            for (LootboxDefinition.LootEntry entry : def.lootTable()) {
                if (random.nextDouble() <= entry.chance()) {
                    ResourceLocation itemRes = ResourceLocation.tryParse(entry.item());
                    if (itemRes != null) {
                        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(itemRes);
                        int count = random.nextInt(entry.maxCount() - entry.minCount() + 1) + entry.minCount();
                        if (count > 0) {
                            drops.put(item, drops.getOrDefault(item, 0) + count);
                        }
                    }
                }
            }
            
            // Give to player
            for (java.util.Map.Entry<net.minecraft.world.item.Item, Integer> entry : drops.entrySet()) {
                int total = entry.getValue();
                net.minecraft.world.item.Item item = entry.getKey();
                int maxStack = item.getDefaultInstance().getMaxStackSize();
                
                while (total > 0) {
                    int split = Math.min(total, maxStack);
                    ItemStack dropStack = new ItemStack(item, split);
                    
                    if (!player.getInventory().add(dropStack)) {
                        player.drop(dropStack, false); // Drop if full
                    }
                    total -= split;
                }
            }
        }

        // Particles
        if (def.particles() != null) {
           for (String pStr : def.particles()) {
               ResourceLocation pRes = ResourceLocation.tryParse(pStr);
               if (pRes != null) {
                   net.minecraft.core.particles.ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(pRes);
                   if (type instanceof net.minecraft.core.particles.SimpleParticleType simple) {
                       int count = 15; 
                       for (int i = 0; i < count; ++i) {
                           level.sendParticles(simple,
                                   pos.getX() + 0.5D + (random.nextGaussian() * 0.5D),
                                   pos.getY() + 0.5D + (random.nextGaussian() * 0.5D),
                                   pos.getZ() + 0.5D + (random.nextGaussian() * 0.5D),
                                   1, 0, 0, 0, 0.1);
                       }
                   }
               }
           }
        }
        
        String msg = def.openMessage();
        if (msg == null || msg.isEmpty()) msg = "§aLootbox ouverte !";
        player.displayClientMessage(Component.literal(msg), true);
        
        if (!player.isCreative()) {
            keyStack.shrink(1);
        }
    }
}
