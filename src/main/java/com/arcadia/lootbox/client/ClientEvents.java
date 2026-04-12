package com.arcadia.lootbox.client;

import com.arcadia.lib.ArcadiaModRegistry;
import com.arcadia.lib.client.ArcadiaModCard;
import com.arcadia.lootbox.ArcadiaLootbox;
import com.arcadia.lootbox.config.LootboxConfig;
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
            if (LootboxConfig.HUB_ENABLED.get()) {
                ArcadiaModRegistry.registerCard(new ArcadiaModCard(
                        "lootbox", "\uD83C\uDF81",
                        "arcadialootbox.hub.title", "arcadialootbox.hub.subtitle",
                        0xFFAA00, 30, true
                ));
                ArcadiaModRegistry.registerCardClickHandler("lootbox", LootboxHubScreen::open);
            }
        });
    }
}
