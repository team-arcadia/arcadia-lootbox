package com.arcadia.lootbox.command;

import com.arcadia.lib.ArcadiaMessages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.arcadia.lootbox.ArcadiaLootbox;
import com.arcadia.lootbox.config.LootboxConfig;
import com.arcadia.lootbox.data.LootboxDefinition;
import com.arcadia.lootbox.item.KeyRegistry;
import com.arcadia.lootbox.manager.FreeLootboxManager;
import com.arcadia.lootbox.manager.HistoryManager;
import com.arcadia.lootbox.manager.LootboxManager;
import com.arcadia.lootbox.manager.UsageTracker;
import com.arcadia.lootbox.util.LootHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * Complete command tree for /arcadia_lootbox.
 *
 * @author vyrriox
 */
public final class LootboxCommands {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_IDS =
            (ctx, b) -> SharedSuggestionProvider.suggest(LootboxManager.getAllIds(), b);
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_KEYS =
            (ctx, b) -> SharedSuggestionProvider.suggest(KeyRegistry.getAllKeyIds(), b);

    private LootboxCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("arcadia_lootbox")
                .requires(s -> s.hasPermission(2))

                .then(Commands.literal("reload").executes(LootboxCommands::cmdReload))
                .then(Commands.literal("list").executes(LootboxCommands::cmdList))
                .then(Commands.literal("listkeys").executes(LootboxCommands::cmdListKeys))
                .then(Commands.literal("stats").executes(LootboxCommands::cmdStats))
                .then(Commands.literal("hub").executes(LootboxCommands::cmdHub))

                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                .executes(LootboxCommands::cmdInfo)))

                .then(Commands.literal("give")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("lootbox_id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                        .executes(ctx -> cmdGive(ctx, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> cmdGive(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))

                .then(Commands.literal("giveall")
                        .then(Commands.argument("lootbox_id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                .executes(ctx -> cmdGiveAll(ctx, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> cmdGiveAll(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))

                .then(Commands.literal("givekey")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("key_id", StringArgumentType.string()).suggests(SUGGEST_KEYS)
                                        .executes(ctx -> cmdGiveKey(ctx, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> cmdGiveKey(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))

                .then(Commands.literal("preview")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("lootbox_id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                        .executes(LootboxCommands::cmdPreview))))

                .then(Commands.literal("history")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(LootboxCommands::cmdHistory)))

                .then(Commands.literal("clearhistory")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(LootboxCommands::cmdClearHistory)))

                .then(Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .then(Commands.argument("displayName", StringArgumentType.greedyString())
                                        .executes(LootboxCommands::cmdCreate))))

                .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                .executes(LootboxCommands::cmdDelete)))

                .then(Commands.literal("setuses")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("uses", IntegerArgumentType.integer(0))
                                        .executes(LootboxCommands::cmdSetUses))))

                .then(Commands.literal("resetcooldown")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(LootboxCommands::cmdResetCooldown)))

                // Free lootbox commands
                .then(Commands.literal("free")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("lootbox_id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                        .executes(LootboxCommands::cmdFree))))

                .then(Commands.literal("freetimer")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("lootbox_id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                        .executes(LootboxCommands::cmdFreeTimer))))

                .then(Commands.literal("resetfree")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> cmdResetFree(ctx, false))
                                .then(Commands.argument("lootbox_id", StringArgumentType.string()).suggests(SUGGEST_IDS)
                                        .executes(ctx -> cmdResetFree(ctx, true)))))
        );
    }

    private static int cmdReload(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        if (LootboxConfig.ASYNC_CONFIG_RELOAD.get()) {
            src.sendSuccess(() -> ArcadiaMessages.info("Reloading..."), true);
            LootboxManager.reloadAsync().thenAccept(c ->
                    src.sendSuccess(() -> ArcadiaMessages.success("Reloaded " + c + " lootbox definitions."), true));
        } else {
            int c = LootboxManager.reload();
            src.sendSuccess(() -> ArcadiaMessages.success("Reloaded " + c + " lootbox definitions."), true);
        }
        return 1;
    }

    private static int cmdList(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        var ids = LootboxManager.getAllIds();
        if (ids.isEmpty()) { src.sendSuccess(() -> ArcadiaMessages.warning("No lootboxes found."), false); return 0; }
        src.sendSuccess(() -> ArcadiaMessages.info("Lootboxes (" + ids.size() + "):"), false);
        for (String id : ids) {
            LootboxDefinition def = LootboxManager.get(id);
            src.sendSuccess(() -> Component.literal("  §7- §e" + id + " §7| " + def.rarityColor() + def.rarityDisplayName() +
                    " §7| §f" + def.displayName() + " §7[" + def.type() + "] (" + def.lootTable().size() + " items)"), false);
        }
        return 1;
    }

    private static int cmdListKeys(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSuccess(() -> ArcadiaMessages.info("Registered Keys (" + KeyRegistry.getKeyCount() + "):"), false);
        for (String keyId : KeyRegistry.getAllKeyIds()) {
            src.sendSuccess(() -> Component.literal("  §7- §earcadialootbox:" + keyId), false);
        }
        return 1;
    }

    private static int cmdInfo(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        var src = ctx.getSource();
        if (!LootboxManager.exists(id)) { src.sendFailure(ArcadiaMessages.error("Not found: " + id)); return 0; }
        LootboxDefinition def = LootboxManager.get(id);
        src.sendSuccess(() -> ArcadiaMessages.info("§6=== " + id + " ==="), false);
        src.sendSuccess(() -> Component.literal("  §7Name: §f" + def.displayName()), false);
        src.sendSuccess(() -> Component.literal("  §7Type: §f" + def.type()), false);
        src.sendSuccess(() -> Component.literal("  §7Rarity: " + def.rarityColor() + def.rarityDisplayName()), false);
        src.sendSuccess(() -> Component.literal("  §7Key: §f" + def.keyItem()), false);
        src.sendSuccess(() -> Component.literal("  §7Cooldown: §f" + def.cooldownTicks() + " ticks"), false);
        src.sendSuccess(() -> Component.literal("  §7Max Uses: §f" + (def.maxUses() > 0 ? def.maxUses() : "∞")), false);
        src.sendSuccess(() -> Component.literal("  §7Destroy on Open: §f" + def.destroyOnOpen()), false);
        src.sendSuccess(() -> Component.literal("  §7Broadcast: §f" + def.broadcastRare()), false);
        src.sendSuccess(() -> Component.literal("  §7Loot Entries: §f" + def.lootTable().size()), false);
        if (def.isGuaranteedType()) {
            src.sendSuccess(() -> Component.literal("  §7Guaranteed: §f" + def.guaranteedItem() +
                    " (" + def.guaranteedMinCount() + "-" + def.guaranteedMaxCount() + ")"), false);
        }
        for (LootboxDefinition.LootEntry e : def.lootTable()) {
            String label = def.isGuaranteedType() ? "weight" : "chance";
            src.sendSuccess(() -> Component.literal("    §8- §f" + e.item() + " §7[" + e.minCount() + "-" + e.maxCount() +
                    "] §e" + (def.isGuaranteedType() ? String.format("%.2f", e.chance()) : String.format("%.1f%%", e.chance() * 100)) +
                    " §7(" + label + ")"), false);
        }
        return 1;
    }

    private static int cmdGive(CommandContext<CommandSourceStack> ctx, int amount) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            String id = StringArgumentType.getString(ctx, "lootbox_id");
            if (!LootboxManager.exists(id)) { ctx.getSource().sendFailure(ArcadiaMessages.error("Not found: " + id)); return 0; }
            for (int i = 0; i < amount; i++) LootHelper.giveLootboxItem(player, id);
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Gave " + amount + "x '" + id + "' to " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdGiveAll(CommandContext<CommandSourceStack> ctx, int amount) {
        String id = StringArgumentType.getString(ctx, "lootbox_id");
        if (!LootboxManager.exists(id)) { ctx.getSource().sendFailure(ArcadiaMessages.error("Not found: " + id)); return 0; }
        var players = ctx.getSource().getServer().getPlayerList().getPlayers();
        for (ServerPlayer p : players) for (int i = 0; i < amount; i++) LootHelper.giveLootboxItem(p, id);
        ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Gave " + amount + "x '" + id + "' to " + players.size() + " players"), true);
        return 1;
    }

    private static int cmdGiveKey(CommandContext<CommandSourceStack> ctx, int amount) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            String keyId = StringArgumentType.getString(ctx, "key_id");
            // Add namespace if missing
            String fullId = keyId.contains(":") ? keyId : ArcadiaLootbox.MODID + ":" + keyId;
            LootHelper.giveKeyItem(player, fullId, amount);
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Gave " + amount + "x " + keyId + " to " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdPreview(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            String id = StringArgumentType.getString(ctx, "lootbox_id");
            if (!LootboxManager.exists(id)) { ctx.getSource().sendFailure(ArcadiaMessages.error("Not found: " + id)); return 0; }
            LootHelper.openPreviewGui(player, id, player.blockPosition());
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdHistory(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            var history = HistoryManager.getHistory(player.getUUID());
            if (history.isEmpty()) { ctx.getSource().sendSuccess(() -> ArcadiaMessages.info("No history."), false); return 0; }
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.info("History (" + history.size() + "):"), false);
            for (int i = 0; i < Math.min(history.size(), 10); i++) {
                HistoryManager.HistoryEntry e = history.get(i);
                ctx.getSource().sendSuccess(() -> Component.literal("  §7[" + e.formattedTime() + "] §e" + e.lootboxName() + " §7→ §f" + String.join(", ", e.itemsReceived())), false);
            }
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdClearHistory(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            HistoryManager.clearHistory(player.getUUID());
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Cleared history for " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdCreate(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        String name = StringArgumentType.getString(ctx, "displayName");
        if (LootboxManager.exists(id)) { ctx.getSource().sendFailure(ArcadiaMessages.error("Already exists: " + id)); return 0; }
        LootboxDefinition def = new LootboxDefinition(name, "white", "minecraft:tripwire_hook", "minecraft:block.chest.open", "",
                List.of(), List.of("minecraft:flame"), "weighted", "", 1, 1, "common", false, "", false, -1, "",
                LootboxDefinition.AnimationConfig.defaults(), false, 20, "", false, "", "", List.of(), 0, "", 0, true,
                "", "", "", "",
                false, 72, "", 48, "");
        if (LootboxManager.createDefinition(id, def)) {
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Created '" + id + "'. Edit the JSON file to add loot."), true);
            return 1;
        }
        return 0;
    }

    private static int cmdDelete(CommandContext<CommandSourceStack> ctx) {
        String id = StringArgumentType.getString(ctx, "id");
        if (LootboxManager.deleteDefinition(id)) {
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Deleted: " + id), true);
            return 1;
        }
        ctx.getSource().sendFailure(ArcadiaMessages.error("Not found: " + id));
        return 0;
    }

    private static int cmdSetUses(CommandContext<CommandSourceStack> ctx) {
        try {
            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
            int uses = IntegerArgumentType.getInteger(ctx, "uses");
            BlockEntity be = ctx.getSource().getLevel().getBlockEntity(pos);
            if (be == null || !be.getPersistentData().contains("ArcadiaLoot")) {
                ctx.getSource().sendFailure(ArcadiaMessages.error("No lootbox at that position.")); return 0;
            }
            UsageTracker.setUsageCount(be, uses);
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Set uses to " + uses + " at " + pos.toShortString()), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdResetCooldown(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            com.arcadia.lib.player.CooldownManager.clearPlayer(player.getUUID());
            ctx.getSource().sendSuccess(() -> ArcadiaMessages.success("Reset cooldowns for " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdStats(CommandContext<CommandSourceStack> ctx) {
        var src = ctx.getSource();
        src.sendSuccess(() -> ArcadiaMessages.info("§6=== Arcadia Lootbox Stats ==="), false);
        src.sendSuccess(() -> Component.literal("  §7Definitions: §f" + LootboxManager.count()), false);
        src.sendSuccess(() -> Component.literal("  §7Keys: §f" + KeyRegistry.getKeyCount()), false);
        src.sendSuccess(() -> Component.literal("  §7Hub: §f" + LootboxConfig.HUB_ENABLED.get()), false);
        src.sendSuccess(() -> Component.literal("  §7Broadcast: §f" + LootboxConfig.BROADCAST_ENABLED.get()), false);
        src.sendSuccess(() -> Component.literal("  §7Shop URL: §f" + LootboxConfig.SHOP_URL.get()), false);
        return 1;
    }

    private static int cmdHub(CommandContext<CommandSourceStack> ctx) {
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            com.arcadia.lib.network.ArcadiaLibNet.sendOpenHub(player);
            return 1;
        }
        ctx.getSource().sendFailure(ArcadiaMessages.error("Must be run by a player."));
        return 0;
    }

    // --- Free lootbox commands ---

    private static int cmdFree(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            String id = StringArgumentType.getString(ctx, "lootbox_id");
            var src = ctx.getSource();

            if (!LootboxManager.exists(id)) { src.sendFailure(ArcadiaMessages.error("Not found: " + id)); return 0; }
            LootboxDefinition def = LootboxManager.get(id);

            if (!def.freeEnabled()) {
                src.sendFailure(ArcadiaMessages.error("Free claiming is not enabled for '" + id + "'."));
                return 0;
            }

            // Atomic claim — prevents double-claim race condition
            if (!FreeLootboxManager.tryAtomicClaim(player, id, def)) {
                String remaining = FreeLootboxManager.getRemainingFormatted(player, id, def);
                src.sendSuccess(() -> ArcadiaMessages.warning(
                        player.getName().getString() + " cannot claim '" + id + "' yet. " + remaining), false);
                return 0;
            }

            // Claim succeeded — give the lootbox
            LootHelper.giveLootboxItem(player, id);

            src.sendSuccess(() -> ArcadiaMessages.success(
                    "Free lootbox '" + id + "' claimed by " + player.getName().getString()), true);
            boolean fr = com.arcadia.lootbox.util.LanguageHelper.isFrench(player);
            String name = (fr && !def.displayNameFR().isEmpty()) ? def.displayNameFR() : def.displayName();
            player.sendSystemMessage(ArcadiaMessages.success(
                    fr ? "Vous avez réclamé un(e) " + name + " gratuit(e) !"
                       : "You claimed a free " + name + "!"));
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdFreeTimer(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            String id = StringArgumentType.getString(ctx, "lootbox_id");
            var src = ctx.getSource();

            if (!LootboxManager.exists(id)) { src.sendFailure(ArcadiaMessages.error("Not found: " + id)); return 0; }
            LootboxDefinition def = LootboxManager.get(id);

            if (!def.freeEnabled()) {
                src.sendSuccess(() -> ArcadiaMessages.info("Free claiming is disabled for '" + id + "'."), false);
                return 0;
            }

            String remaining = FreeLootboxManager.getRemainingFormatted(player, id, def);
            boolean canClaim = FreeLootboxManager.canClaim(player, id, def);

            src.sendSuccess(() -> ArcadiaMessages.info("Free lootbox '" + id + "' for " + player.getName().getString() + ":"), false);
            if (canClaim) {
                src.sendSuccess(() -> Component.literal("  §aReady to claim!"), false);
            } else {
                src.sendSuccess(() -> Component.literal("  §7Remaining: §e" + remaining), false);
            }
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }

    private static int cmdResetFree(CommandContext<CommandSourceStack> ctx, boolean specificLootbox) {
        try {
            ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
            var src = ctx.getSource();

            if (specificLootbox) {
                String id = StringArgumentType.getString(ctx, "lootbox_id");
                FreeLootboxManager.resetClaim(player.getUUID(), id);
                src.sendSuccess(() -> ArcadiaMessages.success("Reset free timer for '" + id + "' for " + player.getName().getString()), true);
            } else {
                FreeLootboxManager.resetAllClaims(player.getUUID());
                src.sendSuccess(() -> ArcadiaMessages.success("Reset ALL free timers for " + player.getName().getString()), true);
            }
            return 1;
        } catch (Exception e) { ctx.getSource().sendFailure(ArcadiaMessages.error(e.getMessage())); return 0; }
    }
}
