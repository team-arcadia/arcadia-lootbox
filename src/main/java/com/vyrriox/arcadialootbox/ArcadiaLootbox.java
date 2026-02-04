package com.vyrriox.arcadialootbox;

import com.vyrriox.arcadialootbox.command.LootCommands;
import com.vyrriox.arcadialootbox.manager.LootboxManager;
import com.vyrriox.arcadialootbox.util.LootHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Mod Class.
 * 
 * @author vyrriox
 */
@Mod(ArcadiaLootbox.MODID)
public class ArcadiaLootbox {
    public static final String MODID = "arcadialootbox";
    private static final Logger LOGGER = LoggerFactory.getLogger(ArcadiaLootbox.class);
    
    public static ArcadiaLootbox instance;

    public ArcadiaLootbox(IEventBus modEventBus) {
        instance = this;
        modEventBus.addListener(this::commonSetup);
        NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LootboxManager.init();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LootCommands.register(event.getDispatcher());
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
                 }
             }
        }
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be != null && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ShulkerBoxBlock) {
            String id = null;
            if (be.getPersistentData().contains("ArcadiaLoot")) {
                id = be.getPersistentData().getString("ArcadiaLoot");
            }
            
            if (id != null) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                LootHelper.openPreviewGui((ServerPlayer) event.getEntity(), id, event.getPos());
            }
        }
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be != null && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ShulkerBoxBlock) {
             if (be.getPersistentData().contains("ArcadiaLoot")) {
                String id = be.getPersistentData().getString("ArcadiaLoot");
                ServerPlayer player = (ServerPlayer) event.getEntity();
                
                // Attempt to open
                if (LootHelper.handleLootboxAttempt(event.getLevel(), event.getPos(), player, id)) {
                    // If opened successfully (or attempt made), cancel the break
                    event.setCanceled(true);
                }
             }
        }
    }
}
