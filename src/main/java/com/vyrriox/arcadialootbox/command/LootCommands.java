package com.vyrriox.arcadialootbox.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.vyrriox.arcadialootbox.data.LootboxDefinition;
import com.vyrriox.arcadialootbox.manager.LootboxManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.Block;

/**
 * Handles all commands for the mod.
 * 
 * @author vyrriox
 */
public class LootCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arcadialoot")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload")
                        .executes(LootCommands::reload))
                .then(Commands.literal("give")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("lootbox_id", StringArgumentType.string())
                                        .executes(LootCommands::giveLootbox)))));
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        LootboxManager.reload();
        context.getSource().sendSuccess(() -> Component.literal("§aArcadia Lootbox Config Reloaded!"), true);
        return 1;
    }

    private static int giveLootbox(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(context, "target");
            String id = StringArgumentType.getString(context, "lootbox_id");

            if (!LootboxManager.exists(id)) {
                context.getSource().sendFailure(Component.literal("§cLootbox ID not found: " + id));
                return 0;
            }

            LootboxDefinition def = LootboxManager.get(id);
            String colorName = def.color();
            DyeColor dye = null;
            for (DyeColor d : DyeColor.values()) {
                if (d.getName().equalsIgnoreCase(colorName)) {
                    dye = d;
                    break;
                }
            }
            if (dye == null) dye = DyeColor.WHITE;

            Block shulkerKey = ShulkerBoxBlock.getBlockByColor(dye);
            ItemStack stack = new ItemStack(shulkerKey);
            
            CompoundTag beTag = new CompoundTag();
            beTag.putString("LootboxID", id);
            
            // USE CUSTOM_DATA to avoid vanilla logic trying to parse it as BlockEntity data
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(beTag));
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("§6Lootbox: §e" + def.displayName()));

            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }

            context.getSource().sendSuccess(
                    () -> Component.literal("§aGave Lootbox '" + id + "' to " + player.getName().getString()),
                    true);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
