package com.arcadia.lootbox.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/**
 * Network packet registration for Arcadia Lootbox.
 *
 * @author vyrriox
 */
public final class LootboxNet {

    private LootboxNet() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(S2COpenLootboxHub.TYPE, S2COpenLootboxHub.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
        registrar.playToClient(S2CSyncLootboxList.TYPE, S2CSyncLootboxList.STREAM_CODEC, (pkt, ctx) -> pkt.handle(ctx));
    }

    public static void sendOpenHub(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new S2COpenLootboxHub());
    }

    public static void sendLootboxList(ServerPlayer player, List<S2CSyncLootboxList.LootboxEntry> entries) {
        PacketDistributor.sendToPlayer(player, new S2CSyncLootboxList(entries));
    }
}
