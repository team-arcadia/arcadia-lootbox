package com.arcadia.lootbox.client;

import com.arcadia.lootbox.network.S2CSyncLootboxList;

import java.util.Collections;
import java.util.List;

/**
 * Client-side cache for lootbox data received from the server.
 *
 * @author vyrriox
 */
public final class LootboxClientData {

    private static volatile List<S2CSyncLootboxList.LootboxEntry> lootboxList = Collections.emptyList();

    private LootboxClientData() {}

    public static void setLootboxList(List<S2CSyncLootboxList.LootboxEntry> list) { lootboxList = List.copyOf(list); }
    public static List<S2CSyncLootboxList.LootboxEntry> getLootboxList() { return lootboxList; }
    public static void clear() { lootboxList = Collections.emptyList(); }
}
