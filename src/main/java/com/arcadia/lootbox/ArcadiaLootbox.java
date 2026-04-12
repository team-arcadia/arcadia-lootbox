package com.arcadia.lootbox;

import com.arcadia.lib.ArcadiaModRegistry;
import com.arcadia.lib.player.CooldownManager;
import com.arcadia.lootbox.command.LootboxCommands;
import com.arcadia.lootbox.config.LootboxConfig;
import com.arcadia.lootbox.item.KeyRegistry;
import com.arcadia.lootbox.manager.FreeLootboxManager;
import com.arcadia.lootbox.manager.HistoryManager;
import com.arcadia.lootbox.manager.LootboxManager;
import com.arcadia.lootbox.manager.UsageTracker;
import com.arcadia.lootbox.util.PermissionHelper;
import com.arcadia.lootbox.network.LootboxNet;
import com.arcadia.lootbox.network.S2CSyncLootboxList;
import com.arcadia.lootbox.util.LootHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Mod Class for Arcadia Lootbox v1.2.0.
 * Integrates with arcadia-lib for hub, messaging, cooldowns, and scheduling.
 *
 * @author vyrriox
 */
@Mod(ArcadiaLootbox.MODID)
public class ArcadiaLootbox {

    public static final String MODID = "arcadialootbox";
    private static final Logger LOGGER = LoggerFactory.getLogger("ArcadiaLootbox");
    public static ArcadiaLootbox instance;

    public ArcadiaLootbox(IEventBus modEventBus, ModContainer container) {
        instance = this;

        // Register items (keys) and creative tab
        KeyRegistry.ITEMS.register(modEventBus);
        KeyRegistry.CREATIVE_TABS.register(modEventBus);

        // Register TOML config
        container.registerConfig(ModConfig.Type.SERVER, LootboxConfig.SPEC, "arcadia/lootbox.toml");

        // Mod bus listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(LootboxNet::registerPayloads);

        // Forge bus listeners
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Detect LuckPerms (caches result)
            PermissionHelper.isLuckPermsLoaded();

            // Register hub opener action (server-side)
            ArcadiaModRegistry.registerServerAction("lootbox.hub", player -> {
                syncLootboxList(player);
                LootboxNet.sendOpenHub(player);
            });

            LOGGER.info("[ArcadiaLootbox] v1.2.0 registered. LuckPerms: {}", PermissionHelper.isLuckPermsLoaded());
        });
    }

    @SubscribeEvent
    public void onServerStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
        // Init here — SERVER config is now loaded
        LootboxManager.init();
        FreeLootboxManager.load();

        // Auto-save free claims periodically (default 5 min if config not yet available)
        int minutes = 5;
        try { minutes = LootboxConfig.FREE_AUTOSAVE_MINUTES.get(); } catch (Exception ignored) {}
        com.arcadia.lib.scheduler.SchedulerService.repeating(minutes * 20 * 60, FreeLootboxManager::autoSave);

        LOGGER.info("[ArcadiaLootbox] v1.2.0 initialized — {} lootboxes loaded, {} keys registered",
                LootboxManager.count(), KeyRegistry.getKeyCount());
    }

    // --- Event Handlers ---

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LootboxCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) return;

        BlockState state = event.getPlacedBlock();
        if (state.getBlock() instanceof ShulkerBoxBlock) {
            String id = LootHelper.getLootboxIdFromStack(player.getMainHandItem());
            if (id == null) {
                id = LootHelper.getLootboxIdFromStack(player.getOffhandItem());
            }
            if (id != null) {
                BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
                if (be != null) {
                    be.getPersistentData().putString("ArcadiaLoot", id);
                    be.setChanged();
                }
            }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be != null && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ShulkerBoxBlock) {
            if (be.getPersistentData().contains("ArcadiaLoot")) {
                String id = be.getPersistentData().getString("ArcadiaLoot");
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                LootHelper.openPreviewGui((ServerPlayer) event.getEntity(), id, event.getPos());
            }
        }
    }

    // Left-click removed — opening now happens via the "Draw!" button in the preview GUI.
    // Right-click opens preview -> player clicks "Draw!" button -> LootHelper.handleLootboxAttempt()

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncLootboxList(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CooldownManager.clearPlayer(player.getUUID());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        FreeLootboxManager.clearAll();
        HistoryManager.clearAll();
        UsageTracker.clearCache();
        LOGGER.info("[ArcadiaLootbox] Saved free claims and cleaned up on server stop.");
    }

    // --- Utility ---

    public static void syncLootboxList(ServerPlayer player) {
        List<S2CSyncLootboxList.LootboxEntry> entries = new ArrayList<>();
        for (var entry : LootboxManager.getAllMap().entrySet()) {
            var def = entry.getValue();
            entries.add(new S2CSyncLootboxList.LootboxEntry(
                    entry.getKey(),
                    def.displayName(),
                    def.rarity() != null ? def.rarity() : "common",
                    def.keyItem(),
                    def.lootTable().size(),
                    def.type() != null ? def.type() : "weighted"
            ));
        }
        LootboxNet.sendLootboxList(player, entries);
    }
}
