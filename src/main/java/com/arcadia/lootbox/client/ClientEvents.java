package com.arcadia.lootbox.client;

import com.arcadia.lib.ArcadiaModRegistry;
import com.arcadia.lib.client.ArcadiaModCard;
import com.arcadia.lootbox.ArcadiaLootbox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side: hub card registration and screen setup.
 *
 * @author vyrriox
 */
@EventBusSubscriber(modid = ArcadiaLootbox.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientEvents {

    private ClientEvents() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Always register — config is SERVER type, not available at client setup
            ArcadiaModRegistry.registerCard(new ArcadiaModCard(
                    "lootbox", "\uD83C\uDF81",
                    "arcadialootbox.hub.title", "arcadialootbox.hub.subtitle",
                    0xFFAA00, 2, 1, true
            ));
            ArcadiaModRegistry.registerCardClickHandler("lootbox", LootboxHubScreen::open);
        });
    }
}
