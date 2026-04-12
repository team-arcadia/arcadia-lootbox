package com.arcadia.lootbox.network;

import com.arcadia.lootbox.ArcadiaLootbox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S2C: open the lootbox hub screen.
 *
 * @author vyrriox
 */
public record S2COpenLootboxHub() implements CustomPacketPayload {

    public static final Type<S2COpenLootboxHub> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcadiaLootbox.MODID, "open_lootbox_hub"));

    public static final StreamCodec<FriendlyByteBuf, S2COpenLootboxHub> STREAM_CODEC =
            StreamCodec.of((buf, pkt) -> {}, buf -> new S2COpenLootboxHub());

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @OnlyIn(Dist.CLIENT)
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.arcadia.lootbox.client.LootboxHubScreen.open());
    }
}
