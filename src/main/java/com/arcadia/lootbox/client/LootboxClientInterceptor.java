package com.arcadia.lootbox.client;

import com.arcadia.lootbox.ArcadiaLootbox;
import com.arcadia.lootbox.client.screen.LootboxPreviewScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Client-side screen interceptor — replaces vanilla chest screens with
 * ArcadiaTheme steampunk screens when the title contains the lootbox marker.
 *
 * @author vyrriox
 */
@EventBusSubscriber(modid = ArcadiaLootbox.MODID, value = Dist.CLIENT)
public final class LootboxClientInterceptor {

    // Must match LootHelper.PREVIEW_TITLE_MARKER — the gear symbol ⚙
    private static final String MARKER = "\u2699";

    private LootboxClientInterceptor() {}

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof AbstractContainerScreen<?> cs)) return;
        if (!(cs.getMenu() instanceof ChestMenu chestMenu)) return;

        String title = cs.getTitle().getString();
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Detect lootbox preview by the gear marker in the title
        if (title.contains(MARKER)) {
            event.setNewScreen(new LootboxPreviewScreen(chestMenu, mc.player.getInventory(), cs.getTitle()));
        }
    }
}
