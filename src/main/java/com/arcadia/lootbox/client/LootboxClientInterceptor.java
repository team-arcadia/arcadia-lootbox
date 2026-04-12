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
 * ArcadiaTheme steampunk screens when the title matches a lootbox preview.
 * Same pattern as Admin Panel's AdminPanelClient.
 *
 * @author vyrriox
 */
@EventBusSubscriber(modid = ArcadiaLootbox.MODID, value = Dist.CLIENT)
public final class LootboxClientInterceptor {

    private LootboxClientInterceptor() {}

    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof AbstractContainerScreen<?> cs)) return;
        if (!(cs.getMenu() instanceof ChestMenu chestMenu)) return;

        String title = cs.getTitle().getString();
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Match lootbox preview titles (they contain rarity color codes + lootbox name + rarity in parentheses)
        // The title format is: "<rarityColor><bold><name> <reset><gray>(<rarityColor><rarityName><gray>)"
        // We detect it by checking if the raw string contains known rarity markers
        if (isLootboxPreviewTitle(title)) {
            event.setNewScreen(new LootboxPreviewScreen(chestMenu, mc.player.getInventory(), cs.getTitle()));
        }
    }

    private static boolean isLootboxPreviewTitle(String title) {
        // Lootbox preview titles always contain a rarity in parentheses
        // e.g. "Treasure Chest (Rare)" or "Lucky Box (Uncommon)"
        return (title.contains("(Common)") || title.contains("(Commune)")
                || title.contains("(Uncommon)") || title.contains("(Peu commune)")
                || title.contains("(Rare)")
                || title.contains("(Epic)") || title.contains("(Epique)")
                || title.contains("(Legendary)") || title.contains("(Legendaire)")
                || title.contains("(Mythic)") || title.contains("(Mythique)"));
    }
}
