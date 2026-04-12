package com.arcadia.lootbox.network;

import com.arcadia.lootbox.ArcadiaLootbox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: sync the lootbox list for hub display.
 *
 * @author vyrriox
 */
public record S2CSyncLootboxList(List<LootboxEntry> entries) implements CustomPacketPayload {

    public record LootboxEntry(String id, String displayName, String rarity, String keyItem, int lootCount, String type) {}

    public static final Type<S2CSyncLootboxList> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ArcadiaLootbox.MODID, "sync_lootbox_list"));

    public static final StreamCodec<FriendlyByteBuf, S2CSyncLootboxList> STREAM_CODEC =
            StreamCodec.of(S2CSyncLootboxList::encode, S2CSyncLootboxList::decode);

    private static void encode(FriendlyByteBuf buf, S2CSyncLootboxList pkt) {
        buf.writeVarInt(pkt.entries.size());
        for (LootboxEntry e : pkt.entries) {
            buf.writeUtf(e.id); buf.writeUtf(e.displayName); buf.writeUtf(e.rarity);
            buf.writeUtf(e.keyItem); buf.writeVarInt(e.lootCount); buf.writeUtf(e.type);
        }
    }

    private static S2CSyncLootboxList decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<LootboxEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new LootboxEntry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readUtf()));
        }
        return new S2CSyncLootboxList(list);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    @OnlyIn(Dist.CLIENT)
    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.arcadia.lootbox.client.LootboxClientData.setLootboxList(entries));
    }
}
