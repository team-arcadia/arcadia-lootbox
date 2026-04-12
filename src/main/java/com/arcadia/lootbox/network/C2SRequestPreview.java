package com.arcadia.lootbox.network;

import com.arcadia.lootbox.ArcadiaLootbox;
import com.arcadia.lootbox.util.LootHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S: client requests to open lootbox preview from the hub.
 *
 * @author vyrriox
 */
public record C2SRequestPreview(String lootboxId) implements CustomPacketPayload {

    public static final Type<C2SRequestPreview> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcadiaLootbox.MODID, "request_preview"));

    public static final StreamCodec<FriendlyByteBuf, C2SRequestPreview> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeUtf(pkt.lootboxId),
                    buf -> new C2SRequestPreview(buf.readUtf())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) {
                LootHelper.openPreviewGui(sp, lootboxId, sp.blockPosition());
            }
        });
    }
}
